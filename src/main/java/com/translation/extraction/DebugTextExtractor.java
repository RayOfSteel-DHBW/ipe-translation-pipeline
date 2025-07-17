package com.translation.extraction;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class DebugTextExtractor {
    private static final Logger logger = Logger.getLogger(DebugTextExtractor.class.getName());

    private static final Set<String> SKIP_ELEMENTS = Set.of(
        "svg", "g", "path", "rect", "circle", "ellipse", "line",
        "polyline", "polygon", "image", "defs", "clipPath", "mask",
        "pattern", "marker", "gradient", "stop", "use", "symbol",
        "metadata", "style"
    );

    private static final Set<String> GRAPHICS_ATTRIBUTES = Set.of(
        "d", "points", "x1", "y1", "x2", "y2", "cx", "cy", "r"
    );

    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/=\\s]+$");
    private static final Pattern HEX_PATTERN   = Pattern.compile("^[0-9A-Fa-f\\s]+$");
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("^[\\d\\s,.-]+$");
    private static final Pattern URL_PATTERN   = Pattern.compile("^(https?://|file://|[a-zA-Z]:\\\\)");
    private static final Pattern CSS_PATTERN   = Pattern.compile("[{};:]");
    private static final Pattern XML_TAG_PATTERN   = Pattern.compile("^<[^>]+>.*</[^>]+>$");
    private static final Pattern PURE_MATH_PATTERN = Pattern.compile("^[\\d\\s\\+\\-\\*/=\\(\\)\\[\\]\\{\\}\\^\\$\\.<>]+$");
    private static final Pattern LATEX_CMD_WITH_BRACES = Pattern.compile("^\\\\[A-Za-z]+\\{[^}]+}.*$");
    private static final Pattern SINGLE_NON_LETTER = Pattern.compile("^[^a-zA-ZäöüÄÖÜß]$");
    private static final Pattern MULTI_LETTERS     = Pattern.compile(".*[A-Za-zÄÖÜäöüß]{2,}.*");

    public static class DebugResult {
        private List<TextElement> acceptedElements = new ArrayList<>();
        private List<RejectedElement> rejectedElements = new ArrayList<>();
        private List<SkippedElement> skippedElements = new ArrayList<>();
        private ExtractionResult.ExtractionStats stats = new ExtractionResult.ExtractionStats();

        // Getters and setters
        public List<TextElement> getAcceptedElements() { return acceptedElements; }
        public void setAcceptedElements(List<TextElement> acceptedElements) { this.acceptedElements = acceptedElements; }
        
        public List<RejectedElement> getRejectedElements() { return rejectedElements; }
        public void setRejectedElements(List<RejectedElement> rejectedElements) { this.rejectedElements = rejectedElements; }
        
        public List<SkippedElement> getSkippedElements() { return skippedElements; }
        public void setSkippedElements(List<SkippedElement> skippedElements) { this.skippedElements = skippedElements; }
        
        public ExtractionResult.ExtractionStats getStats() { return stats; }
        public void setStats(ExtractionResult.ExtractionStats stats) { this.stats = stats; }
    }

    public static class RejectedElement {
        private String xpath;
        private String tagName;
        private String text;
        private String textType;
        private String rejectionReason;
        private Map<String, String> attributes;

        public RejectedElement(String xpath, String tagName, String text, String textType, String rejectionReason) {
            this.xpath = xpath;
            this.tagName = tagName;
            this.text = text;
            this.textType = textType;
            this.rejectionReason = rejectionReason;
            this.attributes = new HashMap<>();
        }

        // Getters and setters
        public String getXpath() { return xpath; }
        public String getTagName() { return tagName; }
        public String getText() { return text; }
        public String getTextType() { return textType; }
        public String getRejectionReason() { return rejectionReason; }
        public Map<String, String> getAttributes() { return attributes; }
        public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
    }

    public static class SkippedElement {
        private String xpath;
        private String tagName;
        private String skipReason;
        private Map<String, String> attributes;

        public SkippedElement(String xpath, String tagName, String skipReason) {
            this.xpath = xpath;
            this.tagName = tagName;
            this.skipReason = skipReason;
            this.attributes = new HashMap<>();
        }

        // Getters and setters
        public String getXpath() { return xpath; }
        public String getTagName() { return tagName; }
        public String getSkipReason() { return skipReason; }
        public Map<String, String> getAttributes() { return attributes; }
        public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
    }

    public DebugResult extractTextWithDebug(String xmlContent) {
        DebugResult result = new DebugResult();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlContent)));

            extractTextFromNodeWithDebug(document.getDocumentElement(), "", result);

            result.stats.setTranslatableFound(result.acceptedElements.size());
            result.stats.calculateNoiseReduction();

        } catch (Exception e) {
            logger.severe("Failed to extract text from XML: " + e.getMessage());
            throw new RuntimeException("Text extraction failed", e);
        }
        return result;
    }

    private void extractTextFromNodeWithDebug(Node node, String parentXpath, DebugResult result) {
        result.stats.setTotalElementsScanned(result.stats.getTotalElementsScanned() + 1);

        if (node.getNodeType() != Node.ELEMENT_NODE) return;

        Element element = (Element) node;
        String tagName = element.getTagName().toLowerCase();
        String currentXpath = parentXpath + "/" + tagName + "[" + getElementIndex(element) + "]";

        // Check if element should be skipped
        if (SKIP_ELEMENTS.contains(tagName)) {
            SkippedElement skipped = new SkippedElement(currentXpath, tagName, "SKIP_ELEMENTS contains " + tagName);
            addElementAttributes(element, skipped.getAttributes());
            result.skippedElements.add(skipped);
            result.stats.setElementsSkipped(result.stats.getElementsSkipped() + 1);
            return;
        }

        if (hasGraphicsAttributes(element)) {
            SkippedElement skipped = new SkippedElement(currentXpath, tagName, "Has graphics attributes");
            addElementAttributes(element, skipped.getAttributes());
            result.skippedElements.add(skipped);
            result.stats.setElementsSkipped(result.stats.getElementsSkipped() + 1);
            return;
        }

        // Check element text content
        String textContent = element.getTextContent();
        if (textContent != null && !textContent.trim().isEmpty()) {
            String rejectionReason = checkTranslatableText(textContent, element);
            if (rejectionReason == null) {
                // Accepted
                TextElement te = createTextElement(
                        result.acceptedElements.size(),
                        currentXpath,
                        element,
                        textContent,
                        "element_text"
                );
                result.acceptedElements.add(te);
            } else {
                // Rejected
                RejectedElement rejected = new RejectedElement(
                        currentXpath, tagName, textContent, "element_text", rejectionReason);
                addElementAttributes(element, rejected.getAttributes());
                result.rejectedElements.add(rejected);
            }
        }

        // Check attribute text
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            String value = attr.getNodeValue();
            if (value != null && !value.trim().isEmpty()) {
                String attrName = attr.getNodeName();
                String attrXpath = currentXpath + "/@" + attrName;
                
                if (!isTranslatableAttribute(attrName)) {
                    RejectedElement rejected = new RejectedElement(
                            attrXpath, tagName, value, "attribute_" + attrName, 
                            "Attribute '" + attrName + "' is not translatable");
                    addElementAttributes(element, rejected.getAttributes());
                    result.rejectedElements.add(rejected);
                    continue;
                }

                String rejectionReason = checkTranslatableText(value, element);
                if (rejectionReason == null) {
                    // Accepted
                    TextElement te = createTextElement(
                            result.acceptedElements.size(),
                            attrXpath,
                            element,
                            value,
                            "attribute_" + attrName
                    );
                    result.acceptedElements.add(te);
                } else {
                    // Rejected
                    RejectedElement rejected = new RejectedElement(
                            attrXpath, tagName, value, "attribute_" + attrName, rejectionReason);
                    addElementAttributes(element, rejected.getAttributes());
                    result.rejectedElements.add(rejected);
                }
            }
        }

        // Recurse into children
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            extractTextFromNodeWithDebug(children.item(i), currentXpath, result);
        }
    }

    private String checkTranslatableText(String text, Element element) {
        text = text.trim();
        
        if (text.length() < 2) return "Text too short (< 2 chars): '" + text + "'";
        if (isBinaryOrEncodedContent(text)) return "Binary or encoded content detected";
        if (PURE_MATH_PATTERN.matcher(text).matches() && !text.contains("\\")) 
            return "Pure math pattern without LaTeX: '" + text + "'";
        if (text.length() == 1 && SINGLE_NON_LETTER.matcher(text).matches()) 
            return "Single non-letter character: '" + text + "'";
        if (URL_PATTERN.matcher(text).find()) return "Contains URL pattern";
        if (CSS_PATTERN.matcher(text).find() && text.split("\\s+").length < 3
                && !text.matches(".*:\\s*[A-Za-zÄÖÜäöüß].*")) 
            return "CSS pattern without enough words: '" + text + "'";
        if (XML_TAG_PATTERN.matcher(text).matches()) return "XML tag pattern: '" + text + "'";
        if (COORDINATE_PATTERN.matcher(text).matches() && text.split("\\s+").length > 3) 
            return "Coordinate pattern with too many parts: '" + text + "'";
        if (text.startsWith("\\") && !LATEX_CMD_WITH_BRACES.matcher(text).matches()) 
            return "LaTeX command without proper braces: '" + text + "'";
        if (!MULTI_LETTERS.matcher(text).matches()) 
            return "No multiple letters found: '" + text + "'";
        if (!text.matches(".*[a-zA-ZäöüÄÖÜßàáâãçèéêëìíîïñòóôõùúûüýÿ].*")) 
            return "No letters found: '" + text + "'";

        // Passed all checks
        return null;
    }

    private boolean isBinaryOrEncodedContent(String text) {
        if (text == null || text.isEmpty()) return false;
        String stripped = text.trim();
        if (stripped.length() > 100 && BASE64_PATTERN.matcher(stripped).matches()) return true;
        if (stripped.length() > 50 && HEX_PATTERN.matcher(stripped).matches()) return true;
        for (String word : stripped.split("\\s+")) if (word.length() > 100) return true;
        return false;
    }

    private boolean hasGraphicsAttributes(Element element) {
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++)
            if (GRAPHICS_ATTRIBUTES.contains(attrs.item(i).getNodeName())) return true;
        return false;
    }

    private boolean isTranslatableAttribute(String name) {
        return switch (name) {
            case "title", "alt", "aria-label", "aria-description", "placeholder", "desc" -> true;
            default -> false;
        };
    }

    private void addElementAttributes(Element element, Map<String, String> attrs) {
        NamedNodeMap nodeAttrs = element.getAttributes();
        for (int i = 0; i < nodeAttrs.getLength(); i++) {
            Node attr = nodeAttrs.item(i);
            attrs.put(attr.getNodeName(), attr.getNodeValue());
        }
    }

    private TextElement createTextElement(int id, String xpath, Element element,
                                          String text, String textType) {

        TextElement te = new TextElement(id, xpath, element.getTagName(), text, textType);

        Map<String, String> attrs = new HashMap<>();
        addElementAttributes(element, attrs);
        te.setAttributes(attrs);

        TextElement.TextContext ctx = new TextElement.TextContext();
        Node parent = element.getParentNode();
        if (parent instanceof Element parentEl) {
            ctx.setParentTag(parentEl.getTagName());

            Map<String, String> parentAttrs = new HashMap<>();
            addElementAttributes(parentEl, parentAttrs);
            ctx.setParentAttributes(parentAttrs);
        }

        String style = attrs.get("style");
        ctx.setStyle(style);
        ctx.setMath("math".equals(style));
        te.setContext(ctx);

        return te;
    }

    private int getElementIndex(Element element) {
        Node parent = element.getParentNode();
        if (parent == null) return 0;
        NodeList siblings = parent.getChildNodes();
        int idx = 0;
        for (int i = 0; i < siblings.getLength(); i++) {
            Node sib = siblings.item(i);
            if (sib.getNodeType() == Node.ELEMENT_NODE &&
                sib.getNodeName().equals(element.getNodeName())) {
                if (sib == element) return idx;
                idx++;
            }
        }
        return idx;
    }
}

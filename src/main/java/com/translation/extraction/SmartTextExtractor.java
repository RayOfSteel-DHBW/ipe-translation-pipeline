package com.translation.extraction;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

import com.google.inject.Singleton;

@Singleton
public class SmartTextExtractor {
    private static final Logger logger = Logger.getLogger(SmartTextExtractor.class.getName());
    
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
    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9A-Fa-f\\s]+$");
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("^[\\d\\s,.-]+$");
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://|file://|[a-zA-Z]:\\\\)");
    private static final Pattern CSS_PATTERN = Pattern.compile("[{};:]");
    private static final Pattern XML_TAG_PATTERN = Pattern.compile("^<[^>]+>.*</[^>]+>$");
    private static final Pattern PURE_MATH_PATTERN = Pattern.compile("^[\\d\\s\\+\\-\\*/=\\(\\)\\[\\]\\{\\}\\^\\$\\.<>]+$");
    private static final Pattern LATEX_PATTERN = Pattern.compile("\\\\[a-zA-Z]+");
    private static final Pattern LATEX_BF   = Pattern.compile("\\\\textbf\\{([^}]+)}");
    private static final Pattern LATEX_CMD_WITH_BRACES = Pattern.compile("^\\\\[A-Za-z]+\\{[^}]+}.*$");
    private static final Pattern SINGLE_NON_LETTER = Pattern.compile("^[^a-zA-ZäöüÄÖÜß]$");
    private static final Pattern MULTI_LETTERS = Pattern.compile(".*[A-Za-zÄÖÜäöüß]{2,}.*");
    
    public ExtractionResult extractText(String xmlContent) {
        ExtractionResult result = new ExtractionResult();
        result.setOriginalXml(xmlContent);
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlContent)));
            
            List<TextElement> textElements = new ArrayList<>();
            ExtractionResult.ExtractionStats stats = new ExtractionResult.ExtractionStats();
            
            extractTextFromNode(document.getDocumentElement(), "", textElements, stats);
            
            String processedXml = replaceTextWithPlaceholders(xmlContent, textElements);
            
            result.setTextElements(textElements);
            result.setProcessedXml(processedXml);
            
            stats.setTranslatableFound(textElements.size());
            stats.calculateNoiseReduction();
            result.setStats(stats);
            
            logger.info("Text extraction completed: " + 
                       "Total scanned: " + stats.getTotalElementsScanned() + 
                       ", Skipped: " + stats.getElementsSkipped() + 
                       ", Translatable found: " + stats.getTranslatableFound() + 
                       ", Noise reduction: " + String.format("%.1f%%", stats.getNoiseReductionPercent()));
            
        } catch (Exception e) {
            logger.severe("Failed to extract text from XML: " + e.getMessage());
            throw new RuntimeException("Text extraction failed", e);
        }
        
        return result;
    }
    
    private void extractTextFromNode(Node node, String parentXpath, List<TextElement> textElements, ExtractionResult.ExtractionStats stats) {
        stats.setTotalElementsScanned(stats.getTotalElementsScanned() + 1);
        
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            String tagName = element.getTagName().toLowerCase();
            
            if (SKIP_ELEMENTS.contains(tagName) || hasGraphicsAttributes(element)) {
                stats.setElementsSkipped(stats.getElementsSkipped() + 1);
                return;
            }
            
            String currentXpath = parentXpath + "/" + tagName + "[" + getElementIndex(element) + "]";
            
            String textContent = element.getTextContent();
            if (textContent != null && !textContent.trim().isEmpty()) {
                for (String seg : splitIntoSegments(textContent)) {
                    if (isTranslatableText(seg, element)) {
                        TextElement te = createTextElement(textElements.size(), currentXpath, element, seg, "element_text");
                        textElements.add(te);
                    }
                }
            }
            
            // Extract attribute text
            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                String attrValue = attr.getNodeValue();
                if (attrValue != null && !attrValue.trim().isEmpty() && 
                    isTranslatableText(attrValue, element) && isTranslatableAttribute(attr.getNodeName())) {
                    TextElement textElement = createTextElement(textElements.size(), currentXpath + "/@" + attr.getNodeName(), 
                                                              element, attrValue, "attribute_" + attr.getNodeName());
                    textElements.add(textElement);
                }
            }
            
            // Process child nodes
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                extractTextFromNode(children.item(i), currentXpath, textElements, stats);
            }
        }
    }
    
    private boolean isTranslatableText(String text, Element element) {
        text = text.trim();
        
        if (text.length() < 2) {
            return false;
        }
        
        if (isBinaryOrEncodedContent(text)) {
            return false;
        }
        
        if (PURE_MATH_PATTERN.matcher(text).matches() && !text.contains("\\")) {
            return false;
        }
        
        if (text.length() == 1 && SINGLE_NON_LETTER.matcher(text).matches()) {
            return false;
        }
        
        if (URL_PATTERN.matcher(text).find()) {
            return false;
        }
        
        if (CSS_PATTERN.matcher(text).find() && text.split("\\s+").length < 3) {
            return false;
        }
        
        if (XML_TAG_PATTERN.matcher(text).matches()) {
            return false;
        }
        
        if (COORDINATE_PATTERN.matcher(text).matches() && text.split("\\s+").length > 3) {
            return false;
        }
        
        /* skip plain LaTeX macros such as \helmholz */
        if (text.startsWith("\\")) {
            if (!LATEX_CMD_WITH_BRACES.matcher(text).matches()) {
                return false;
            }
        }
        
        /* require at least one run of two consecutive letters (ignores N(8), v_0, e = {u,v}) */
        if (!MULTI_LETTERS.matcher(text).matches()) {
            return false;
        }
        
        if (!text.matches(".*[a-zA-ZäöüÄÖÜßàáâãçèéêëìíîïñòóôõùúûüýÿ].*")) {
            return false;
        }
        
        if (text.startsWith("\\")) {
            if (text.length() < 50 && text.matches(".*[a-zA-Z].*")) {
                return true;
            }
        }
        
        if (element != null && "math".equals(element.getAttribute("style"))) {
            return true;
        }
        
        return true;
    }
    
    private boolean isBinaryOrEncodedContent(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        String stripped = text.trim();
        
        if (stripped.length() > 100 && BASE64_PATTERN.matcher(stripped).matches()) {
            return true;
        }
        
        if (stripped.length() > 50 && HEX_PATTERN.matcher(stripped).matches()) {
            return true;
        }
        
        String[] words = stripped.split("\\s+");
        for (String word : words) {
            if (word.length() > 100) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean hasGraphicsAttributes(Element element) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            String attrName = attributes.item(i).getNodeName();
            if (GRAPHICS_ATTRIBUTES.contains(attrName)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isTranslatableAttribute(String attributeName) {
        return attributeName.equals("title") || 
               attributeName.equals("alt") || 
               attributeName.equals("aria-label") ||
               attributeName.equals("aria-description") ||
               attributeName.equals("placeholder") ||
               attributeName.equals("desc");
    }
    
    private TextElement createTextElement(int id, String xpath, Element element, String text, String textType) {
        TextElement textElement = new TextElement(id, xpath, element.getTagName(), text, textType);
        
        Map<String, String> attributes = new HashMap<>();
        NamedNodeMap nodeAttributes = element.getAttributes();
        for (int i = 0; i < nodeAttributes.getLength(); i++) {
            Node attr = nodeAttributes.item(i);
            attributes.put(attr.getNodeName(), attr.getNodeValue());
        }
        textElement.setAttributes(attributes);

        TextElement.TextContext context = new TextElement.TextContext();
        Node parent = element.getParentNode();
        if (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
            Element parentElement = (Element) parent;
            context.setParentTag(parentElement.getTagName());
            
            Map<String, String> parentAttrs = new HashMap<>();
            NamedNodeMap parentAttributes = parentElement.getAttributes();
            for (int i = 0; i < parentAttributes.getLength(); i++) {
                Node attr = parentAttributes.item(i);
                parentAttrs.put(attr.getNodeName(), attr.getNodeValue());
            }
            context.setParentAttributes(parentAttrs);
        }
        
        // Some "Math" tags actually contain stuff
        String style = attributes.get("style");
        context.setStyle(style);
        context.setMath("math".equals(style));
        context.setLatex(LATEX_PATTERN.matcher(text).find());
        
        textElement.setContext(context);
        
        return textElement;
    }
    
    private int getElementIndex(Element element) {
        Node parent = element.getParentNode();
        if (parent == null) return 0;
        
        NodeList siblings = parent.getChildNodes();
        int index = 0;
        for (int i = 0; i < siblings.getLength(); i++) {
            Node sibling = siblings.item(i);
            if (sibling.getNodeType() == Node.ELEMENT_NODE && 
                sibling.getNodeName().equals(element.getNodeName())) {
                if (sibling == element) {
                    return index;
                }
                index++;
            }
        }
        return index;
    }
    
    private String replaceTextWithPlaceholders(String xmlContent, List<TextElement> textElements) {
        String result = xmlContent;
        
        textElements.sort((a, b) -> Integer.compare(b.getOriginalText().length(), a.getOriginalText().length()));
        
        for (TextElement element : textElements) {
            result = result.replace(element.getOriginalText(), element.getPlaceholder());
        }
        
        return result;
    }

    private List<String> splitIntoSegments(String raw) {
        List<String> parts = new ArrayList<>();
        Matcher m = LATEX_BF.matcher(raw);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                String plain = raw.substring(last, m.start()).trim();
                if (!plain.isEmpty()) parts.add(plain);
            }
            String bold = m.group(1).trim();
            if (!bold.isEmpty()) parts.add(bold);
            last = m.end();
        }
        if (last < raw.length()) {
            String rest = raw.substring(last).trim();
            if (!rest.isEmpty()) parts.add(rest);
        }
        return parts;
    }
}

package com.translation.extraction;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;
import java.util.logging.Logger;

import com.google.inject.Singleton;

@Singleton
public class SmartTextExtractor {
    private static final Logger logger = Logger.getLogger(SmartTextExtractor.class.getName());
    
    private static final Set<String> SKIP_ELEMENTS = Set.of(
        "svg", "g", "path", "rect", "circle", "ellipse", "line", 
        "polyline", "polygon", "image", "defs", "clipPath", "mask"
    );
    
    private static final Pattern BINARY_PATTERN = Pattern.compile("^[A-Za-z0-9+/]+=*$");
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("^[\\d\\s,.-]+$");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://|^file://|^/");
    private static final Pattern LATEX_PATTERN = Pattern.compile("\\\\[a-zA-Z]+\\{");
    
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
            
            if (SKIP_ELEMENTS.contains(tagName)) {
                stats.setElementsSkipped(stats.getElementsSkipped() + 1);
                return;
            }
            
            String currentXpath = parentXpath + "/" + tagName + "[" + getElementIndex(element) + "]";
            
            // Extract text content
            String textContent = element.getTextContent();
            if (textContent != null && !textContent.trim().isEmpty() && isTranslatableText(textContent)) {
                TextElement textElement = createTextElement(textElements.size(), currentXpath, element, textContent, "element_text");
                textElements.add(textElement);
            }
            
            // Extract attribute text
            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                String attrValue = attr.getNodeValue();
                if (attrValue != null && !attrValue.trim().isEmpty() && 
                    isTranslatableText(attrValue) && isTranslatableAttribute(attr.getNodeName())) {
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
    
    private boolean isTranslatableText(String text) {
        text = text.trim();
        
        if (text.length() < 2) {
            return false;
        }
        
        // Skip binary/base64 content
        if (BINARY_PATTERN.matcher(text).matches() && text.length() > 20) {
            return false;
        }
        
        // Skip coordinate patterns
        if (COORDINATE_PATTERN.matcher(text).matches()) {
            return false;
        }
        
        // Skip URLs and file paths
        if (URL_PATTERN.matcher(text).find()) {
            return false;
        }
        
        // Preserve LaTeX commands
        if (LATEX_PATTERN.matcher(text).find()) {
            return true;
        }
        
        // Require at least 30% letter content
        long letterCount = text.chars().filter(Character::isLetter).count();
        double letterRatio = (double) letterCount / text.length();
        
        return letterRatio >= 0.3;
    }
    
    private boolean isTranslatableAttribute(String attributeName) {
        return attributeName.equals("title") || 
               attributeName.equals("alt") || 
               attributeName.equals("label") ||
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
}

package com.translation.extraction;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import com.google.inject.Singleton;

@Singleton
public class SmartTextExtractor {

	// Simple patterns used by shouldKeep()
	private static final Pattern FORMATTING_LATEX_CMD = Pattern.compile("\\\\(?:textbf|emph|textit|text|section|subsection|chapter|title|label|mathrm|mathit|mathbf)\\{([^}]*)}");
	private static final Pattern OTHER_LATEX_CMD = Pattern.compile("\\\\[a-zA-Z]+(?:\\{[^}]*})?");

	public ExtractionResult extractText(String xml) {
		List<TextElement> elements = new ArrayList<>();
		int elementId = 1; // Counter for element IDs
		Document doc = null; // Declare doc at method level

		try {
			// separate factory configuration from builder creation
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setValidating(false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));

			doc.getDocumentElement().normalize();

			// 1. page/@title
			NodeList pages = doc.getElementsByTagName("page");
			for (int i = 0; i < pages.getLength(); i++) {
				Element page = (Element) pages.item(i);
				if (page.hasAttribute("title")) {
					elements.add(new TextElement(
							elementId++,
							"/page[" + (i + 1) + "]/@title",
							"page",
							page.getAttribute("title"),
							"attribute_title"));
				}
			}

			// 2. <preamble>
			NodeList preambles = doc.getElementsByTagName("preamble");
			for (int i = 0; i < preambles.getLength(); i++) {
				String preText = preambles.item(i).getTextContent();
				String searchString = "\\newcommand{\\prestitle}{";
				int start = preText.indexOf(searchString);
				if (start >= 0) {
					int end = preText.indexOf('}', start + searchString.length());
					if (end > start) {
						String title = preText.substring(start + searchString.length(), end);
						elements.add(new TextElement(
								elementId++,
								"/preamble[" + (i + 1) + "]",
								"preamble",
								title,
								"prestitle"));
					}
				}
			}

			// 3. every <text> element
			NodeList texts = doc.getElementsByTagName("text");
			for (int i = 0; i < texts.getLength(); i++) {
				Element txt = (Element) texts.item(i);
				String raw = txt.getTextContent();
				if (shouldKeep(raw)) {
					String placeholder = "@PLACEHOLDER(" + elementId + ")@";
					
					TextElement element = new TextElement(
							elementId++,
							"/text[" + (i + 1) + "]",
							"text",
							raw,
							"element_text");
					
					// Set context information
					TextElement.TextContext context = element.getContext();
					context.setParentTag("text");
					
					// Check if text contains math notation (dollar signs)
					boolean hasMath = raw.contains("$");
					context.setMath(hasMath);
					
					// Set style attribute if present
					if (txt.hasAttribute("style")) {
						context.setStyle(txt.getAttribute("style"));
					}
					
					// Copy parent attributes
					if (txt.hasAttributes()) {
						for (int j = 0; j < txt.getAttributes().getLength(); j++) {
							org.w3c.dom.Attr attr = (org.w3c.dom.Attr) txt.getAttributes().item(j);
							context.getParentAttributes().put(attr.getName(), attr.getValue());
						}
					}
					
					elements.add(element);
					
					// Replace text content directly in DOM
					txt.setTextContent(placeholder);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		// Create ExtractionResult and set the text elements
		ExtractionResult result = new ExtractionResult();
		result.setOriginalXml(xml);
		result.setProcessedXml(createProcessedXml(doc));
		result.setTextElements(elements);
		return result;
	}

	/** Create processed XML by serializing the modified DOM */
	private String createProcessedXml(Document doc) {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
			
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(writer));
			return writer.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return ""; // fallback
		}
	}

	/** Strip simple LaTeX commands; keep if a real word remains. */
	private boolean shouldKeep(String text) {
		if (text == null) return false;
		
		// Extract text from ALL LaTeX commands with braces (including math commands)
		String extractedText = text;
		
		// Extract content from ALL LaTeX commands with braces
		java.util.regex.Matcher matcher = FORMATTING_LATEX_CMD.matcher(extractedText);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			// Replace the entire command with just its content (group 1)
			// Use Matcher.quoteReplacement to escape special characters
			matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(matcher.group(1)));
		}
		matcher.appendTail(sb);
		extractedText = sb.toString();
		
		// Remove remaining LaTeX commands without braces or empty braces
		extractedText = OTHER_LATEX_CMD.matcher(extractedText).replaceAll("").trim();
		
		// Remove mathematical notation and symbols to focus on text content
		String cleanedText = extractedText.replaceAll("[${}()\\[\\]\\\\=<>+*/-]", " ")
									.replaceAll("\\s+", " ")
									.trim();
		
		// Check if we have meaningful text with words of length >= 3
		// Split by whitespace and check each word
		String[] words = cleanedText.split("\\s+");
		for (String word : words) {
			// Check if word has at least 3 consecutive letters
			if (word.length() >= 3 && word.matches(".*[A-Za-zÄÖÜäöüß]{3,}.*")) {
				return true;
			}
		}
		
		return false;
	}
}

package com.translation.extraction;

import java.util.Map;
import java.util.HashMap;

public class TextElement {
    private int id;
    private String xpath;
    private String tagName;
    private Map<String, String> attributes;
    private String originalText;
    private String textType;
    private TextContext context;
    
    public TextElement(int id, String xpath, String tagName, String originalText, String textType) {
        this.id = id;
        this.xpath = xpath;
        this.tagName = tagName;
        this.originalText = originalText;
        this.textType = textType;
        this.attributes = new HashMap<>();
        this.context = new TextContext();
    }
    
    public int getId() {
        return id;
    }
    
    public String getXpath() {
        return xpath;
    }
    
    public String getTagName() {
        return tagName;
    }
    
    public Map<String, String> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
    
    public String getOriginalText() {
        return originalText;
    }
    
    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }
    
    public String getTextType() {
        return textType;
    }
    
    public TextContext getContext() {
        return context;
    }
    
    public void setContext(TextContext context) {
        this.context = context;
    }
    
    public String getPlaceholder() {
        return "{" + id + "}";
    }
    
    public static class TextContext {
        private String parentTag;
        private Map<String, String> parentAttributes;
        private String style;
        private boolean isMath;
        private boolean isLatex;
        
        public TextContext() {
            this.parentAttributes = new HashMap<>();
        }
        
        public String getParentTag() {
            return parentTag;
        }
        
        public void setParentTag(String parentTag) {
            this.parentTag = parentTag;
        }
        
        public Map<String, String> getParentAttributes() {
            return parentAttributes;
        }
        
        public void setParentAttributes(Map<String, String> parentAttributes) {
            this.parentAttributes = parentAttributes;
        }
        
        public String getStyle() {
            return style;
        }
        
        public void setStyle(String style) {
            this.style = style;
        }
        
        public boolean isMath() {
            return isMath;
        }
        
        public void setMath(boolean math) {
            isMath = math;
        }
        
        public boolean isLatex() {
            return isLatex;
        }
        
        public void setLatex(boolean latex) {
            isLatex = latex;
        }
    }
}

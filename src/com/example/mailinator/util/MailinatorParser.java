package com.example.mailinator.util;

import org.ccil.cowan.tagsoup.AttributesImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class MailinatorParser extends DefaultHandler {
    private final StringBuilder output;
    public int elements;
    private boolean append;

    public MailinatorParser(StringBuilder output) {
        this.output = output;
        elements = 0;
        append = false;
    }

    @Override
    public void startElement(String uri, String localName,
                             String qName, Attributes at) throws SAXException {
        StringBuilder allAttrs = new StringBuilder();
        AttributesImpl attributes = (AttributesImpl) at;
        String space = "";
        for (int i = 0; i < attributes.getLength(); i++) {
            if (attributes.getValue(i).equals("mailview")) {
                append = true;
            }
            allAttrs.append(space)
                    .append(attributes.getLocalName(i))
                    .append("=\"")
                    .append(attributes.getValue(i))
                    .append("\"");
            space = " ";
        }

        if (append) {
            output.append("<").append(localName)
                    .append(" ").append(allAttrs).append(">");
            ++elements;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (append) {
            output.append(new String(ch, start, length).trim());
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (append) {
            output.append("</").append(localName).append(">");
            if (elements-- == 0) {
                append = false;
            }
        }
    }
}

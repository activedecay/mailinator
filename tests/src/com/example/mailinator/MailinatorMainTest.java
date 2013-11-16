package com.example.mailinator;

import android.test.ActivityInstrumentationTestCase2;
import org.ccil.cowan.tagsoup.AttributesImpl;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.util.logging.Logger;

public class MailinatorMainTest extends ActivityInstrumentationTestCase2<MailinatorMain> {

    private Logger logger;

    public MailinatorMainTest() {
        super("com.example.mailinator", MailinatorMain.class);
        logger = Logger.getLogger("MailinatorMainTest");
    }

    public void testfuggit() throws Exception {
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        MailinatorMain activity = getActivity();
//        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
//        parser.setInput(activity.getResources().openRawResource(R.raw.mail_message_info), null);
//        parser.nextTag();
        Parser tagSoup = new Parser();
        tagSoup.setContentHandler(new DefaultHandler(){
            @Override
            public void startElement(String uri, String localName,
                                     String qName, Attributes at) throws SAXException {
                StringBuilder builder = new StringBuilder();
                StringBuilder allAttrs = new StringBuilder();
                AttributesImpl attributes = (AttributesImpl) at;
                String space = "";
                for (int i = 0; i < attributes.getLength(); i++) {
                    allAttrs.append(space)
                            .append(attributes.getLocalName(i))
                            .append("=\"")
                            .append(attributes.getValue(i))
                            .append("\"");
                    space = " ";
                }
                logger.info(builder.append("startElement():")
//                        .append(" uri=").append(uri)
                        .append("<").append(localName)
                        .append(" ").append(allAttrs).append(">")
                        .toString());
            }
        });
        tagSoup.parse(new InputSource(activity.getResources().openRawResource(R.raw.mail_message_info)));
    }
}

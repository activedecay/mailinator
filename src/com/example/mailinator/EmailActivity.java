package com.example.mailinator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Xml;
import android.widget.TextView;
import com.example.mailinator.util.TaskProgressListener;
import com.example.mailinator.util.URLReaderTask;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class EmailActivity extends Activity {
    public static final String RENDER_JSP_URI = MailinatorMain.MAILINATOR_COM_URL + "/rendermail.jsp";
    public static final String MSGID_PARAM = "?msgid=";
    private String ns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.email);

        Intent intent = getIntent();

        TextView textLabel = (TextView)findViewById(R.id.email_subject);

        String emailSubject = intent.getStringExtra("email_subject");
        String emailId = intent.getStringExtra("email_id");

        textLabel.setText(emailSubject);

        //http://www.mailinator.com/rendermail.jsp?msgid=1384477718-45432902-mail&time=1384477728583

        List<URL> messageUrl = new ArrayList<URL>();
        try {
            messageUrl.add(new URL(RENDER_JSP_URI + MSGID_PARAM + emailId + MailinatorMain.TIME_PARAM + MailinatorMain.now()));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        URLReaderTask urlReaderTask = new URLReaderTask();
        urlReaderTask.setListener(new TaskProgressListener<List<String>, Integer>() {
            @Override
            public void onPostExecute(List<String> strings) {
                String xml = strings.get(0);
                setMessageText(xml);
                InputStream in = null;
                int bgnMsgIdx = xml.indexOf('>', xml.indexOf("mailview")) + 1;
                int endMsgIdx = xml.lastIndexOf("</div>");
                Logger l = Logger.getAnonymousLogger();
                String mailinatorData = "<root>" + xml
                        .substring(0, bgnMsgIdx)
                        .concat(xml.substring(endMsgIdx))
                        .concat("</div></div></root>")
                        .replaceFirst("</span>", "")
                        .replaceAll("<img[^>]*?>", "")
                        .replaceAll("<input[^>]*?>", "");

                l.info(mailinatorData);
                try {
                    in = new ByteArrayInputStream(
                            mailinatorData.getBytes("UTF-8"));
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                    parser.setInput(in, null);
                    parser.nextTag();
                    StringBuilder sb = new StringBuilder();
                    for (Object s : readFeed(parser)) {
                        sb.append(s);
                    }
                    setMessageText(sb.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    assert in != null;
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //String prettyXml = getHtmlUsingXpath(strings.get(0));
                //setMessageText(Html.fromHtml(prettyXml));
            }

            @Override
            public void onProgressUpdate(Integer... values) {
                // intentionally blank
            }
        });
        urlReaderTask.execute(messageUrl.toArray(new URL[messageUrl.size()]));
    }

    private List readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List entries = new ArrayList();

        parser.require(XmlPullParser.START_TAG, ns, "div");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("entry")) {
                entries.add(readEntry(parser));
            } else {
                skip(parser);
            }
        }
        return entries;
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
// to their respective "read" methods for processing. Otherwise, skips the tag.
    private MailEntry readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "entry");
        String title = null;
        String summary = null;
        String link = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("title")) {
                title = readTitle(parser);
            } else if (name.equals("summary")) {
                summary = readSummary(parser);
            } else if (name.equals("link")) {
                link = readLink(parser);
            } else {
                skip(parser);
            }
        }
        return new MailEntry(title, summary, link);
    }

    // Processes title tags in the feed.
    private String readTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "title");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "title");
        return title;
    }

    // Processes link tags in the feed.
    private String readLink(XmlPullParser parser) throws IOException, XmlPullParserException {
        String link = "";
        parser.require(XmlPullParser.START_TAG, ns, "link");
        String tag = parser.getName();
        String relType = parser.getAttributeValue(null, "rel");
        if (tag.equals("link")) {
            if (relType.equals("alternate")){
                link = parser.getAttributeValue(null, "href");
                parser.nextTag();
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, "link");
        return link;
    }

    // Processes summary tags in the feed.
    private String readSummary(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "summary");
        String summary = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "summary");
        return summary;
    }

    // For the tags title and summary, extracts their text values.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    /** needed to wrap the xml string with a root element, and massage the innards to get
     * the valid xml format. very sad, very weak strategy. */
    private String getHtmlUsingXpath(String xmlString) {
        try {
            XPathExpression expr = XPathFactory.newInstance().newXPath().compile("/root/div[3]");
            Document doc = getDocument("<root>"
                    + xmlString.replaceFirst("</span>", "")
                    .replaceAll("<img[^>]*?>", "")
                    .replaceAll("<input[^>]*?>", "")
                    .replaceAll("<br[^/>]*?>", "<br/>")
                    .replaceAll("rel=nofollow", "")
                    + "</root>");
            NodeList evaluate = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            return getPrettyXml(evaluate.item(0));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getPrettyXml(Node item) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(item);
        transformer.transform(source, result);
        return result.getWriter().toString();
    }

    private Document getDocument(String xml) throws ParserConfigurationException, IOException {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        try {
            return builder.parse(new StringBufferInputStream(xml));
        } catch (SAXException e) {
            e.printStackTrace();
            setMessageText(xml);
        }
        return null;
    }

    private void setMessageText(CharSequence text) {
        TextView textLabel = (TextView)findViewById(R.id.email_message);
        textLabel.setText(text);
    }


}

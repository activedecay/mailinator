package com.example.mailinator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;
import com.example.mailinator.util.TaskProgressListener;
import com.example.mailinator.util.URLReaderTask;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class EmailActivity extends Activity {
    public static final String RENDER_JSP_URI = MailinatorMain.MAILINATOR_COM_URL + "/rendermail.jsp";
    public static final String MSGID_PARAM = "?msgid=";

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
                //setMessageText(strings.get(0));
                XPathFactory factory = XPathFactory.newInstance();
                XPath xPath = factory.newXPath();
                try {
                    XPathExpression expr = xPath.compile("/root/div[3]");
                    String xml = "<root>" + strings.get(0).replaceFirst("</span>", "")
                            .replaceAll("<img[^>]*?>", "")
                            .replaceAll("<input[^>]*?>", "")
                            .replaceAll("<br[^/>]*?>", "<br/>")+ "</root>";
                    Document doc = getDocument(xml);
                    NodeList evaluate = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                    Node item = evaluate.item(0);

                    String prettyXml = getPrettyXml(item);

                    setMessageText(item != null ? Html.fromHtml(prettyXml) : "");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onProgressUpdate(Integer... values) {
                // intentionally blank
            }
        });
        urlReaderTask.execute(messageUrl.toArray(new URL[messageUrl.size()]));
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

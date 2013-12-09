package com.example.mailinator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;
import com.example.mailinator.util.MailinatorParser;
import com.example.mailinator.util.TaskProgressListener;
import com.example.mailinator.util.TimeUtil;
import com.example.mailinator.util.URLReaderTask;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.InputSource;

import java.io.CharArrayReader;
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

        List<String> messageUrl = new ArrayList<String>();
        messageUrl.add(new String(RENDER_JSP_URI + MSGID_PARAM + emailId + MailinatorMain.TIME_PARAM + TimeUtil.now()));
        URLReaderTask urlReaderTask = new URLReaderTask();
        urlReaderTask.setListener(new TaskProgressListener<List<String>, Integer>() {
            @Override
            public void onPostExecute(List<String> strings) {
                String xml = strings.get(0);

                Parser tagSoup = new Parser();
                final StringBuilder output = new StringBuilder();
                tagSoup.setContentHandler(new MailinatorParser(output));
                InputSource input = new InputSource(new CharArrayReader(xml.toCharArray()));
                try {
                    tagSoup.parse(input);
                    setMessageText(Html.fromHtml(output.toString()));
                } catch (Exception e) {
                    setMessageText(Html.fromHtml(xml));
                }
            }

            @Override
            public void onProgressUpdate(Integer... values) {
                // intentionally blank
            }
        });
        urlReaderTask.execute(messageUrl.toArray(new String[messageUrl.size()]));
    }

    private void setMessageText(CharSequence text) {
        TextView textLabel = (TextView)findViewById(R.id.email_message);
        textLabel.setText(text);
    }


}

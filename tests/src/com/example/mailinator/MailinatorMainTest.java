package com.example.mailinator;

import android.test.ActivityInstrumentationTestCase2;
import com.example.mailinator.util.MailinatorParser;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.InputSource;

import java.util.logging.Logger;

public class MailinatorMainTest extends ActivityInstrumentationTestCase2<MailinatorMain> {

    private Logger logger = Logger.getLogger("MailinatorMainTest");

    public MailinatorMainTest() {
        super("com.example.mailinator", MailinatorMain.class);
    }

    public void testfuggit() throws Exception {
        MailinatorMain activity = getActivity();
        Parser tagSoup = new Parser();
        final StringBuilder output = new StringBuilder();
        tagSoup.setContentHandler(new MailinatorParser(output));
        InputSource input = new InputSource(activity.getResources().openRawResource(R.raw.mail_message_info));
        tagSoup.parse(input);
        logger.severe(output.toString());
    }

}

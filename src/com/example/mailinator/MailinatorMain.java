package com.example.mailinator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.example.mailinator.util.JsonHelper;
import com.example.mailinator.util.TaskProgressListener;
import com.example.mailinator.util.TimeUtil;
import com.example.mailinator.util.URLReaderTask;
import org.json.JSONArray;
import org.json.JSONObject;
import android.support.v4.app.Fragment;

import java.util.*;

public class MailinatorMain extends Activity {
    public static final String MAILINATOR_COM_URL = "http://www.mailinator.com";
    public static final String USE_IT_URI = MAILINATOR_COM_URL + "/useit?box=";
    public static final String GRAB_URI = MAILINATOR_COM_URL + "/grab?inbox=";

    public static final String ADDRESS_KEY = "address";
    public static final String BEEN_READ_KEY = "been_read";
    public static final String ID_KEY = "id";
    public static final String FROMFULL_KEY = "fromfull";
    public static final String FROM_KEY = "from";
    public static final String MAILDIR_KEY = "maildir";
    public static final String SECONDS_AGO_KEY = "seconds_ago";
    public static final String SNIPPET_KEY = "snippet";
    public static final String SUBJECT_KEY = "subject";
    public static final String TIME_KEY = "time";
    public static final String TO_KEY = "to";

    public static final String ADDRESS_PARAM = "&address=";
    public static final String TIME_PARAM = "&time=";

    private List<String> usernames = Arrays.asList("0x1337b33f", "mailinator", "jfaust", "god");

    private Map<String, List<Map<String, String>>> messages = new TreeMap<String, List<Map<String, String>>>();

    private Map<String, SimpleAdapter> adapters = new TreeMap<String, SimpleAdapter>();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        List<String> useItUrls = createUseItInboxURLList();
        URLReaderTask urlReaderTask = new URLReaderTask();
        urlReaderTask.setListener(new TaskProgressListener<List<String>, Integer>() {
            @Override
            public void onPostExecute(List<String> inboxUseItJson) {
                onInboxesUseItEvent(inboxUseItJson);
            }

            @Override
            public void onProgressUpdate(Integer... values) {
                setProgress("using " + usernames.get(values[0]));
            }
        });
        urlReaderTask.execute(useItUrls.toArray(new String[useItUrls.size()]));
    }

    public static class DemoObjectFragment extends Fragment {

        public static final String ARG_OBJECT = "object";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            SimpleAdapter adapter = createEmailAdapter(username);
            ListView emailListView = createEmailListView(username, adapter);
            View rootView = inflater.inflate(R.layout.fragment_collection_object, container, false);
            Bundle args = getArguments();
            ((TextView) rootView.findViewById(android.R.id.text1)).setText(
                    Integer.toString(args.getInt(ARG_OBJECT)));
            return rootView;
        }
    }

    private ListView createEmailListView(String username, ListAdapter adapter) {
        ListView emailListView = new ListView(this);
        emailListView.setAdapter(adapter);
        emailListView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        emailListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                Map<String, String> email = (Map<String, String>) parent.getItemAtPosition(position);
                Intent intent = new Intent(getApplicationContext(), EmailActivity.class);
                intent.putExtra("email_subject", email.get(SUBJECT_KEY));
                intent.putExtra("email_id", email.get(ID_KEY));
                startActivity(intent);
            }
        });

        TextView header = new TextView(this);
        header.setText(username);
        emailListView.addHeaderView(header);
        return emailListView;
    }

    private SimpleAdapter createEmailAdapter(String username) {
        ArrayList<Map<String, String>> emailList = new ArrayList<Map<String, String>>();
        messages.put(username, emailList);
        SimpleAdapter adapter = new SimpleAdapter(
                MailinatorMain.this, emailList, R.layout.email_list_item,
                new String[]{SUBJECT_KEY, FROMFULL_KEY, TO_KEY},
                new int[]{R.id.subject, R.id.from, R.id.to});
        adapters.put(username, adapter);
        return adapter;
    }

    private List<String> createUseItInboxURLList() {
        List<String> urls = new ArrayList<String>();
        for (String username : usernames) {
            urls.add(USE_IT_URI + username + TIME_PARAM + TimeUtil.now());
        }
        return urls;
    }

    private void onInboxesUseItEvent(List<String> inboxesJson) {
        List<String> inboxUrls = new ArrayList<String>();
        for (int i = 0; i < inboxesJson.size(); i++) {
            String json = inboxesJson.get(i);
            String address = JsonHelper.getJsonString(json, ADDRESS_KEY);
            String username = usernames.get(i);

            inboxUrls.add(GRAB_URI + username + ADDRESS_PARAM + address + TIME_PARAM + TimeUtil.now());
        }
        TaskProgressListener<List<String>, Integer> listener;
        final URLReaderTask inboxesRead = new URLReaderTask();

        listener = new TaskProgressListener<List<String>, Integer>() {
            @Override
            public void onPostExecute(List<String> strings) {
                setProgress("done.");
            }

            /**
             * @param values
             */
            @Override
            public void onProgressUpdate(Integer... values) {
                setProgress("reading inbox " + usernames.get(values[0]));
                List<String> mailboxes = inboxesRead.getStrings();
                String mailboxJson = mailboxes.get(mailboxes.size() - 1);
                collectEmailSummary(mailboxJson, values[0]);
                adapters.get(values[0]).notifyDataSetChanged();
            }
        };

        inboxesRead.setListener(listener);
        inboxesRead.execute(inboxUrls.toArray(new String[inboxUrls.size()]));
    }

    private void collectEmailSummary(String json, int messageIdx) {
        JSONArray jsonArray = JsonHelper.getJsonArray(json, MAILDIR_KEY);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject o = jsonArray.optJSONObject(i);
            if (o.length() != 0) {
                Map<String, String> email = new HashMap<String, String>();
                if (!o.has(FROMFULL_KEY)) continue;

                collectStringData(o, email, FROMFULL_KEY);
                collectStringData(o, email, TO_KEY);
                collectStringData(o, email, SUBJECT_KEY);
                collectStringData(o, email, SNIPPET_KEY);
                collectStringData(o, email, FROM_KEY);
                collectStringData(o, email, ID_KEY);

                collectBooleanData(o, email, BEEN_READ_KEY);

                collectLongData(o, email, SECONDS_AGO_KEY);
                collectLongData(o, email, TIME_KEY);

                messages.get(messageIdx).add(email);
            }
        }
    }

    private void collectStringData(JSONObject o, Map<String, String> email, String key) {
        String from = JsonHelper.getJsonString(o, key);
        email.put(key, from);
    }

    private void collectBooleanData(JSONObject o, Map<String, String> email, String key) {
        Boolean from = JsonHelper.getJsonBoolean(o, key);
        email.put(key, from.toString());
    }

    private void collectLongData(JSONObject o, Map<String, String> email, String key) {
        Long from = JsonHelper.getJsonLong(o, key);
        email.put(key, from.toString());
    }

    private void setProgress(String text) {
        TextView view = (TextView) findViewById(R.id.progress);
        view.setText(text);
    }
}

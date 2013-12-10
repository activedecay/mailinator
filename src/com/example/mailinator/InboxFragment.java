package com.example.mailinator;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import com.example.mailinator.util.JsonHelper;
import com.example.mailinator.util.TaskProgressListener;
import com.example.mailinator.util.TimeUtil;
import com.example.mailinator.util.URLReaderTask;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 *
 */
public class InboxFragment extends ListFragment {
    /** bundle arguments */
    public static final String USERNAME_ARGS = "username";

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
    public static final String ADDRESS_KEY = "address";

    private ArrayList<Map<String,String>> emailList;
    private SimpleAdapter emailAdapter;
    private String username;
    private Bundle args;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        args = getArguments();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        args = getArguments();
        username = args.getString(USERNAME_ARGS);

        List<String> useItUrls = new ArrayList<String>(
                Arrays.asList(MailinatorUrl.USE_IT_URI + username + MailinatorUrl.TIME_PARAM + TimeUtil.now()));
        URLReaderTask urlReaderTask = new URLReaderTask();
        urlReaderTask.setListener(new TaskProgressListener<List<String>, Integer>() {
            @Override
            public void onPostExecute(List<String> inboxUseItJson) {
                onInboxUseItEvent(inboxUseItJson);
            }

            @Override
            public void onProgressUpdate(Integer... values) {
            }
        });
        urlReaderTask.execute(useItUrls.toArray(new String[useItUrls.size()]));

        return inflater.inflate(R.layout.inbox_fragment, container, false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onListItemClick(ListView parent, final View view, int position, long id) {
        Map<String, String> email = (Map<String, String>) parent.getItemAtPosition(position);
        Intent intent = new Intent(view.getContext(), EmailActivity.class);
        intent.putExtra("email_subject", email.get(SUBJECT_KEY));
        intent.putExtra("email_id", email.get(ID_KEY));
        startActivity(intent);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        emailAdapter = createEmailAdapter(getView());
        setListAdapter(emailAdapter);
    }

    private SimpleAdapter createEmailAdapter(View view) {
        emailList = new ArrayList<Map<String, String>>();
        return new SimpleAdapter(
                view.getContext(), emailList, R.layout.email_list_item,
                new String[]{SUBJECT_KEY, FROMFULL_KEY, TO_KEY},
                new int[]{R.id.subject, R.id.from, R.id.to});
    }

    private void onInboxUseItEvent(List<String> inboxJson) {
        List<String> inboxUrls = new ArrayList<String>();
        for (String json : inboxJson) {
            String address = JsonHelper.getJsonString(json, ADDRESS_KEY);
            inboxUrls.add(MailinatorUrl.GRAB_URI + username + MailinatorUrl.ADDRESS_PARAM + address + MailinatorUrl.TIME_PARAM + TimeUtil.now());
        }
        TaskProgressListener<List<String>, Integer> listener;
        final URLReaderTask inboxReadTask = new URLReaderTask();

        listener = new TaskProgressListener<List<String>, Integer>() {
            @Override
            public void onPostExecute(List<String> strings) {
            }

            @Override
            public void onProgressUpdate(Integer... values) {
                List<String> mailboxes = inboxReadTask.getStrings();
                String mailboxJson = mailboxes.get(mailboxes.size() - 1);
                collectEmailSummary(mailboxJson);
                emailAdapter.notifyDataSetChanged();
            }
        };

        inboxReadTask.setListener(listener);
        inboxReadTask.execute(inboxUrls.toArray(new String[inboxUrls.size()]));
    }

    private void collectEmailSummary(String json) {
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

                emailList.add(email);
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
}

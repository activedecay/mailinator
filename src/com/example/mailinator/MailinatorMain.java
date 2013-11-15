package com.example.mailinator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.mailinator.util.TaskProgressListener;
import com.example.mailinator.util.URLReaderTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class MailinatorMain extends Activity {
    public static final String FROMFULL_KEY = "fromfull";
    public static final String SUBJECT_KEY = "subject";
    public static final String SNIPPET_KEY = "snippet";
    public static final String TO_KEY = "to";
    public static final String ADDRESS_KEY = "address";
    public static final String BEEN_READ_KEY = "been_read";
    public static final String MAILINATOR_COM_URL = "http://www.mailinator.com";
    public static final String UNSET_URI = MAILINATOR_COM_URL + "/unset?box=";
    public static final String GRAB_URI = MAILINATOR_COM_URL + "/grab?inbox=";
    public static final String ADDRESS_PARAM = "&address=";
    public static final String TIME_PARAM = "&time=";
    public static final String ID_KEY = "id";
    public static final String MAILDIR_KEY = "maildir";
    public static final String SECONDS_AGO_KEY = "seconds_ago";
    public static final String TIME_KEY = "time";
    public static final String FROM_KEY = "from";
    private String[] mailboxUsername = {"high","hi","god","mail","mailinator","devil","satan","mary"};
    private List<Map<String,String>> emails = new ArrayList<Map<String, String>>();
    private SimpleAdapter adapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        adapter = new SimpleAdapter(
                MailinatorMain.this, emails, R.layout.email_list_item,
                new String[]{SUBJECT_KEY, FROMFULL_KEY, TO_KEY},
                new int[]{R.id.subject, R.id.from, R.id.to});

        ListView emailListView = (ListView) findViewById(R.id.listView);
        emailListView.setAdapter(adapter);
        emailListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                Map<String, String> email = (Map<String, String>) parent.getItemAtPosition(position);
                Intent intent = new Intent(getApplicationContext(), EmailActivity.class);
                intent.putExtra("email_subject", email.get(SUBJECT_KEY));
                intent.putExtra("email_id", email.get(ID_KEY));
                startActivity(intent);
            }
        });

        List<URL> unsetUrls = createUnsetInboxURLList();
        URLReaderTask urlReaderTask = new URLReaderTask();
        urlReaderTask.setListener(new TaskProgressListener<List<String>, Integer>() {
            @Override
            public void onPostExecute(List<String> inboxUnsetJson) {
                onInboxesUnset(inboxUnsetJson);
            }

            @Override
            public void onProgressUpdate(Integer... values) {
                setProgress("unsetting " + mailboxUsername[values[0]]);
            }
        });
        urlReaderTask.execute(unsetUrls.toArray(new URL[unsetUrls.size()]));
    }

    private List<URL> createUnsetInboxURLList() {
        List<URL> urls = new ArrayList<URL>();
        for (String username : mailboxUsername) {
            try {
                urls.add(new URL(UNSET_URI + username + TIME_PARAM + now()));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return urls;
    }

    private void onInboxesUnset(List<String> inboxUnsetJson) {
        List<URL> inboxUrls = new ArrayList<URL>();
        for (int i = 0; i < inboxUnsetJson.size(); i++) {
            String json = inboxUnsetJson.get(i);
            String address = getJsonString(json, ADDRESS_KEY);
            String username = mailboxUsername[i];
            try {
                inboxUrls.add(new URL(GRAB_URI + username + ADDRESS_PARAM + address + TIME_PARAM + now()));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        TaskProgressListener<List<String>, Integer> listener;
        final URLReaderTask urlReaderTask = new URLReaderTask();

        listener = new TaskProgressListener<List<String>, Integer>() {
            @Override
            public void onPostExecute(List<String> strings) {
                setProgress("");
            }

            @Override
            public void onProgressUpdate(Integer... values) {
                setProgress("reading inbox " + mailboxUsername[values[0]]);
                List<String> strings = urlReaderTask.getStrings();
                String mailboxJson = strings.get(strings.size() - 1);
                collectEmailSummary(mailboxJson);
                adapter.notifyDataSetChanged();
            }
        };

        urlReaderTask.setListener(listener);
        urlReaderTask.execute(inboxUrls.toArray(new URL[inboxUrls.size()]));
    }

    private void collectEmailSummaries(List<String> strings) {
        for (String json : strings) {
            collectEmailSummary(json);
        }
    }

    private void collectEmailSummary(String json) {
        JSONArray jsonArray = getJsonArray(json, MAILDIR_KEY);

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

                emails.add(email);
            }
        }
    }

    private JSONArray getJsonArray(String json, String key) {

        JSONObject obj = null;
        try {
            obj = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        assert obj != null;
        return getJsonArray(obj, key);
    }

    private JSONArray getJsonArray(JSONObject o, String key) {
        JSONArray jsonArray = null;
        try {
            jsonArray = o.getJSONArray(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    private String getJsonString(String json, String key) {
        JSONObject unsetObj = null;
        try {
            unsetObj = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        assert unsetObj != null;
        return getJsonString(unsetObj, key);
    }

    private String getJsonString(JSONObject o, String key) {
        String value = null;
        try {
            value = o.getString(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return value;
    }

    private Boolean getJsonBoolean(JSONObject o, String key) {
        Boolean value = null;
        try {
            value = o.getBoolean(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return value;
    }

    private Long getJsonLong(JSONObject o, String key) {
        Long value = null;
        try {
            value = o.getLong(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return value;
    }

    private void collectStringData(JSONObject o, Map<String, String> email, String key) {
        String from = getJsonString(o, key);
        email.put(key, from);
    }

    private void collectBooleanData(JSONObject o, Map<String, String> email, String key) {
        Boolean from = getJsonBoolean(o, key);
        email.put(key, from.toString());
    }

    private void collectLongData(JSONObject o, Map<String, String> email, String key) {
        Long from = getJsonLong(o, key);
        email.put(key, from.toString());
    }

    public static long now() {
        return System.currentTimeMillis();
    }

    private void debugJson(String json) {
        TextView view = (TextView) findViewById(R.id.debug);
        try {
            view.setText(new JSONObject(json).toString(1));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void debug(String text) {
        TextView view = (TextView) findViewById(R.id.debug);
        view.setText(text);
    }

    private void setProgress(String text) {
        TextView view = (TextView) findViewById(R.id.progress);
        view.setText(text);
    }
}

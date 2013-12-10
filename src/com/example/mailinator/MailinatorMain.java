package com.example.mailinator;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.view.ViewPager;
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

import java.util.*;

public class MailinatorMain extends FragmentActivity {
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

    private static List<String> userNames = Arrays.asList("0x1337b33f", "mailinator", "jfaust", "god");

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        InboxPagerAdapter inboxPagerAdapter = new InboxPagerAdapter(getSupportFragmentManager());

        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(inboxPagerAdapter);
    }

    public static class InboxPagerAdapter extends FragmentPagerAdapter {
        public InboxPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new InboxFragment();
            Bundle args = new Bundle();
            String value;
            try {
                value = userNames.get(i);
            } catch (Exception ignored) {
                return null;
            }
            args.putString(InboxFragment.USERNAME, value);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            // For this contrived example, we have a 100-object collection.
            return userNames.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return userNames.get(position);
        }
    }

    public static class InboxFragment extends Fragment {
        public static final String USERNAME = "username";
        private SimpleAdapter emailMessageAdapter;
        private Bundle args;
        private ArrayList<Map<String,String>> emailList;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            args = getArguments();
            String username = args.getString(USERNAME);

            List<String> useItUrls = new ArrayList<String>(
                    Arrays.asList(USE_IT_URI + username + TIME_PARAM + TimeUtil.now()));
            URLReaderTask urlReaderTask = new URLReaderTask();
            urlReaderTask.setListener(new TaskProgressListener<List<String>, Integer>() {
                @Override
                public void onPostExecute(List<String> inboxUseItJson) {
                    onInboxUseItEvent(inboxUseItJson);
                }

                @Override
                public void onProgressUpdate(Integer... values) {
                    //setProgress("using " + userNames.get(values[0]));
                }
            });
            urlReaderTask.execute(useItUrls.toArray(new String[useItUrls.size()]));

            View view = inflater.inflate(R.layout.inbox, container, false);
            emailMessageAdapter = createEmailAdapter(view);
            ((TextView) view.findViewById(R.id.inbox_title)).setText(args.getString(USERNAME));
            ListView emailListView = (ListView) view.findViewById(R.id.messages);
            createEmailListView(emailMessageAdapter, emailListView);
            return view;
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
            for (int i = 0; i < inboxJson.size(); i++) {
                String json = inboxJson.get(i);
                String address = JsonHelper.getJsonString(json, ADDRESS_KEY);
                String username = userNames.get(i);

                inboxUrls.add(GRAB_URI + username + ADDRESS_PARAM + address + TIME_PARAM + TimeUtil.now());
            }
            TaskProgressListener<List<String>, Integer> listener;
            final URLReaderTask inboxesRead = new URLReaderTask();

            listener = new TaskProgressListener<List<String>, Integer>() {
                @Override
                public void onPostExecute(List<String> strings) {
                }

                @Override
                public void onProgressUpdate(Integer... values) {
                    List<String> mailboxes = inboxesRead.getStrings();
                    String mailboxJson = mailboxes.get(mailboxes.size() - 1);
                    collectEmailSummary(mailboxJson);
                    emailMessageAdapter.notifyDataSetChanged();
                }
            };

            inboxesRead.setListener(listener);
            inboxesRead.execute(inboxUrls.toArray(new String[inboxUrls.size()]));
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

        private void createEmailListView(ListAdapter adapter, ListView emailListView) {
            emailListView.setAdapter(adapter);

            emailListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                @SuppressWarnings("unchecked")
                public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                    Map<String, String> email = (Map<String, String>) parent.getItemAtPosition(position);
                    Intent intent = new Intent(view.getContext(), EmailActivity.class);
                    intent.putExtra("email_subject", email.get(SUBJECT_KEY));
                    intent.putExtra("email_id", email.get(ID_KEY));
                    startActivity(intent);
                }
            });
        }
    }
}

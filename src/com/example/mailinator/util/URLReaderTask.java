package com.example.mailinator.util;

import android.os.AsyncTask;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class URLReaderTask extends AsyncTask<String, Integer, Long> {
    private List<String> strings = new Vector<String>();
    private DefaultHttpClient defaultHttpClient = new DefaultHttpClient();

    private TaskProgressListener<List<String>, Integer> listener;

    public void setListener(TaskProgressListener<List<String>, Integer> listener) {
        this.listener = listener;
    }

    public List<String> getStrings() {
        return strings;
    }

    @Override
    protected Long doInBackground(String... urls) {
        int i = 0;
        for (; i < urls.length; i++) {
            String url = urls[i];
            try {
                HttpResponse httpResponse = defaultHttpClient.execute(new HttpGet(url));
                HttpEntity entity = httpResponse.getEntity();

                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
                String line;
                StringBuilder buffer = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                strings.add(buffer.toString());
                publishProgress(i);
            } catch (IOException e) {
                e.printStackTrace();
                return 1L;
            }
        }
        return 0L;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        listener.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Long result) {
        if (result == 0L) {
            listener.onPostExecute(Collections.unmodifiableList(strings));
        }
    }
}

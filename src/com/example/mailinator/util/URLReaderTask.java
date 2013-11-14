package com.example.mailinator.util;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class URLReaderTask extends AsyncTask<URL, Integer, Long> {
    private List<String> strings = new Vector<String>();
    private TaskProgressListener<List<String>, Integer> listener;

    public URLReaderTask(TaskProgressListener<List<String>, Integer> listener) {
        this.listener = listener;
    }

    public List<String> getStrings() {
        return strings;
    }

    @Override
    protected Long doInBackground(URL... urls) {
        int i = 0;
        for (; i < urls.length; i++) {
            publishProgress(i);
            URL url = urls[i];
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String line;
                StringBuilder buffer = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                strings.add(buffer.toString());
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

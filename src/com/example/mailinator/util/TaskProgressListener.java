package com.example.mailinator.util;

public interface TaskProgressListener<T, U> {
    void onPostExecute(T t);
    void onProgressUpdate(U... values);
}

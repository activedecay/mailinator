package com.example.mailinator;

public class MailEntry {
    public final String title;
    public final String link;
    public final String summary;

    MailEntry(String title, String summary, String link) {
        this.title = title;
        this.summary = summary;
        this.link = link;
    }
}

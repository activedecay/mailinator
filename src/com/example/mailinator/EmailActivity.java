package com.example.mailinator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class EmailActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.email);

        Intent intent = getIntent();

        TextView textLabel = (TextView)findViewById(R.id.email_subject);

        String emailSubject = intent.getStringExtra("email_subject");
        String emailId = intent.getStringExtra("email_id");

        textLabel.setText(emailSubject);


    }
}

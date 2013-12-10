package com.example.mailinator;

import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.view.ViewPager;

import java.util.*;

public class MailinatorMain extends FragmentActivity {
    private static List<String> usernames = Arrays.asList("0x1337b33f", "mailinator", "jfaust", "god");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        InboxPagerAdapter inboxPagerAdapter = new InboxPagerAdapter(getSupportFragmentManager());

        // the ViewPager automatically sets up paging using the InboxPagerAdapter
        ((ViewPager) findViewById(R.id.pager)).setAdapter(inboxPagerAdapter);
    }

    public static class InboxPagerAdapter extends FragmentPagerAdapter {
        public InboxPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        /**
         * called when a new fragment needs to be constructed
         */
        @Override
        public Fragment getItem(int index) {
            InboxFragment inboxFragment = new InboxFragment();
            Bundle args = new Bundle();
            args.putString(InboxFragment.USERNAME_ARGS, usernames.get(index));
            inboxFragment.setArguments(args);
            return inboxFragment;
        }

        /**
         * the fixed count of fragments to create
         */
        @Override
        public int getCount() {
            return usernames.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return usernames.get(position);
        }
    }

}

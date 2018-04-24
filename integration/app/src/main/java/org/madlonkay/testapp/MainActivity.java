package org.madlonkay.testapp;

import android.os.Bundle;


public class MainActivity extends L10nActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.lib_name);
    }
}

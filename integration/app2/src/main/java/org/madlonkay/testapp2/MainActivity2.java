package org.madlonkay.testapp2;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;


public class MainActivity2 extends L10nFixActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d("L10nFixApp", "Initial locales: " + getResources().getConfiguration().getLocales().toLanguageTags());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.d("L10nFixApp", "Locales after loading WebView: " + getResources().getConfiguration().getLocales().toLanguageTags());
                L10nUtil.fixLocales(getResources());
            }
        }
    }
}

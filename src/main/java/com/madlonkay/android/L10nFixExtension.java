package com.madlonkay.android;

public class L10nFixExtension {
    public String defaultLocale;

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        System.out.println("Setting default language: " + defaultLocale);
        this.defaultLocale = defaultLocale;
    }
}

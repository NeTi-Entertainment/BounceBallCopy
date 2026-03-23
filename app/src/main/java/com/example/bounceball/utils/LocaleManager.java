package com.example.bounceball.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import java.util.Locale;

public class LocaleManager {

    public static void applyLocale(Context context) {
        GamePreferences prefs = new GamePreferences(context);
        String langCode = prefs.getLanguage();
        setLocale(context, langCode);
        Strings.load(context, langCode);
    }

    public static void setLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources     res    = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }

        res.updateConfiguration(config, res.getDisplayMetrics());
    }
}
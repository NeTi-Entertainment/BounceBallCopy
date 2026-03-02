package com.example.bounceball.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import java.util.Locale;

/**
 * Gère la langue de l'application.
 * Appeler LocaleManager.applyLocale(context) dans chaque Activity.onCreate()
 * après avoir défini la langue via GamePreferences.
 */
public class LocaleManager {

    /**
     * Applique la langue sauvegardée dans les préférences.
     * @param context L'activité ou le contexte de l'app.
     */
    public static void applyLocale(Context context) {
        GamePreferences prefs = new GamePreferences(context);
        String langCode = prefs.getLanguage();
        setLocale(context, langCode);
    }

    /**
     * Force une locale spécifique dans le contexte.
     */
    public static void setLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config);
        }

        res.updateConfiguration(config, res.getDisplayMetrics());
    }
}

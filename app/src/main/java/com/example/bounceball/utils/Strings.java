package com.example.bounceball.utils;

import android.content.Context;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Singleton de localisation basé sur des fichiers JSON dans assets/lang/.
 *
 * USAGE :
 *   Strings.get("main.tap_to_play")
 *   Strings.fmt("main.record_fmt", 42.5f)
 *
 * INITIALISATION (dans LocaleManager.applyLocale ou Application.onCreate) :
 *   Strings.load(context, "en");
 *
 * CLÉS : notation pointée suivant la hiérarchie du JSON.
 *   "shop.upgrades.upg_air.name"
 *   "common.btn_back"
 *
 * FALLBACK : si une clé est absente dans la langue courante,
 *   on cherche dans l'anglais. Si absente aussi, on retourne la clé brute.
 */
public class Strings {

    private static JSONObject sCurrent  = null;
    private static JSONObject sFallback = null;
    private static String     sLang     = "";

    private Strings() {}

    public static void load(Context context, String langCode) {
        if (langCode.equals(sLang) && sCurrent != null) return;
        sLang    = langCode;
        sCurrent = parse(context, langCode);
        sFallback = "en".equals(langCode) ? null : parse(context, "en");
    }

    public static String get(String key) {
        String v = resolve(sCurrent, key);
        if (v == null) v = resolve(sFallback, key);
        return v != null ? v : key;
    }

    public static String fmt(String key, Object... args) {
        try {
            return String.format(get(key), args);
        } catch (Exception e) {
            return get(key);
        }
    }

    public static String currentLang() { return sLang; }

    private static String resolve(JSONObject root, String key) {
        if (root == null || key == null) return null;
        String[] parts = key.split("\\.");
        try {
            JSONObject node = root;
            for (int i = 0; i < parts.length - 1; i++) {
                node = node.getJSONObject(parts[i]);
            }
            return node.getString(parts[parts.length - 1]);
        } catch (Exception e) {
            return null;
        }
    }

    private static JSONObject parse(Context context, String langCode) {
        try {
            InputStream is  = context.getAssets().open("lang/" + langCode + ".json");
            byte[]      buf = new byte[is.available()];
            //noinspection ResultOfMethodCallIgnored
            is.read(buf);
            is.close();
            return new JSONObject(new String(buf, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}
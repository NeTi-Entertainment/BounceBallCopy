package com.example.bounceball;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.*;
import com.example.bounceball.utils.GamePreferences;
import com.example.bounceball.utils.ImmersiveHelper;
import com.example.bounceball.utils.LocaleManager;
import com.example.bounceball.utils.Strings;

public class SettingsActivity extends Activity {

    // ── AJOUTER UNE LANGUE ──────────────────────────────────────────────
    // 1. Ajouter une ligne {"code", "Nom natif"} dans ce tableau
    // 2. Déposer assets/lang/<code>.json
    // C'est tout — le menu se met à jour automatiquement.
    public static final String[][] LANGUAGES = {

            // Langues mondiales / marchés gaming majeurs
            {"en", "English"},
            {"zh", "中文(简体)"},
            {"zh_tw", "中文(繁體)"},
            {"es", "Español"},
            {"hi", "हिन्दी"},
            {"ar", "العربية"},
            {"pt", "Português"},
            {"fr", "Français"},
            {"ru", "Русский"},
            {"de", "Deutsch"},
            {"ja", "日本語"},
            {"ko", "한국어"},

            // Grands marchés régionaux
            {"id", "Bahasa Indonesia"},
            {"vi", "Tiếng Việt"},
            {"th", "ภาษาไทย"},
            {"ms", "Bahasa Melayu"},
            {"tr", "Türkçe"},
            {"it", "Italiano"},
            {"nl", "Nederlands"},
            {"pl", "Polski"},
            {"uk", "Українська"},
            {"bn", "বাংলা"},
            {"fa", "فارسی"},

            // Europe secondaire
            {"sv", "Svenska"},
            {"no", "Norsk"},
            {"da", "Dansk"},
            {"fi", "Suomi"},
            {"cs", "Čeština"},
            {"ro", "Română"},
            {"hu", "Magyar"},
            {"el", "Ελληνικά"},
            {"he", "עברית"},
            {"sk", "Slovenčina"},
            {"bg", "Български"},
            {"sr", "Srpski"},
            {"hr", "Hrvatski"},
            {"ca", "Català"},
            {"lt", "Lietuvių"},
            {"sq", "Shqip"},
            {"is", "Íslenska"},

            // Asie du Sud / du Sud-Est secondaire
            {"ta", "தமிழ்"},
            {"te", "తెలుగు"},
            {"mr", "मराठी"},
            {"gu", "ગુજરાતી"},
            {"pa", "ਪੰਜਾਬੀ"},
            {"ml", "മലയാളം"},
            {"kn", "ಕನ್ನಡ"},
            {"ur", "اردو"},
            {"my", "မြန်မာဘာသာ"},
            {"km", "ភាសាខ្មែរ"},
            {"lo", "ພາສາລາວ"},
            {"si", "සිංහල"},
            {"ne", "नेपाली"},
            {"mn", "Монгол"},
            {"tl", "Filipino"},

            // Afrique & Asie centrale
            {"sw", "Kiswahili"},
            {"ha", "Hausa"},
            {"am", "አማርኛ"},
            {"yo", "Yorùbá"},
            {"ig", "Igbo"},
            {"af", "Afrikaans"},
            {"uz", "O'zbek"},
            {"kk", "Қазақша"},
            {"az", "Azərbaycan"},
            {"ps", "پښتو"},
    };
    // ────────────────────────────────────────────────────────────────────

    private GamePreferences prefs;
    private TextView langValueTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleManager.applyLocale(this);

        prefs = new GamePreferences(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#0D1B2A"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 80, 40, 60);
        root.setGravity(Gravity.TOP);

        TextView title = new TextView(this);
        title.setText(Strings.get("settings.title"));
        title.setTextColor(Color.parseColor("#FFD700"));
        title.setTextSize(28f);
        title.setPadding(0, 0, 0, 30);
        root.addView(title);

        // ── Son ──────────────────────────────────────────────────────────
        addSectionHeader(root, Strings.get("settings.section_sound"));

        LinearLayout soundRow = new LinearLayout(this);
        soundRow.setOrientation(LinearLayout.HORIZONTAL);
        soundRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView soundLabel = new TextView(this);
        soundLabel.setText(Strings.get("settings.label_sfx"));
        soundLabel.setTextColor(Color.WHITE);
        soundLabel.setTextSize(18f);
        soundLabel.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch soundSwitch = new Switch(this);
        soundSwitch.setChecked(prefs.isSoundEnabled());
        soundSwitch.setOnCheckedChangeListener((btn, checked) ->
                prefs.setSoundEnabled(checked));

        soundRow.addView(soundLabel);
        soundRow.addView(soundSwitch);
        root.addView(soundRow);

        addDivider(root);

        // ── Langue ───────────────────────────────────────────────────────
        addSectionHeader(root, Strings.get("settings.section_language"));
        root.addView(buildLanguageDropdown());

        addDivider(root);

        // ── Retour ───────────────────────────────────────────────────────
        Button backBtn = new Button(this);
        backBtn.setText(Strings.get("common.btn_back"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setBackgroundColor(Color.parseColor("#1B5E20"));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, 40, 0, 0);
        backBtn.setLayoutParams(btnLp);
        backBtn.setOnClickListener(v -> finish());
        root.addView(backBtn);

        scroll.addView(root);
        setContentView(scroll);
    }

    // ── Menu déroulant langue ─────────────────────────────────────────────
    private android.view.View buildLanguageDropdown() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(24, 18, 24, 18);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1A2A3A"));
        bg.setCornerRadius(12f);
        bg.setStroke(2, Color.parseColor("#2A3A4A"));
        row.setBackground(bg);

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 8, 0, 0);
        row.setLayoutParams(rowLp);

        langValueTv = new TextView(this);
        langValueTv.setText(nativeNameFor(prefs.getLanguage()));
        langValueTv.setTextColor(Color.WHITE);
        langValueTv.setTextSize(16f);
        langValueTv.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = new TextView(this);
        arrow.setText("▾");
        arrow.setTextColor(Color.parseColor("#80DEEA"));
        arrow.setTextSize(18f);

        row.addView(langValueTv);
        row.addView(arrow);
        row.setOnClickListener(v -> showLanguagePicker());
        return row;
    }

    private void showLanguagePicker() {
        String currentCode = prefs.getLanguage();

        String[] names = new String[LANGUAGES.length];
        int checkedIndex = 0;
        for (int i = 0; i < LANGUAGES.length; i++) {
            names[i] = LANGUAGES[i][1];
            if (LANGUAGES[i][0].equals(currentCode)) checkedIndex = i;
        }

        new AlertDialog.Builder(this)
                .setTitle(Strings.get("settings.section_language"))
                .setSingleChoiceItems(names, checkedIndex, (dialog, which) -> {
                    String selectedCode = LANGUAGES[which][0];
                    dialog.dismiss();
                    if (!selectedCode.equals(prefs.getLanguage())) {
                        prefs.setLanguage(selectedCode);
                        recreate();
                    }
                })
                .show();
    }

    private String nativeNameFor(String code) {
        for (String[] lang : LANGUAGES) {
            if (lang[0].equals(code)) return lang[1];
        }
        return code;
    }

    // ── Helpers UI ────────────────────────────────────────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        ImmersiveHelper.enable(getWindow());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) ImmersiveHelper.enable(getWindow());
    }

    private void addSectionHeader(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#80DEEA"));
        tv.setTextSize(16f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 20, 0, 6);
        tv.setLayoutParams(lp);
        parent.addView(tv);
    }

    private void addDivider(LinearLayout parent) {
        android.view.View divider = new android.view.View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, 20, 0, 10);
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(Color.parseColor("#2A3A4A"));
        parent.addView(divider);
    }
}
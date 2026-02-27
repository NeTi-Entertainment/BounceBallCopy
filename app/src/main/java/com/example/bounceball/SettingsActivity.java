package com.example.bounceball;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.example.bounceball.utils.GamePreferences;
import com.example.bounceball.utils.LocaleManager;

public class SettingsActivity extends Activity {

    private GamePreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Applique la langue sauvegardée
        LocaleManager.applyLocale(this);

        prefs = new GamePreferences(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#0D1B2A"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 80, 40, 60);
        root.setGravity(Gravity.TOP);

        // ── Titre ──
        TextView title = new TextView(this);
        title.setText(getString(R.string.settings_title));
        title.setTextColor(Color.parseColor("#FFD700"));
        title.setTextSize(28f);
        title.setPadding(0, 0, 0, 30);
        root.addView(title);

        // ══════════════════════════════════════
        // SECTION SON
        // ══════════════════════════════════════
        addSectionHeader(root, getString(R.string.settings_sound));

        LinearLayout soundRow = new LinearLayout(this);
        soundRow.setOrientation(LinearLayout.HORIZONTAL);
        soundRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView soundLabel = new TextView(this);
        soundLabel.setText(getString(R.string.settings_sound_toggle));
        soundLabel.setTextColor(Color.WHITE);
        soundLabel.setTextSize(18f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        soundLabel.setLayoutParams(lp);

        Switch soundSwitch = new Switch(this);
        soundSwitch.setChecked(prefs.isSoundEnabled());
        soundSwitch.setOnCheckedChangeListener((btn, checked) -> {
            prefs.setSoundEnabled(checked);
        });

        soundRow.addView(soundLabel);
        soundRow.addView(soundSwitch);
        root.addView(soundRow);

        addDivider(root);

        // ══════════════════════════════════════
        // SECTION LANGUE
        // ══════════════════════════════════════
        addSectionHeader(root, getString(R.string.settings_language));

        String[] langNames = {"Français", "English", "Español", "Deutsch", "日本語"};
        String[] langCodes = {"fr", "en", "es", "de", "ja"};

        String currentLang = prefs.getLanguage();

        RadioGroup langGroup = new RadioGroup(this);
        langGroup.setOrientation(RadioGroup.VERTICAL);

        for (int i = 0; i < langNames.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(langNames[i]);
            rb.setTextColor(Color.WHITE);
            rb.setTextSize(16f);
            rb.setTag(langCodes[i]);
            rb.setPadding(10, 8, 10, 8);
            if (langCodes[i].equals(currentLang)) {
                rb.setChecked(true);
            }
            langGroup.addView(rb);
        }

        langGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selected = group.findViewById(checkedId);
            if (selected != null) {
                String code = (String) selected.getTag();
                prefs.setLanguage(code);
                LocaleManager.applyLocale(this);
                // Redémarre l'activité pour appliquer la nouvelle langue
                recreate();
            }
        });

        root.addView(langGroup);
        addDivider(root);

        // ══════════════════════════════════════
        // BOUTON RETOUR
        // ══════════════════════════════════════
        Button backBtn = new Button(this);
        backBtn.setText(getString(R.string.settings_back));
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

package com.example.bounceball;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.example.bounceball.utils.GamePreferences;

/**
 * Construit le contenu scrollable de l'onglet Cosmétiques.
 * Appeler CosmeticsPage.build(context, prefs) pour obtenir la ScrollView.
 *
 * Clés SharedPreferences utilisées :
 *   "owned_<id>"    boolean  — cosmétique débloqué
 *   "equipped_ball" String   — id de la balle équipée
 *   "equipped_fx"   String   — id de l'effet équipé
 *   "equipped_bg"   String   — id du background équipé
 */
public class CosmeticsPage {

// {id, nom, couleur preview, gold, diamants, cat_stats, sous_cat}
// cat_stats : "classic" | "metal" | "space" | "sport" | "elemental"
// sous_cat  : label affiché en en-tête de section dans le shop
    private static final Object[][] BALLS = {

            // ── CLASSIC ────────────────────────────────────────
            {"ball_red",       "Rouge",      "#E82020", 0,  0, "classic", "Classique"},
            {"ball_blue",      "Bleu",       "#2050E8", 0,  0, "classic", "Classique"},
            {"ball_yellow",    "Jaune",      "#F5D800", 0,  0, "classic", "Classique"},
            {"ball_green",     "Vert",       "#20B020", 0,  0, "classic", "Classique"},
            {"ball_orange",    "Orange",     "#F07010", 50, 0, "classic", "Classique"},
            {"ball_pink",      "Rose",       "#F060A0", 50, 0, "classic", "Classique"},
            {"ball_purple",    "Violet",     "#8030D0", 50, 0, "classic", "Classique"},
            {"ball_cyan",      "Cyan",       "#00C8D8", 50, 0, "classic", "Classique"},
            {"ball_lime",      "Lime",       "#80E000", 50, 0, "classic", "Classique"},
            {"ball_brown",     "Marron",     "#8B4513", 50, 0, "classic", "Classique"},
            {"ball_beige",     "Beige",      "#F5DEB3", 50, 0, "classic", "Classique"},
            {"ball_white",     "Blanc",      "#F5F5F5", 80, 0, "classic", "Classique"},
            {"ball_black",     "Noir",       "#1A1A1A", 80, 0, "classic", "Classique"},
            {"ball_lightgray", "Gris clair", "#C0C0C0", 80, 0, "classic", "Classique"},
            {"ball_darkgray",  "Gris foncé", "#505050", 80, 0, "classic", "Classique"},

            // ── SPORT ──────────────────────────────────────────
            {"ball_soccer",   "Ballon Foot",      "#F5F5F5", 450,  15, "sport", "Sport"},
            {"ball_basket",   "Ballon Basket",    "#E65100", 450,  15, "sport", "Sport"},
            {"ball_tennis",   "Balle Tennis",     "#CDDC39", 450,  15, "sport", "Sport"},
            {"ball_bowling",  "Boule Bowling",    "#311B92", 600,  22, "sport", "Sport"},
            {"ball_petanque", "Pétanque",         "#A0A8B0", 1, 12,    "sport", "Sport"},
            {"ball_golf",     "Golf",             "#F5F5F5", 1, 10,    "sport", "Sport"},
            {"ball_cateye",   "Œil de Chat",      "#44DDBB", 1, 16,    "sport", "Sport"},
            {"ball_beach",    "Plage",            "#FFD700", 1, 8,     "sport", "Sport"},
            {"ball_volleyball", "Volleyball",     "#F5E6C8", 1, 9,     "sport", "Sport"},
        {"ball_baseball", "Baseball", "#F5EED8", 1, 9, "sport", "Sport"},
        {"ball_8ball", "Billard 8", "#111111", 1, 10, "sport", "Sport"},

            // ── METAL ──────────────────────────────────────────
            {"ball_lead",      "Balle Plomb",      "#707880", 1,  18, "metal", "Metal"},
            {"ball_nickel",    "Nickel",           "#C8B87A", 1,  9, "metal",  "Metal"},
            {"ball_copper",    "Balle Cuivrée",    "#BF6830", 1,  22, "metal", "Metal"},
            {"ball_chrome",    "Balle Chrome",     "#90CAF9", 1,  35, "metal", "Metal"},
            {"ball_bronze",    "Bronze",           "#CD7F32", 1, 10, "metal",  "Metal"},
            {"ball_steel",     "Acier",            "#6B7FA8", 1, 10, "metal",  "Metal"},
            {"ball_silver",    "Balle Argentée",   "#B0BEC5", 1,  22, "metal", "Metal"},
            {"ball_gold",      "Balle Dorée",      "#FFD700", 1,  30, "metal", "Metal"},
            {"ball_rosegold",  "Or Rose",          "#E8A090", 1, 14, "metal",  "Metal"},
            {"ball_titanium",  "Titane",           "#5B6B7C", 1, 12, "metal",  "Metal"},
            {"ball_platinum",  "Balle Platine",    "#D8DCE0", 1, 45, "metal",  "Metal"},
            {"ball_bismuth",   "Bismuth",          "#C8A0C0", 1, 22, "metal",  "Metal"},
            {"ball_damascus",  "Damas",            "#4A4A4A", 1, 26, "metal",  "Metal"},
            {"ball_meteorite", "Météorite",        "#7A7060", 1, 24, "metal",  "Metal"},

            // ── SPACE ──────────────────────────────────────────
            {"ball_void",     "Balle Néant",      "#212121", 1,  35, "space",   "Space"},
            {"ball_nebula",   "Balle Nébuleuse",  "#7B1FA2", 1,  28, "space",   "Space"},
            {"ball_comet",    "Balle Comète",     "#A0825A", 1,  26, "space",   "Space"},
            {"ball_mercury", "Mercure",           "#9E9585", 1, 18, "space",    "Space"},
            {"ball_venus",   "Vénus",             "#E8D5A3", 1, 20, "space",    "Space"},
            {"ball_earth",   "Terre",             "#1A6FA8", 1, 20, "space",    "Space"},
            {"ball_moon",    "Lune",              "#B8B8B8", 1, 18, "space",    "Space"},
            {"ball_mars",   "Mars",               "#C1440E", 1, 25, "space",    "Space"},
            {"ball_jupiter",   "Jupiter",         "#C88B3A", 1, 25, "space",    "Space"},
            {"ball_saturn",   "Saturn",           "#C8A96E", 1, 30, "space",    "Space"},
            {"ball_uranus",   "Uranus",           "#7DE8E8", 1, 30, "space",    "Space"},
            {"ball_neptune",   "Neptune",         "#2A5FD4", 1, 40, "space",    "Space"},
            {"ball_pluto",   "Pluton",            "#C4A882", 1, 40, "space",    "Space"},
            {"ball_red_dwarf",    "Naine Rouge",  "#CC2200", 1, 18, "space",    "Space"},
            {"ball_yellow_dwarf", "Naine Jaune",  "#FFD700", 1, 18, "space",    "Space"},
            {"ball_blue_giant",   "Géante Bleue", "#4488FF", 1, 22, "space",    "Space"},
            {"ball_black_hole", "Trou Noir",      "#0A0010", 1, 40, "space",    "Space"},
            {"ball_pulsar", "Pulsar",             "#4488FF", 1, 44, "space",    "Space"},

            // ── ELEMENTAL ──────────────────────────────────────
            {"ball_elem_fire",  "Feu",   "#FF4400", 1, 18, "elemental", "Élémental"},
            {"ball_elem_water", "Eau",   "#0088CC", 1, 18, "elemental", "Élémental"},
            {"ball_elem_earth", "Terre", "#6B4226", 1, 18, "elemental", "Élémental"},
            {"ball_elem_ice",   "Glace", "#A8D8EA", 1, 18, "elemental", "Élémental"},
            {"ball_elem_darkness", "Ténèbres", "#050505", 1, 18, "elemental", "Élémental"},
            {"ball_elem_light", "Lumière", "#FFFFEE", 1, 18, "elemental", "Élémental"},
            {"ball_elem_air", "Air", "#E0F7FA", 1, 18, "elemental", "Élémental"},
            {"ball_elem_lightning", "Foudre", "#FFF176", 1, 18, "elemental", "Élémental"},
            {"ball_elem_plasma", "Plasma", "#E040FB", 1, 18, "elemental", "Élémental"},
            {"ball_elem_lava", "Lave", "#FF3300", 1, 18, "elemental", "Élémental"},
    };

    // ── Données Effets ─────────────────────────────────
    // {id, nom, couleur preview, gold, diamants}
    private static final Object[][] EFFECTS = {
            {"fx_none",     "Pas d'effet",       "#CCCCCC", 0,   0  },
            {"fx_fire",     "Traînée Feu",       "#FF5722", 600, 25 },
            {"fx_ice",      "Traînée Glace",     "#80DEEA", 600, 25 },
            {"fx_rainbow",  "Arc-en-ciel",       "#9C27B0", 900, 35 },
            {"fx_stars",    "Étoiles",           "#FFD700", 700, 28 },
    };

    // ── Données Backgrounds ────────────────────────────
    // {id, nom, couleur preview, gold, diamants}
    private static final Object[][] BACKGROUNDS = {
            {"bg_white",  "Fond Blanc",       "#FFFFFF", 0,   0  },
            {"bg_night",  "Ciel Nocturne",    "#0D1B2A", 400, 18 },
            {"bg_sunset", "Coucher de Soleil","#FF7043", 500, 20 },
            {"bg_forest", "Forêt",           "#2E7D32", 400, 18 },
            {"bg_space",  "Espace",          "#1A0A2E", 800, 30 },
    };

    // Clés equipped par défaut
    private static final String DEFAULT_BALL = "ball_basic";
    private static final String DEFAULT_FX   = "fx_none";
    private static final String DEFAULT_BG   = "bg_white";

    // ──────────────────────────────────────────────────

    public static ScrollView build(Context ctx, GamePreferences prefs) {
        SharedPreferences raw = prefs.getRaw();

        // Initialise les défauts si première ouverture
        if (!raw.contains("equipped_ball")) {
            raw.edit()
                    .putString("equipped_ball", DEFAULT_BALL)
                    .putString("equipped_fx",   DEFAULT_FX)
                    .putString("equipped_bg",   DEFAULT_BG)
                    .putBoolean("owned_" + DEFAULT_BALL, true)
                    .putBoolean("owned_" + DEFAULT_FX,   true)
                    .putBoolean("owned_" + DEFAULT_BG,   true)
                    .apply();
        }

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout page = new LinearLayout(ctx);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(px(ctx, 12), px(ctx, 8), px(ctx, 12), px(ctx, 24));

        // Affichage devises en haut
        LinearLayout currRow = new LinearLayout(ctx);
        currRow.setOrientation(LinearLayout.HORIZONTAL);
        currRow.setPadding(px(ctx, 4), px(ctx, 6), px(ctx, 4), px(ctx, 10));
        TextView goldTv = new TextView(ctx);
        goldTv.setTextColor(Color.parseColor("#FFD700"));
        goldTv.setTextSize(14f);
        TextView diamTv = new TextView(ctx);
        diamTv.setTextColor(Color.parseColor("#80DEEA"));
        diamTv.setTextSize(14f);
        diamTv.setPadding(px(ctx, 20), 0, 0, 0);
        currRow.addView(goldTv);
        currRow.addView(diamTv);
        page.addView(currRow);

        Runnable refreshCurr = () -> {
            goldTv.setText("⬡ " + prefs.getGold() + " Or");
            diamTv.setText("◆ " + prefs.getDiamonds() + " Diam");
        };
        refreshCurr.run();
        scroll.setTag(R.id.tag_refresh, refreshCurr);

        // ── Section Balles ──
        addSectionHeader(ctx, page, "Balles");
        String lastSubCat = "";
        for (Object[] ball : BALLS) {
            String subCat = (String) ball[6];
            if (!subCat.equals(lastSubCat)) {
                addSubSectionHeader(ctx, page, subCat);
                lastSubCat = subCat;
            }
            page.addView(buildCosmeticRow(ctx, prefs, raw, ball, "equipped_ball", refreshCurr));
        }

        // ── Section Effets ──
        addSectionHeader(ctx, page, "Effets");
        for (Object[] fx : EFFECTS) {
            page.addView(buildCosmeticRow(ctx, prefs, raw, fx, "equipped_fx", refreshCurr));
        }

        // ── Section Backgrounds ──
        addSectionHeader(ctx, page, "Backgrounds");
        for (Object[] bg : BACKGROUNDS) {
            page.addView(buildCosmeticRow(ctx, prefs, raw, bg, "equipped_bg", refreshCurr));
        }

        scroll.addView(page);
        return scroll;
    }

    // ──────────────────────────────────────────────────
    // ROW
    // ──────────────────────────────────────────────────
    private static View buildCosmeticRow(Context ctx, GamePreferences prefs,
                                         SharedPreferences raw, Object[] data,
                                         String equippedKey, Runnable refreshCurr) {
        String id       = (String) data[0];
        String name     = (String) data[1];
        String colorHex = (String) data[2];
        int goldCost    = (int)    data[3];
        int diamCost    = (int)    data[4];
        boolean isFree  = goldCost == 0 && diamCost == 0;

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(px(ctx, 12), px(ctx, 10), px(ctx, 12), px(ctx, 10));
        row.setBackgroundColor(Color.parseColor("#1A2A3A"));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, px(ctx, 6), 0, 0);
        row.setLayoutParams(rowLp);

        // Preview (cercle coloré 48×48)
        View preview = new View(ctx) {
            private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                int c = Color.parseColor(colorHex);
                p.setColor(c);
                float r = getWidth() / 2f;
                canvas.drawCircle(r, r, r, p);
                // Reflet blanc
                p.setColor(Color.WHITE);
                p.setAlpha(80);
                canvas.drawCircle(r * 0.65f, r * 0.35f, r * 0.3f, p);
            }
        };
        int previewSize = px(ctx, 48);
        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(previewSize, previewSize);
        prevLp.setMargins(0, 0, px(ctx, 12), 0);
        preview.setLayoutParams(prevLp);
        row.addView(preview);

        // Colonne texte + bouton
        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView nameView = new TextView(ctx);
        nameView.setText(name);
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(15f);
        textCol.addView(nameView);

        // Boutons (état recalculé à chaque refresh)
        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, px(ctx, 4), 0, 0);

        // On stocke les refs pour le refresh
        Button[] btns = { new Button(ctx), new Button(ctx) };
        Button actionBtn = btns[0];
        Button diam2Btn  = btns[1];

        // Style commun
        styleSmallBtn(actionBtn);
        styleSmallBtn(diam2Btn);
        LinearLayout.LayoutParams d2Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        d2Lp.setMargins(px(ctx, 8), 0, 0, 0);
        diam2Btn.setLayoutParams(d2Lp);

        btnRow.addView(actionBtn);
        btnRow.addView(diam2Btn);
        textCol.addView(btnRow);
        row.addView(textCol);

        // Logique refresh état
        Runnable[] r = {null};

        r[0] = () -> {
            boolean owned    = raw.getBoolean("owned_" + id, isFree);
            boolean equipped = id.equals(raw.getString(equippedKey, ""));

            if (equipped) {
                actionBtn.setText("Équipée");
                actionBtn.setTextColor(Color.parseColor("#00E676"));
                actionBtn.setBackgroundColor(Color.parseColor("#0A2A0A"));
                actionBtn.setEnabled(false);
                diam2Btn.setVisibility(View.GONE);
            } else if (owned) {
                actionBtn.setText("Équiper");
                actionBtn.setTextColor(Color.WHITE);
                actionBtn.setBackgroundColor(Color.parseColor("#1B3A5A"));
                actionBtn.setEnabled(true);
                diam2Btn.setVisibility(View.GONE);
                actionBtn.setOnClickListener(v -> {
                    raw.edit().putString(equippedKey, id).apply();
                    View page = (View) row.getParent();
                    if (page instanceof LinearLayout) {
                        LinearLayout ll = (LinearLayout) page;
                        for (int i = 0; i < ll.getChildCount(); i++) {
                            View child = ll.getChildAt(i);
                            if (child.getTag() != null && child.getTag().equals(equippedKey)) {
                                Object tag2 = child.getTag(R.id.tag_refresh);
                                if (tag2 instanceof Runnable) ((Runnable) tag2).run();
                            }
                        }
                    }
                });
            } else {
                actionBtn.setText("⬡ " + goldCost + " Or");
                actionBtn.setTextColor(Color.parseColor("#FFD700"));
                actionBtn.setBackgroundColor(Color.parseColor("#1B3A1B"));
                actionBtn.setEnabled(prefs.getGold() >= goldCost);
                diam2Btn.setText("◆ " + diamCost + " Diam");
                diam2Btn.setTextColor(Color.parseColor("#80DEEA"));
                diam2Btn.setBackgroundColor(Color.parseColor("#1A1A3A"));
                diam2Btn.setEnabled(prefs.getDiamonds() >= diamCost);
                diam2Btn.setVisibility(View.VISIBLE);
                actionBtn.setOnClickListener(v -> {
                    if (prefs.getGold() >= goldCost) {
                        prefs.spendGold(goldCost);
                        raw.edit().putBoolean("owned_" + id, true)
                                .putString(equippedKey, id).apply();
                        View pageView = (View) row.getParent();
                        if (pageView instanceof LinearLayout) {
                            LinearLayout ll = (LinearLayout) pageView;
                            for (int i = 0; i < ll.getChildCount(); i++) {
                                View child = ll.getChildAt(i);
                                if (child.getTag() != null && child.getTag().equals(equippedKey)) {
                                    Object tag2 = child.getTag(R.id.tag_refresh);
                                    if (tag2 instanceof Runnable) ((Runnable) tag2).run();
                                }
                            }
                        }
                        refreshCurr.run();
                    }
                });
                diam2Btn.setOnClickListener(v -> {
                    if (prefs.getDiamonds() >= diamCost) {
                        prefs.spendDiamonds(diamCost);
                        raw.edit().putBoolean("owned_" + id, true)
                                .putString(equippedKey, id).apply();
                        View pageView = (View) row.getParent();
                        if (pageView instanceof LinearLayout) {
                            LinearLayout ll = (LinearLayout) pageView;
                            for (int i = 0; i < ll.getChildCount(); i++) {
                                View child = ll.getChildAt(i);
                                if (child.getTag() != null && child.getTag().equals(equippedKey)) {
                                    Object tag2 = child.getTag(R.id.tag_refresh);
                                    if (tag2 instanceof Runnable) ((Runnable) tag2).run();
                                }
                            }
                        }
                        refreshCurr.run();
                    }
                });
            }
        };

        row.setTag(equippedKey);
        row.setTag(R.id.tag_refresh, r[0]);
        r[0].run();
        return row;
    }

    // ──────────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────────

    private static void addSectionHeader(Context ctx, LinearLayout parent, String label) {
        // Ligne séparatrice avec titre
        LinearLayout header = new LinearLayout(ctx);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hLp.setMargins(0, px(ctx, 18), 0, px(ctx, 2));
        header.setLayoutParams(hLp);

        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextColor(Color.parseColor("#80DEEA"));
        tv.setTextSize(17f);
        tv.setPadding(0, 0, px(ctx, 10), 0);
        header.addView(tv);

        View line = new View(ctx);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(0, px(ctx, 1), 1f);
        lineLp.gravity = Gravity.CENTER_VERTICAL;
        line.setLayoutParams(lineLp);
        line.setBackgroundColor(Color.parseColor("#2A3A4A"));
        header.addView(line);

        parent.addView(header);
    }

    private static void styleSmallBtn(Button btn) {
        btn.setTextSize(11f);
        btn.setPadding(px(btn.getContext(), 10), px(btn.getContext(), 4),
                px(btn.getContext(), 10), px(btn.getContext(), 4));
    }

    private static int px(Context ctx, int dp) {
        return (int) (dp * ctx.getResources().getDisplayMetrics().density);
    }

    public static void refreshAll(ScrollView scroll) {
        Object currRefresh = scroll.getTag(R.id.tag_refresh);
        if (currRefresh instanceof Runnable) ((Runnable) currRefresh).run();
        LinearLayout page = (LinearLayout) scroll.getChildAt(0);
        for (int i = 0; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            Object r = child.getTag(R.id.tag_refresh);
            if (r instanceof Runnable) ((Runnable) r).run();
        }
    }

    private static void addSubSectionHeader(Context ctx, LinearLayout parent, String label) {
        TextView tv = new TextView(ctx);
        tv.setText("— " + label + " —");
        tv.setTextColor(Color.parseColor("#607D8B"));
        tv.setTextSize(12f);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, px(ctx, 12), 0, px(ctx, 2));
        tv.setLayoutParams(lp);
        parent.addView(tv);
    }
}
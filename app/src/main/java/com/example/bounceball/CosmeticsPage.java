package com.example.bounceball;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.example.bounceball.utils.GamePreferences;
import com.example.bounceball.utils.Strings;
import java.util.ArrayList;
import java.util.List;

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

            // ── DEFAULT ────────────────────────────────────────
            {"ball_basic",     "Balle Basique", "#F5EDD0", 0, 0, "classic", "default", null},

            // ── CLASSIC ────────────────────────────────────────
            {"ball_red",       "Rouge",      "#E82020", 0,  0, "classic", "classic", null},
            {"ball_blue",      "Bleu",       "#2050E8", 0,  0, "classic", "classic", null},
            {"ball_yellow",    "Jaune",      "#F5D800", 0,  0, "classic", "classic", null},
            {"ball_green",     "Vert",       "#20B020", 0,  0, "classic", "classic", null},
            {"ball_orange",    "Orange",     "#F07010", 50, 0, "classic", "classic", null},
            {"ball_pink",      "Rose",       "#F060A0", 50, 0, "classic", "classic", null},
            {"ball_purple",    "Violet",     "#8030D0", 50, 0, "classic", "classic", null},
            {"ball_cyan",      "Cyan",       "#00C8D8", 50, 0, "classic", "classic", null},
            {"ball_lime",      "Lime",       "#80E000", 50, 0, "classic", "classic", null},
            {"ball_brown",     "Marron",     "#8B4513", 50, 0, "classic", "classic", null},
            {"ball_beige",     "Beige",      "#F5DEB3", 50, 0, "classic", "classic", null},
            {"ball_white",     "Blanc",      "#F5F5F5", 80, 0, "classic", "classic", null},
            {"ball_black",     "Noir",       "#1A1A1A", 80, 0, "classic", "classic", null},
            {"ball_lightgray", "Gris clair", "#C0C0C0", 80, 0, "classic", "classic", null},
            {"ball_darkgray",  "Gris foncé", "#505050", 80, 0, "classic", "classic", null},

            // ── SPORT ──────────────────────────────────────────
            {"ball_soccer",    "Ballon Foot",    "#F5F5F5", 450, 15, "sport", "sport", "common"},
            {"ball_basket",    "Ballon Basket",  "#E65100", 450, 15, "sport", "sport", "common"},
            {"ball_tennis",    "Balle Tennis",   "#CDDC39", 450, 15, "sport", "sport", "common"},
            {"ball_bowling",   "Boule Bowling",  "#311B92", 600, 22, "sport", "sport", "rare"},
            {"ball_petanque",  "Pétanque",       "#A0A8B0",   1, 12, "sport", "sport", "common"},
            {"ball_golf",      "Golf",           "#F5F5F5",   1, 10, "sport", "sport", "common"},
            {"ball_cateye",    "Œil de Chat",    "#44DDBB",   1, 16, "sport", "sport", "rare"},
            {"ball_beach",     "Plage",          "#FFD700",   1,  8, "sport", "sport", "common"},
            {"ball_volleyball","Volleyball",     "#F5E6C8",   1,  9, "sport", "sport", "common"},
            {"ball_baseball",  "Baseball",       "#F5EED8",   1,  9, "sport", "sport", "common"},
            {"ball_8ball",     "Billard 8",      "#111111",   1, 10, "sport", "sport", "common"},

            // ── METAL ──────────────────────────────────────────
            {"ball_lead",      "Balle Plomb",    "#707880",   1, 18, "metal", "metal", "rare"},
            {"ball_nickel",    "Nickel",         "#C8B87A",   1,  9, "metal", "metal", "common"},
            {"ball_copper",    "Balle Cuivrée",  "#BF6830",   1, 22, "metal", "metal", "rare"},
            {"ball_chrome",    "Balle Chrome",   "#90CAF9",   1, 35, "metal", "metal", "legendary"},
            {"ball_bronze",    "Bronze",         "#CD7F32",   1, 10, "metal", "metal", "common"},
            {"ball_steel",     "Acier",          "#6B7FA8",   1, 10, "metal", "metal", "common"},
            {"ball_silver",    "Balle Argentée", "#B0BEC5",   1, 22, "metal", "metal", "rare"},
            {"ball_gold",      "Balle Dorée",    "#FFD700",   1, 30, "metal", "metal", "legendary"},
            {"ball_rosegold",  "Or Rose",        "#E8A090",   1, 14, "metal", "metal", "common"},
            {"ball_titanium",  "Titane",         "#5B6B7C",   1, 12, "metal", "metal", "common"},
            {"ball_platinum",  "Balle Platine",  "#D8DCE0",   1, 45, "metal", "metal", "legendary"},
            {"ball_bismuth",   "Bismuth",        "#C8A0C0",   1, 22, "metal", "metal", "rare"},
            {"ball_damascus",  "Damas",          "#4A4A4A",   1, 26, "metal", "metal", "rare"},
            {"ball_meteorite", "Météorite",      "#7A7060",   1, 24, "metal", "metal", "rare"},

            // ── SPACE ──────────────────────────────────────────
            {"ball_comet",        "Balle Comète",   "#A0825A",   1, 26, "space", "space", "rare"},
            {"ball_mercury",      "Mercure",        "#9E9585",   1, 18, "space", "space", "rare"},
            {"ball_venus",        "Vénus",          "#E8D5A3",   1, 20, "space", "space", "rare"},
            {"ball_earth",        "Terre",          "#1A6FA8",   1, 20, "space", "space", "rare"},
            {"ball_moon",         "Lune",           "#B8B8B8",   1, 18, "space", "space", "rare"},
            {"ball_mars",         "Mars",           "#C1440E",   1, 25, "space", "space", "rare"},
            {"ball_jupiter",      "Jupiter",        "#C88B3A",   1, 25, "space", "space", "rare"},
            {"ball_saturn",       "Saturn",         "#C8A96E",   1, 30, "space", "space", "legendary"},
            {"ball_uranus",       "Uranus",         "#7DE8E8",   1, 30, "space", "space", "legendary"},
            {"ball_neptune",      "Neptune",        "#2A5FD4",   1, 40, "space", "space", "legendary"},
            {"ball_pluto",        "Pluton",         "#C4A882",   1, 40, "space", "space", "legendary"},
            {"ball_red_dwarf",    "Naine Rouge",    "#CC2200",   1, 18, "space", "space", "rare"},
            {"ball_yellow_dwarf", "Naine Jaune",    "#FFD700",   1, 18, "space", "space", "rare"},
            {"ball_blue_giant",   "Géante Bleue",   "#4488FF",   1, 22, "space", "space", "rare"},
            {"ball_black_hole",   "Trou Noir",      "#0A0010",   1, 40, "space", "space", "legendary"},
            {"ball_pulsar",       "Pulsar",         "#4488FF",   1, 44, "space", "space", "legendary"},

            // ── ELEMENTAL ──────────────────────────────────────
            {"ball_elem_fire",      "Feu",      "#FF4400", 1, 18, "elemental", "elemental", "rare"},
            {"ball_elem_water",     "Eau",      "#0088CC", 1, 18, "elemental", "elemental", "rare"},
            {"ball_elem_earth",     "Terre",    "#6B4226", 1, 18, "elemental", "elemental", "rare"},
            {"ball_elem_ice",       "Glace",    "#A8D8EA", 1, 18, "elemental", "elemental", "rare"},
            {"ball_elem_darkness",  "Ténèbres", "#050505", 1, 18, "elemental", "elemental", "rare"},
            {"ball_elem_light",     "Lumière",  "#FFFFEE", 1, 18, "elemental", "elemental", "rare"},
            {"ball_elem_air",       "Air",      "#E0F7FA", 1, 18, "elemental", "elemental", "rare"},
            {"ball_elem_lightning", "Foudre",   "#FFF176", 1, 18, "elemental", "elemental", "rare"},
            {"ball_elem_plasma",    "Plasma",   "#E040FB", 1, 18, "elemental", "elemental", "rare"},
            {"ball_elem_lava",      "Lave",     "#FF3300", 1, 18, "elemental", "elemental", "rare"},
    };

    // ── Données Effets ─────────────────────────────────
    // {id, nom, couleur preview, gold, diamants}
    private static final Object[][] EFFECTS = {
            {"fx_none",     "Pas d'effet",       "#CCCCCC", 0,   0, null  },
            {"fx_fire",     "Traînée Feu",       "#FF5722", 600, 25, null },
            {"fx_ice",      "Traînée Glace",     "#80DEEA", 600, 25, null },
            {"fx_rainbow",  "Arc-en-ciel",       "#9C27B0", 900, 35, null },
            {"fx_stars",    "Étoiles",           "#FFD700", 700, 28, null },
    };

    // ── Données Backgrounds ────────────────────────────
    // {id, nom, couleur preview, gold, diamants}
    private static final Object[][] BACKGROUNDS = {
            {"bg_default",  "Fond Blanc",       "#FFFFFF", 0,   0, null},
            {"bg_grad_rainbow", "Dégradé Arc-en-ciel", "#DD0040", 5, 20, "rare"},
            {"bg_grad_sunset",  "Dégradé Sunset",      "#EE4400", 5, 20, "common"},
            {"bg_grad_ocean",   "Dégradé Océan",       "#0033AA", 5, 20, "common"},
            {"bg_grad_forest",  "Dégradé Forêt",       "#226622", 5, 20, "common"},
            {"bg_grad_aurora",  "Dégradé Aurore",      "#00CC66", 5, 22, "common"},
            {"bg_grad_candy",   "Dégradé Candy",       "#FF3388", 5, 22, "common"},
            {"bg_grad_volcano", "Dégradé Volcan",      "#880000", 5, 24, "common"},
            {"bg_grad_galaxy",  "Dégradé Galaxie",     "#4400AA", 5, 24, "common"},
            {"bg_grad_toxic",   "Dégradé Toxique",     "#33DD00", 5, 24, "common"},
            {"bg_minimal_triangles",  "Triangles", "#4AADA8", 1, 12, "rare"},
            {"bg_minimal_circles",   "Cercles",         "#4A5568", 3, 12, "rare"},
            {"bg_minimal_lines",     "Lignes",          "#2A3550", 3, 12, "rare"},
            {"bg_minimal_dots",      "Points",          "#3D1F2A", 3, 12, "rare"},
            {"bg_minimal_hexagons",  "Hexagones",       "#3A2D1A", 3, 12, "rare"},
            {"bg_urban_rain",    "Pluie Néon",    "#05050F", 4, 15, "legendary"},
            {"bg_urban_circuit", "Circuit",       "#020D05", 4, 15, "legendary"},
            {"bg_urban_grid",    "Grille",        "#060010", 4, 15, "legendary"},
            {"bg_urban_tunnel",    "Tunnel Néon",   "#020008", 4, 15, "legendary"},
            {"bg_urban_equalizer", "Equalizer",     "#050505", 4, 15, "legendary"},
    };

    // Clés equipped par défaut
    private static final String DEFAULT_BALL = "ball_basic";
    private static final String DEFAULT_FX   = "fx_none";
    private static final String DEFAULT_BG = "bg_default";

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
            goldTv.setText(Strings.fmt("common.currency_gold_fmt", prefs.getGold()));
            diamTv.setText(Strings.fmt("common.currency_diam_fmt", prefs.getDiamonds()));
        };
        refreshCurr.run();
        scroll.setTag(R.id.tag_refresh, refreshCurr);

        LinearLayout ballsContainer = addCollapsibleSection(ctx, page, Strings.get("cosmetics.section_balls"));
        String lastSubCat = "";
        for (Object[] ball : BALLS) {
            String subCat = (String) ball[6];
            if (!subCat.equals(lastSubCat)) {
                addSubSectionHeader(ctx, ballsContainer, Strings.get("cosmetics.subcats." + subCat));
                lastSubCat = subCat;
            }
            ballsContainer.addView(buildCosmeticRow(ctx, prefs, raw, ball, "equipped_ball", refreshCurr));
        }

        LinearLayout bgContainer = addCollapsibleSection(ctx, page, Strings.get("cosmetics.section_backgrounds"));
        for (Object[] bg : BACKGROUNDS) {
            bgContainer.addView(buildCosmeticRow(ctx, prefs, raw, bg, "equipped_bg", refreshCurr));
        }

        scroll.addView(page);
        return scroll;
    }

    public static List<GachaSystem.SkinEntry> getAllGachaSkins() {
        List<GachaSystem.SkinEntry> out = new ArrayList<>();
        for (Object[] ball : BALLS) {
            String rarity = (String) ball[ball.length - 1];
            if (rarity != null)
                out.add(new GachaSystem.SkinEntry((String) ball[0], (String) ball[1], (String) ball[2], rarity));
        }
        for (Object[] bg : BACKGROUNDS) {
            String rarity = (String) bg[bg.length - 1];
            if (rarity != null)
                out.add(new GachaSystem.SkinEntry((String) bg[0], (String) bg[1], (String) bg[2], rarity));
        }
        return out;
    }

    // ──────────────────────────────────────────────────
    // ROW
    // ──────────────────────────────────────────────────
    private static String skinName(String id, String fallback) {
        String key = "cosmetics.skins." + id;
        String v   = Strings.get(key);
        return key.equals(v) ? fallback : v;
    }

    private static View buildCosmeticRow(Context ctx, GamePreferences prefs,
                                         SharedPreferences raw, Object[] data,
                                         String equippedKey, Runnable refreshCurr) {
        String id       = (String) data[0];
        String name     = skinName(id, (String) data[1]);
        String colorHex = (String) data[2];
        String rarity = (String) data[data.length - 1];
        int goldCost    = EconomyBalance.cosmeticGoldCost(id, (int) data[3], (int) data[4], rarity);
        int diamCost    = EconomyBalance.cosmeticDiamondCost(id, (int) data[3], (int) data[4], rarity);
        boolean isFree  = goldCost == 0 && diamCost == 0;
        boolean isGacha = "common".equals(rarity) || "rare".equals(rarity) || "legendary".equals(rarity);
        int fragThreshold = isGacha ? GachaSystem.getFragmentThreshold(rarity) : 0;

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(px(ctx, 12), px(ctx, 10), px(ctx, 12), px(ctx, 10));
        setRoundedBackground(row, "#1A2A3A", 10);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, px(ctx, 6), 0, 0);
        row.setLayoutParams(rowLp);

        // Preview (cercle coloré 48×48)
        // NOUVEAU
        View preview = new View(ctx) {
            @Override protected void onDraw(Canvas canvas) {
                float cx = getWidth()  / 2f;
                float cy = getHeight() / 2f;
                float r  = getWidth()  * 0.30f;
                if (id.startsWith("bg_")) {
                    BgPreviewRenderer.draw(canvas, cx, cy, r, id);
                } else {
                    try { BallRenderer.setColor(Color.parseColor(colorHex)); } catch (Exception ignored) {}
                    BallRenderer.setAnimState(id, 0f, 0f, 0f);
                    BallRenderer.draw(canvas, cx, cy, r, id);
                }
            }
        };
        int previewSize = px(ctx, 80);
        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(previewSize, previewSize);
        prevLp.setMargins(0, 0, px(ctx, 8), 0);
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

        if (isGacha) {
            TextView rarityBadge = new TextView(ctx);
            rarityBadge.setText(GachaSystem.getRarityLabel(rarity));
            rarityBadge.setTextColor(GachaSystem.getRarityColor(rarity));
            rarityBadge.setTextSize(11f);
            textCol.addView(rarityBadge);
        }

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
        LinearLayout fragRow = new LinearLayout(ctx);
        fragRow.setOrientation(LinearLayout.HORIZONTAL);
        fragRow.setPadding(0, px(ctx, 4), 0, 0);
        Button fragBtn = new Button(ctx);
        styleSmallBtn(fragBtn);
        fragRow.addView(fragBtn);
        textCol.addView(fragRow);
        fragRow.setVisibility(View.GONE);

        row.addView(textCol);

        if (isGacha) {
            fragBtn.setOnClickListener(v -> {
                int frags = prefs.getFragments(id);
                if (frags >= fragThreshold) {
                    prefs.spendFragments(id, fragThreshold);
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

        // Logique refresh état
        Runnable[] r = {null};

        r[0] = () -> {
            boolean owned    = raw.getBoolean("owned_" + id, isFree);
            boolean equipped = id.equals(raw.getString(equippedKey, ""));

            if (equipped) {
                actionBtn.setText(Strings.get("cosmetics.btn_equipped"));
                actionBtn.setTextColor(Color.parseColor("#00E676"));
                setRoundedBackground(actionBtn, "#0A2A0A", 8);
                actionBtn.setEnabled(false);
                diam2Btn.setVisibility(View.GONE);
                fragRow.setVisibility(View.GONE);
            } else if (owned) {
                actionBtn.setText(Strings.get("cosmetics.btn_equip"));
                actionBtn.setTextColor(Color.WHITE);
                setRoundedBackground(actionBtn, "#1B3A5A", 8);
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
                fragRow.setVisibility(View.GONE);
            } else {
                actionBtn.setText(Strings.fmt("common.currency_gold_fmt", goldCost));
                actionBtn.setTextColor(Color.parseColor("#FFD700"));
                setRoundedBackground(actionBtn, "#1B3A1B", 8);
                actionBtn.setEnabled(prefs.getGold() >= goldCost);
                diam2Btn.setText(Strings.fmt("common.currency_diam_fmt", diamCost));
                diam2Btn.setTextColor(Color.parseColor("#80DEEA"));
                setRoundedBackground(diam2Btn, "#1A1A3A", 8);
                diam2Btn.setEnabled(true);
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
                    } else if (ctx instanceof MainActivity) {
                        ((MainActivity) ctx).showInsufficientDiamondsPopup();
                    }
                });
                if (isGacha) {
                    int frags = prefs.getFragments(id);
                    fragBtn.setText(Strings.fmt("cosmetics.frag_btn_fmt", frags, fragThreshold));
                    fragBtn.setTextColor(frags >= fragThreshold
                            ? Color.parseColor("#FFD700")
                            : Color.parseColor("#666666"));
                    setRoundedBackground(fragBtn, frags >= fragThreshold
                            ? "#2A1E00"
                            : "#1A1A1A", 8);
                    fragBtn.setEnabled(frags >= fragThreshold);
                    fragRow.setVisibility(View.VISIBLE);
                } else {
                    fragRow.setVisibility(View.GONE);
                }
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

    private static LinearLayout addCollapsibleSection(Context ctx, LinearLayout page, String label) {
        boolean[] expanded = {true};

        LinearLayout header = new LinearLayout(ctx);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hLp.setMargins(0, px(ctx, 18), 0, px(ctx, 2));
        header.setLayoutParams(hLp);
        header.setClickable(true);
        header.setFocusable(true);

        TextView arrow = new TextView(ctx);
        arrow.setText("v");
        arrow.setTextColor(Color.parseColor("#80DEEA"));
        arrow.setTextSize(15f);
        arrow.setPadding(0, 0, px(ctx, 8), 0);
        header.addView(arrow);

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

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setVisibility(View.VISIBLE);

        header.setOnClickListener(v -> {
            expanded[0] = !expanded[0];
            container.setVisibility(expanded[0] ? View.VISIBLE : View.GONE);
            arrow.setText(expanded[0] ? "v" : ">");
        });

        page.addView(header);
        page.addView(container);
        return container;
    }

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

    private static void setRoundedBackground(View view, String colorHex, int radiusDp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(colorHex));
        bg.setCornerRadius(px(view.getContext(), radiusDp));
        view.setBackground(bg);
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

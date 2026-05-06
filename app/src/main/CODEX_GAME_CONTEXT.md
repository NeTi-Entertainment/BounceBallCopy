# Contexte Codex - BounceBall

A lire avant chaque nouvelle demande sur ce projet.

## Regles de travail prioritaires

- Ne jamais effacer, reecrire ou modifier une fonctionnalite existante qui n'est pas directement visee par la demande.
- Garder les changements tres localises: toucher uniquement les fichiers necessaires a la finition ou petite fonctionnalite demandee.
- Preserver le comportement, les sauvegardes, les cles `SharedPreferences`, les assets, les traductions et les effets visuels existants sauf demande explicite.
- Le jeu est deja tres avance: traiter les demandes comme des finitions, corrections ciblees ou petits ajouts, sauf indication contraire.
- Ne pas tenter de compiler depuis ce dossier: `app/src/main` est volontairement bas dans l'arborescence Android Studio.
- Avant modification, relire le fichier concerne et ses appels proches; ne pas supposer qu'une classe est inutilisee sans verification par recherche.
- Si des fichiers sont deja modifies dans le worktree, les considerer comme des changements utilisateur et travailler autour sans les revert.

## Vue generale

Jeu Android mobile Java nomme `BounceBall`.

- Package principal: `com.example.bounceball`.
- Activite de lancement: `MainActivity` dans `AndroidManifest.xml`.
- UI construite principalement en Java, sans layouts XML applicatifs.
- Gameplay rendu via `GameView`, un `SurfaceView` avec boucle de rendu manuelle autour de 60 FPS.
- Sauvegarde centralisee dans `GamePreferences` avec `SharedPreferences` nomme `TrampolineGamePrefs`.
- Localisation via JSON dans `assets/lang/*.json`, chargee par `LocaleManager` et lue par `Strings`.

## Gameplay principal

`GameView.java` gere le jeu en run.

- Le joueur trace un trampoline avec le doigt; le trace consomme de l'encre.
- La balle subit gravite, resistance a l'air, rebonds lateraux, rotation, collisions avec le trampoline.
- La camera monte quand la balle depasse une zone haute; la hauteur gagnee alimente `totalHeightMeters` et l'or de run.
- Fin de partie quand la balle sort sous l'ecran.
- Power-ups: blobs d'encre, portails warp, metal rare et aliens apres eclosion.
- Les portails utilisent des etats internes: absorb, scroll vertical rapide, eject.
- Les skins de balle peuvent modifier le gameplay par categorie:
  - `metal`: gravite plus forte, rebond reduit.
  - `sport`: gravite legerement reduite, rebond augmente.
  - `space`: gravite reduite.
  - `elemental`: consommation d'encre reduite, magnetisme augmente.

## Hub et progression

`MainActivity.java` combine le hub et le jeu dans le meme ecran.

- Affiche `GameView` en fond, boutons settings/shop/egg, record et texte "tap to play".
- Cache l'UI du hub pendant une partie via callbacks `onGameStarted` / `onGameOver`.
- En fin de partie:
  - met a jour le record avec `prefs.updateMaxHeight`,
  - ajoute l'or selon `heightReached * goldMultiplier`,
  - declenche l'etat d'eclosion si le seuil est atteint,
  - affiche un ecran de resultats transparent avant retour menu.
- L'ecran resultats peut proposer une seule reprise par partie via rewarded ad; si la reprise est utilisee, la run repart a la hauteur atteinte, avec balle/encre remises en etat initial, et l'or deja gagne ne doit pas etre recompte.
- L'ecran resultats propose aussi de doubler l'or de run via rewarded ad; l'or de run est deja credite une fois a la fin, donc le doublement ajoute une seconde fois `pendingResultGoldEarned`.
- Overlays principaux: settings, shop, egg, gacha.
- `GameActivity.java` existe aussi mais le flux courant semble surtout passer par `MainActivity`.

## Economie, upgrades, shop

- `UpgradeStats.java` lit les niveaux `upg_*` depuis `SharedPreferences`.
- Upgrades connus: air, weight, elasticity, boost, boost recharge, ink reserve, ink efficiency, gold multiplier, warp.
- `MainActivity.buildShopOverlay` contient l'onglet upgrades.
- Le shop contient une ligne "Acheter des diamants" sous les categories; elle ouvre une page de packs geres via Google Play Billing quand les produits Play Console existent.
- Les boutons de depense en diamants du shop doivent rester cliquables quand le solde manque et afficher le popup de solde insuffisant qui peut ouvrir cette page d'achat.
- `CosmeticsPage.java` contient les donnees et UI des cosmetiques visibles:
  - balles,
  - backgrounds.
- Les effets/trainees `fx_*` sont des donnees legacy a ne pas reexposer dans les menus sauf demande explicite.
- Cles importantes:
  - possession: `owned_<id>`,
  - fragments: `frag_<skinId>`,
  - equipement: `equipped_ball`, `equipped_fx`, `equipped_bg`.
- `GachaSystem.java` gere couts, pool, poids et seuils de fragments.
- `GachaPage.java` gere l'overlay de roue, les spins et l'attribution de fragments.
- Equilibrage economique centralise dans `EconomyBalance.java`:
  - reference: `1 diamant = 1000 or`,
  - upgrades: tables/formules de cout via `upgradeGoldCost` / `upgradeDiamondCost`; les couts diamants suivent une progression de niveau plus marquee que la conversion pure,
  - cosmetiques: prix recalcules selon type et rarete via `cosmeticGoldCost` / `cosmeticDiamondCost`; les skins/fonds de roulette communs sont a x2, rares a x5 et legendaires a x10 en or, aucun cosmetique payant ne descend sous 5 diamants,
  - roulette: 1000 or ou 1 diamant par spin.

## Oeuf et colonie

- `EggHatchManager.java` declenche l'oeuf pret quand une run atteint `HATCH_HEIGHT_THRESHOLD` actuellement `1000f`.
- Avant eclosion: overlay oeuf verrouille.
- A eclosion: animation, puis alien interactif avec citations chargees depuis `assets/idle_quotes/<lang>_idle_quotes.json`.
- Apres eclosion, le jeu peut faire apparaitre metal rare et aliens.
- `ColonyActivity.java`, `ColonyView.java`, `ColonyManager.java`, `ColonyBuilding.java`, `ColonyBuildingSlot.java` gerent la colonie.
- La colonie a 4 slots actifs dans `ColonyManager.SLOT_LAYOUT`: habitation, eau, oxygene, nourriture.
- Les batiments ont niveaux, couts or/metal, durees d'upgrade et stats de capacite.
- Collecte d'alien autorisee seulement si population, oxygene, eau et nourriture peuvent supporter un colon supplementaire.

## Rendus et assets

- `BallRenderer.java`: rendu detaille des skins de balles.
- `BackgroundRenderer.java`: rendu des backgrounds en jeu, avec parallax.
- `BgPreviewRenderer.java` et `SkinPreviewRenderer.java`: previews shop/gacha.
- Sons dans `res/raw`: bounce, fall, warp in/out, ink pickup, sons elementaires.
- `SoundManager.java` centralise `SoundPool`, respecte `sound_enabled`, et gere sons elementaires/warp.
- Assets de colonie dans `res/drawable`: sol lunaire, hub, habitation, eau, oxygene, ferme.
- `cpp/` contient un squelette natif GameActivity/OpenGL, mais aucune reference Java/manifest evidente ne montre qu'il pilote le jeu actuel.

## Fichiers souvent impliques

- Gameplay: `java/com/example/bounceball/GameView.java`
- Hub/overlays: `java/com/example/bounceball/MainActivity.java`
- Sauvegardes: `java/com/example/bounceball/utils/GamePreferences.java`
- Textes: `assets/lang/*.json`, `java/com/example/bounceball/utils/Strings.java`
- Cosmetiques: `java/com/example/bounceball/CosmeticsPage.java`
- Gacha: `java/com/example/bounceball/GachaPage.java`, `GachaSystem.java`
- Colonie: `java/com/example/bounceball/colony/*`
- Rendus visuels: `BallRenderer.java`, `BackgroundRenderer.java`, `BgPreviewRenderer.java`, `SkinPreviewRenderer.java`

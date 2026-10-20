# REEB — projet Android

Le logo fourni a été ajouté dans `app/src/main/res/drawable/reeb_logo.png` et l'application est renommée REEB.

Cette version source affiche le pourcentage estimé de surface restante et de surface manquante, sans valeur monétaire proportionnelle. Le contour est encore défini manuellement par quatre points. La détection automatique fiable, la correction de perspective et la comparaison à la surface complète du billet doivent encore être développées avant de s'appuyer sur les résultats.

## Compilation
Ouvrir ce dossier dans Android Studio, synchroniser Gradle puis sélectionner **Build > Build Bundle(s) / APK(s) > Build APK(s)**. L'APK debug sera normalement créé dans `app/build/outputs/apk/debug/app-debug.apk`.

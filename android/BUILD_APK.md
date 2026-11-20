# BatteryShare AI V1.0 — Guide de génération de l'APK

## Prérequis
- **Android Studio** Ladybug (2024.2+) ou supérieur
- **JDK 17** (Eclipse Temurin recommandé)
- **Android SDK** Platform 35 + Build-Tools 35.0.0 (compatibilité runtime dès Android 8.0 / API 26)
- **Gradle** 8.9 (inclus via `gradle-wrapper.properties` ou `gradle` système)

## 1. Compiler et installer via Android Studio
1. Extraire l'archive du projet Android (`BatteryShareAI`).
2. Ouvrir Android Studio > **File > Open...** et sélectionner le dossier racine contenant `settings.gradle.kts`.
3. Attendre la synchronisation Gradle (`Sync Project with Gradle Files`).
4. Connecter un téléphone Android (Android 8.0+) avec le **Débogage USB** activé, ou utiliser un émulateur.
5. Cliquer sur **Run 'app'** (`Shift + F10`) ou **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. L'APK généré se trouve dans :
   `app/build/outputs/apk/debug/app-debug.apk`

## 2. Générer l'APK en ligne de commande (CLI)
Dans le dossier racine du projet Android :

```bash
# Exécuter les tests unitaires
gradle testDebugUnitTest

# Vérifier Android Lint
gradle lintDebug

# Générer l'APK Debug
gradle assembleDebug
```

Le fichier APK est produit dans :
`app/build/outputs/apk/debug/app-debug.apk`

## 3. Générer un APK Release signé
1. Créer un keystore :
```bash
keytool -genkeypair -v -keystore batteryshare-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias batteryshare
```
2. Configurer `signingConfigs` dans `app/build.gradle.kts` ou utiliser **Build > Generate Signed Bundle / APK...** dans Android Studio.
3. Exécuter :
```bash
gradle assembleRelease
```

## 4. Pipeline automatique GitHub Actions
Le workflow `.github/workflows/android-ci.yml` exécute automatiquement à chaque `push` :
- `gradle testDebugUnitTest`
- `gradle lintDebug`
- `gradle assembleDebug`
- Publication de l'artefact `BatteryShareAI-v1.0-debug-apk`.

## Note matérielle importante
- Les fonctions **logicielles** (lecture batterie/température, base Room, protections thermiques, découverte Bluetooth & Wi-Fi Direct, synchronisation des états) sont 100 % opérationnelles sur Android 8.0+.
- Les fonctions **matérielles** (alimentation inversée USB-C/OTG, activation matérielle de la bobine Qi inversée) dépendent strictement du contrôleur PMIC, du câble OTG/USB-PD et des permissions constructeur (OEM) de chaque appareil.

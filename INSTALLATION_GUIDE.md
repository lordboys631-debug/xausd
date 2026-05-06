# 🔧 Manuel d'installation et déploiement

## Prérequis système

### Requis
- **Android Studio** : 2022.1.0 ou plus récent
- **Java JDK** : 11 ou plus (17 recommandé)
- **Android SDK** : 24 (Android 7.0) minimum
- **Gradle** : 7.0+

### Optionnel mais recommandé
- **Émulateur Android** : Pixel 4a (API 30+) ou
- **Appareil physique** : Android 7.0+ avec USB Debug activé

---

## Étape 1 : Préparation de l'environnement

### Windows (PowerShell)

```powershell
# Vérifier Java
java -version

# Vérifier Gradle
gradle -v

# Définir JAVA_HOME
[Environment]::SetEnvironmentVariable(\"JAVA_HOME\", \"C:\\Program Files\\Java\\jdk-17\", \"User\")
$env:JAVA_HOME = \"C:\\Program Files\\Java\\jdk-17\"
```

### macOS/Linux

```bash
# Vérifier Java
java -version

# Vérifier Gradle
gradle -v

# Définir JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
echo $JAVA_HOME
```

---

## Étape 2 : Configuration du projet

### 1. Cloner/Accéder au dossier
```bash
cd C:\\Users\\is\\Desktop\\backtest
```

### 2. Synchroniser Gradle
```bash
# Sur Windows
.\\gradlew clean
.\\gradlew --version

# Sur macOS/Linux
./gradlew clean
./gradlew --version
```

### 3. Vérifier local.properties
```properties
# File: C:\\Users\\is\\Desktop\\backtest\\local.properties
sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
```

### 4. Vérifier gradle.properties
```properties
# File: C:\\Users\\is\\Desktop\\backtest\\gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
```

---

## Étape 3 : Compilation

### Build Debug (rapide)
```bash
# Windows
.\\gradlew assembleDebug

# macOS/Linux
./gradlew assembleDebug

# Temps : 3-5 minutes
# Résultat : app/build/outputs/apk/debug/app-debug.apk
```

### Build Release (optimisé)
```bash
# Windows
.\\gradlew assembleRelease

# macOS/Linux
./gradlew assembleRelease

# Temps : 5-10 minutes (avec optimisations)
# Résultat : app/build/outputs/apk/release/app-release.apk
```

### Build complet (tous les tests)
```bash
# Windows
.\\gradlew build

# macOS/Linux
./gradlew build

# Temps : 10-15 minutes
# Inclut tous les tests
```

---

## Étape 4 : Déploiement sur appareil

### Préparation appareil Android

1. **Activer le mode développeur**
   ```
   Paramètres → À propos du téléphone → Appuyer 7 fois sur \"Numéro de version\"
   ```

2. **Activer le débogage USB**
   ```
   Paramètres → Options pour développeurs → Débogage USB
   ```

3. **Accepter la clé RSA**
   - Brancher l'USB
   - Cliquer \"Accepter\" sur le téléphone

### Installation via gradlew

```bash
# Windows
.\\gradlew installDebug

# macOS/Linux
./gradlew installDebug

# Alternative : Installation directe APK
# adb install -r app\\build\\outputs\\apk\\debug\\app-debug.apk
```

### Vérification
```bash
# Lister les appareils connectés
adb devices

# Voir les logs
adb logcat | grep -i backtest

# Lancer l'app
adb shell am start -n com.bthr.backtest/.MainActivity
```

---

## Étape 5 : Déploiement sur émulateur

### Créer un émulateur (Android Studio)

1. Tools → Device Manager
2. Create Device
3. Sélectionner device model (ex: Pixel 4a)
4. Sélectionner API level (30+ recommandé)
5. Finish

### Lancer l'émulateur

```bash
# Via Android Studio
# Virtual Device Manager → Play button

# Ou via CLI
emulator -avd Pixel_4a_API_30 &
```

### Installer l'APK

```bash
# Attendre que l'émulateur soit chargé
adb devices

# Installer
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Vérifier
adb logcat
```

---

## Étape 6 : Lancement et test

### Via Android Studio

1. Ouvrir le projet dans Android Studio
2. File → Open → Sélectionner le dossier
3. Wait for Gradle sync
4. Click \"Run\" (Maj+F10) ou Click icône Play
5. Sélectionner device/émulateur
6. Attendre le lancement

### Via Terminal

```bash
# Build et deploy en une commande
./gradlew installDebug

# Ou avec logs
./gradlew installDebug && adb logcat
```

### Vérifier le fonctionnement

```bash
# Voir les logs
adb logcat -s CandlestickChart

# Voir les erreurs
adb logcat -s *:E
```

---

## Étape 7 : Tester la fonctionnalité

### Checklist rapide

1. ✅ L'app se lance sans crash
2. ✅ L'icône \"Ligne de tendance\" est visible en haut à gauche
3. ✅ Cliquer l'icône → elle devient DORÉE
4. ✅ Cliquer sur le graphique → cercle jaune apparaît
5. ✅ Bouger la souris → viseur suit le mouvement
6. ✅ Cliquer à nouveau → ligne tracée
7. ✅ L'icône redevient GRISE

### Tests complets

Voir **TESTING_GUIDE.md** pour les 7 scénarios de test détaillés.

---

## Troubleshooting

### Erreur : \"JAVA_HOME not set\"

```bash
# Windows
setx JAVA_HOME \"C:\\Program Files\\Java\\jdk-17\"

# macOS/Linux
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

### Erreur : \"SDK not found\"

Créer/Éditer `local.properties`:
```properties
sdk.dir=/path/to/Android/Sdk
```

### Erreur : \"Gradle timeout\"

```bash
# Augmenter les ressources
.\\gradlew build --no-daemon -Dorg.gradle.jvmargs=\"-Xmx2048m\"
```

### Erreur : \"No device found\"

```bash
# Vérifier les appareils
adb devices

# Relancer le serveur adb
adb kill-server
adb start-server
```

### Erreur : Crash au lancement

```bash
# Voir les logs d'erreur
adb logcat | grep -A5 \"FATAL\"

# Vérifier la version Android minimale
# Vérifier que toutes les permissions sont déclarées
```

---

## Configuration avancée

### Gradle properties optimisés

Ajouter à `gradle.properties`:
```properties
# Parallel execution
org.gradle.parallel=true

# Build cache
org.gradle.caching=true

# Memory
org.gradle.jvmargs=-Xmx4g

# Daemon
org.gradle.daemon=true
```

### Build variants

```bash
# Lister les variants
./gradlew tasks | grep assemble

# Build spécifique
./gradlew assembleDebugTest
./gradlew assembleReleaseTest
```

### Signing configuration (pour Release)

File: `app/build.gradle.kts`
```kotlin
signingConfigs {
    create(\"release\") {
        storeFile = file(\"keystore.jks\")
        storePassword = \"password\"
        keyAlias = \"release\"
        keyPassword = \"password\"
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName(\"release\")
    }
}
```

---

## Commandes utiles

### Compilation
```bash
./gradlew clean                 # Nettoyer
./gradlew assembleDebug         # Build debug
./gradlew build                 # Build complet
./gradlew build --no-daemon     # Sans daemon
```

### Installation
```bash
./gradlew installDebug          # Installer debug
./gradlew uninstallDebug        # Désinstaller debug
./gradlew installDebugTest      # Installer tests
```

### Tests
```bash
./gradlew test                  # Tests unitaires
./gradlew connectedAndroidTest  # Tests sur device
```

### Logs et Debug
```bash
adb logcat                      # Tous les logs
adb logcat -s TAG               # Logs d'un tag
adb logcat -c                   # Effacer les logs
./gradlew build --info          # Build verbose
```

### Performance
```bash
./gradlew build --profile       # Profiling build
./gradlew build --scan          # Build scan
```

---

## Checklist de déploiement

- [ ] Java/Gradle installés
- [ ] Android SDK configuré
- [ ] local.properties correct
- [ ] Projet synchronisé Gradle
- [ ] Build sans erreur (./gradlew build)
- [ ] Device/émulateur connecté
- [ ] Installation réussie
- [ ] App lance sans crash
- [ ] Icône \"Ligne de tendance\" visible
- [ ] Tests TESTING_GUIDE.md réussis

---

## Support et aide

### Erreurs de compilation
→ Consulter BUILDING_ERRORS.md (si disponible)  
→ Vérifier TECHNICAL_DOCUMENTATION.md

### Erreurs runtime
→ Vérifier logcat (`adb logcat`)  
→ Consulter TESTING_GUIDE.md

### Questions techniques
→ Consulter INDEX.md pour naviguer la doc

---

## Ressources

- [Android Studio](https://developer.android.com/studio)
- [Android SDK Setup](https://developer.android.com/tools/sdk)
- [Gradle Guide](https://gradle.org/)
- [Android Developers](https://developer.android.com/)

---

**Version** : 1.0  
**Dernière mise à jour** : 2026-04-20  
**Statut** : Prêt pour production


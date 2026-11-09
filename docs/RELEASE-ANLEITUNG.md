# Releases für Schnurrfeld

Diese Anleitung erklärt, wie das Repository eingerichtet wurde und wie du ein Release erstellst.
Die App findet neue Versionen über die GitHub-Releases dieses Repositorys.

## 1. Was bereits erledigt ist

- Der komplette Quellcode liegt auf dem Branch `main`.
- Diese Dateien sind absichtlich **nicht** im Repository (siehe `.gitignore`):
  - `app/schnurrfeld.jks`: der Signaturschlüssel
  - `app/keystore.properties`: die Passwörter dazu
  - fertige APKs, ZIPs und die MP3-Klangvorschauen
- In `app/build.gradle.kts` steht kein Passwort mehr. Gradle liest es aus `app/keystore.properties`.

Warum der Schlüssel draußen bleibt: Das Repository ist öffentlich. Mit dem Schlüssel könnte jeder eine APK bauen,
die Android als echtes Schnurrfeld-Update akzeptiert.

**Wichtig:** Bewahre `schnurrfeld.jks` gut auf, zum Beispiel aus dem Quellcode-ZIP. Ohne diese Datei
lässt sich die installierte App nie wieder aktualisieren. Man müsste sie deinstallieren und verlöre den Spielstand.

Zum lokalen Bauen legst du `app/keystore.properties` neben `app/build.gradle.kts`.
Die echten Werte kennst du: Sie stehen in der gleichnamigen Datei im Quellcode-ZIP, nicht hier.

```
storeFile=schnurrfeld.jks
storePassword=DEIN_PASSWORT
keyAlias=DEIN_ALIAS
keyPassword=DEIN_PASSWORT
```

Fehlt die Datei, baut Gradle die Release-APKs unsigniert. Die lassen sich nicht installieren.

## 2. Wo findet man die Releases?

- **Am Computer:** Öffne https://github.com/arthurtschik/Schnurrfeld. In der rechten Spalte steht unter
  "About" der Abschnitt **Releases**. Ein Klick darauf zeigt alle Releases.
- **Direkt zum Anlegen:** https://github.com/arthurtschik/Schnurrfeld/releases/new
- **Am Handy:** Die GitHub-App kann keine Releases anlegen. Nimm den Browser, tippe oben rechts auf das
  Menü und wähle "Desktop-Website". Ohne diese Ansicht ist die Releases-Spalte oft ausgeblendet.

## 3. Das erste Release (1.2) erstellen

1. Öffne https://github.com/arthurtschik/Schnurrfeld/releases/new
2. **Choose a tag:** Tippe `v1.2` ein und klicke auf **"Create new tag: v1.2 on publish"**.
3. **Target:** `main` (ist normalerweise schon ausgewählt).
4. **Release title:** `Schnurrfeld 1.2`
5. **Beschreibung:** Kopiere den Text aus `docs/release-notes-1.2.md` hinein.
   Genau dieser Text erscheint später in der App unter "Neu:".
6. **Dateien anhängen:** Ziehe beide APKs in das Feld "Attach binaries …" oder klicke darauf:
   - `Schnurrfeld-1.2.apk`
   - `Schnurrfeld-1.2-Dev.apk`

   Die Dateinamen müssen genau so lauten. Die normale App nimmt die Datei ohne "Dev", die Dev-App die mit "Dev".
   Warte, bis beide fertig hochgeladen sind.
7. **"Set as a pre-release"** NICHT anhaken. Pre-Releases ignoriert die App.
8. **"Set as the latest release"** anhaken.
9. Klicke auf **Publish release**. Nicht auf "Save draft": Entwürfe sieht die App nicht.

Kontrolle: Unter https://github.com/arthurtschik/Schnurrfeld/releases/latest sollte jetzt 1.2 mit den beiden APKs
stehen. Öffnest du danach in der App Einstellungen → App → "Nach Updates suchen", erscheint
"Du hast die neueste Version (1.2)".

## 4. Optional: Beschreibung des Repositorys

Klicke auf der Startseite des Repositorys rechts neben "About" auf das Zahnrad und trage zum Beispiel ein:

- Description: `Gemütliches Katzen-Logikrätsel für Android: eine Katze pro Zeile, Spalte und Farbfeld – mit Tamagotchi-Kätzchen im Katzenkörbchen.`
- Website: `https://github.com/arthurtschik/Schnurrfeld/releases/latest`

## 5. Jedes weitere Update (zum Beispiel 1.3)

1. Erhöhe in `app/build.gradle.kts` beide Werte:
   - `versionCode = 13`: muss bei jedem Update größer werden
   - `versionName = "1.3"`
2. Baue mit demselben Schlüssel:
   ```
   ./gradlew :app:assemblePlayRelease :app:assembleDevRelease
   ```
3. Benenne die APKs um:
   - `app/build/outputs/apk/play/release/app-play-release.apk` → `Schnurrfeld-1.3.apk`
   - `app/build/outputs/apk/dev/release/app-dev-release.apk` → `Schnurrfeld-1.3-Dev.apk`
4. Committe und pushe den Code auf `main`.
5. Lege ein Release an wie in Abschnitt 3, nur mit Tag `v1.3`, Titel `Schnurrfeld 1.3` und den neuen APKs.

Beim nächsten Start bietet die App das Update automatisch an.

## 6. Wenn es nicht klappt

| Problem | Ursache |
|---|---|
| App meldet "Du hast die neueste Version", obwohl ein Release da ist | Tag nicht höher als die installierte Version, Release ist Entwurf oder Pre-Release, oder keine `.apk` angehängt |
| Android sagt "App nicht installiert" | APK mit anderem Schlüssel signiert, oder `versionCode` nicht erhöht |
| "Suche fehlgeschlagen" | Keine Internetverbindung, oder GitHub hat kurzzeitig zu viele Anfragen (60 pro Stunde je Gerät) |
| Installer öffnet sich nicht | Einmalig "Unbekannte Apps installieren" für Schnurrfeld erlauben; der Update-Dialog führt dorthin |

# Schnurrfeld 🐱

Ein Katzen-Logikrätsel für Android (Kotlin + Jetpack Compose).

**Regeln:** In jede Zeile, jede Spalte und jeden Farbbereich gehört genau eine Katze. Katzen dürfen sich nicht berühren – auch nicht diagonal.

## Installieren
`Schnurrfeld.apk` aufs Handy kopieren, öffnen und „Installation aus unbekannten Quellen“ erlauben (Android 8+).

## Features
- Fehlende Aufnahme (wird nicht genutzt, solange Finish/Nice job vorhanden sind): `voice_you_did_it.ogg` in `app/src/main/res/raw/` ablegen oder in der App aufnehmen
- 200 Levels (5×5 bis 10×10) mit sanftem Einstieg: Level 1–12 brauchen nur einfache Schritte, schwierigere Logik kommt nach und nach; jedes Rätsel hat genau eine Lösung
- ✕ setzen und entfernen ist immer kostenlos – nur falsche Katzen kosten ein Leben
- Tagesrätsel (7×7 bis 10×10 je nach Wochentag, mit Tage-Serie) und Endlos-Modus (Rekord)
- 3 Leben pro Rätsel; falsche Katzen kosten ein Leben und die Kombo
- Gesprochenes Lob (eigene Aufnahmen): Nice → Great → Incredible, steigert sich im Rätsel; nicht bei jeder Katze und nie bei der ersten
- Erfolgs-Glöckchen in 5 Stufen, die mit der fehlerfreien Serie immer festlicher werden
- Kombo-Ansagen kommen immer: „Kombo“ (kein ✕ zwischen zwei Katzen), „Quick Kombo“ (< 20 s), „Super fast“ (< 10 s)
- Kombo-Leiste mit schimmernder Milch, feste Punkte: 3 Katzen in Folge +1 · Kombo +1 · Quick Kombo +2 · Super fast +3 · Purrfect +3 · Tagesrätsel +1; bei 9 Punkten abwechselnd Tipp/Joker, ein Fehler leert sie. Die Schriftzüge verflüssigen sich und fließen als Milchtropfen in die Leiste
- Rundenende: „Purrfect!“ + Lichtstrahlen + großer PURRFECT-Schriftzug nur ohne verlorenes Leben, sonst „Nice job“ (bzw. Finish / You did it) mit Lametta-Regen
- Tipps erklären den nächsten logischen Schritt (z. B. „Nur noch ein Platz“, „Eingesperrte Katze“, „Was wäre, wenn …?“) und markieren die betroffenen Felder
- Alle 5 fehlerfreien Katzen ein Tipp, alle 12 ein Joker
- Chibi-Katzen mit 7 Animationen (tanzen, putzen, schlafen, Schwanzwedeln, Kopf neigen, hüpfen, umschauen); beim Sieg tanzen alle
- Hell- und Dunkelmodus, 12 gut unterscheidbare Farben, jede mit eigenem dezenten Motiv (Punkte, Herzen, Sterne, Pfoten, Blüten, Monde …), optionale Farbhilfe mit kräftigeren Mustern
- Einstellungen: Design, Timer, Auto-✕, Sounds, Vibration, Lob-Stimme, Computerstimme als Ersatz
- Eigene Lob-Stimme direkt in der App aufnehmen oder Audiodateien importieren

## Bedienung
Tippen → ✕ setzen · ✕ nochmal antippen → ✕ entfernen (kostenlos) · schnell doppeltippen → Katze · wischen → viele ✕ auf einmal

## Bauen
```bash
./gradlew :app:assembleRelease
```
Die Levels sind in `app/src/main/assets/levels.txt` vorberechnet. Neu erzeugen (z. B. nach Änderungen am Generator):
```bash
DUMP_LEVELS=1 ./gradlew :app:testDebugUnitTest --tests '*DumpLevels*'
```
Sounds werden synthetisch erzeugt (`app/src/main/res/raw/*.wav`) und lassen sich durch eigene WAV-Dateien mit gleichem Namen ersetzen.


## Katzenkörbchen (ab 1.2)
Ein Bündel im Körbchen, das man mit Leckerli, Katzenmilch, Streichelbürste und Wollknäuel großzieht.
Taler: 5 Startkapital, +1 pro neu gelöstem Level, +1 fürs Tagesrätsel, +1 pro Tag fürs Öffnen der App und am Taler-Automaten (10 Tipps oder 5 Joker = 1 Taler). Die Katze entwickelt sich, sobald Pflege und Level reichen,
eine Stufe nach der anderen. Stufen, Preise und Regeln: `app/src/main/java/de/schnurrfeld/app/data/Pet.kt` (`PetRules`).

## Dev-Version
`./gradlew :app:assembleDevRelease` baut „Schnurrfeld Dev“ (eigene App-ID, eigene Spielstände) mit Entwickler-Menü in den Einstellungen.

## Bedürfnisse (ab dem Rausspringen)
Hunger (Futter), Durst (Milch), Spieltrieb (Wollknäuel), Kuscheln (Bürste oder kostenlos streicheln). Sie sinken in Echtzeit – auch bei geschlossener App –
von voll bis „vermisst dich“ in etwa 18 Stunden, aber nie unter eine Untergrenze. Eine glückliche Katze bringt beim Tages-Taler +1 Bonus-Taler.
Regeln: `NeedRules` in `app/src/main/java/de/schnurrfeld/app/data/Pet.kt`.

## Schwierigkeits-Etiketten
`app/src/main/assets/level_tags.txt`: pro Level 0 (normal), 1 („Schwer“) oder 2 („Super schwer“). Jedes Level wird mit den anderen derselben
Feldgröße verglichen (schwierigste 20 % bzw. 5 %). Neu erzeugen: `DUMP_LEVELS=1 ./gradlew :app:testPlayDebugUnitTest --tests '*DifficultyReport*'`.

## Updates

Die App sucht beim Start (abschaltbar) und auf Knopfdruck in den Einstellungen nach dem neuesten GitHub-Release von `arthurtschik/Schnurrfeld`.

Konvention für ein Release:
- Tag `v1.3` (oder `1.3`), kein Pre-Release, kein Entwurf
- APK als Asset anhängen: `Schnurrfeld-1.3.apk`, optional `Schnurrfeld-1.3-Dev.apk` für die Dev-App
- gleiche Signatur (`app/schnurrfeld.jks`, nicht im Repo) und höherer `versionCode` als die installierte Version
- der Release-Text wird im Update-Dialog als "Neu:" angezeigt

Schritt-für-Schritt-Anleitung: [docs/RELEASE-ANLEITUNG.md](docs/RELEASE-ANLEITUNG.md)

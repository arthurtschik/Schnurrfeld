# Schnurrfeld 🐱

Ein Katzen-Logikrätsel für Android (Kotlin + Jetpack Compose).

**Regeln:** In jede Zeile, jede Spalte und jeden Farbbereich gehört genau eine Katze. Katzen dürfen sich nicht berühren – auch nicht diagonal.

## Installieren
Die neueste APK unter [Releases](https://github.com/arthurtschik/Schnurrfeld/releases/latest) herunterladen, öffnen und „Installation aus unbekannten Quellen“ erlauben (Android 8+).

## Features
- Fehlende Aufnahme (wird nicht genutzt, solange Finish/Nice job vorhanden sind): `voice_you_did_it.ogg` in `app/src/main/res/raw/` ablegen oder in der App aufnehmen
- 500 Levels (5×5 bis 10×10) mit sanftem Einstieg: Level 1–12 brauchen nur einfache Schritte, schwierigere Logik kommt nach und nach; ab Level 201 beginnt eine zweite, durchgehend anspruchsvolle Runde. Jedes Rätsel hat genau eine Lösung
- Ein laufendes Rätsel (Level, Tagesrätsel, Endlos) geht bei einem Anruf oder App-Wechsel nicht verloren: Es wird laufend gesichert und nach einem Neustart genau dort fortgesetzt
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
Mit `DUMP_FROM=201` bleiben die ersten 200 Levels unverändert und nur die neuen werden erzeugt (das gilt auch für die Schwierigkeits-Etiketten).
Sounds werden synthetisch erzeugt (`app/src/main/res/raw/*.wav`) und lassen sich durch eigene WAV-Dateien mit gleichem Namen ersetzen.


## Katzenkörbchen (ab 1.2)
Ein Bündel im Körbchen, das man mit Leckerli, Katzenmilch, Streichelbürste und Wollknäuel großzieht.
Ab der Teenie-Stufe (Stufe 5) wird das Körbchen zu klein: Ein großer Kratzbaum zieht ein (ab 1.3), mit zwei Wohnhöhlen in Katzengröße,
Kissen auf den Dächern und einer niedrigen Stufe. Die Katze kratzt, klettert, schläft oben oder verschwindet in einer Höhle; von außen sieht man eine Vorschau auf das Zimmer dahinter.
Ein Tipp auf eine Höhle öffnet das Zimmer: ein Wohnzimmer mit Kamin, Schaukelstuhl und Fischbild, ein Schlafzimmer mit Bett, Duftspender und Nachtfenster.
Die Zimmer sind in Perspektive gezeichnet, die Fenster zeigen die echte Tageszeit (Sonne, Dämmerung, Mond und Sterne) mit kleinen Ereignissen (Vögel, Schmetterling, Glühwürmchen, Eule, Sternschnuppe).
Vieles reagiert auf Antippen: Kamin (Funken), Fischbild, Schaukelstuhl, Bettdecke, Duftspender (lockt die Katze an), Lampen, Kuckucksuhr, Kerze, Pflanzen, Bücher, Kissen, Bilder, Teppich, Fenster.
Eine Aufzieh-Maus (Wohnzimmer) und ein Hüpfball (Schlafzimmer) sowie der Duftspender locken die Katze in den Raum, auch wenn sie gerade woanders ist.
Dazu gibt es echte Miau-Aufnahmen, abhängig vom Alter der Katze.

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
Die App sucht beim Start und auf Knopfdruck unter Einstellungen › App nach neuen Versionen.

## Datenschutz
- Keine Konten, keine Werbung, kein Tracking, keine Analyse-Dienste. Der Spielstand liegt nur auf dem Gerät.
- Das Mikrofon wird nur benutzt, wenn du selbst ein Lob-Wort aufnimmst. Aufnahmen bleiben auf dem Gerät und sind vom Android-Cloud-Backup ausgenommen.
- Die einzige Netzwerkverbindung ist die Update-Suche bei GitHub (abschaltbar unter Einstellungen › App). Dabei sieht GitHub wie bei jedem Webseitenaufruf deine IP-Adresse.

Mehr zur Sicherheit: [SECURITY.md](SECURITY.md)

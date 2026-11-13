# Sicherheit

## Unterstützte Versionen
Sicherheitskorrekturen gibt es für die jeweils neueste Version unter [Releases](https://github.com/arthurtschik/Schnurrfeld/releases/latest).

## Schwachstelle melden
Bitte nicht öffentlich als Issue beschreiben. Nutze stattdessen im Reiter **Security** die Schaltfläche
**Report a vulnerability**. Falls es sie dort nicht gibt, eröffne ein Issue ohne technische Details mit der Bitte um Kontakt.

## APK prüfen
Alle Versionen sind mit demselben Schlüssel signiert. Den Fingerabdruck kannst du mit dem Android-Tool `apksigner` prüfen:

```bash
apksigner verify --print-certs Schnurrfeld-1.4.apk
```

Erwartet wird dieser SHA-256-Fingerabdruck des Zertifikats:

```
94:EA:6F:7D:4D:96:FA:A5:68:3D:0E:91:C8:1E:8F:A2:9E:52:BF:7A:42:6D:48:9E:2C:D6:BC:BC:AB:90:44:54
```

Zu jedem Release gehört außerdem eine Datei `SHA256SUMS.txt` mit den Prüfsummen der APKs.
Stimmt der Fingerabdruck nicht, installiere die Datei nicht.

## Wie Updates abgesichert sind
- Die App lädt Updates nur per HTTPS und nur von GitHub aus diesem Repository.
- Vor der Installation prüft sie: gleicher App-Name, höhere Version, gleicher Signaturschlüssel.
- Android lehnt Updates mit fremder Signatur ohnehin ab.

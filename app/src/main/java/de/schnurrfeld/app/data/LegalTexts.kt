package de.schnurrfeld.app.data

/** Rechtstexte, die in den Einstellungen angezeigt werden. Gleicher Inhalt wie auf der Download-Seite. */
object LegalTexts {
    class Block(val heading: String, val text: String)
    class Doc(val title: String, val blocks: List<Block>)

    const val CONTACT_MAIL = "arthurtschik.business@gmail.com"
    private const val ADDRESS = "Arthurtschik\nc/o Block Services\nStuttgarter Str. 106\n70736 Fellbach"

    val impressum = Doc(
        "Impressum",
        listOf(
            Block("Angaben gemäß § 5 DDG", ADDRESS),
            Block("Kontakt", "Anfragen: $CONTACT_MAIL"),
            Block("Verantwortlich für den Inhalt (§ 18 Abs. 2 MStV)", "Arthurtschik, Anschrift wie oben."),
            Block("Hinweis", "Schnurrfeld ist ein privates, nicht kommerzielles Hobbyprojekt."),
        )
    )

    val datenschutz = Doc(
        "Datenschutzerklärung",
        listOf(
            Block("Kurz gesagt", "Schnurrfeld hat keine Konten, keine Werbung und kein Tracking. Dein Spielstand bleibt auf deinem Gerät. Die App verbindet sich nur mit GitHub, um nach Updates zu suchen. Das kannst du in den Einstellungen abschalten."),
            Block("Verantwortlicher", "$ADDRESS\nE-Mail: $CONTACT_MAIL"),
            Block("Was die App speichert", "Spielstand, Einstellungen und der Zustand deines Kätzchens liegen nur lokal auf dem Gerät. Die App legt kein Konto an, sendet keine Spielstände ab und nutzt keine Analyse- oder Werbedienste. Wenn du die Android-Datensicherung aktiviert hast, kann Android deinen Spielstand in dein Google-Konto sichern. Das geschieht über Android und Google, nicht über die App."),
            Block("Verbindung zu GitHub (Update-Suche)", "Beim Start der App und wenn du „Nach Updates suchen“ antippst, fragt die App bei GitHub (api.github.com, GitHub Inc., USA) nach der neuesten Version. Dabei erhält GitHub wie bei jedem Aufruf einer Webseite deine IP-Adresse und den Namen der App („Schnurrfeld-App“). Sonst wird nichts gesendet. Rechtsgrundlage ist unser berechtigtes Interesse, dich mit Sicherheitskorrekturen zu versorgen (Art. 6 Abs. 1 lit. f DSGVO). Die automatische Suche kannst du unter Einstellungen › App abschalten, dann fragt die App nur noch, wenn du selbst suchst.\n\nEin Update lädt die App nur von GitHub aus dem Projekt Schnurrfeld per HTTPS herunter. Vor der Installation prüft sie, dass die Datei dieselbe App, eine höhere Version und denselben Signaturschlüssel hat. Installiert wird erst, wenn du es in Android bestätigst."),
            Block("Berechtigungen", "• Internet: nur für die Update-Suche\n• Vibration: für das Feedback beim Spielen, abschaltbar\n• Apps installieren: damit die App ein heruntergeladenes Update nach deiner Bestätigung installieren kann\n\nDie App braucht kein Mikrofon, keinen Standort, keine Kontakte und keinen Speicherzugriff."),
            Block("Sprachausgabe", "Das gesprochene Lob kommt aus mitgelieferten Aufnahmen. Für Wörter ohne Aufnahme kann die App optional die Sprachausgabe deines Geräts nutzen („Computerstimme als Ersatz“). Die verarbeitet dein Gerät beziehungsweise dein Sprachdienst, nicht die App."),
            Block("Kinder", "Die App erhebt keine personenbezogenen Daten von Kindern, weil sie überhaupt keine Daten erhebt. Eine Anmeldung oder Eingabe persönlicher Angaben gibt es nicht."),
            Block("Deine Rechte", "Auskunft, Berichtigung, Löschung, Einschränkung, Datenübertragbarkeit und Widerspruch sowie Beschwerde bei einer Datenschutz-Aufsichtsbehörde. Schreib dafür an $CONTACT_MAIL. Deine Spieldaten löschst du, indem du in den Einstellungen den Fortschritt zurücksetzt oder die App deinstallierst."),
            Block("Stand", "November 2026"),
        )
    )

    val nutzung = Doc(
        "Nutzungsbedingungen",
        listOf(
            Block("Wer die App nutzen darf", "Schnurrfeld ist kostenlos und für die private Nutzung gedacht. Eine Registrierung gibt es nicht."),
            Block("Installation und Updates", "Die App wird nicht über den Play Store, sondern als Datei von GitHub verteilt. Installiere sie nur aus der offiziellen Quelle (Projekt Schnurrfeld auf GitHub) oder über die Update-Funktion der App. Die Echtheit prüfst du am Signatur-Fingerabdruck, den du in der Datei SECURITY.md im Projekt findest."),
            Block("Spielstand", "Der Spielstand liegt nur auf deinem Gerät. Beim Zurücksetzen, Deinstallieren oder Gerätewechsel ohne Datensicherung gehen Fortschritt, Sterne und Katzen verloren. Einen Anspruch auf Wiederherstellung gibt es nicht."),
            Block("Verfügbarkeit", "Die App wird ohne Gewähr bereitgestellt. Es gibt keinen Anspruch auf bestimmte Funktionen, Levels oder Updates, und Inhalte können sich ändern."),
            Block("Haftung", "Weil die App kostenlos ist, hafte ich nur für Vorsatz und grobe Fahrlässigkeit. Die gesetzliche Haftung für die Verletzung von Leben, Körper und Gesundheit sowie nach dem Produkthaftungsgesetz bleibt unberührt."),
            Block("Inhalte", "Grafiken, Levels, Töne und Texte der App gehören dem Entwickler beziehungsweise ihren jeweiligen Urhebern. Bitte verwende sie nicht ohne Erlaubnis in eigenen Projekten."),
            Block("Kontakt", "Fragen und Hinweise an $CONTACT_MAIL"),
        )
    )
}

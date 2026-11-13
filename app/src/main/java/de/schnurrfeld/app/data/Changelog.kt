package de.schnurrfeld.app.data

/** What is new in each version – shown in the settings ("Was ist neu"). The newest version comes first. */
object Changelog {
    data class Section(val title: String, val items: List<String>)
    data class Entry(val version: String, val title: String, val sections: List<Section>)

    val entries = listOf(
        Entry(
            "1.4", "Der Kratzbaum",
            listOf(
                Section("Kratzbaum mit Wohnhöhlen", listOf(
                    "Ab Stufe 5 ersetzt ein großer Kratzbaum das Körbchen",
                    "Zwei Wohnhöhlen in Katzengröße, Kissen auf den Dächern und eine niedrige Stufe",
                    "Die Katze kratzt am Pfosten, klettert hinauf, schläft oben oder verschwindet in einer Höhle",
                    "Von außen sieht man eine kleine Vorschau auf das Zimmer dahinter",
                    "Kommt Futter an, während die Katze oben oder zu Hause ist, kommt sie heraus",
                )),
                Section("Zwei Zimmer zum Entdecken", listOf(
                    "Ein Tipp auf eine Höhle zeigt das Zuhause der Katze: ein Wohnzimmer mit Kamin, Schaukelstuhl und Bücherregal, ein Schlafzimmer mit Bett, Kleiderschrank und Duftspender",
                    "Die Fenster zeigen die echte Tageszeit: Sonne, Dämmerung, Mond und Sterne",
                    "Draußen passiert etwas: Vögel, ein Schmetterling, Glühwürmchen, eine Eule, Sternschnuppen",
                    "Vieles reagiert auf Antippen: der Kamin sprüht Funken, der Fisch im Bild schwimmt davon, die Bettdecke rutscht weg, Lampen gehen an und aus, die Uhr hat einen Kuckuck und mehr",
                    "Eine Aufzieh-Maus und ein Hüpfball laden zum Spielen ein. Antippen setzt sie in Bewegung, und die Katze kommt in den Raum und spielt mit. Der Duftspender lockt sie ebenfalls herbei",
                )),
                Section("Rätsel gehen nicht mehr verloren", listOf(
                    "Ein Anruf oder ein kurzer Wechsel in eine andere App beendet dein Rätsel nicht mehr",
                    "Level, Tagesrätsel und Endlos werden laufend gesichert und nach einem Neustart genau dort fortgesetzt",
                    "Wer ein Rätsel bewusst mit Zurück verlässt, beendet es wie bisher",
                )),
                Section("300 neue Levels", listOf(
                    "Jetzt 500 Levels, die ersten 200 sind unverändert",
                    "Die zweite Runde beginnt bei 8×8 und geht bis 10×10, durchgehend anspruchsvoll",
                )),
                Section("Echte Miaus", listOf(
                    "Aufnahmen von Kätzchen und größeren Katzen, passend zum Alter deiner Katze, bewusst leise",
                    "Sie miaut beim Schlüpfen, wenn sie dich vermisst, nach dem Streicheln und manchmal einfach so",
                )),
                Section("Verbessert", listOf(
                    "Die Katze ist in den Stufen 5 und 6 etwas kleiner, damit sie zu den neuen Möbeln passt",
                    "Gibst du der Katze etwas, lässt sie ihre Pläne sofort fallen und kommt zum Futter, zur Milch oder zum Spielzeug",
                    "Beschädigte Spieldaten bringen die App nicht mehr zum Absturz",
                    "Entfernt: die Aufnahme und der Import eigener Lob-Stimmen. Die App braucht dadurch keine Mikrofon-Berechtigung mehr",
                    "Neu in den Einstellungen: der Bereich \"Was ist neu\"",
                )),
            ),
        ),
        Entry(
            "1.2.2", "Sicherheit und Datenschutz",
            listOf(
                Section("Sicherheit", listOf(
                    "Updates werden nur noch per HTTPS und nur von GitHub aus diesem Repository angenommen",
                    "Die heruntergeladene Datei wird geprüft, bevor Android sie sieht: richtige App, höhere Version, gleicher Signaturschlüssel",
                    "Unverschlüsselter Netzwerkverkehr ist komplett abgeschaltet",
                )),
                Section("Datenschutz", listOf(
                    "Eigene Sprachaufnahmen sind vom Android-Cloud-Backup ausgenommen und bleiben auf dem Gerät",
                    "Die Update-Einstellung sagt offen, was gesendet wird: nur die IP-Adresse an GitHub",
                )),
            ),
        ),
        Entry(
            "1.2", "Das Katzenkörbchen",
            listOf(
                Section("Katzenkörbchen", listOf(
                    "Ein Bündel im Körbchen, aus dem je nach Pflege eine von sechs Katzen schlüpft und heranwächst",
                    "Hunger, Durst, Spielen und Kuscheln sinken langsam, das Kätzchen wird nur etwas traurig, nie krank",
                    "Taler als Währung, ein Shop mit Rabattmarken und ein Taler-Automat für Tipps und Joker",
                )),
                Section("Spiel", listOf(
                    "Tutorial: die Regeln Schritt für Schritt an einem kleinen Rätsel",
                    "200 Levels bis 10×10, schwere und super schwere Levels sind mit Flammen markiert",
                    "Helfer-Katze in den ersten 10 Levels mit Gratis-Tipp",
                    "Automatische Updates über GitHub (Einstellungen › App)",
                )),
            ),
        ),
    )
}

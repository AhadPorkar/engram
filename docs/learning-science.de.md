# Die Lernwissenschaft hinter Engram

[English](learning-science.md) · Deutsch

Engram verbindet zwei Traditionen:

1. **Planung verteilter Wiederholung** (Anki, SuperMemo) entscheidet, *wann* ein Wort geübt wird.
2. **Übungsdesign** (Memrise, Babbel, Duolingo) entscheidet, *wie* es geübt wird.

Dieses Dokument erklärt beides und verweist auf den Code, der es umsetzt.

---

## 1. Wann wiederholen: FSRS-6

### Die Vergessenskurve
Erinnerungen verblassen mit der Zeit, und jede erfolgreiche Wiederholung lässt sie langsamer verblassen
(Ebbinghaus, 1885). FSRS beschreibt jede Karte mit drei Zahlen, dem **DSR-Modell**:

| Symbol | Name | Bedeutung |
|---|---|---|
| **R** | Abrufwahrscheinlichkeit (Retrievability) | Wahrscheinlichkeit, sich *jetzt* an die Karte zu erinnern (0–1) |
| **S** | Stabilität | Tage, bis R von 100 % auf 90 % fällt |
| **D** | Schwierigkeit | Wie schwer die Karte für dich ist, 1–10 |

FSRS-6 nutzt eine Vergessenskurve nach einem Potenzgesetz mit trainierbarem Zerfall $w_{20}$:

$$R(t, S) = \left(1 + F \cdot \frac{t}{S}\right)^{-w_{20}}, \qquad F = 0{,}9^{-1/w_{20}} - 1$$

$F$ ist so gewählt, dass $R(S, S) = 0{,}9$ gilt. Die Umkehrung der Kurve liefert das **nächste Intervall** für
eine gewünschte Behaltensquote $r$:

$$I(r, S) = \frac{S}{F}\left(r^{-1/w_{20}} - 1\right)$$

Bei $r = 0{,}9$ entspricht das Intervall der Stabilität. Wer $r$ auf 0,95 erhöht, bekommt kürzere Intervalle.
Wer $r$ auf 0,8 senkt, bekommt längere Intervalle, vergisst aber mehr.
→ `FsrsAlgorithm.retrievability`, `FsrsAlgorithm.interval`

### Aktualisierung nach einer Antwort
Bewertungen sind $G \in \{1{:}\text{Nochmal}, 2{:}\text{Schwer}, 3{:}\text{Gut}, 4{:}\text{Leicht}\}$.

**Erste Wiederholung**: $S_0 = w_{G-1}$, $\;D_0 = w_4 - e^{w_5 (G-1)} + 1$

**Schwierigkeit** (mit linearer Dämpfung und Rückkehr zum Mittelwert $D_0(4)$):

$$D' = w_7 \, D_0(4) + (1 - w_7)\left(D - w_6 (G-3) \cdot \frac{10 - D}{9}\right)$$

**Erfolgreicher Abruf** ($G \ge 2$):

$$S' = S\left(1 + e^{w_8}\,(11 - D)\,S^{-w_9}\,\left(e^{w_{10}(1-R)} - 1\right)\cdot h \cdot b\right)$$

mit dem Abzug für „Schwer“ $h = w_{15}$ (nur bei Schwer) und dem Bonus für „Leicht“ $b = w_{16}$ (nur bei Leicht).
Zwei Eigenschaften sind fürs Lernen wichtig:

- **Je niedriger R war, desto größer der Gewinn.** Sich an fast Vergessenes zu erinnern, stärkt das
  Gedächtnis mehr, als Bekanntes erneut zu bestätigen. Anstrengendes Abrufen wirkt besser als leichtes Abrufen.
- **Stabile Erinnerungen wachsen langsamer** ($S^{-w_9}$), und **schwierige Karten wachsen langsamer** ($11 - D$).

**Vergessen** ($G = 1$):

$$S'_f = \min\left(w_{11}\,D^{-w_{12}}\,\left((S+1)^{w_{13}} - 1\right)e^{w_{14}(1-R)},\; \frac{S}{e^{w_{17} w_{18}}}\right)$$

**Wiederholung am selben Tag** (Lernschritte): $S' = S \cdot e^{w_{17}(G - 3 + w_{18})} \cdot S^{-w_{19}}$, bei Gut oder Leicht nie kleiner als $S$.

Die 21 Standardgewichte sind die veröffentlichten FSRS-6-Standardwerte, trainiert auf Wiederholungsdaten von etwa 10.000 Anki-Nutzern.
→ `core/srs/FsrsAlgorithm.kt`

### Lernschritte, Übergang in Wiederholung und Streuung
Neue und vergessene Karten durchlaufen zuerst kurze **Lernschritte** (Standard 1 Min. → 10 Min.; Wiederlernen 10 Min.),
wie bei Anki. Sie kommen *in derselben Sitzung* zurück, was kurzfristige Abstände nutzt. Nach dem letzten Schritt
wechselt die Karte in tagesbasierte Intervalle. Lange Intervalle bekommen eine kleine zufällige **Streuung**
(±15 % für 2,5–7 Tage, ±10 % bis 20 Tage, ±5 % darüber). So entfernen sich zusammen gelernte Wörter mit der Zeit voneinander.
→ `core/srs/FsrsScheduler.kt`

### Überprüfung
`FsrsSchedulerTest` reproduziert die Intervallfolge der offiziellen `py-fsrs`-6-Testsuite
(`0, 2, 11, 46, 163, 498, 0, 0, 2, 4, 7, 12, 21` Tage für Gut×6, Nochmal×2, Gut×5). Damit ist belegt, dass die
Kotlin-Portierung numerisch der Referenzimplementierung entspricht.

### Sitzungsplanung
→ `core/learning/session/SessionPlanner.kt`
- **Lernkarten zuerst.** Das Kurzzeitgedächtnis ist am empfindlichsten.
- **Wiederholungen nach niedrigster Abrufwahrscheinlichkeit sortiert.** Wird das Tageslimit erreicht, sind die Wörter
  wiederholt worden, die am kürzesten vor dem Vergessen standen.
- **Geschwisterkarten werden zurückgestellt.** Höchstens eine Karte pro Wort und Sitzung. Sonst würde die
  Erkennungskarte (*Hund → dog*) die Produktionskarte (*dog → Hund*) wenige Minuten später verraten.
- **Neue Karten werden verschachtelt** (eine neue nach je vier Wiederholungen), und Decks werden reihum gemischt.
  Verschachteltes Üben verbessert die Unterscheidung ähnlicher Inhalte (Kornell & Bjork, 2008).
- **Ein Lerntag beginnt um 4 Uhr.** Späte Lerneinheiten zählen noch zum Vortag, wie bei Anki.

---

## 2. Wie üben: die Übungsleiter

### Abrufen statt erneut lesen
Jede Übung in Engram verlangt, Wissen *abzurufen*. Sich selbst zu testen verbessert das langfristige Behalten
deutlich stärker als erneutes Lernen desselben Stoffs: der **Testeffekt** (Roediger & Karpicke, 2006).

### Erwünschte Erschwernisse
Abrufen soll **anstrengend, aber erfolgreich** sein (Bjork, 1994). Ist es zu leicht, wird wenig gelernt.
Ist es zu schwer, misslingt es und frustriert. Engram wählt das Übungsformat nach der aktuellen
**Stabilität S** der Karte, sodass die Schwierigkeit mit der Erinnerung wächst:

| Gedächtnisstabilität S | Erkennungskarte (Wort → Bedeutung) | Produktionskarte (Bedeutung → Wort) |
|---|---|---|
| neu | *Kontakt*: Einführung (Wort, Bedeutung, Beispiel, Audio, Eselsbrücke) | noch nicht eingeführt |
| < 1 Tag | *Wiedererkennen*: Multiple Choice | *Wiedererkennen*: umgekehrtes Multiple Choice |
| 1–4 Tage | Multiple Choice | *Gestütztes Abrufen*: Buchstabenkacheln · umgekehrtes Multiple Choice |
| 4–14 Tage | Multiple Choice · Hörverstehen | Buchstabenkacheln · Lückentext im Beispielsatz · *freies Abrufen*: Tippen |
| ≥ 14 Tage | Hörverstehen · selbst bewertete Karteikarte | Tippen · Lückentext · Sprechen |

→ `core/learning/exercise/ExerciseSelector.kt`

Ein neues Wort wird **zuerst vorgestellt** und **einige Karten später abgefragt**, nicht sofort. Eine kurze
Verzögerung vor dem ersten Abruf stärkt das Einprägen mehr als eine sofortige Wiederholung (expandierendes Abrufen).
→ `StudySession.completePresentation`

### Rezeptiv vor produktiv
Ein Wort zu verstehen (Wiedererkennen) kommt meist vor seiner Verwendung (Produktion) (Nation, 2001). Jede Notiz hat
deshalb zwei Karten. Die **Produktionskarte** (Bedeutung → Wort) wird erst eingeführt, *nachdem* die
Erkennungskarte nicht mehr neu ist. Sie steigt auch weiter auf der Leiter, bis zum Tippen und Sprechen.

### Der Generierungseffekt
Was man selbst erzeugt, behält man besser als das, was man nur liest (Slamecka & Graf, 1978).
Bei Tipp-, Lückentext- und Sprechübungen erzeugst du das Wort selbst.

### Plausible Distraktoren
Die Antwortoptionen im Multiple Choice werden so gewählt, dass sie der Lösung *ähneln*: ähnliche Länge, gleicher
Anfangsbuchstabe. Lernende müssen sich dann wirklich erinnern, statt Optionen nach dem Aussehen auszuschließen.
→ `DistractorPicker`

### Automatische Bewertung
Menschen schätzen ihr eigenes Gedächtnis schlecht ein. Engram leitet die FSRS-Bewertung deshalb aus dem
Verhalten ab (→ `RatingPolicy`):

| Ergebnis | Bewertung |
|---|---|
| Falsch | Nochmal |
| Tippfehler, falscher/fehlender Artikel, Synonym, Tipp genutzt oder sehr langsam | Schwer |
| Richtiges Wiedererkennen oder gestütztes Abrufen | Gut (nie Leicht: Wiedererkennen ist leichter als Abrufen) |
| Richtiges, flüssiges freies Abrufen | Leicht |
| Sonst richtig | Gut |

Der Modus *Karteikarten* behält die klassische Selbstbewertung für alle, die sie bevorzugen. Jede der vier Tasten
zeigt das Intervall, das sie ergeben würde.

### Feedback, das lehrt
Das Feedback kommt sofort und ist **konkret** (Hattie & Timperley, 2007). Ein Vergleich auf Buchstabenebene zeigt
genau, welche Buchstaben fehlten oder zu viel waren. Fehlende Artikel und Tippfehler werden als solche benannt.
Synonyme gelten als „fast richtig“, und das Zielwort wird angezeigt. Bei vergessenen Wörtern erscheint die
Eselsbrücke. Mit „Ich lag richtig“ können Lernende die Bewertung bei gültigen Alternativen überstimmen.

### Großzügig bei der Form, streng beim Inhalt
Groß-/Kleinschreibung, Satzzeichen, zusätzliche Leerzeichen, Anmerkungen in Klammern und optional Akzente werden
ignoriert (`strasse` = `Straße`). Englische Füllwörter („to go“, „the house“) sind optional. Der **Artikel eines deutschen
Nomens ist nicht optional**: Das grammatische Geschlecht gehört zum Wort, deshalb gilt *die Hund* als „fast richtig“.
→ `TextNormalizer`, `AnswerEvaluator`

### Duale Kodierung und Elaboration
Wörter werden gezeigt *und* gesprochen (Text-to-Speech, normal oder langsam), und Hörübungen trainieren die
gesprochene Form (duale Kodierung, Paivio). Beispielsätze liefern Kontext, und Eselsbrücken unterstützen
elaboratives Enkodieren. Wird ein Wort zum **Problemwort** (8-mal vergessen), schlägt Engram eine Eselsbrücke vor
statt weiterer Wiederholungen, denn reine Wiederholung wirkt bei diesem Wort nicht mehr.

### Motivation ohne Manipulation
Tagesziel, Serien und eine Erinnerung helfen beim Aufbau einer Gewohnheit. Die Erinnerung kommt nur, wenn etwas
zu wiederholen ist **und** das Ziel noch nicht erreicht ist. Die Statistik zeigt die **tatsächliche Behaltensquote**
und eine Schätzung, **wie viele Wörter du gerade kannst** (Σ R). So bleibt der Fokus auf echtem Fortschritt statt auf Punkten.

---

## Literatur
- Bjork, R. A. (1994). Memory and metamemory considerations in the training of human beings. In *Metacognition: Knowing about Knowing*. MIT Press.
- Cepeda, N. J., Pashler, H., Vul, E., Wixted, J. T., & Rohrer, D. (2006). Distributed practice in verbal recall tasks: A review and quantitative synthesis. *Psychological Bulletin, 132*(3).
- Ebbinghaus, H. (1885). *Über das Gedächtnis*. Duncker & Humblot.
- Hattie, J., & Timperley, H. (2007). The power of feedback. *Review of Educational Research, 77*(1).
- Kornell, N., & Bjork, R. A. (2008). Learning concepts and categories: Is spacing the "enemy of induction"? *Psychological Science, 19*(6).
- Nation, I. S. P. (2001). *Learning Vocabulary in Another Language*. Cambridge University Press.
- Paivio, A. (1986). *Mental Representations: A Dual Coding Approach*. Oxford University Press.
- Roediger, H. L., & Karpicke, J. D. (2006). Test-enhanced learning. *Psychological Science, 17*(3).
- Slamecka, N. J., & Graf, P. (1978). The generation effect. *Journal of Experimental Psychology: Human Learning and Memory, 4*(6).
- Ye, J., Su, J., & Cao, Y. (2022). A stochastic shortest path algorithm for optimizing spaced repetition scheduling. *KDD '22*.
- open-spaced-repetition: *The Algorithm* (FSRS-Wiki) und `py-fsrs`, https://github.com/open-spaced-repetition

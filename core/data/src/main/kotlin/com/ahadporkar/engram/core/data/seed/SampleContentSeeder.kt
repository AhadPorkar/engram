package com.ahadporkar.engram.core.data.seed

import com.ahadporkar.engram.core.data.repository.DeckRepository
import com.ahadporkar.engram.core.data.repository.NoteDraft
import com.ahadporkar.engram.core.data.repository.NoteRepository
import com.ahadporkar.engram.core.data.repository.SettingsRepository
import com.ahadporkar.engram.core.model.Deck
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Adds two starter decks on the very first launch so the app is useful immediately. */
@Singleton
class SampleContentSeeder @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val deckRepository: DeckRepository,
    private val noteRepository: NoteRepository,
    private val clock: Clock,
) {
    suspend fun seedIfNeeded() {
        if (settingsRepository.isSampleContentSeeded()) return
        if (deckRepository.getDecks().isEmpty()) {
            SampleDecks.all.forEach { sample ->
                val deckId = deckRepository.saveDeck(sample.deck.copy(createdAt = clock.instant()))
                noteRepository.importNotes(deckId, sample.notes, withProductionCards = true)
            }
        }
        settingsRepository.markSampleContentSeeded()
    }
}

data class SampleDeck(val deck: Deck, val notes: List<NoteDraft>)

object SampleDecks {

    private fun note(front: String, back: String, synonyms: String, example: String, vararg tags: String) =
        NoteDraft(front = front, back = back, synonyms = synonyms, example = example, tags = tags.toSet())

    val germanA1 = SampleDeck(
        deck = Deck(
            name = "German A1 · Everyday words",
            description = "High-frequency words with example sentences.",
            frontLanguage = "de-DE",
            backLanguage = "en-US",
            newCardsPerDay = 10,
            createdAt = java.time.Instant.EPOCH,
        ),
        notes = listOf(
            note("der Hund", "the dog", "", "Der Hund schläft im Garten.", "a1", "noun"),
            note("die Katze", "the cat", "", "Die Katze trinkt Milch.", "a1", "noun"),
            note("das Haus", "the house", "", "Unser Haus ist klein, aber gemütlich.", "a1", "noun"),
            note("das Wasser", "the water", "", "Kann ich ein Glas Wasser haben?", "a1", "noun"),
            note("das Brot", "the bread", "", "Ich kaufe jeden Morgen frisches Brot.", "a1", "noun"),
            note("der Apfel", "the apple", "", "Der Apfel ist rot und süß.", "a1", "noun"),
            note("die Arbeit", "the work; the job", "", "Ich fahre mit dem Fahrrad zur Arbeit.", "a1", "noun"),
            note("der Bahnhof", "the train station", "", "Der Bahnhof ist zehn Minuten entfernt.", "a1", "noun"),
            note("die Wohnung", "the apartment; the flat", "", "Die Wohnung hat drei Zimmer.", "a1", "noun"),
            note("der Freund", "the friend", "", "Mein Freund wohnt in Berlin.", "a1", "noun"),
            note("die Zeit", "the time", "", "Hast du heute Zeit?", "a1", "noun"),
            note("das Jahr", "the year", "", "Das Jahr hat zwölf Monate.", "a1", "noun"),
            note("die Stadt", "the city; the town", "", "Die Stadt ist im Winter sehr ruhig.", "a1", "noun"),
            note("gehen", "to go; to walk", "laufen", "Wir gehen heute ins Kino.", "a1", "verb"),
            note("kommen", "to come", "", "Kommen Sie aus Spanien?", "a1", "verb"),
            note("sprechen", "to speak", "reden", "Sprechen Sie Englisch?", "a1", "verb"),
            note("verstehen", "to understand", "", "Ich kann dich gut verstehen.", "a1", "verb"),
            note("arbeiten", "to work", "", "Wir arbeiten von neun bis fünf.", "a1", "verb"),
            note("kaufen", "to buy", "", "Ich möchte ein Ticket kaufen.", "a1", "verb"),
            note("wohnen", "to live (reside)", "leben", "Wir wohnen in Hamburg.", "a1", "verb"),
            note("lernen", "to learn; to study", "", "Wir lernen jeden Tag Deutsch.", "a1", "verb"),
            note("schön", "beautiful; nice", "hübsch", "Das Wetter ist heute schön.", "a1", "adjective"),
            note("groß", "big; tall", "", "Mein Bruder ist sehr groß.", "a1", "adjective"),
            note("klein", "small; little", "", "Das Zimmer ist klein.", "a1", "adjective"),
            note("teuer", "expensive", "", "Das Auto ist zu teuer.", "a1", "adjective"),
            note("billig", "cheap", "günstig", "Das Brot ist billig.", "a1", "adjective"),
            note("heute", "today", "", "Heute ist Montag.", "a1"),
            note("morgen", "tomorrow", "", "Morgen fahre ich nach München.", "a1"),
            note("danke", "thank you; thanks", "", "Danke für deine Hilfe!", "a1"),
            note("Entschuldigung", "excuse me; sorry", "", "Entschuldigung, wo ist der Bahnhof?", "a1"),
        ),
    )

    val englishAcademic = SampleDeck(
        deck = Deck(
            name = "English · Academic words",
            description = "Word, meaning and synonyms — the format of the original ProjectShaco app.",
            frontLanguage = "en-US",
            backLanguage = "en-US",
            newCardsPerDay = 10,
            createdAt = java.time.Instant.EPOCH,
        ),
        notes = listOf(
            note("ubiquitous", "present everywhere", "omnipresent, pervasive", "Smartphones have become ubiquitous in modern life."),
            note("meticulous", "showing great attention to detail", "thorough, careful", "She keeps meticulous records of every experiment."),
            note("ambiguous", "open to more than one interpretation", "unclear, vague", "The wording of the contract was ambiguous."),
            note("mitigate", "to make less severe", "alleviate, reduce", "Regular backups mitigate the risk of data loss."),
            note("inevitable", "certain to happen", "unavoidable", "Change is inevitable in software projects."),
            note("resilient", "able to recover quickly", "tough, robust", "Resilient systems recover from failures automatically."),
            note("feasible", "possible and practical to do", "viable, achievable", "The plan is technically feasible."),
            note("coherent", "logical and consistent", "consistent, clear", "Please write a coherent summary of the results."),
            note("scrutinize", "to examine closely", "inspect, examine", "Reviewers scrutinize every line of code."),
            note("pragmatic", "dealing with things sensibly and realistically", "practical, realistic", "We chose a pragmatic solution."),
            note("concise", "giving information clearly in few words", "brief, succinct", "Keep your answers concise."),
            note("diligent", "showing care and effort in work", "hard-working, conscientious", "A diligent student reviews every day."),
            note("deteriorate", "to become progressively worse", "decline, worsen", "Memories deteriorate without review."),
            note("substantial", "of considerable importance or size", "significant, considerable", "The update brought a substantial speed-up."),
            note("advocate", "to publicly support", "champion, support", "Many teachers advocate spaced repetition."),
            note("consolidate", "to make stronger or more solid", "strengthen, unify", "Sleep helps consolidate new memories."),
            note("comprehensive", "including all or nearly all elements", "thorough, complete", "The guide gives a comprehensive overview."),
            note("emphasize", "to give special importance to", "stress, highlight", "Good teachers emphasize pronunciation."),
            note("retain", "to continue to have or keep", "keep, remember", "Testing yourself helps you retain information."),
            note("enhance", "to improve the quality of", "improve, boost", "Pictures can enhance understanding."),
        ),
    )

    val all = listOf(germanA1, englishAcademic)
}

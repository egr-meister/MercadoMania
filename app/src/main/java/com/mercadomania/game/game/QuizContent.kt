package com.mercadomania.game.game

/** One single-answer question. [correctIndex] indexes into [options]. */
data class QuizQuestion(
    val text: String,
    val options: List<String>,
    val correctIndex: Int
) {
    val isValid: Boolean
        get() = options.size >= 2 && correctIndex in options.indices && text.isNotBlank()
}

/**
 * A quiz category.
 *
 * @param id stable storage key - do not change it once players have scores
 * @param iconIndex index into `Assets.items` used as the category emblem
 */
data class QuizCategory(
    val id: String,
    val title: String,
    val blurb: String,
    val iconIndex: Int,
    val questions: List<QuizQuestion>
)

/**
 * ===========================================================================
 * PLACEHOLDER CONTENT - REPLACE ME
 * ===========================================================================
 * Every question below is themed filler written for the "Golden Bazaar" world
 * of Mercado Mania. It exists so the game is playable end to end today.
 *
 * To ship real content: swap the [QuizQuestion] lists here and nothing else.
 * Keep the category [QuizCategory.id] values (`treasures`, `trade`, `legends`)
 * unchanged, or previously saved best scores will no longer line up.
 * Keep ten questions per category, or adjust
 * [GameLogic.QUIZ_QUESTIONS_PER_CATEGORY].
 * ===========================================================================
 */
object QuizContent {

    val categories: List<QuizCategory> = listOf(
        QuizCategory(
            id = "treasures",
            title = "Bazaar Treasures",
            blurb = "The relics on the stalls",
            iconIndex = 3, // chest
            questions = listOf(
                QuizQuestion(
                    "Which relic marks the moment the bazaar closes for the night?",
                    listOf("The Hourglass", "The Crown", "The Anchor", "The Compass"),
                    0
                ),
                QuizQuestion(
                    "The Gilded Lamp is traded only in exchange for what?",
                    listOf("Ten gold coins", "A promise", "A rival's lamp", "Nothing at all"),
                    1
                ),
                QuizQuestion(
                    "What sits at the very centre of the Blue Diamond's setting?",
                    listOf("A pearl", "A drop of ink", "A single facet of ice", "A grain of sand"),
                    2
                ),
                QuizQuestion(
                    "Which treasure is never sold, only lent?",
                    listOf("The Crown", "The Chest", "The Scepter", "The Map"),
                    3
                ),
                QuizQuestion(
                    "The Sunburst medallion is the emblem of which stall row?",
                    listOf("The Morning Row", "The Ember Row", "The Quiet Row", "The Salt Row"),
                    0
                ),
                QuizQuestion(
                    "How many locks guard a merchant's Blue Chest?",
                    listOf("One", "Two", "Three", "Nine"),
                    2
                ),
                QuizQuestion(
                    "What is the Golden Key said to open?",
                    listOf("Any door twice", "The last stall of the night", "A rival's purse", "Nothing yet built"),
                    1
                ),
                QuizQuestion(
                    "The Compass of the bazaar always points toward what?",
                    listOf("North", "The nearest gem", "The loudest haggle", "The way home"),
                    3
                ),
                QuizQuestion(
                    "Which relic is handed to the fairest trader of the season?",
                    listOf("The Scepter", "The Lamp", "The Hourglass", "The Shield"),
                    0
                ),
                QuizQuestion(
                    "The Blue Bloom flower is prized because it does what?",
                    listOf("Never wilts", "Changes colour at noon", "Smells of rain", "Doubles in size"),
                    0
                )
            )
        ),

        QuizCategory(
            id = "trade",
            title = "Trade & Coin",
            blurb = "Haggling, coin and custom",
            iconIndex = 1, // crown
            questions = listOf(
                QuizQuestion(
                    "What is the first rule of the Mercado floor?",
                    listOf("Never quote first", "Never trade at dawn", "Never count aloud", "Never look up"),
                    0
                ),
                QuizQuestion(
                    "A trader who taps the counter twice is signalling what?",
                    listOf("A refusal", "A final offer", "A request for tea", "A closed stall"),
                    1
                ),
                QuizQuestion(
                    "Which coin is worth the most on the Mercado floor?",
                    listOf("The copper sol", "The silver luna", "The gold corona", "The glass ficha"),
                    2
                ),
                QuizQuestion(
                    "How is a deal sealed at the golden bazaar?",
                    listOf("A written note", "A shared cup", "A struck bell", "A traded token"),
                    3
                ),
                QuizQuestion(
                    "What does an upturned basket in front of a stall mean?",
                    listOf("Sold out", "Back shortly", "Free samples", "New arrival"),
                    0
                ),
                QuizQuestion(
                    "Apprentice traders begin their day by doing what?",
                    listOf("Counting the till", "Sweeping the aisle", "Polishing the sign", "Opening the awning"),
                    3
                ),
                QuizQuestion(
                    "The market bell rings how many times at closing?",
                    listOf("Once", "Three times", "Seven times", "Twelve times"),
                    1
                ),
                QuizQuestion(
                    "A trader who offers a second price without being asked is called what?",
                    listOf("Generous", "Impatient", "Honest", "Lost"),
                    1
                ),
                QuizQuestion(
                    "What may never be bartered inside the bazaar walls?",
                    listOf("Water", "Maps", "Names", "Lamps"),
                    2
                ),
                QuizQuestion(
                    "The busiest hour on the Mercado floor is known as what?",
                    listOf("The Gold Hour", "The Long Noon", "The Quiet Turn", "The Late Call"),
                    0
                )
            )
        ),

        QuizCategory(
            id = "legends",
            title = "Bazaar Legends",
            blurb = "Tales told between the stalls",
            iconIndex = 5, // lamp
            questions = listOf(
                QuizQuestion(
                    "Who is said to host the golden bazaar after dark?",
                    listOf("Pico the gecko", "Zafir the lamp genie", "Tino the automaton", "The Quiet Trader"),
                    1
                ),
                QuizQuestion(
                    "Pico the golden gecko is famous for what?",
                    listOf("Never losing a bet", "Running the fastest errand", "Reading any map", "Counting in his sleep"),
                    1
                ),
                QuizQuestion(
                    "Tino the automaton was built to do what?",
                    listOf("Guard the chest", "Weigh every coin", "Light the lamps", "Fold the awnings"),
                    1
                ),
                QuizQuestion(
                    "The legend of the Hourglass warns traders against what?",
                    listOf("Greed", "Haste", "Silence", "Sleep"),
                    1
                ),
                QuizQuestion(
                    "What is said to happen if the market bell rings at midnight?",
                    listOf("A stall disappears", "A wish is granted", "The lamps go out", "The gates reopen"),
                    3
                ),
                QuizQuestion(
                    "In the old tale, the Crown was won by whom?",
                    listOf("A king", "A thief", "A child", "A merchant"),
                    2
                ),
                QuizQuestion(
                    "The Blue Bloom is supposed to grow only where what has happened?",
                    listOf("A fair trade", "A broken promise", "A first visit", "A last goodbye"),
                    0
                ),
                QuizQuestion(
                    "Zafir grants a wish only to a trader who has done what?",
                    listOf("Paid in gold", "Asked three times", "Given something away", "Waited a year"),
                    2
                ),
                QuizQuestion(
                    "What does the Treasure Map in the tales never show?",
                    listOf("The way back", "The final step", "The starting point", "The true north"),
                    1
                ),
                QuizQuestion(
                    "How does every bazaar legend end?",
                    listOf("With a bargain", "With a storm", "With a song", "With a question"),
                    0
                )
            )
        )
    )

    /** Stable ids, in display order. */
    val categoryIds: List<String> = categories.map { it.id }

    /** Lookup by id. Returns `null` for an unknown or stale id. */
    fun category(id: String?): QuizCategory? =
        if (id.isNullOrBlank()) null else categories.firstOrNull { it.id == id }
}

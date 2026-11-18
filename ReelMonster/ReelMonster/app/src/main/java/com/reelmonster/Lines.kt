package com.reelmonster

object Lines {
    private val milestones = mapOf(
        1 to "Just one, right? Right?",
        5 to "Five. I'm getting a little chubby.",
        10 to "Ten! Your thumb filed a complaint.",
        15 to "Blink. Drink water. Anything.",
        20 to "Twenty. I can see my reflection in your eyes.",
        30 to "Thirty. Your couch says hi.",
        40 to "Forty. Your future self just sighed.",
        50 to "FIFTY. I'm the size of your regrets.",
        75 to "75. I have eaten your whole afternoon.",
        100 to "100! Go touch grass. I'll wait."
    )
    private val tiers = listOf(
        listOf("Nom nom.", "Another one? Okay.", "I'm just watching with you."),
        listOf("Scroll, scroll, scroll...", "Is this really what you wanted?", "I'm growing. You're shrinking."),
        listOf("Your neck hurts. Admit it.", "I'm bigger than your plans now.", "Tomorrow-you is so disappointed."),
        listOf("I'm a MONSTER now. Thanks.", "Put. The. Phone. Down.", "Even I am tired, and I'm made of pixels.")
    )

    fun forCount(n: Int): String {
        milestones[n]?.let { return it }
        val tier = when { n < 10 -> 0; n < 30 -> 1; n < 60 -> 2; else -> 3 }
        return tiers[tier].random()
    }
}

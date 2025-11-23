package com.mobdeve.s17.group39.itismob_mco.utils

import android.view.View
import android.widget.TextView

object LoadingUtils {
    private const val TAG = "LoadingUtils"

    // List of quotes
    private val fallbackQuotes = listOf(
        Pair("Act out being alive, like a play. And after a while, a long while, it will be true.",
            "John Steinbeck, East of Eden"),
        Pair("There is some good in this world, and it’s worth fighting for.",
            " J.R.R. Tolkien, The Two Towers"),
        Pair("It is only with the heart that one can see rightly; what is essential is invisible to the eye.",
            "Antoine de Saint-Exupéry, The Little Prince"),
        Pair("The only way out of the labyrinth of suffering is to forgive.",
            "John Green, Looking for Alaska"),
        Pair("Love is or it ain’t. Thin love ain’t love at all.",
            "Toni Morrison, Beloved"),
        Pair("We accept the love we think we deserve.",
            "Stephen Chbosky, The Perks of Being a Wallflower"),
        Pair("And so we beat on, boats against the current, borne back ceaselessly into the past.",
            "F. Scott Fitzgerald, The Great Gatsby"),
        Pair("There are years that ask questions and years that answer.",
            "Zora Neale Hurston, Their Eyes Were Watching God"),
        Pair("Memories warm you up from the inside. But they also tear you apart.",
            "Haruki Murakami, Kafka on the Shore"),
        Pair("It is nothing to die; it is dreadful not to live.",
            "Victor Hugo, Les Misérables"),
        Pair("When you play the game of thrones you win or you die.",
            "George R. R. Martin, A Game of Thrones")
    )

    // Show loading with random quote
    fun showLoading(
        loadingContainer: View,
        mainContentContainer: View,
        quoteText: TextView,
        quoteAuthor: TextView
    ) {
        loadingContainer.visibility = View.VISIBLE
        mainContentContainer.visibility = View.GONE

        // Display a random quote
        displayRandomQuote(quoteText, quoteAuthor)
    }

    // Hide loading
    fun hideLoading(loadingContainer: View, mainContentContainer: View) {
        loadingContainer.visibility = View.GONE
        mainContentContainer.visibility = View.VISIBLE
    }

    // Display a random quote from the hardcoded list
    private fun displayRandomQuote(quoteText: TextView, quoteAuthor: TextView) {
        val randomQuote = fallbackQuotes.random()
        displayQuote(quoteText, quoteAuthor, randomQuote.first, randomQuote.second)
    }

    // Display specific quote
    private fun displayQuote(quoteText: TextView, quoteAuthor: TextView, content: String, author: String) {
        quoteText.text = "\"$content\""
        quoteAuthor.text = "- $author"
    }
}
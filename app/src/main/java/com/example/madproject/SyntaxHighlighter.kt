package com.example.madproject

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import java.util.regex.Pattern

class SyntaxHighlighter(private val context: Context) : TextWatcher {

    private val kotlinKeywords: List<String> by lazy {
        try {
            context.resources.openRawResource(R.raw.kotlin_keywords)
                .bufferedReader()
                .readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            listOf("fun", "val", "var", "class", "package", "import")
        }
    }

    private val keywordPattern by lazy { Pattern.compile("\\b(" + kotlinKeywords.joinToString("|") + ")\\b") }
    private val stringPattern = Pattern.compile("\".*?\"")
    private val commentPattern = Pattern.compile("//.*|/\\*.*?\\*/", Pattern.DOTALL)
    private val markdownHeaderPattern = Pattern.compile("^#+.*", Pattern.MULTILINE)
    private val markdownBoldPattern = Pattern.compile("\\*\\*.*?\\*\\*|__.*?__")

    private var isHighlighting = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (isHighlighting || s == null) return
        isHighlighting = true

        // Clear existing spans
        val spans = s.getSpans(0, s.length, ForegroundColorSpan::class.java)
        for (span in spans) {
            s.removeSpan(span)
        }

        // Highlight Kotlin Keywords
        highlight(s, keywordPattern, Color.BLUE)
        
        // Highlight Strings
        highlight(s, stringPattern, Color.parseColor("#008000")) // Green
        
        // Highlight Comments
        highlight(s, commentPattern, Color.GRAY)

        // Basic Markdown Highlighting
        highlight(s, markdownHeaderPattern, Color.parseColor("#800080")) // Purple
        highlight(s, markdownBoldPattern, Color.RED)

        isHighlighting = false
    }

    private fun highlight(s: Editable, pattern: Pattern, color: Int) {
        val matcher = pattern.matcher(s)
        while (matcher.find()) {
            s.setSpan(
                ForegroundColorSpan(color),
                matcher.start(),
                matcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}

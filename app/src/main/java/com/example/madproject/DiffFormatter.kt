package com.example.madproject

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import com.github.difflib.text.DiffRow

object DiffFormatter {

    fun buildSpannableDiff(rows: List<DiffRow>): SpannableStringBuilder {
        val builder = SpannableStringBuilder()

        for (row in rows) {
            when (row.tag) {
                DiffRow.Tag.INSERT -> {
                    val line = "+ ${row.newLine}\n"
                    val start = builder.length
                    builder.append(line)
                    val end = builder.length

                    builder.setSpan(
                        ForegroundColorSpan(Color.parseColor("#4CAF50")), // Soft Green Text
                        start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    builder.setSpan(
                        BackgroundColorSpan(Color.parseColor("#1B381E")), // Dark Green Background
                        start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                DiffRow.Tag.DELETE -> {
                    val line = "- ${row.oldLine}\n"
                    val start = builder.length
                    builder.append(line)
                    val end = builder.length

                    builder.setSpan(
                        ForegroundColorSpan(Color.parseColor("#FF5252")), // Soft Red Text
                        start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    builder.setSpan(
                        BackgroundColorSpan(Color.parseColor("#3E1A1A")), // Dark Red Background
                        start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                DiffRow.Tag.CHANGE -> {
                    // Show original line removed
                    if (row.oldLine.isNotEmpty()) {
                        val oldLine = "- ${row.oldLine}\n"
                        val startOld = builder.length
                        builder.append(oldLine)
                        val endOld = builder.length

                        builder.setSpan(
                            ForegroundColorSpan(Color.parseColor("#FF5252")),
                            startOld, endOld, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        builder.setSpan(
                            BackgroundColorSpan(Color.parseColor("#3E1A1A")),
                            startOld, endOld, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    // Show new line inserted
                    if (row.newLine.isNotEmpty()) {
                        val newLine = "+ ${row.newLine}\n"
                        val startNew = builder.length
                        builder.append(newLine)
                        val endNew = builder.length

                        builder.setSpan(
                            ForegroundColorSpan(Color.parseColor("#4CAF50")),
                            startNew, endNew, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        builder.setSpan(
                            BackgroundColorSpan(Color.parseColor("#1B381E")),
                            startNew, endNew, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
                DiffRow.Tag.EQUAL -> {
                    val line = "  ${row.newLine}\n"
                    val start = builder.length
                    builder.append(line)
                    val end = builder.length

                    builder.setSpan(
                        ForegroundColorSpan(Color.parseColor("#CCCCCC")), // Light Gray
                        start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                else -> {}
            }
        }

        return builder
    }
}
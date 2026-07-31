package com.example.madproject

import com.github.difflib.DiffUtils
import com.github.difflib.patch.Patch
import com.github.difflib.text.DiffRowGenerator
import com.github.difflib.UnifiedDiffUtils
// ✅ Correct
import com.github.difflib.text.DiffRow

object DiffUtilsManager {

    /**
     * Calculates the delta patch between original and revised text strings
     * and returns a Unified Diff formatted string.
     */
    fun createPatch(originalText: String, revisedText: String): String {
        val originalLines = originalText.lines()
        val revisedLines = revisedText.lines()

        val patch: Patch<String> = DiffUtils.diff(originalLines, revisedLines)
        val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
            "base.txt",
            "revised.txt",
            originalLines,
            patch,
            1 // Context line size
        )

        return unifiedDiff.joinToString("\n")
    }

    /**
     * Reconstructs text by taking base text lines and applying a series of Unified Diff patch strings.
     */
    fun applyPatches(baseText: String, patchStrings: List<String>): String {
        var currentLines = baseText.lines()

        for (patchStr in patchStrings) {
            if (patchStr.isBlank()) continue
            val unifiedDiffLines = patchStr.lines()
            val parsedPatch = UnifiedDiffUtils.parseUnifiedDiff(unifiedDiffLines)
            currentLines = DiffUtils.patch(currentLines, parsedPatch)
        }

        return currentLines.joinToString("\n")
    }

    /**
     * Generates a side-by-side or line-by-line HTML-styled comparison string for viewing diffs.
     */
    // ✅ Change line 51 to this:
    fun generateDiffRows(originalText: String, revisedText: String): List<DiffRow> {
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .inlineDiffByWord(true)
            .oldTag { _ -> "~~" } // Simple Markdown / text tags for deleted text
            .newTag { _ -> "**" } // Simple tags for added text
            .build()

        return generator.generateDiffRows(originalText.lines(), revisedText.lines())
    }
}
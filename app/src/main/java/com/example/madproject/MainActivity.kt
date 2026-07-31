package com.example.madproject

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var editorEditText: EditText
    private lateinit var recentFilesRecyclerView: RecyclerView

    private lateinit var recentAdapter: RecentFilesAdapter
    private val recentFiles = mutableListOf<File>()

    private var currentFile: File? = null
    private lateinit var versionRepo: VersionRepository
    
    private lateinit var undoRedoManager: UndoRedoManager
    private val autoSaveHandler = Handler(Looper.getMainLooper())
    private val AUTO_SAVE_INTERVAL = 10000L // 10 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        versionRepo = VersionRepository(this)

        drawerLayout = findViewById(R.id.drawerLayout)
        toolbar = findViewById(R.id.toolbar)
        editorEditText = findViewById(R.id.editorEditText)
        recentFilesRecyclerView = findViewById(R.id.recentFilesRecyclerView)

        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        setupRecentFilesRecyclerView()
        loadRecentFilesFromPrefs()

        // Setup Highlighter
        editorEditText.addTextChangedListener(SyntaxHighlighter(this))
        
        // Setup Undo/Redo
        undoRedoManager = UndoRedoManager(editorEditText)
        
        // Check for crashed session recovery
        checkForRecovery()
    }

    override fun onPause() {
        super.onPause()
        autoSaveHandler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        startAutoSaveTask()
    }

    private fun startAutoSaveTask() {
        autoSaveHandler.postDelayed(object : Runnable {
            override fun run() {
                cacheActiveBuffer()
                autoSaveHandler.postDelayed(this, AUTO_SAVE_INTERVAL)
            }
        }, AUTO_SAVE_INTERVAL)
    }

    private fun cacheActiveBuffer() {
        val content = editorEditText.text.toString()
        if (content.isNotEmpty()) {
            val cacheFile = File(cacheDir, "crash_recovery_buffer.tmp")
            try {
                cacheFile.writeText(content)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkForRecovery() {
        val cacheFile = File(cacheDir, "crash_recovery_buffer.tmp")
        if (cacheFile.exists() && cacheFile.length() > 0) {
            AlertDialog.Builder(this)
                .setTitle("Crash Recovery")
                .setMessage("It looks like the app closed unexpectedly. Would you like to recover your unsaved changes?")
                .setPositiveButton("Recover") { _, _ ->
                    val content = cacheFile.readText()
                    editorEditText.setText(content)
                    cacheFile.delete()
                    Toast.makeText(this, "Content recovered", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Dismiss") { _, _ ->
                    cacheFile.delete()
                }
                .show()
        }
    }

    private fun setupRecentFilesRecyclerView() {
        recentAdapter = RecentFilesAdapter(recentFiles) { file ->
            openFile(file)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        recentFilesRecyclerView.layoutManager = LinearLayoutManager(this)
        recentFilesRecyclerView.adapter = recentAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_undo -> {
                undoRedoManager.undo()
                true
            }
            R.id.action_redo -> {
                undoRedoManager.redo()
                true
            }
            R.id.action_search -> {
                showSearchReplaceDialog()
                true
            }
            R.id.action_toggle_wrap -> {
                toggleWordWrap()
                true
            }
            R.id.action_toggle_readonly -> {
                toggleReadOnly()
                true
            }
            R.id.action_markdown_preview -> {
                showMarkdownPreview()
                true
            }
            R.id.action_new -> {
                handleNewFile()
                true
            }
            R.id.action_save -> {
                handleSave()
                true
            }
            R.id.action_save_as -> {
                showSaveAsDialog()
                true
            }
            R.id.action_history -> {
                showVersionHistoryDialog()
                true
            }
            R.id.action_delete -> {
                confirmDeleteCurrentFile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private var isWordWrapEnabled = true
    private fun toggleWordWrap() {
        isWordWrapEnabled = !isWordWrapEnabled
        editorEditText.setHorizontallyScrolling(!isWordWrapEnabled)
        Toast.makeText(this, "Word Wrap: ${if (isWordWrapEnabled) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
    }

    private var isReadOnly = false
    private fun toggleReadOnly() {
        isReadOnly = !isReadOnly
        applyReadOnlyState()
        
        currentFile?.let { file ->
            lifecycleScope.launch {
                versionRepo.setReadOnly(file, isReadOnly)
            }
        }
        
        Toast.makeText(this, "Read-Only: ${if (isReadOnly) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
    }

    private fun applyReadOnlyState() {
        editorEditText.isEnabled = !isReadOnly
        editorEditText.isFocusable = !isReadOnly
        editorEditText.isFocusableInTouchMode = !isReadOnly
    }

    private fun showSearchReplaceDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_search_replace, null)
        val findInput = dialogView.findViewById<EditText>(R.id.findEditText)
        val replaceInput = dialogView.findViewById<EditText>(R.id.replaceEditText)

        AlertDialog.Builder(this)
            .setTitle("Find and Replace")
            .setView(dialogView)
            .setPositiveButton("Replace All") { _, _ ->
                val findText = findInput.text.toString()
                val replaceText = replaceInput.text.toString()
                if (findText.isNotEmpty()) {
                    val content = editorEditText.text.toString()
                    val newContent = content.replace(findText, replaceText)
                    editorEditText.setText(newContent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMarkdownPreview() {
        val content = editorEditText.text.toString()
        val html = content
            .replace(Regex("^# (.*)$", RegexOption.MULTILINE), "<h1>$1</h1>")
            .replace(Regex("^## (.*)$", RegexOption.MULTILINE), "<h2>$1</h2>")
            .replace(Regex("^### (.*)$", RegexOption.MULTILINE), "<h3>$1</h3>")
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
            .replace(Regex("__(.*?)__"), "<b>$1</b>")
            .replace(Regex("\\*(.*?)\\*"), "<i>$1</i>")
            .replace(Regex("_(.*?)_"), "<i>$1</i>")
            .replace("\n", "<br>")

        val textView = TextView(this)
        textView.setPadding(40, 40, 40, 40)
        textView.text = android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY)

        AlertDialog.Builder(this)
            .setTitle("Markdown Preview")
            .setView(textView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun handleNewFile() {
        editorEditText.text.clear()
        currentFile = null
        toolbar.title = "Untitled.txt"
        undoRedoManager.reset("")
        isReadOnly = false
        applyReadOnlyState()
        Toast.makeText(this, "New file created", Toast.LENGTH_SHORT).show()
    }

    private fun handleSave() {
        if (currentFile == null) {
            showSaveAsDialog()
        } else {
            saveToFile(currentFile!!)
        }
    }

    private fun showSaveAsDialog() {
        val input = EditText(this)
        input.hint = "e.g., notes.txt or app.kt"

        AlertDialog.Builder(this)
            .setTitle("Save As")
            .setMessage("Enter file name:")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val fileName = input.text.toString().trim()
                if (fileName.isNotEmpty()) {
                    val file = FileManager.createNewFile(this, fileName)
                    saveToFile(file)
                } else {
                    Toast.makeText(this, "Filename cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveToFile(file: File) {
        val content = editorEditText.text.toString()
        lifecycleScope.launch {
            val savedVersion = versionRepo.saveVersion(file, content)
            currentFile = file
            toolbar.title = file.name
            addToRecentFiles(file)
            Toast.makeText(this@MainActivity, "Saved ${savedVersion.versionName}", Toast.LENGTH_SHORT).show()
            
            // Delete cache on manual save
            File(cacheDir, "crash_recovery_buffer.tmp").delete()
        }
    }

    private fun openFile(file: File) {
        if (file.exists()) {
            val content = FileManager.readFile(file)
            editorEditText.setText(content)
            currentFile = file
            toolbar.title = file.name
            addToRecentFiles(file)
            undoRedoManager.reset(content)
            
            lifecycleScope.launch {
                isReadOnly = versionRepo.isReadOnly(file)
                applyReadOnlyState()
            }
            
            Toast.makeText(this, "Opened: ${file.name}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "File no longer exists", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteCurrentFile() {
        val file = currentFile
        if (file == null || !file.exists()) {
            Toast.makeText(this, "No saved file to delete", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete File")
            .setMessage("Are you sure you want to delete '${file.name}'? This will also remove its version history.")
            .setPositiveButton("Delete") { _, _ ->
                deleteFileAndHistory(file)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteFileAndHistory(file: File) {
        lifecycleScope.launch {
            versionRepo.deleteHistoryForFile(file)
            val isDeleted = file.exists() && file.delete()

            if (isDeleted) {
                recentFiles.remove(file)
                recentAdapter.updateFiles(recentFiles)
                saveRecentFilesToPrefs()
                handleNewFile()
                Toast.makeText(this@MainActivity, "File deleted successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Failed to delete file from storage", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showVersionHistoryDialog() {
        val file = currentFile
        if (file == null) {
            Toast.makeText(this, "Save the file first to view version history", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val versions = versionRepo.getVersionHistory(file)
            if (versions.isEmpty()) {
                Toast.makeText(this@MainActivity, "No versions found for this file", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val dialogView = LayoutInflater.from(this@MainActivity).inflate(R.layout.dialog_version_history, null)
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.versionsRecyclerView)
            recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)

            val dialog = AlertDialog.Builder(this@MainActivity)
                .setTitle("Version History (${file.name})")
                .setView(dialogView)
                .setNegativeButton("Close", null)
                .create()

            val adapter = VersionAdapter(
                versions,
                onDiffClick = { selectedVersion ->
                    showDiffViewerDialog(file, selectedVersion)
                },
                onRestoreClick = { selectedVersion ->
                    lifecycleScope.launch {
                        val restoredText = versionRepo.getVersionContent(file, selectedVersion.versionNumber)
                        editorEditText.setText(restoredText)
                        Toast.makeText(this@MainActivity, "Rolled back to ${selectedVersion.versionName}", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
            )

            recyclerView.adapter = adapter
            dialog.show()
        }
    }

    private fun showDiffViewerDialog(file: File, selectedVersion: VersionEntity) {
        lifecycleScope.launch {
            val currentContent = editorEditText.text.toString()
            val historicalContent = versionRepo.getVersionContent(file, selectedVersion.versionNumber)

            val diffRows = DiffUtilsManager.generateDiffRows(historicalContent, currentContent)
            val spannableDiff = DiffFormatter.buildSpannableDiff(diffRows)

            val dialogView = LayoutInflater.from(this@MainActivity).inflate(R.layout.dialog_diff_viewer, null)
            val diffTextView = dialogView.findViewById<TextView>(R.id.diffContentTextView)
            val titleTextView = dialogView.findViewById<TextView>(R.id.diffTitleTextView)

            titleTextView.text = "Diff: ${selectedVersion.versionName} ➔ Current Editor"
            diffTextView.text = spannableDiff

            AlertDialog.Builder(this@MainActivity)
                .setTitle("Visual Diff")
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun addToRecentFiles(file: File) {
        recentFiles.remove(file)
        recentFiles.add(0, file)
        recentAdapter.updateFiles(recentFiles)
        saveRecentFilesToPrefs()
    }

    private fun saveRecentFilesToPrefs() {
        val prefs = getSharedPreferences("MAD_PREFS", Context.MODE_PRIVATE)
        val paths = recentFiles.map { it.absolutePath }.toSet()
        prefs.edit().putStringSet("RECENT_FILES", paths).apply()
    }

    private fun loadRecentFilesFromPrefs() {
        val prefs = getSharedPreferences("MAD_PREFS", Context.MODE_PRIVATE)
        val paths = prefs.getStringSet("RECENT_FILES", emptySet()) ?: emptySet()
        recentFiles.clear()
        recentFiles.addAll(paths.map { File(it) }.filter { it.exists() })
        recentAdapter.updateFiles(recentFiles)
    }
}

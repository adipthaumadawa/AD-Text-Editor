package com.example.madproject

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.util.Stack

class UndoRedoManager(private val editText: EditText) {

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()
    private var isUndoRedoing = false

    init {
        undoStack.push("")
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isUndoRedoing) {
                    val currentText = s.toString()
                    if (undoStack.isEmpty() || undoStack.peek() != currentText) {
                        undoStack.push(currentText)
                        redoStack.clear()
                    }
                }
            }
        })
    }

    fun undo() {
        if (undoStack.size > 1) {
            isUndoRedoing = true
            redoStack.push(undoStack.pop())
            val previousText = undoStack.peek()
            editText.setText(previousText)
            editText.setSelection(previousText.length)
            isUndoRedoing = false
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            isUndoRedoing = true
            val nextText = redoStack.pop()
            undoStack.push(nextText)
            editText.setText(nextText)
            editText.setSelection(nextText.length)
            isUndoRedoing = false
        }
    }
    
    fun reset(initialText: String) {
        undoStack.clear()
        redoStack.clear()
        undoStack.push(initialText)
    }
}

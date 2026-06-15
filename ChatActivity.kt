package com.example.aichat.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aichat.R
import com.example.aichat.data.MessageRole
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: MessageAdapter

    // Views
    private lateinit var recyclerView: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnMic: ImageButton
    private lateinit var btnNewChat: ImageButton

    // Voice input launcher
    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                etInput.setText(spokenText)
                etInput.setSelection(spokenText.length)
            }
        }
    }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceInput() else showToast("Microphone permission needed for voice input")
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        bindViews()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    // ── Setup ──────────────────────────────────────────────────────────────────

    private fun bindViews() {
        recyclerView = findViewById(R.id.recyclerViewMessages)
        etInput = findViewById(R.id.etMessageInput)
        btnSend = findViewById(R.id.btnSend)
        btnMic = findViewById(R.id.btnMic)
        btnNewChat = findViewById(R.id.btnNewChat)
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true   // New messages grow from the bottom
        }
    }

    private fun setupClickListeners() {
        btnSend.setOnClickListener { sendMessage() }

        btnMic.setOnClickListener {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                showToast("Speech recognition not available on this device")
                return@setOnClickListener
            }
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED -> startVoiceInput()
                else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        btnNewChat.setOnClickListener {
            viewModel.newSession()
            etInput.setText("")
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.messages.collectLatest { messages ->
                adapter.submitList(messages) {
                    // Scroll to bottom whenever the list updates
                    if (messages.isNotEmpty()) recyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is ChatUiState.Streaming -> {
                        btnSend.setImageResource(R.drawable.ic_stop)  // Stop icon while streaming
                        btnMic.isEnabled = false
                    }
                    is ChatUiState.Error -> {
                        btnSend.setImageResource(R.drawable.ic_send)
                        btnMic.isEnabled = true
                        showToast("Error: ${state.message}")
                    }
                    is ChatUiState.Idle -> {
                        btnSend.setImageResource(R.drawable.ic_send)
                        btnMic.isEnabled = true
                    }
                }
            }
        }
    }

    // ── Actions ────────────────────────────────────────────────────────────────

    private fun sendMessage() {
        val uiState = viewModel.uiState.value
        if (uiState is ChatUiState.Streaming) {
            viewModel.cancelStreaming()
            return
        }
        val text = etInput.text.toString().trim()
        if (text.isBlank()) return
        etInput.setText("")
        viewModel.sendMessage(text)
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message…")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechLauncher.launch(intent)
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

package com.gymbuddy

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gymbuddy.databinding.FragmentAnalysisBinding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalysisFragment : Fragment() {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!

    private lateinit var serviceManager: AIServiceManager
    private lateinit var aiService: AIService
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private val conversationMessages = mutableListOf<AIMessage>()
    private val gson = GsonBuilder().setPrettyPrinting().create()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        serviceManager = AIServiceManager(requireContext())
        aiService = serviceManager.getAIService(serviceManager.getSelectedProvider())

        setupRecyclerView()
        setupClickListeners()

        // Check if API key is set
        if (!aiService.hasApiKey()) {
            showSetupDialog()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun setupClickListeners() {
        binding.startButton.setOnClickListener {
            startAnalysis()
        }

        binding.sendButton.setOnClickListener {
            sendUserMessage()
        }

        binding.generateRoutineButton.setOnClickListener {
            generateRoutine()
        }

        binding.settingsButton.setOnClickListener {
            navigateToSetup()
        }
    }

    private fun showSetupDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Setup Required")
            .setMessage("Please go to the Analysis Setup to configure your AI provider and API key.")
            .setPositiveButton("Go to Setup") { _, _ ->
                // Navigate to setup fragment
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AnalysisSetupFragment())
                    .addToBackStack(null)
                    .commit()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startAnalysis() {
        if (!aiService.hasApiKey()) {
            showSetupDialog()
            return
        }

        binding.startButton.visibility = View.GONE
        binding.loadingProgress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val routine = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(requireContext()).routineDao().getAll()
                }

                val routineJson = serializeRoutine(routine)

                // Clear previous conversation
                messages.clear()
                conversationMessages.clear()
                chatAdapter.notifyDataSetChanged()

                // Add system message
                val systemMessage = AIMessage(
                    role = "system",
                    content = "You are a fitness expert helping analyze and improve workout routines. Ask the user questions about how their current routine is working, their goals, and any issues they're experiencing. ONLY provide JSON output when the user specifically asks for the final routine. Do not output JSON at any other time. When providing the final routine, output ONLY the JSON array with no additional text, comments, or explanations."
                )
                conversationMessages.add(systemMessage)

                // Add initial user message with routine
                val initialMessage = AIMessage(
                    role = "user",
                    content = "Please analyze this workout routine and suggest improvements:\n\n$routineJson"
                )
                conversationMessages.add(initialMessage)

                // Show initial message in chat
                addMessage(ChatMessage("You", "Please analyze my current workout routine and suggest improvements."))

                // Show input layout and generate routine button
                binding.inputLayout.visibility = View.VISIBLE
                binding.generateRoutineButton.visibility = View.VISIBLE

                // Send to AI service
                sendToAI()

            } catch (e: Exception) {
                Toast.makeText(context, "Error loading routine: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.startButton.visibility = View.VISIBLE
                binding.loadingProgress.visibility = View.GONE
            }
        }
    }

    private fun serializeRoutine(routine: List<RoutineDayEntity>): String {
        val exportDays = routine.map { day ->
            ExportRoutineDay(
                dayOfWeek = day.dayOfWeek,
                isRest = day.isRest,
                exercises = day.exercises.map { exercise ->
                    ExportExercise(
                        title = exercise.title,
                        weight = exercise.weight,
                        reps = exercise.reps,
                        sets = exercise.sets,
                        notes = exercise.notes
                    )
                }
            )
        }
        return gson.toJson(exportDays)
    }

    private fun sendUserMessage() {
        val messageText = binding.messageInput.text.toString().trim()
        if (messageText.isEmpty()) return

        binding.messageInput.text.clear()
        addMessage(ChatMessage("You", messageText))

        val userMessage = AIMessage(role = "user", content = messageText)
        conversationMessages.add(userMessage)

        sendToAI()
    }

    private fun generateRoutine() {
        val generateMessage = "Please provide the final workout routine as JSON only, no additional text."

        addMessage(ChatMessage("You", generateMessage))

        val userMessage = AIMessage(role = "user", content = generateMessage)
        conversationMessages.add(userMessage)

        // Hide the generate button since we're requesting the final routine
        binding.generateRoutineButton.visibility = View.GONE

        sendToAI()
    }

    private fun sendToAI() {
        binding.inputLayout.visibility = View.GONE
        binding.loadingProgress.visibility = View.VISIBLE

        aiService.sendMessage(conversationMessages) { result ->
            requireActivity().runOnUiThread {
                binding.loadingProgress.visibility = View.GONE
                binding.inputLayout.visibility = View.VISIBLE

                result.onSuccess { response ->
                    addMessage(ChatMessage(aiService.getProviderName(), response))

                    val assistantMessage = AIMessage(role = "assistant", content = response)
                    conversationMessages.add(assistantMessage)

                    // Check if response contains JSON (final routine)
                    if (isRoutineJson(response)) {
                        copyToClipboard(response)
                        showCompletionDialog()
                    }
                }.onFailure { error ->
                    addMessage(ChatMessage("System", "Error: ${error.message}"))
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isRoutineJson(text: String): Boolean {
        return text.trim().startsWith("[") && text.trim().endsWith("]")
    }

    private fun copyToClipboard(json: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Updated Routine", json)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Routine JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun showCompletionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Analysis Complete")
            .setMessage("The updated routine has been copied to your clipboard. Go to the Routine tab and use the Import button to apply the changes.")
            .setPositiveButton("OK") { _, _ ->
                resetChat()
            }
            .show()
    }

    private fun resetChat() {
        messages.clear()
        conversationMessages.clear()
        chatAdapter.notifyDataSetChanged()
        binding.startButton.visibility = View.VISIBLE
        binding.inputLayout.visibility = View.GONE
        binding.generateRoutineButton.visibility = View.GONE
    }

    private fun navigateToSetup() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, AnalysisSetupFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun addMessage(message: ChatMessage) {
        chatAdapter.addMessage(message)
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear conversation when fragment is destroyed
        messages.clear()
        conversationMessages.clear()
        _binding = null
    }
}
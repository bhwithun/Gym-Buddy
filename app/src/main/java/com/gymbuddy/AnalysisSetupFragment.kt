package com.gymbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.gymbuddy.databinding.FragmentAnalysisSetupBinding

class AnalysisSetupFragment : Fragment() {

    private var _binding: FragmentAnalysisSetupBinding? = null
    private val binding get() = _binding!!

    private lateinit var serviceManager: AIServiceManager
    private var currentProvider: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        serviceManager = AIServiceManager(requireContext())

        setupProviderSpinner()
        setupClickListeners()
        loadCurrentSettings()
    }

    private fun setupProviderSpinner() {
        val providers = serviceManager.getAvailableProviders()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, providers)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.providerSpinner.adapter = adapter

        binding.providerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedProvider = providers[position]
                if (selectedProvider != currentProvider) {
                    currentProvider = selectedProvider
                    updateModelSpinner()
                    loadApiKeyForProvider()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateModelSpinner() {
        val service = serviceManager.getAIService(currentProvider)
        val models = service.getAvailableModels()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, models)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.modelSpinner.adapter = adapter

        // Select the saved model
        val savedModel = serviceManager.getSelectedModel(currentProvider)
        val modelIndex = models.indexOf(savedModel)
        if (modelIndex >= 0) {
            binding.modelSpinner.setSelection(modelIndex)
        }

        binding.modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedModel = models[position]
                service.setSelectedModel(selectedModel)
                serviceManager.saveSelectedModel(currentProvider, selectedModel)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadCurrentSettings() {
        currentProvider = serviceManager.getSelectedProvider()
        val providers = serviceManager.getAvailableProviders()
        val providerIndex = providers.indexOf(currentProvider)
        if (providerIndex >= 0) {
            binding.providerSpinner.setSelection(providerIndex)
        }
        updateModelSpinner()
        loadApiKeyForProvider()
    }

    private fun loadApiKeyForProvider() {
        val service = serviceManager.getAIService(currentProvider)
        val apiKey = service.getApiKey()
        binding.apiKeyInput.setText(apiKey ?: "")
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            saveSettings()
        }

        binding.testButton.setOnClickListener {
            testConnection()
        }

        binding.fetchModelsButton.setOnClickListener {
            fetchModels()
        }

        binding.clearButton.setOnClickListener {
            clearApiKey()
        }
    }

    private fun saveSettings() {
        val apiKey = binding.apiKeyInput.text.toString().trim()
        if (apiKey.isNotEmpty()) {
            val service = serviceManager.getAIService(currentProvider)
            service.saveApiKey(apiKey)
            serviceManager.saveSelectedProvider(currentProvider)
            binding.statusText.text = "Settings saved successfully"
            Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
        } else {
            binding.statusText.text = "Please enter an API key"
            Toast.makeText(context, "Please enter an API key", Toast.LENGTH_SHORT).show()
        }
    }

    private fun testConnection() {
        val service = serviceManager.getAIService(currentProvider)
        if (!service.hasApiKey()) {
            binding.statusText.text = "No API key set for this provider"
            Toast.makeText(context, "No API key set", Toast.LENGTH_SHORT).show()
            return
        }

        binding.statusText.text = "Testing connection..."
        binding.testButton.isEnabled = false

        // Send a simple test message
        val testMessages = listOf(
            AIMessage("user", "Hello, this is a test message. Please respond with 'Connection successful'.")
        )

        service.sendMessage(testMessages) { result ->
            requireActivity().runOnUiThread {
                binding.testButton.isEnabled = true
                result.onSuccess { response ->
                    if (response.contains("successful", ignoreCase = true)) {
                        binding.statusText.text = "Connection test successful! You can now fetch available models."
                        binding.fetchModelsButton.visibility = View.VISIBLE
                        Toast.makeText(context, "Connection successful", Toast.LENGTH_SHORT).show()
                    } else {
                        binding.statusText.text = "Connection test completed (unexpected response)"
                        Toast.makeText(context, "Test completed", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { error ->
                    binding.statusText.text = "Connection test failed: ${error.message}"
                    binding.fetchModelsButton.visibility = View.GONE
                    Toast.makeText(context, "Connection failed: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun fetchModels() {
        val service = serviceManager.getAIService(currentProvider)
        if (!service.hasApiKey()) {
            binding.statusText.text = "No API key set for this provider"
            Toast.makeText(context, "No API key set", Toast.LENGTH_SHORT).show()
            return
        }

        binding.statusText.text = "Fetching available models..."
        binding.fetchModelsButton.isEnabled = false

        service.fetchAvailableModelsFromAPI { result ->
            requireActivity().runOnUiThread {
                binding.fetchModelsButton.isEnabled = true
                result.onSuccess { models ->
                    if (models.isNotEmpty()) {
                        // Update the model spinner with real API data
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, models)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        binding.modelSpinner.adapter = adapter

                        // Select the first model by default
                        if (models.isNotEmpty()) {
                            binding.modelSpinner.setSelection(0)
                            service.setSelectedModel(models[0])
                            serviceManager.saveSelectedModel(currentProvider, models[0])
                        }

                        binding.statusText.text = "Successfully loaded ${models.size} models from API"
                        Toast.makeText(context, "Models loaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        binding.statusText.text = "No models available from API"
                        Toast.makeText(context, "No models found", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { error ->
                    binding.statusText.text = "Failed to fetch models: ${error.message}"
                    Toast.makeText(context, "Failed to fetch models: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun clearApiKey() {
        val service = serviceManager.getAIService(currentProvider)
        service.saveApiKey("") // Clear the key
        binding.apiKeyInput.setText("")
        binding.fetchModelsButton.visibility = View.GONE
        binding.statusText.text = "API key cleared"
        Toast.makeText(context, "API key cleared", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
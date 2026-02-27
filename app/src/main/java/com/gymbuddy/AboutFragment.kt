package com.gymbuddy

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.gymbuddy.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadAboutInfo()
    }

    private fun loadAboutInfo() {
        // Version
        val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        binding.versionText.text = "Version ${packageInfo.versionName}"

        // App name with colors
        val appName = "FitBitch"
        val spannableAppName = SpannableString(appName)
        spannableAppName.setSpan(ForegroundColorSpan(Color.parseColor("#FFFF00")), 0, 3, 0) // Fit yellow
        spannableAppName.setSpan(ForegroundColorSpan(Color.parseColor("#00FF00")), 3, appName.length, 0) // Bitch green
        binding.appNameText.text = spannableAppName

        // Credits with colors
        val credits = "Made on Earth for Sam"
        val spannableCredits = SpannableString(credits)
        spannableCredits.setSpan(ForegroundColorSpan(Color.parseColor("#00FFFF")), 8, 13, 0) // Earth cyan
        spannableCredits.setSpan(ForegroundColorSpan(Color.parseColor("#FF6B6B")), 17, credits.length, 0) // Sam red
        binding.creditsText.text = spannableCredits
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

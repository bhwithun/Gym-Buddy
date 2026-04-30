package com.gymbuddy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.gymbuddy.databinding.FragmentAboutBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    private var logoTapCount = 0

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

        binding.logoImage.setOnClickListener {
            logoTapCount++
            if (logoTapCount >= 5) {
                toggleEasterEgg()
                logoTapCount = 0
            }
        }
    }

    private fun loadAboutInfo() {
        val prefs = requireContext().getSharedPreferences("gym_buddy_prefs", Context.MODE_PRIVATE)
        val isEasterEggActive = prefs.getBoolean("easter_egg_active", false)

        // Version
        val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        binding.versionText.text = "Version ${packageInfo.versionName}"

        if (isEasterEggActive) {
            // Easter egg active
            binding.logoImage.setImageResource(R.drawable.ut_logo)

            // App name with colors
            val appName = "Fit Bitch"
            val spannableAppName = SpannableString(appName)
            spannableAppName.setSpan(ForegroundColorSpan(Color.parseColor("#FFFF00")), 0, 3, 0) // Fit yellow
            spannableAppName.setSpan(ForegroundColorSpan(Color.parseColor("#00FF00")), 4, appName.length, 0) // Bitch green
            binding.appNameText.text = spannableAppName

            // Credits with colors
            val credits = "Made on Earth for Sam"
            val spannableCredits = SpannableString(credits)
            spannableCredits.setSpan(ForegroundColorSpan(Color.parseColor("#00FFFF")), 8, 13, 0) // Earth cyan
            spannableCredits.setSpan(ForegroundColorSpan(Color.parseColor("#FF6B6B")), 17, credits.length, 0) // Sam red
            binding.creditsText.text = spannableCredits

            // Generate insult QR code
            val qrBitmap = generateQRCode("Fuck off, I'm lifting")
            binding.qrCodeImage.setImageBitmap(qrBitmap)
        } else {
            // Normal state
            binding.logoImage.setImageResource(R.drawable.plate)

            // App name with colors
            val appName = "Gym Buddy"
            val spannableAppName = SpannableString(appName)
            spannableAppName.setSpan(ForegroundColorSpan(Color.parseColor("#FFFF00")), 0, 3, 0) // Gym yellow
            spannableAppName.setSpan(ForegroundColorSpan(Color.parseColor("#00FF00")), 4, appName.length, 0) // Buddy green
            binding.appNameText.text = spannableAppName

            // Credits with colors
            val credits = "Made on Earth by Brian"
            val spannableCredits = SpannableString(credits)
            spannableCredits.setSpan(ForegroundColorSpan(Color.parseColor("#00FFFF")), 8, 13, 0) // Earth cyan
            spannableCredits.setSpan(ForegroundColorSpan(Color.parseColor("#FF6B6B")), 17, credits.length, 0) // Brian red
            binding.creditsText.text = spannableCredits

            // Normal QR code
            binding.qrCodeImage.setImageResource(R.drawable.qr_code)
        }
    }

    private fun toggleEasterEgg() {
        val prefs = requireContext().getSharedPreferences("gym_buddy_prefs", Context.MODE_PRIVATE)
        val isEasterEggActive = prefs.getBoolean("easter_egg_active", false)
        prefs.edit().putBoolean("easter_egg_active", !isEasterEggActive).apply()
        loadAboutInfo()
    }

    private fun generateQRCode(text: String): Bitmap? {
        val writer = QRCodeWriter()
        return try {
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: WriterException) {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

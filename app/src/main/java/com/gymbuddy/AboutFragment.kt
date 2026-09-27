package com.gymbuddy

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.gymbuddy.databinding.FragmentAboutBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!
    private var manualOpen = false
    private var loadedStatsUrl: String? = null
    private var nameRequest = 0

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        val contents = result.contents ?: return@registerForActivityResult
        val parsed = WorkerRemote.parseProfileQr(contents)
        if (parsed == null) {
            Toast.makeText(requireContext(), R.string.scan_profile_invalid, Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        saveProfile(parsed.first, parsed.second)
    }

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startProfileScan()
        else Toast.makeText(requireContext(), R.string.scan_camera_denied, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        paintTitle()
        val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        binding.versionText.text = "Version ${packageInfo.versionName}"
        binding.creditsText.text = coloredCredits(getString(R.string.about_credits))

        binding.statsWeb.setBackgroundColor(Color.parseColor("#121212"))
        binding.statsWeb.settings.javaScriptEnabled = true
        binding.statsWeb.settings.domStorageEnabled = true
        binding.statsWeb.settings.useWideViewPort = true
        binding.statsWeb.settings.loadWithOverviewMode = true
        binding.statsWeb.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return !isProfileHost(request.url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (_binding == null || !WorkerRemote.isConfigured(requireContext())) return
                binding.statsStatus.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (!request.isForMainFrame || _binding == null) return
                binding.statsStatus.visibility = View.VISIBLE
                binding.statsStatus.setText(R.string.about_stats_failed)
            }
        }

        binding.openSiteButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.about_site_url))))
        }
        binding.scanProfileButton.setOnClickListener {
            val granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) startProfileScan() else cameraPermission.launch(Manifest.permission.CAMERA)
        }
        binding.disconnectLink.setOnClickListener { disconnectProfile() }
        binding.manualToggle.setOnClickListener {
            manualOpen = !manualOpen
            renderConnection()
        }
        binding.workerSaveButton.setOnClickListener {
            saveProfile(
                binding.workerUrlInput.text.toString(),
                binding.workerTokenInput.text.toString()
            )
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.statsWeb, true)
        binding.workerUrlInput.setText(WorkerRemote.getUrl(requireContext()) ?: "")
        binding.workerTokenInput.setText(WorkerRemote.getToken(requireContext()) ?: "")
        renderConnection()
    }

    private fun saveProfile(url: String, token: String) {
        WorkerRemote.save(requireContext(), url, token)
        binding.workerUrlInput.setText(WorkerRemote.getUrl(requireContext()) ?: "")
        binding.workerTokenInput.setText(WorkerRemote.getToken(requireContext()) ?: "")
        val configured = WorkerRemote.isConfigured(requireContext())
        if (configured) manualOpen = false
        loadedStatsUrl = null
        renderConnection()
        Toast.makeText(
            requireContext(),
            if (configured) R.string.worker_saved else R.string.worker_cleared,
            Toast.LENGTH_SHORT
        ).show()
        if (configured) {
            (activity as? MainActivity)?.let { StandardRoutineOffer.maybeCheck(it) }
        }
    }

    private fun renderConnection() {
        if (_binding == null) return
        val context = requireContext()
        val url = WorkerRemote.getUrl(context)
        val connected = !url.isNullOrBlank()
        val scrollParams = binding.aboutScroll.layoutParams as LinearLayout.LayoutParams
        val setupVisibility = if (connected) View.GONE else View.VISIBLE
        binding.setupBody.visibility = setupVisibility
        binding.openSiteButton.visibility = setupVisibility
        binding.scanProfileButton.visibility = setupVisibility
        binding.manualToggle.visibility = setupVisibility
        binding.manualFields.visibility = if (!connected && manualOpen) View.VISIBLE else View.GONE
        binding.manualToggle.setText(if (manualOpen) R.string.about_hide_manual else R.string.about_manual)
        binding.disconnectLink.visibility = if (connected) View.VISIBLE else View.GONE
        if (connected) {
            showConnected(WorkerRemote.profileSlug(context) ?: url!!)
            binding.statsWeb.visibility = View.VISIBLE
            binding.statsStatus.visibility = View.VISIBLE
            binding.statsStatus.setText(R.string.about_stats_loading)
            scrollParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            scrollParams.weight = 0f
            loadDisplayName(url!!)
            loadStats(url)
        } else {
            binding.profileStatus.movementMethod = null
            binding.profileStatus.setText(R.string.about_not_connected)
            binding.statsWeb.visibility = View.GONE
            binding.statsStatus.visibility = View.GONE
            scrollParams.height = 0
            scrollParams.weight = 1f
        }
        binding.aboutScroll.layoutParams = scrollParams
    }

    private fun showConnected(name: String) {
        binding.profileStatus.movementMethod = null
        binding.profileStatus.text = getString(R.string.about_connected, name)
    }

    private fun disconnectProfile() {
        manualOpen = false
        WorkerRemote.save(requireContext(), "", "")
        loadedStatsUrl = null
        binding.statsWeb.loadUrl("about:blank")
        renderConnection()
        Toast.makeText(requireContext(), R.string.worker_cleared, Toast.LENGTH_SHORT).show()
    }

    private fun loadStats(url: String) {
        val target = "$url/?app=1"
        if (loadedStatsUrl == target) return
        loadedStatsUrl = target
        binding.statsStatus.visibility = View.VISIBLE
        binding.statsStatus.setText(R.string.about_stats_loading)
        val token = WorkerRemote.getToken(requireContext())
        val slug = WorkerRemote.profileSlug(requireContext())
        val origin = Uri.parse(url).let { parsed ->
            val port = if (parsed.port == -1) "" else ":${parsed.port}"
            "${parsed.scheme}://${parsed.host}$port"
        }
        if (!token.isNullOrBlank() && !slug.isNullOrBlank()) {
            CookieManager.getInstance().setCookie(origin, "gb_profiles=$slug.$token; Path=/; Secure; SameSite=Lax") {
                if (_binding != null && loadedStatsUrl == target) binding.statsWeb.loadUrl(target)
            }
            CookieManager.getInstance().flush()
        } else {
            binding.statsWeb.loadUrl(target)
        }
    }

    private fun loadDisplayName(url: String) {
        val requestId = ++nameRequest
        val token = WorkerRemote.getToken(requireContext())
        thread {
            val name = try {
                val request = Request.Builder().url(url).apply {
                    if (!token.isNullOrBlank()) header("Authorization", "Bearer $token")
                }
                http.newCall(request.build()).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    Regex("""name="gb-profile" content="([^"]*)"""").find(body)?.groupValues?.get(1)?.trim()
                }
            } catch (_: Exception) {
                null
            }
            activity?.runOnUiThread {
                if (_binding == null || requestId != nameRequest) return@runOnUiThread
                if (!name.isNullOrEmpty() && WorkerRemote.isConfigured(requireContext())) {
                    showConnected(name)
                }
            }
        }
    }

    private fun isProfileHost(uri: Uri): Boolean {
        val base = WorkerRemote.getUrl(requireContext()) ?: return false
        val host = Uri.parse(base).host ?: return false
        return uri.scheme == "https" && uri.host == host
    }

    private fun startProfileScan() {
        scanLauncher.launch(
            ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt(getString(R.string.scan_profile))
                setBeepEnabled(false)
                setOrientationLocked(false)
            }
        )
    }

    private fun paintTitle() {
        val appName = "Gym Buddy"
        val title = SpannableString(appName)
        title.setSpan(ForegroundColorSpan(Color.parseColor("#FFFF00")), 0, 3, 0)
        title.setSpan(ForegroundColorSpan(Color.parseColor("#00FF00")), 4, appName.length, 0)
        binding.appNameText.text = title
    }

    private fun coloredCredits(credits: String): SpannableString {
        val text = SpannableString(credits)
        val earth = credits.indexOf("Earth")
        val brian = credits.indexOf("Brian")
        if (earth >= 0) {
            text.setSpan(ForegroundColorSpan(Color.parseColor("#00FFFF")), earth, earth + "Earth".length, 0)
        }
        if (brian >= 0) {
            text.setSpan(ForegroundColorSpan(Color.parseColor("#FF6B6B")), brian, brian + "Brian".length, 0)
        }
        return text
    }

    override fun onDestroyView() {
        binding.statsWeb.apply {
            stopLoading()
            webViewClient = WebViewClient()
            destroy()
        }
        _binding = null
        super.onDestroyView()
    }
}

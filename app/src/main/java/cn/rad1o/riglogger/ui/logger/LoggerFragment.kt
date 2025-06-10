package cn.rad1o.riglogger.ui.logger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import cn.rad1o.riglogger.databinding.FragmentLoggerBinding


class LoggerFragment : Fragment() {

    private var _binding: FragmentLoggerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoggerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val webView: WebView = binding.webviewCloudlog
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        var cloudlogUrl: String? = context?.let {
            PreferenceManager
                .getDefaultSharedPreferences(it)
                .getString("cloudlog_url", null)
        }

        if (cloudlogUrl != null) {
            webView.loadUrl(cloudlogUrl)
        } else {
            webView.loadData(
                "<p>Unable to find a URL to visit, please configure it in settings.</p>",
                "text/html", "UTF-8")
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        /* TODO: Disable the weird switching animation here? */
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
    }
}
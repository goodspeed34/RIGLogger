/*
 *     RIGLogger - A Ham Radio Logging Solution for Android with Cloudlog
 *     Copyright (C) 2025 Gong Zhile
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.rad1o.riglogger.ui.logger

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import cn.rad1o.riglogger.MainActivity
import cn.rad1o.riglogger.R
import cn.rad1o.riglogger.databinding.FragmentLoggerBinding


class LoggerFragment : Fragment() {

    private var _binding: FragmentLoggerBinding? = null

    lateinit var webView: WebView

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @SuppressLint("SetJavaScriptEnabled") /* can't function without JavaScript ofc... */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoggerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val swipeRefresh = binding.swipeRefresh

        webView = binding.webviewCloudlog
        webView.settings.javaScriptEnabled = true
        swipeRefresh.setOnRefreshListener { webView.reload() }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                swipeRefresh.isRefreshing = false
            }
        }

        /* Allowing the back button to properly function,
           https://stackoverflow.com/questions/6077141. */
        webView.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    val webView = v as WebView

                    when (keyCode) {
                        KeyEvent.KEYCODE_BACK -> if (webView.canGoBack()) {
                            webView.goBack()
                            return true
                        }
                    }
                }

                return false
            }
        })

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cloudlogUrl: String? = context?.let {
            PreferenceManager
                .getDefaultSharedPreferences(it)
                .getString("cloudlog_url", null)
        }

        webView = binding.webviewCloudlog

        val loggerWebviewState = (activity as MainActivity).viewModel.loggerWebviewState
        if ((loggerWebviewState) != null) {
            webView.restoreState(loggerWebviewState)
            webView.loadUrl(loggerWebviewState.getString("url").toString())
        } else if (cloudlogUrl != null) {
            webView.loadUrl(cloudlogUrl)
        } else {
            webView.loadData(
                getString(R.string.p_unable_to_find_a_url_to_visit_please_configure_it_in_settings_p),
                "text/html", "UTF-8")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        var loggerWebviewState = (activity as MainActivity).viewModel.loggerWebviewState
        if (loggerWebviewState == null) {
            loggerWebviewState = Bundle()
        }

        webView.saveState(loggerWebviewState)
        loggerWebviewState.putString("url", webView.url)
        (activity as MainActivity).viewModel.loggerWebviewState = loggerWebviewState

        _binding = null
    }
}
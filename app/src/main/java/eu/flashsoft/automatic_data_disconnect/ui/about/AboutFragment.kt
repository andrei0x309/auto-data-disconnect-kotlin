package eu.flashsoft.automatic_data_disconnect.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import eu.flashsoft.automatic_data_disconnect.R
import kotlinx.coroutines.launch



class AboutFragment : Fragment() {

    private lateinit var aboutViewModel: AboutViewModel


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        aboutViewModel =
            ViewModelProvider(this)[AboutViewModel::class.java]
        val root = inflater.inflate(R.layout.fragment_about, container, false)
        val aboutProgress:ProgressBar = root.findViewById(R.id.aboutProgressBar)


        viewLifecycleOwner.lifecycleScope.launch {
            withStarted {
                val aboutWebView:WebView = root.findViewById(R.id.aboutWebView)
                aboutViewModel.htmlData.observe(viewLifecycleOwner, Observer {
                    aboutWebView.loadData(it, "text/html; charset=utf-8", "UTF-8")
                })
            }
            aboutProgress.isGone = true
        }


        return root
    }
}
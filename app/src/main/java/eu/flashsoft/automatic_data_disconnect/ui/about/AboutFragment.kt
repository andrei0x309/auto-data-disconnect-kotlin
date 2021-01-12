package eu.flashsoft.automatic_data_disconnect.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import eu.flashsoft.automatic_data_disconnect.R


class AboutFragment : Fragment() {

    private lateinit var aboutViewModel: AboutViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        aboutViewModel =
                ViewModelProvider(this).get(AboutViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_about, container, false)



        val aboutWebView:WebView = root.findViewById(R.id.aboutWebview)


        aboutViewModel.htmlData.observe(viewLifecycleOwner, Observer {

            aboutWebView.loadData(it, "text/html; charset=utf-8", "UTF-8")

        })
        return root
    }
}
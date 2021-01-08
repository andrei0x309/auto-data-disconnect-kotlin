package eu.flashsoft.automatic_data_disconnect.ui.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import eu.flashsoft.automatic_data_disconnect.R

class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        dashboardViewModel =
                ViewModelProvider(this).get(DashboardViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_logs, container, false)
        /*
       val textView: TextView = root.findViewById(R.id.text_dashboard)
       dashboardViewModel.text.observe(viewLifecycleOwner, Observer {
           textView.text = it
       })
       root.findViewById<Switch>(R.id.switch1).setOnCheckedChangeListener { _, isChecked ->
           val da = if (isChecked) "DA" else "NU"
           Toast.makeText(activity, da as CharSequence, Toast.LENGTH_SHORT).show()
       }*/


        return root
    }
}
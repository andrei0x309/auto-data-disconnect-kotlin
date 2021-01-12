package eu.flashsoft.automatic_data_disconnect.ui.home

import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import eu.flashsoft.automatic_data_disconnect.MainActivity
import eu.flashsoft.automatic_data_disconnect.R


class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
                ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        /*val textView: TextView = root.findViewById(R.id.text_home)
        homeViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })*/
        val mA:MainActivity = activity as MainActivity
        mA.swAppEnable = root.findViewById(R.id.appSwitch)
        mA.swLogs  = root.findViewById(R.id.logsSwitch)
        mA.timerMinTextEdit = root.findViewById(R.id.timerMinTextEdit)
        mA.timerMinTextEdit.filters = arrayOf<InputFilter>(LengthFilter(9))
        mA.grantRootBtn = root.findViewById(R.id.grantRootBtn)
        mA.homeFragmentLoaded()

        return root
    }
}
package eu.flashsoft.automatic_data_disconnect.ui.home

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import eu.flashsoft.automatic_data_disconnect.MainActivity
import eu.flashsoft.automatic_data_disconnect.R


class HomeFragment : Fragment() {

//    private lateinit var homeViewModel: HomeViewModel

    lateinit var root: View

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        root = inflater.inflate(R.layout.fragment_home, container, false)
        /*val textView: TextView = root.findViewById(R.id.text_home)
        homeViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })*/

        initView()
        return root
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun initView () {
        val mA:MainActivity = activity as MainActivity
        mA.swAppEnable = root.findViewById(R.id.appSwitch)
        mA.swLogs  = root.findViewById(R.id.logsSwitch)
        mA.timerMinTextEdit = root.findViewById(R.id.timerMinTextEdit)
        mA.timerMinTextEdit.filters = arrayOf<InputFilter>(LengthFilter(9))
        mA.grantRootBtn = root.findViewById(R.id.grantRootBtn)
        mA.timerTextView = root.findViewById(R.id.textViewCounter)
        mA.statusTextView = root.findViewById(R.id.textViewStatus)
        mA.swWiFIAlso = root.findViewById(R.id.wifiSwitch)
        mA.homeFragment = root
        mA.homeFragmentLoaded()
    }
}
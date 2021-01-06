package eu.flashsoft.automatic_data_disconnect

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.Editor
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.lang.reflect.Method


class MainActivity : AppCompatActivity() {

    var swService:Switch? = null
    var swLogs:Switch? = null
    var timerMinTextEdit:EditText? = null


    fun homeFragmentLoaded(){

        val sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        var ed: Editor
        if (!sharedPrefs.contains("initialized")) {
            ed = sharedPrefs.edit()

            //Indicate that the default shared prefs have been set
            ed.putBoolean("initialized", true)

            //Set some default shared pref
            ed.putBoolean("enableService", true)
            ed.putBoolean("enableLogs", true)
            ed.putBoolean("disconnectPending", true)
            ed.putLong("disconnectStamp", System.currentTimeMillis())
            ed.putInt("disconnectTimerMin", 20)
            ed.commit()
        }

        // Get pref
        val boolServiceEnabled = sharedPrefs.getBoolean("enableService", false)
        val boolLogsEnabled = sharedPrefs.getBoolean("enableLogs", false)
        val intTimerDisconnectMin = sharedPrefs.getInt("disconnectTimerMin", 20)

        // Set View comp
        swService?.isChecked = boolServiceEnabled
        swLogs?.isChecked = boolLogsEnabled
        timerMinTextEdit?.text = Editable.Factory.getInstance().newEditable(intTimerDisconnectMin.toString())

        if(boolServiceEnabled) DisconnectStickyService.selfStart(this)


        val serviceStarted = DisconnectStickyService.isServiceStarted
        swService?.setOnCheckedChangeListener { _, isChecked ->
            ed = sharedPrefs.edit()
            if (isChecked) {
                if (!serviceStarted) {
                    startService(Intent(this, DisconnectStickyService::class.java))
                }
                ed.putBoolean("enableService", true)
            } else {
                if (serviceStarted) {
                    stopService(Intent(this, DisconnectStickyService::class.java))
                }
                ed.putBoolean("enableService", false)
            }
            ed.commit()
        }

        swLogs?.setOnCheckedChangeListener { _, isChecked ->
            ed = sharedPrefs.edit()
            if (isChecked) {
                ed.putBoolean("enableLogs", true)
            }else{
                ed.putBoolean("enableLogs", false)
            }
            ed.commit()
        }

        timerMinTextEdit?.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                var sInInt = try {
                    s.toString().toInt()
                } catch (ex: NumberFormatException) {
                    0
                }
                if (sInInt < 1 || sInInt > 600) {
                    timerMinTextEdit?.error = "Minutes must be between 1 and 600"
                } else {
                    timerMinTextEdit?.error = null
                    ed = sharedPrefs.edit()
                    ed.putInt("disconnectTimerMin", sInInt)
                    ed.commit()
                }

            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
            }

        })


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)


        val navView: BottomNavigationView = findViewById(R.id.nav_view)


        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = navHostFragment.navController

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        /*val moveBackBool = intent.getBooleanExtra("MOVE_BACK_BOOL", false)
        DisconnectService.startActionFoo(this, "a", "a")
        if (moveBackBool === true) moveTaskToBack(true);
        else {

        }*/



    }

    override fun onDestroy() {
        super.onDestroy()

        //val sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        //val boolServiceEnabled = sharedPrefs.getBoolean("enableService", false)
        //DisconnectStickyService.selfStart(this)
    }

    fun scrapCode(){

        var mobileDataEnabled = false // Assume disabled

        val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            val cmClass = Class.forName(cm.javaClass.name)
            val method: Method = cmClass.getDeclaredMethod("getMobileDataEnabled")
            method.isAccessible = true // Make the method callable
            // get the setting for "mobile data"
            mobileDataEnabled = method.invoke(cm) as Boolean
        } catch (e: Exception) {
            // Some problem accessible private API
            // TODO do whatever error handling you want here
        }

    }





}
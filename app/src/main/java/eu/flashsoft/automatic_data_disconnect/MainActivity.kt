package eu.flashsoft.automatic_data_disconnect


import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences.Editor
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.IOException





class MainActivity : AppCompatActivity() {

    lateinit var swAppEnable:Switch
    lateinit var swLogs:Switch
    lateinit var timerMinTextEdit:EditText
    lateinit var grantRootBtn: Button




    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun homeFragmentLoaded(){


        grantRootBtn.setOnClickListener {

            try{
                Runtime.getRuntime().exec("su")
                Toast.makeText(applicationContext, "Root rights received", Toast.LENGTH_LONG).show()
            }catch (e: IOException){
                e.printStackTrace()
                Toast.makeText(applicationContext, "Root rights not received", Toast.LENGTH_LONG).show()
            }

        }


        val sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        var ed: Editor
        if (!sharedPrefs.contains("initialized")) {
            ed = sharedPrefs.edit()

            //Indicate that the default shared prefs have been set
            ed.putBoolean("initialized", true)

            //Set some default shared pref
            ed.putBoolean("enableApp", true)
            ed.putBoolean("enableLogs", true)
            ed.putBoolean("disconnectPending", true)
            ed.putLong("disconnectStamp", System.currentTimeMillis())
            ed.putInt("disconnectTimerMin", 15)
            ed.apply()

            val rootUtil = RootUtil
            if(!rootUtil.isDeviceRooted) alertNoRoot()

        }

        // Get pref
        val boolAppEnabled = sharedPrefs.getBoolean("enableApp", false)
        val boolLogsEnabled = sharedPrefs.getBoolean("enableLogs", false)
        val intTimerDisconnectMin = sharedPrefs.getInt("disconnectTimerMin", 15)

        // Set View comp
        swAppEnable.isChecked = boolAppEnabled
        swLogs.isChecked = boolLogsEnabled
        timerMinTextEdit.text = Editable.Factory.getInstance().newEditable(intTimerDisconnectMin.toString())

        if(boolAppEnabled) startApp(applicationContext)



        swAppEnable.setOnCheckedChangeListener { _, isChecked ->
            ed = sharedPrefs.edit()
            if (isChecked) {
                startApp(applicationContext)
                Log.d("main", "switch on")
                ed.putBoolean("enableApp", true)
                ed.putBoolean("enableLogs", true)
                swLogs.isEnabled = true
                swLogs.isChecked = true
            } else {

                if(DisconnectHelper.getConnectionType(applicationContext) ==  DisconnectHelper.CONNECTION_TYPE_MOBILE){
                    WorkManager.getInstance(applicationContext).cancelAllWorkByTag("DisconnectWorker");
                }else{
                    DisconnectHelper.unregisterPIntent(applicationContext)
                }

                ed.putBoolean("enableApp", false)
                ed.putBoolean("enableLogs", false)
                swLogs.isEnabled = false
                swLogs.isChecked = false
            }
            ed.commit()
        }

        swLogs.setOnCheckedChangeListener { _, isChecked ->
            ed = sharedPrefs.edit()
            if (isChecked) {
                ed.putBoolean("enableLogs", true)
            }else{
                ed.putBoolean("enableLogs", false)
            }
            ed.commit()
        }

        timerMinTextEdit.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                val sInInt = try {
                    s.toString().toInt()
                } catch (ex: NumberFormatException) {
                    0
                }
                if (sInInt < 1 || sInInt > 600) {
                    timerMinTextEdit.error = "Minutes must be between 1 and 600"
                } else {
                    timerMinTextEdit.error = null
                    ed = sharedPrefs.edit()
                    ed.putInt("disconnectTimerMin", sInInt)
                    ed.apply()
                }

            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
            }

        })

        timerMinTextEdit.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
            }
            false
        }


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

   }


    private fun alertNoRoot() {

        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle("Root Check")
        alertDialog.setMessage("It seems you don't have a rooted device, this app needs root in order to disconnect data network.\n\nIn case your device is really rooted try pressing the button to grand root privileges")

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { dialogInterface: DialogInterface, _ : Int ->
            dialogInterface.dismiss()
        }

        alertDialog.show()
    }

companion object{

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun startApp(context: Context) {
        if(DisconnectHelper.getConnectionType(context) == 1){
            DisconnectHelper.registerDisconnectWorker(context)
        }else{
            DisconnectHelper.registerPIntent(context)
        }
    }

}


}
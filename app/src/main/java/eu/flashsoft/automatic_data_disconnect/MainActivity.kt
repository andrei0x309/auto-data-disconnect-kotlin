package eu.flashsoft.automatic_data_disconnect


import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences.Editor
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.IOException


class MainActivity : AppCompatActivity() {

    //On Create

    // Home Fragment
    lateinit var swAppEnable: SwitchCompat
    lateinit var swLogs: SwitchCompat
    lateinit var timerMinTextEdit: EditText
    lateinit var grantRootBtn: Button


    @RequiresApi(Build.VERSION_CODES.M)
    fun homeFragmentLoaded() {


        grantRootBtn.setOnClickListener {

            try {
                Runtime.getRuntime().exec("su")
                Toast.makeText(applicationContext, "Root rights received", Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(applicationContext, "Root rights not received", Toast.LENGTH_LONG)
                    .show()
            }

        }


        val sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        var ed: Editor


        // Get pref
        val boolAppEnabled = sharedPrefs.getBoolean("enableApp", false)
        val boolLogsEnabled = sharedPrefs.getBoolean("enableLogs", false)
        val intTimerDisconnectMin = sharedPrefs.getInt("disconnectTimerMin", 15)

        // Set View comp
        swAppEnable.isChecked = boolAppEnabled
        swLogs.isChecked = boolLogsEnabled
        timerMinTextEdit.text =
            Editable.Factory.getInstance().newEditable(intTimerDisconnectMin.toString())


        val mainLayout = findViewById<ConstraintLayout>(R.id.container)
        mainLayout.setOnClickListener {
            it.requestFocus()
        }

        swAppEnable.setOnCheckedChangeListener { _, isChecked ->
            ed = sharedPrefs.edit()
            if (isChecked) {
                startApp(applicationContext)
                ed.putBoolean("enableApp", true)
                ed.putBoolean("enableLogs", true)
                ed.putBoolean("disconnectPending", true)
                swLogs.isEnabled = true
                swLogs.isChecked = true
            } else {
                stopApp(applicationContext)
                ed.putBoolean("enableApp", false)
                ed.putBoolean("enableLogs", false)
                swLogs.isEnabled = false
                swLogs.isChecked = false
            }
            ed.apply()
        }

        swLogs.setOnCheckedChangeListener { _, isChecked ->
            ed = sharedPrefs.edit()
            if (isChecked) {
                ed.putBoolean("enableLogs", true)
            } else {
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
                    if (sharedPrefs.getBoolean("enableApp", false)) {
                        ed.putBoolean("disconnectPending", false)
                        Toast.makeText(applicationContext, "Timer Reset", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(applicationContext, "Timer Set", Toast.LENGTH_SHORT).show()
                    }
                    ed.apply()
                }

            }

            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
            }

        })

        timerMinTextEdit.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
            }
            false
        }

        timerMinTextEdit.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val imm: InputMethodManager =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val ed: Editor

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
            if (!rootUtil.isDeviceRooted) alertNoRoot()

        }


        val boolAppEnabled = sharedPrefs.getBoolean("enableApp", false)
        if (boolAppEnabled) startApp(applicationContext)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)


        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = navHostFragment.navController

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

    }


    private fun alertNoRoot() {

        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle("Root Check")
        alertDialog.setMessage("It seems you don't have a rooted device, this app needs root in order to disconnect data network.\n\nIn case your device is really rooted try pressing the button to grant root privileges.")

        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            "OK"
        ) { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss()
        }

        alertDialog.show()
    }

    companion object {

        @RequiresApi(Build.VERSION_CODES.M)
        fun startApp(context: Context) {
            //Log.d("mobile on", DisconnectHelper.isMobileOnAllNetworks(context).toString())
            if (DisconnectHelper.isMobileOnAllNetworks(context)) {
                DisconnectHelper.registerDisconnectWorker(context)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    DisconnectHelper.registerPIntent(context)
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.M)
        fun stopApp(context: Context) {
            if (DisconnectHelper.isMobileOnAllNetworks(context)) {
                WorkManager.getInstance(context).cancelAllWorkByTag("DisconnectWorker")
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    DisconnectHelper.unregisterPIntent(context)
                }
            }
        }


    }


}
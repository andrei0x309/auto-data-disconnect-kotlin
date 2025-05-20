package eu.flashsoft.automatic_data_disconnect

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView // Import TextView
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.* // Import coroutine dependencies
import java.io.IOException
import java.util.concurrent.TimeUnit // Import TimeUnit
import androidx.core.content.edit
import android.content.SharedPreferences

// --- Imports for Permission Handling ---
//import android.content.pm.PackageManager
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.core.content.ContextCompat
// ------------------------------------

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    //On Create

    // Home Fragment
    lateinit var swAppEnable: SwitchCompat
    lateinit var swLogs: SwitchCompat
    lateinit var swWiFIAlso: SwitchCompat
    lateinit var timerMinTextEdit: EditText
    lateinit var grantRootBtn: Button
    lateinit var timerTextView: TextView // Reference to the timer TextView
    lateinit var statusTextView: TextView
    lateinit var homeFragment: View

    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob()) // Coroutine scope for UI updates

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.M)
    fun homeFragmentLoaded() {
        // Initialize UI elements assuming they are in your layout
        Log.d(AppConstants.MAIN_ACTIVITY_TAG, "Fragment mounted")

        grantRootBtn.setOnClickListener {
            try {
                // Note: Directly executing "su" here might not always work reliably
                // for granting persistent root access. Libraries like libsu are better.
                Runtime.getRuntime().exec("su")
                Toast.makeText(applicationContext, "Root rights received", Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(applicationContext, "Root rights not received", Toast.LENGTH_LONG).show()
            }
        }

        val sharedPrefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
        val mainLayout = findViewById<ConstraintLayout>(R.id.container) // Replace with your root layout ID

        // Get pref
        val boolAppEnabled = sharedPrefs.getBoolean(AppConstants.IS_SERVICE_ENABLED_KEY, true)
        val boolLogsEnabled = sharedPrefs.getBoolean(AppConstants.IS_LOGS_ENABLED_KEY, true)
        var networkStatus = sharedPrefs.getBoolean(AppConstants.NETWORK_STATUS_KEY, false)
        var boolWiFIAlso = sharedPrefs.getBoolean(AppConstants.IS_WIFI_DISCONNECT_ENABLED_KEY, false)
        val intTimerDisconnectMin = sharedPrefs.getInt(AppConstants.DISCONNECT_TIMER_KEY, 15)

        // Set View comp
        swAppEnable.isChecked = boolAppEnabled
        swLogs.isChecked = boolLogsEnabled
        timerMinTextEdit.text = Editable.Factory.getInstance().newEditable(intTimerDisconnectMin.toString())
        swWiFIAlso.isChecked = boolWiFIAlso

        // Initial state for logs switch based on app enable state
        swLogs.isEnabled = boolAppEnabled
        if (!boolAppEnabled) {
            swLogs.isChecked = false // Uncheck logs if app is disabled
            swWiFIAlso.isChecked = false // Uncheck wifi switch if app is disabled
            swLogs.isEnabled = false
            swWiFIAlso.isEnabled = false
        }

        if(!boolAppEnabled || !networkStatus) {
            statusTextView.text = resources.getString(R.string.service_off)
        } else {
            statusTextView.text = resources.getString(
                if(boolWiFIAlso) R.string.status_networks_on
                else R.string.status_on
            )

        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            getNotificationPermission()
//
//            swNotificationsA13.setOnCheckedChangeListener { _, isChecked ->
//                if(isChecked) {
//                    getNotificationPermission()
//                } else {
//                    sharedPrefs.edit{
//                        putBoolean(AppConstants.IS_SERVICE_NOTIFICATION_ENABLED_A13_KEY, false)
//                    }
//                }
//            }
//
//        } else {
//            swNotificationsA13.visibility = View.GONE
//        }


        // Set initial timer state when fragment is loaded
        startTimer()
        updateTimerDisplay() // Update display immediately

        mainLayout.setOnClickListener {
            // Clear focus from EditText when clicking outside
            timerMinTextEdit.clearFocus()
            // Hide the keyboard if it's open
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }


        swAppEnable.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                boolWiFIAlso = sharedPrefs.getBoolean(AppConstants.IS_WIFI_DISCONNECT_ENABLED_KEY, false)
                networkStatus = sharedPrefs.getBoolean(AppConstants.NETWORK_STATUS_KEY, false)
                MobileDataMonitorService.enableService(applicationContext)
                // When enabling, assume logs should be enabled by default unless user changes
                sharedPrefs.edit {
                    putBoolean(AppConstants.IS_LOGS_ENABLED_KEY, true)
                    putLong(AppConstants.DISCONNECT_TIMESTAMP, System.currentTimeMillis()) // Set timestamp on enable
                }
                swLogs.isChecked = true

                swLogs.isEnabled = true
                swWiFIAlso.isEnabled = true

                if(boolWiFIAlso && !networkStatus) {
                    statusTextView.text = resources.getString(R.string.status_off)
                }else if(!networkStatus) {
                    statusTextView.text = resources.getString(R.string.status_networks_off)
                } else if(boolWiFIAlso) {
                    statusTextView.text = resources.getString(R.string.status_networks_on)
                } else {
                    statusTextView.text = resources.getString(R.string.status_on)
                }

                if(boolWiFIAlso) {
                    swWiFIAlso.isChecked = true
                } else {
                    restartApp(applicationContext)
                }
                startTimer() // Start timer when app is enabled
                updateTimerDisplay()
            } else {
                stopApp(applicationContext)
                MobileDataMonitorService.disableService(applicationContext)
                sharedPrefs.edit{
                    putBoolean(AppConstants.IS_LOGS_ENABLED_KEY, false) // Disable logs when app is disabled
                }
                // When disabling, no disconnect is pending
                swLogs.isEnabled = false
                swLogs.isChecked = false // Uncheck logs when disabling
                swWiFIAlso.isChecked = false // Uncheck wifi switch when disabling
                swWiFIAlso.isEnabled = false // Disable wifi switch when disabling

                stopTimer() // Stop timer when app is disabled
                timerTextView.text = "Timer stopped" // Update timer display
                timerTextView.visibility = View.INVISIBLE // Hide timer when stopped
                statusTextView.text = resources.getString(R.string.service_off)
            }
        }

        swLogs.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit {
                putBoolean(AppConstants.IS_LOGS_ENABLED_KEY, isChecked)
            }
        }

        swWiFIAlso.setOnCheckedChangeListener { _, isChecked ->
            networkStatus = sharedPrefs.getBoolean(AppConstants.NETWORK_STATUS_KEY, false)
            if (isChecked) {
                sharedPrefs.edit{
                    putBoolean(AppConstants.IS_WIFI_DISCONNECT_ENABLED_KEY, true)
                }
                statusTextView.text = resources.getString(
                    if(networkStatus) R.string.status_networks_on
                    else R.string.status_networks_off
                )
            } else {
                sharedPrefs.edit{
                    putBoolean(AppConstants.IS_WIFI_DISCONNECT_ENABLED_KEY, false)
                }
                statusTextView.text = resources.getString(
                    if(networkStatus) R.string.status_on
                    else R.string.status_off
                )
            }
            if(MobileDataMonitorService.isServiceEnabled(applicationContext)) {
                restartApp(applicationContext)
                updateTimerDisplay()
                startTimer()
            }
        }

        timerMinTextEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val sInInt = try {
                    s.toString().toInt()
                } catch (_: NumberFormatException) {
                    0
                }
                val currentTimer = sharedPrefs.getInt(AppConstants.DISCONNECT_TIMER_KEY, 15)
                val isSame = sInInt.compareTo(currentTimer) == 0
                if (isSame) {
                    return
                }
                if (sInInt < 1 || sInInt > 600) {
                    timerMinTextEdit.error = "Minutes must be between 1 and 600"
                    // Don't save invalid value
                } else {
                    timerMinTextEdit.error = null
                    sharedPrefs.edit{
                        putInt(AppConstants.DISCONNECT_TIMER_KEY, sInInt)
                    }

                    // If service is enabled, changing the timer should reset the pending state
                    if (MobileDataMonitorService.isServiceEnabled(applicationContext)) {
                        sharedPrefs.edit{
                            putLong(AppConstants.DISCONNECT_TIMESTAMP, System.currentTimeMillis()) // Reset timestamp
                        }
                        Toast.makeText(applicationContext, "Timer Reset", Toast.LENGTH_SHORT).show()
                        startTimer() // Restart timer with new duration
                    } else {
                        Toast.makeText(applicationContext, "Timer Set", Toast.LENGTH_SHORT).show()
                        // If service is disabled, just set the timer value, don't start/reset pending
                    }

                    restartApp(applicationContext)
                    updateTimerDisplay()
                    startTimer()
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        timerMinTextEdit.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus() // Clear focus to hide keyboard
                true // Consume the event
            } else {
                false // Let the system handle other actions
            }
        }

        timerMinTextEdit.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                // Hide the keyboard when focus is lost
                val imm: InputMethodManager =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }

    }

    @SuppressLint("SetTextI18n")
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // This method is called whenever a shared preference changes
        // Check the 'key' parameter to see which preference changed
        when (key) {
            AppConstants.NETWORK_STATUS_KEY -> {
                val isOn = sharedPreferences?.getBoolean(key, false) == true
                val isWifiAlso = sharedPreferences?.getBoolean(AppConstants.IS_WIFI_DISCONNECT_ENABLED_KEY, false) == true
                Log.d("PrefsListener", "$key changed to $isOn")
                updateTimerDisplay()
                startTimer()
                if(isOn) {
                        statusTextView.text = resources.getString(
                            if(isWifiAlso)R.string.status_networks_on
                            else R.string.status_on
                        )
                    timerTextView.visibility = View.VISIBLE
                } else {
                    statusTextView.text = resources.getString(
                        if(isWifiAlso)R.string.status_networks_off
                        else R.string.status_off
                    )
                    timerTextView.visibility = View.INVISIBLE
                }

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val sharedPrefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)


        if (!sharedPrefs.contains(AppConstants.PREFERENCES_INITIALIZED_KEY)) {
            //Indicate that the default shared prefs have been set
            sharedPrefs.edit{
                putBoolean(AppConstants.PREFERENCES_INITIALIZED_KEY, true)
                //Set some default shared pref
                putBoolean(AppConstants.IS_SERVICE_ENABLED_KEY, true)
                putBoolean(AppConstants.IS_LOGS_ENABLED_KEY, true)
                putLong(AppConstants.DISCONNECT_TIMESTAMP, System.currentTimeMillis()) // Set initial timestamp
                putInt(AppConstants.DISCONNECT_TIMER_KEY, 15)
                putBoolean(AppConstants.NETWORK_STATUS_KEY, WorkerHelper.isMobileOnAllNetworks(applicationContext))
                putBoolean(AppConstants.IS_WIFI_DISCONNECT_ENABLED_KEY, false)
                putBoolean(AppConstants.IS_SERVICE_NOTIFICATION_ENABLED_A13_KEY, false)
            }


            val rootUtil = RootUtil // Assuming RootUtil exists and has isDeviceRooted
            if (!rootUtil.isDeviceRooted) alertNoRoot()
        }

        // The service start logic is now handled in homeFragmentLoaded based on the switch state
        // and potentially by the WorkerHelper on boot/periodic checks.
        // We don't necessarily need to start it directly in onCreate of the Activity.
        // If the app was enabled last time, the WorkerHelper should ensure the service is running.


        val navView: BottomNavigationView = findViewById(R.id.nav_view)


        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = navHostFragment.navController

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            Log.d(AppConstants.MAIN_ACTIVITY_TAG, "Destination changed ${destination.id} ${R.id.navigation_home}")

        }

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val isServiceEnabled = sharedPrefs.getBoolean(AppConstants.IS_SERVICE_ENABLED_KEY, true)

        Log.d(AppConstants.MAIN_ACTIVITY_TAG, " isServiceEnabled $isServiceEnabled ")

        if(isServiceEnabled){
            startApp(applicationContext)
        }

    }

//    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
//    fun getNotificationPermission() {
//        when {
//            ContextCompat.checkSelfPermission(
//                this,
//                android.Manifest.permission.POST_NOTIFICATIONS
//            ) == PackageManager.PERMISSION_GRANTED -> {
//                // Permission is already granted, start the service
//                val sharedPrefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
//                sharedPrefs.edit {
//                    putBoolean(AppConstants.IS_SERVICE_NOTIFICATION_ENABLED_A13_KEY, true)
//                }
//                return
//            }
//            shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
//                // Show rationale
//                Log.d(AppConstants.MAIN_ACTIVITY_TAG, "API 33+: Showing notification permission rationale.")
//                AlertDialog.Builder(this)
//                    .setTitle("Notification Permission Recommended") // Changed title
//                    .setMessage("Allow notifications to see the countdown timer and status updates in the background.") // Changed message
//                    .setPositiveButton("OK") { dialog, _ ->
//                        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
//                        dialog.dismiss()
//                    }
//                    .setNegativeButton("Cancel") { dialog, _ ->
//                        Log.d(AppConstants.MAIN_ACTIVITY_TAG, "API 33+: Notification permission rationale declined.")
//                        // User declined permission, but service can still run without notification.
//                        // Service can still start without notification if app is enabled
//                        Log.d(AppConstants.MAIN_ACTIVITY_TAG, "API 33+: Rationale declined, but app enabled. Starting service without notification.")
//                        dialog.dismiss()
//                    }
//                    .show()
//            }
//            else -> {
//                // Directly request the permission
//                Log.d(AppConstants.MAIN_ACTIVITY_TAG, "API 33+: Requesting notification permission.")
//                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
//            }
//        }
//    }

//    private val requestPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
//            if (isGranted) {
//                // Permission was granted!
//                Log.d(AppConstants.MAIN_ACTIVITY_TAG, "Notification permission granted.")
//                // Now that permission is granted, check if the service should be started
//                // based on the app enable switch and notification enable switch.
//                val sharedPrefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
//                sharedPrefs.edit {
//                    putBoolean(AppConstants.IS_SERVICE_NOTIFICATION_ENABLED_A13_KEY, true)
//                }
//            } else {
//                // Permission denied.
//                Log.d(AppConstants.MAIN_ACTIVITY_TAG, "Notification permission denied.")
//                Toast.makeText(this, "Notification permission is recommended for the background service.", Toast.LENGTH_LONG).show()
//                // If permission is denied, disable the notification switch and potentially the app switch
//                swNotificationsA13.isChecked = false // Disable notification switch
//                val sharedPrefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
//                sharedPrefs.edit {
//                    putBoolean(AppConstants.IS_SERVICE_NOTIFICATION_ENABLED_A13_KEY, false)
//                }
//            }
//        }

    override fun onResume() {
        super.onResume()
        // Start the timer when the activity becomes visible if the service is enabled and pending
        val sharedPrefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
        startTimer()
        timerTextView.visibility = View.VISIBLE // Ensure timer is visible

        if(sharedPrefs.getBoolean(AppConstants.NETWORK_STATUS_KEY, false)) {
            val isServiceOn = sharedPrefs.getBoolean(AppConstants.IS_SERVICE_ENABLED_KEY, false)
            val isWifiAlso = sharedPrefs.getBoolean(AppConstants.IS_WIFI_DISCONNECT_ENABLED_KEY, false)

            fun getStatusText() = if(isWifiAlso) R.string.status_networks_on else R.string.status_on

            statusTextView.text = resources.getString(
                if(!isServiceOn) R.string.service_off
                else getStatusText()
            )
        } else {
            statusTextView.text = resources.getString(R.string.status_off)
            timerTextView.visibility = View.INVISIBLE // Hide timer
        }

        sharedPrefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        // Stop the timer when the activity is paused
        stopTimer()
        val sharedPrefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the coroutine scope when the activity is destroyed
        activityScope.cancel()
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



    /**
     * Starts the coroutine timer to update the UI.
     */
    @SuppressLint("SetTextI18n")
    private fun startTimer() {
        // Cancel any existing timer job before starting a new one
        stopTimer()

        val sharedPrefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
        val disconnectTimerMinutes = sharedPrefs.getInt(AppConstants.DISCONNECT_TIMER_KEY, 15)
        val disconnectTimestamp = sharedPrefs.getLong(AppConstants.DISCONNECT_TIMESTAMP, System.currentTimeMillis())
        val dataStatus =  sharedPrefs.getBoolean(AppConstants.NETWORK_STATUS_KEY, false)

        // Calculate the target disconnect time in milliseconds
        val targetDisconnectTimeMillis = disconnectTimestamp + TimeUnit.MINUTES.toMillis(disconnectTimerMinutes.toLong())

        // Launch a coroutine in the activity's scope to update the UI every second
        activityScope.launch {
            while (isActive) { // Loop while the coroutine is active
                val currentTimeMillis = System.currentTimeMillis()
                val remainingTimeMillis = targetDisconnectTimeMillis - currentTimeMillis

                if (remainingTimeMillis <= 0 || !dataStatus) {
                    // Timer has reached zero or gone past
                    updateTimerDisplay(0) // Display 00:00
                    stopTimer() // Stop the timer
                    timerTextView.text = "Disconnecting..." // Or similar message
                    timerTextView.visibility = View.INVISIBLE // Hide timer after disconnect


                    break // Exit the loop
                } else {
                    // Update the timer display
                    updateTimerDisplay(remainingTimeMillis)
                    timerTextView.visibility = View.VISIBLE // Ensure timer is visible
                }

                delay(1000) // Wait for 1 second
            }
        }
        Log.d(AppConstants.SERVICE_TAG, "Timer started")
    }

    /**
     * Stops the coroutine timer.
     */
    private fun stopTimer() {
        activityScope.coroutineContext.cancelChildren() // Cancel all coroutines launched in this scope
        Log.d(AppConstants.SERVICE_TAG, "Timer stopped")
    }

    /**
     * Updates the timer TextView with the remaining time.
     * @param remainingTimeMillis The remaining time in milliseconds.
     */
    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun updateTimerDisplay(remainingTimeMillis: Long? = null) {

        if (remainingTimeMillis != null) {
            // Calculate minutes and seconds from milliseconds
            val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTimeMillis)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTimeMillis) % 60 // Get remaining seconds

            // Format the time as MM:SS
            val timeString = String.format("%02d:%02d", minutes, seconds)
            timerTextView.text = timeString
        } else {
            timerTextView.text = "Timer not active"
            timerTextView.visibility = View.INVISIBLE
        }
    }


    private companion object { // Changed to private companion object

        fun startApp(context: Context) {
            Log.d(AppConstants.MAIN_ACTIVITY_TAG, "Starting app monitoring")
            //Log.d("mobile on", WorkerHelper.isMobileOnAllNetworks(context).toString())
            MobileDataMonitorService.startService(context)
            WorkerHelper.registerServiceMonitorWorker(context) // Ensure service monitor worker is scheduled
            val sharedPrefs = context.getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
            if(WorkerHelper.isMobileOnAllNetworks(context)) {
                sharedPrefs.edit {
                    putBoolean(AppConstants.NETWORK_STATUS_KEY, true)
                }
            } else {
                sharedPrefs.edit {
                    putBoolean(AppConstants.NETWORK_STATUS_KEY, false)
                }
            }
        }

        fun stopApp(context: Context) {
            Log.d(AppConstants.MAIN_ACTIVITY_TAG, "Stopping app monitoring")
            MobileDataMonitorService.stopService(context)
            // Cancel the service monitor worker when stopping the app
            WorkerHelper.unregisterServiceMonitorWorker(context)
            // Also cancel any pending disconnect worker if app is stopped
            WorkerHelper.unregisterDisconnectWorker(context)
        }

        fun restartApp(context: Context) {
            stopApp(context)
            startApp(context)
        }
    }
}

package eu.flashsoft.automatic_data_disconnect

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.Editor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Switch
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    var swService:Switch? = null
    var swLogs:Switch? = null
    var timerMinTextEdit:EditText? = null
    var networkPendingIntent: PendingIntent? = null


    private fun getNetworkIntent(): PendingIntent {
        if (networkPendingIntent != null) {
            return networkPendingIntent!!
        }

        val intent = Intent(this, DataConnReceiver::class.java)
        networkPendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        return networkPendingIntent!!
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getNetworkRequest(): NetworkRequest {
        return NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun registerNetworkUpdates() {
        var cm =
                applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        cm?.registerNetworkCallback(getNetworkRequest(), getNetworkIntent())
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun homeFragmentLoaded(){

        val root = Runtime.getRuntime().exec("su")


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

        //if(boolServiceEnabled) DisconnectStickyService.selfStart(this)


        val serviceStarted = DisconnectStickyService.isServiceStarted
        swService?.setOnCheckedChangeListener { _, isChecked ->
            ed = sharedPrefs.edit()
            if (isChecked) {

            val uploadWorkRequest: WorkRequest =
                       OneTimeWorkRequestBuilder<DisconnectWorker>()
                               .setInitialDelay(29, TimeUnit.SECONDS)
                               .addTag("DisconnectWorker")
                               // Additional configuration
                               .build()
               WorkManager.getInstance(applicationContext)
                       .enqueue(uploadWorkRequest)

               /*
                registerNetworkUpdates()
 */
               Log.d("main", "switch on")
               ed.putBoolean("enableService", true)
           } else {
               WorkManager.getInstance(applicationContext).cancelAllWorkByTag("DisconnectWorker");
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

       // --------------------------------------------------
       // count lines
       val reader = BufferedReader(FileReader("file.txt"))
       var lines = 0
       while (reader.readLine() != null) lines++
       reader.close()

       // --------------------------------------------------



       fun removeLines(fileName: String, startLine: Int, numLines: Int) {
           require(!fileName.isEmpty() && startLine >= 1 && numLines >= 1)
           val f = File(fileName)
           if (!f.exists()) {
               println("$fileName does not exist")
               return
           }
           var lines = f.readLines()
           val size = lines.size
           if (startLine > size) {
               println("The starting line is beyond the length of the file")
               return
           }
           var n = numLines
           if (startLine + numLines - 1 > size) {
               println("Attempting to remove some lines which are beyond the end of the file")
               n = size - startLine + 1
           }
           lines = lines.take(startLine - 1) + lines.drop(startLine + n - 1)
           val text = lines.joinToString(System.lineSeparator())
           f.writeText(text)
       }


   }

   // --------------------------------------------------



}
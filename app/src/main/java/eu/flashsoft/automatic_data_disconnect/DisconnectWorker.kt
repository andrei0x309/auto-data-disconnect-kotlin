package eu.flashsoft.automatic_data_disconnect

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import java.io.DataOutputStream
import java.text.SimpleDateFormat
import java.util.*


class DisconnectWorker(appContext: Context, workerParams: WorkerParameters):
        Worker(appContext, workerParams) {

    private fun disableMobileData() {
        try {
            val commands = arrayOf("svc data disable")
            val p = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(p.outputStream)
            for (tmpCmd in commands) {
                os.writeBytes(
                        """
    $tmpCmd
    
    """.trimIndent()
                )
            }
            os.writeBytes("exit\n")
            os.flush()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun writeLogLine(success: Boolean){
        val currentDate: String = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val filename = "logs.txt"
        val logLine = if(success) "<font color='#114444'>[ $currentDate ]</font> DATA was disconnected\n<br>" else "<font color='#114444'>[ $currentDate ]</font> Failed to disconnect DATA - missing root rights\n<br>"
        applicationContext.openFileOutput(filename, Context.MODE_APPEND).use {
            it.write(logLine.toByteArray())
        }
    }

    private fun restartDisconnect(sharedPrefs: SharedPreferences){
        val ed = sharedPrefs.edit()
        ed.putBoolean("disconnectPending", true)
        val minSettings = sharedPrefs.getInt("disconnectTimerMin", 15)
        val offTime = System.currentTimeMillis() + (minSettings *60000)
        ed.putLong("disconnectStamp", offTime)
        ed.putBoolean("disconnectPending", true)
        WorkManager.getInstance(applicationContext).cancelAllWorkByTag("DisconnectWorker")
        ed.apply()
    }




    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun doWork(): Result {


        val isMobile = DisconnectHelper.isMobileOnAllNetworks(applicationContext)
        val sharedPrefs = applicationContext.getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        //Log.d("--- test", "mobile? $isMobile")

        var disableSuccess = false

        if(isMobile){


            val curTime = System.currentTimeMillis()
            val disTime = sharedPrefs.getLong("disconnectStamp", curTime)
            val disPending = sharedPrefs.getBoolean("disconnectPending", false)

            if(!disPending) {
                restartDisconnect(sharedPrefs)
            }


            if(disPending && (curTime  >= disTime) ){
                //Log.d("-- Can I reach here", "yes")
                val ed = sharedPrefs.edit()
                ed.putBoolean("disconnectPending", false)
                ed.apply()
                disableMobileData()
                Thread.sleep(1500)
                disableSuccess = !DisconnectHelper.isMobileOnAllNetworks(applicationContext)
                val logOn = sharedPrefs.getBoolean("enableLogs", false)
                if (logOn) writeLogLine(disableSuccess)
            }

            if(disableSuccess){

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                    DisconnectHelper.registerPIntent(applicationContext)
                }

            }else{
                DisconnectHelper.registerDisconnectWorker(applicationContext)
            }


        }else{
            val ed = sharedPrefs.edit()
            ed.putBoolean("disconnectPending", false)
            ed.apply()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                DisconnectHelper.registerPIntent(applicationContext)
            }

        }


        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }


}
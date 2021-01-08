package eu.flashsoft.automatic_data_disconnect

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import java.io.DataOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class DisconnectWorker(appContext: Context, workerParams: WorkerParameters):
        Worker(appContext, workerParams) {

    private fun disableMobileData() {
        try {
            val cmds = arrayOf("svc data disable")
            val p = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(p.outputStream)
            for (tmpCmd in cmds) {
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

    private fun writeLogLine(){
        val currentDate: String = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val filename = "logs.txt"
        val logLine = "[ON: $currentDate] App Disconnected DATA\n"
        applicationContext.openFileOutput(filename, Context.MODE_APPEND).use {
            it.write(logLine.toByteArray())
        }

    }

    fun restartDisconnect(sharedPrefs: SharedPreferences){
        val ed = sharedPrefs.edit()
        ed.putBoolean("disconnectPending", true)
        val minSettings = sharedPrefs.getInt("disconnectTimerMin", 20)
        val offTime = System.currentTimeMillis() + (minSettings *60000)
        ed.putLong("disconnectStamp", offTime)
        ed.putBoolean("disconnectPending", true)
        WorkManager.getInstance(applicationContext).cancelAllWorkByTag("DisconnectWorker")
        ed.commit()
    }

    override fun doWork(): Result {

        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isMetered = cm.isActiveNetworkMetered
        val sharedPrefs = applicationContext.getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)

        if(isMetered){

            val curTime = System.currentTimeMillis()
            val disTime = sharedPrefs.getLong("disconnectStamp", curTime)
            val disPending = sharedPrefs.getBoolean("disconnectPending", false)
            val logOn = sharedPrefs.getBoolean("enableLogs", false)
            if (logOn) writeLogLine()

            if(!disPending) {
                restartDisconnect(sharedPrefs)
            }


            if(disPending && (curTime  >= disTime) ){
                val ed = sharedPrefs.edit()
                ed.putBoolean("disconnectPending", false)
                ed.commit()
                disableMobileData()
            }

        }else{
            val ed = sharedPrefs.edit()
            ed.putBoolean("disconnectPending", false)
            ed.commit()
        }

        val uploadWorkRequest = OneTimeWorkRequestBuilder<DisconnectWorker>()
                .setInitialDelay(29, TimeUnit.SECONDS)
                .addTag("DisconnectWorker")
                // Additional configuration
                .build()
        WorkManager.getInstance(applicationContext)
                .enqueue(uploadWorkRequest)

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }


}
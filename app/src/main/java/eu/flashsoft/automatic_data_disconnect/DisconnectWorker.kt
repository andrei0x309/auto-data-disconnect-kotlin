package eu.flashsoft.automatic_data_disconnect

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.DataOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


enum class LogMode(val value: Int) {
    DATA_ONLY(1),
    ALSO_WIFI(2)
}

class DisconnectWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    private fun disableMobileData(wifiAlso : Boolean = false) {
        try {
            val commands = arrayOf("svc data disable", "svc wifi disable")

            for (cmd in commands) {
            var command = arrayOf(cmd)
            if (!wifiAlso && command[0] == "svc wifi disable") continue
            val p = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(p.outputStream)

                for (commandText in command) {
                    os.writeBytes(
                        """
                $commandText
                    \n
                    """.trimIndent()
                    )
                }
                os.writeBytes("exit\n")
                os.flush()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun writeLogLine(success: Boolean, mode: LogMode = LogMode.DATA_ONLY) {
        val currentDate: String =
            SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val filename = "logs.txt"

        val whatDisconnected = if(mode == LogMode.DATA_ONLY) "DATA" else "DATA and WIFI"

        val logLine =
            if (success) "<font color='#114444'>[ $currentDate ]</font> $whatDisconnected was disconnected\n<br>" else "<font color='#114444'>[ $currentDate ]</font> Failed to disconnect DATA - missing root rights\n<br>"
        applicationContext.openFileOutput(filename, Context.MODE_APPEND).use {
            it.write(logLine.toByteArray())
        }
    }

    override fun doWork(): Result {
        val sharedPrefs =
            applicationContext.getSharedPreferences(
                AppConstants.PREFS_NAME,
                AppCompatActivity.MODE_PRIVATE
            )

        val logOn = sharedPrefs.getBoolean(AppConstants.IS_LOGS_ENABLED_KEY, false)
        val isWifiDisconnectEnabled = sharedPrefs.getBoolean(AppConstants.IS_WIFI_DISCONNECT_ENABLED_KEY, false)
        Log.d(AppConstants.WORKER_DISCONNECT_TAG, "Disconnect Worker: doWork, also WIFI: $isWifiDisconnectEnabled")

        disableMobileData(isWifiDisconnectEnabled)
        Thread.sleep(1500)
        val disableSuccess = !WorkerHelper.isMobileOnAllNetworks(applicationContext)
        if (logOn) writeLogLine(disableSuccess, if(isWifiDisconnectEnabled) LogMode.ALSO_WIFI else LogMode.DATA_ONLY)


        if (disableSuccess) {

            sharedPrefs.edit {
                putBoolean(AppConstants.NETWORK_STATUS_KEY, false)
            }

        } else {
            WorkerHelper.registerDisconnectWorker(applicationContext)
        }


        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }


}
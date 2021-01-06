package eu.flashsoft.automatic_data_disconnect

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Binder
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import java.io.DataOutputStream

class DisconnectStickyService : Service() {

    private val mBinder: IBinder = Binder()


    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()
        isServiceStarted = true
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceStarted = false
        selfStart(this)
    }


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


    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        try {
            registerReceiver(DataConnReceiver(), intentFilter)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace();
        }


        /* val task = DisconnectServiceAsyncTask()
        task.context = this
        task.executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR, startId)

         */

        val sharedPrefs = this.getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)

        while (true){

            val curTime = System.currentTimeMillis()
            val disTime = sharedPrefs.getLong("disconnectStamp", curTime)
            val disPending = sharedPrefs.getBoolean("disconnectPending", false)
            if(disPending && (curTime  >= disTime) ){
                val ed = sharedPrefs.edit()
                ed.putBoolean("disconnectPending", false)
                ed.commit()
                disableMobileData()
            }
            Thread.sleep(1000)
        }

        return START_REDELIVER_INTENT
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        selfStart(this)
    }


    companion object {
        var isServiceStarted = false

        fun selfStart(packageContext: Context) {
            val sharedPrefs = packageContext.getSharedPreferences("app_settings", MODE_PRIVATE)
            val service = Intent(packageContext, DisconnectStickyService::class.java)
            val boolLogsEnabled = sharedPrefs.getBoolean("enableLogs", false)
            if (!isServiceStarted && boolLogsEnabled) {
                try {
                    packageContext.startService(service)
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace();
                }
            }
        }

        fun selfStartApp(packageContext: Context) {
            val reLaunchMain = Intent(packageContext, MainActivity::class.java)
            reLaunchMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            reLaunchMain.addFlags(Intent.FLAG_FROM_BACKGROUND);
            reLaunchMain.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            reLaunchMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            reLaunchMain.putExtra("MOVE_BACK_BOOL", true)
            packageContext.startActivity(reLaunchMain)
        }

    }

}
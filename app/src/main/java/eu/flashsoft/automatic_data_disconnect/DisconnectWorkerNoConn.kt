package eu.flashsoft.automatic_data_disconnect

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.work.*
import java.io.DataOutputStream
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class DisconnectWorkerNoConn (appContext: Context, workerParams: WorkerParameters):
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

    override fun doWork(): Result {


        Log.d("workLog", "------ no Network")

        val uploadWorkRequest: WorkRequest =
                OneTimeWorkRequestBuilder<DisconnectWorkerNoConn>()
                        .setInitialDelay(35, TimeUnit.SECONDS)
                        .setConstraints(Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                                .build())
                        .addTag("DisconnectWorker")
                        // Additional configuration
                        .build()
        WorkManager.getInstance(applicationContext)
                .enqueue(uploadWorkRequest)


        //val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager




        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}
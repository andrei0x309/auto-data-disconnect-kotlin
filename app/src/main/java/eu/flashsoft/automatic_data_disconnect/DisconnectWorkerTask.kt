package eu.flashsoft.automatic_data_disconnect

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.work.*
import java.io.DataOutputStream
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class DisconnectWorkerTask(appContext: Context, workerParams: WorkerParameters):
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

        // Do the work here--in this case, upload the images.
        //uploadImages()
        val currentTime: Date? = Calendar.getInstance().getTime()

        val filename = "logs.txt"
        val logLine = "Log line \n"
        applicationContext.openFileOutput(filename, Context.MODE_APPEND).use {
            it.write(logLine.toByteArray())
        }

        val file = File(applicationContext.filesDir, filename)
        val contents = file.readText() // Read file


        var haveConnectedWifi = false
        var haveConnectedMobile = false

        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isMetered = cm.isActiveNetworkMetered


        Log.d("workLog", contents)

        val uploadWorkRequest: WorkRequest =
                OneTimeWorkRequestBuilder<DisconnectWorker>()
                        .setInitialDelay(35, TimeUnit.SECONDS)
                        .setConstraints(Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.METERED)
                                .build())
                        .addTag("DisconnectWorker")
                        // Additional configuration
                        .build()
        WorkManager.getInstance(applicationContext)
                .enqueue(uploadWorkRequest)


        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}
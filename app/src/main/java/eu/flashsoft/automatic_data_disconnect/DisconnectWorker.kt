package eu.flashsoft.automatic_data_disconnect

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class DisconnectWorker (appContext: Context, workerParams: WorkerParameters):
        Worker(appContext, workerParams) {
    override fun doWork(): Result {

        // Do the work here--in this case, upload the images.
        //uploadImages()
        Log.d("workLog", "Work done")

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}
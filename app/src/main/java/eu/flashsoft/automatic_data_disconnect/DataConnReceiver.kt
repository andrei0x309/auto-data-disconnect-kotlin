package eu.flashsoft.automatic_data_disconnect

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import java.util.concurrent.TimeUnit


class DataConnReceiver : BroadcastReceiver() {

    var networkPendingIntent: PendingIntent? = null

    private fun getNetworkIntent(context: Context): PendingIntent {
        if (networkPendingIntent != null) {
            return networkPendingIntent!!
        }

        val intent = Intent(context, DataConnReceiver::class.java)
        networkPendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        return networkPendingIntent!!
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getNetworkRequest(): NetworkRequest {
        return NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun registerNetworkUpdates(context: Context) {
        var cm =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        cm?.registerNetworkCallback(getNetworkRequest(), getNetworkIntent(context))
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent) {

        Log.d("----test", "br invoked network")

        val sharedPrefs = context.getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        val discTime = sharedPrefs.getInt("disconnectTimerMin", 20)

        val uploadWorkRequest: WorkRequest =
                OneTimeWorkRequestBuilder<DisconnectWorker>()
                        .setInitialDelay(discTime.toLong(), TimeUnit.MINUTES)
                        .addTag("DisconnectWorker")
                        .setConstraints(Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.METERED)
                                .build())
                        // Additional configuration
                        .build()
        WorkManager.getInstance(context)
                .enqueue(uploadWorkRequest)

        registerNetworkUpdates(context)


        /*val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val aN = cm.isActiveNetworkMetered

        val sharedPrefs = context.getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)


        //val status = if (aN) "11111" else "22222"
        if(aN){
           val ed = sharedPrefs.edit()
            ed.putBoolean("disconnectPending", true)
            val minSettings = sharedPrefs.getInt("disconnectTimerMin", 20)
            val offTime = System.currentTimeMillis() + (minSettings *60000)
            ed.putLong("disconnectStamp", offTime)
            ed.commit()

        }
        */
        //Log.d("myTag", status);
        //Toast.makeText(context, status, Toast.LENGTH_LONG).show()
    }
}
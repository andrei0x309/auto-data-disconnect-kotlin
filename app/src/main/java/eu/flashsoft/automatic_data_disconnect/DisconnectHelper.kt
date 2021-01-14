package eu.flashsoft.automatic_data_disconnect

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class DisconnectHelper {



companion object{

    fun isMobileOnAllNetworks(context: Context): Boolean {
        var result = false;
        try
        {
            result = if ( Build.VERSION.SDK_INT  >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                //Settings comes from the namespace Android.Provider
                Settings.Global.getInt(context.contentResolver, "mobile_data", 1) == 1;
            } else {
                Settings.Secure.getInt(context.contentResolver, "mobile_data", 1) == 1;
            }
        }
        catch (ex: Exception)
        {
            ex.printStackTrace()
        }
        return result;
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun registerPIntent(context: Context){
        val nR = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()

        val intent = Intent(context, DataConnReceiver::class.java)
        val networkPendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerNetworkCallback(nR, networkPendingIntent)

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun unregisterPIntent(context: Context){

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val intent = Intent(context, DataConnReceiver::class.java)
        val networkPendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            cm?.releaseNetworkRequest(networkPendingIntent)
    }


    fun registerDisconnectWorker(context: Context) {
        val uploadWorkRequest = OneTimeWorkRequestBuilder<DisconnectWorker>()
                .setInitialDelay(23, TimeUnit.SECONDS)
                .addTag("DisconnectWorker")
                // Additional configuration
                .build()
        WorkManager.getInstance(context)
                .enqueue(uploadWorkRequest)
    }

}



}
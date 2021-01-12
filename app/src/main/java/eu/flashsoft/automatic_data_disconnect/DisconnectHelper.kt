package eu.flashsoft.automatic_data_disconnect

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class DisconnectHelper {



companion object{

    const val CONNECTION_TYPE_MOBILE = 1

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun isMobileOnAllNetworks(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm?.run {
                for (network in allNetworks){
                    getNetworkCapabilities(network)?.run {
                        val hasMobile = hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        if (hasMobile)
                            return hasMobile
                    }
                }
            }
            }else{
            cm?.run {
                for (netInfo in allNetworkInfo){
                    netInfo.run {
                        if (type == ConnectivityManager.TYPE_MOBILE) {
                            return true
                        }
                    }
                }
            }
        }
        return  false
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Suppress("DEPRECATION")
    fun getConnectionType(context: Context): Int {
        var result = 0 // Returns connection type. 0: none; 1: mobile data; 2: wifi
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm?.run {
                cm.getNetworkCapabilities(cm.activeNetwork)?.run {
                    if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        result = 2
                    } else if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        result = 1
                    } else if (hasTransport(NetworkCapabilities.TRANSPORT_VPN)){
                        result = 3
                    }
                }
            }
        } else {
            cm?.run {
                cm.activeNetworkInfo?.run {
                    if (type == ConnectivityManager.TYPE_WIFI) {
                        result = 2
                    } else if (type == ConnectivityManager.TYPE_MOBILE) {
                        result = 1
                    } else if(type == ConnectivityManager.TYPE_VPN) {
                        result = 3
                    }
                }
            }
        }
        return result
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun registerPIntent(context: Context){
        val nR = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()

        val intent = Intent(context, DataConnReceiver::class.java)
        val networkPendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerNetworkCallback(nR, networkPendingIntent)

        Log.d("---- pending", "register pending intent")
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun unregisterPIntent(context: Context){

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val intent = Intent(context, DataConnReceiver::class.java)
        val networkPendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

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
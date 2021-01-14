package eu.flashsoft.automatic_data_disconnect


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import android.os.Build
import androidx.annotation.RequiresApi

class DataConnReceiver : BroadcastReceiver() {


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onReceive(context: Context, intent: Intent) {

        if(DisconnectHelper.isMobileOnAllNetworks(context)) {
            DisconnectHelper.registerDisconnectWorker(context)
        }

    }
}
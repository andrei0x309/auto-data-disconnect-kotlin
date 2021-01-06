package eu.flashsoft.automatic_data_disconnect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootStartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Log.d("----test", "br invoked")
        DisconnectStickyService.selfStart(context)
    }
}
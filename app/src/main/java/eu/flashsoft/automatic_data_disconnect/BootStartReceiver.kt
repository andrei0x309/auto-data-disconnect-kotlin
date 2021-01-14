package eu.flashsoft.automatic_data_disconnect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class BootStartReceiver : BroadcastReceiver() {


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent) {

        val sharedPrefs = context.getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        val boolAppEnabled = sharedPrefs.getBoolean("enableApp", false)
        if(boolAppEnabled) MainActivity.startApp(context)

    }
}
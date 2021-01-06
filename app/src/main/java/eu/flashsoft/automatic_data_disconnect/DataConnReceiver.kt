package eu.flashsoft.automatic_data_disconnect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import android.net.ConnectivityManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity



class DataConnReceiver : BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

        //Log.d("myTag", status);
        //Toast.makeText(context, status, Toast.LENGTH_LONG).show()
    }
}
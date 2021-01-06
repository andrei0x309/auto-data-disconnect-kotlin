package eu.flashsoft.automatic_data_disconnect
import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import java.io.DataOutputStream

class DisconnectServiceAsyncTask : AsyncTask<Int, Int, Unit>() {

    lateinit var context:Context


    private fun enableMobileData() {
        try {
            val cmds = arrayOf("svc data enable")
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


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


    override fun doInBackground(vararg params: Int?): Unit {

        val sharedPrefs = context.getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)

        while (true){

            val curTime = System.currentTimeMillis()
            val disTime = sharedPrefs.getLong("disconnectStamp", curTime)
            val disPending = sharedPrefs.getBoolean("disconnectPending", false)
            if(disPending && (curTime  >= disTime) ){
                val ed = sharedPrefs.edit()
                ed.putBoolean("disconnectPending", false)
                ed.commit()
                disableMobileData()
            }
            Thread.sleep(1000)
        }


    }


}

package eu.flashsoft.automatic_data_disconnect

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager

// TODO: Rename actions, choose action names that describe tasks that this
// IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
private const val ACTION_FOO = "eu.flashsoft.automatic_data_disconnect.action.FOO"
private const val ACTION_BAZ = "eu.flashsoft.automatic_data_disconnect.action.BAZ"

// TODO: Rename parameters
private const val EXTRA_PARAM1 = "eu.flashsoft.automatic_data_disconnect.extra.PARAM1"
private const val EXTRA_PARAM2 = "eu.flashsoft.automatic_data_disconnect.extra.PARAM2"

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.

 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.

 */
class DisconnectService : IntentService("DisconnectService") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_FOO -> {
                val param1 = intent.getStringExtra(EXTRA_PARAM1)
                val param2 = intent.getStringExtra(EXTRA_PARAM2)
                handleActionFoo(param1, param2)
            }
            ACTION_BAZ -> {
                val param1 = intent.getStringExtra(EXTRA_PARAM1)
                val param2 = intent.getStringExtra(EXTRA_PARAM2)
                handleActionBaz(param1, param2)
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionFoo(param1: String, param2: String) {

        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(DataConnReceiver(), intentFilter)

        while (true){
            Thread.sleep(100)
        }

    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionBaz(param1: String, param2: String) {
        TODO("Handle action Baz")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val reLaunchMain = Intent(this, MainActivity::class.java)
        reLaunchMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        reLaunchMain.addFlags(Intent.FLAG_FROM_BACKGROUND);
        reLaunchMain.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        reLaunchMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        reLaunchMain.putExtra("MOVE_BACK_BOOL", true)
        startActivity(reLaunchMain)
        //Code here

    }


    companion object {
        /**
         * Starts this service to perform action Foo with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        // TODO: Customize helper method
        @JvmStatic
        fun startActionFoo(context: Context, param1: String, param2: String) {
            val intent = Intent(context, DisconnectService::class.java).apply {
                action = ACTION_FOO
                putExtra(EXTRA_PARAM1, param1)
                putExtra(EXTRA_PARAM2, param2)
            }
            context.startService(intent)
        }

        /**
         * Starts this service to perform action Baz with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        // TODO: Customize helper method
        @JvmStatic
        fun startActionBaz(context: Context, param1: String, param2: String) {
            val intent = Intent(context, DisconnectService::class.java).apply {
                action = ACTION_BAZ
                putExtra(EXTRA_PARAM1, param1)
                putExtra(EXTRA_PARAM2, param2)
            }
            context.startService(intent)
        }
    }
}
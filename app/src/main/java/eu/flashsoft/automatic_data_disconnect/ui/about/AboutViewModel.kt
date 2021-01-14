package eu.flashsoft.automatic_data_disconnect.ui.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AboutViewModel : ViewModel() {


    private val _htmlData = MutableLiveData<String>().apply {
        value =  """
            <style> .answer{
margin-left:10px;
} 
.body { width:97%;
        margin-left:auto;
        margin-right:auto;
        padding-bottom:80px;
}
  ul {
    padding: 15px;
  }
</style>
            <div class="body">
            <h2>FAQ</h2>
            <ul>
            <li><p><b>Q:</b> Why the APP needs root?</p>
              <p class="answer"><b>A:</b> On newer <i>Android</i> systems data connection can't activated or deactivated without root privileges</p>
            </li>

            <li><p><b>Q:</b> What time interval can be set to disconnect Data connection?</p>
              <p class="answer"><b>A:</b> Between 1 minute and 600 minutes</p>
            </li>
              <li><p><b>Q:</b> Is the APP battery efficient?</p>
              <p class="answer"><b>A:</b> On newer Android systems ( after Android 7 - Nougat ) the system doesn't send events to wake APPs when the phone isn't connected to the internet, you can only register an event to be triggered when certain type of network is available, so in order to detect when the phone has the data disconnected this APP uses <i>WorkManager()</i> and <i>NetworkRequest().</i> In other words it is as efficient as the newer systems allow, that being said it should not consume more then 1-3% battery depending on the device and on the frequency you use data connection. It will consume 0% when you don't use data connection.
                </p>
            </li>
                          <li><p><b>Q:</b> Does the APP collect any data?</p>
              <p class="answer"><b>A:</b> No.</p>
            </li>
                                      <li><p><b>Q:</b> What does the enable logs mean?</p>
              <p class="answer"><b>A:</b> It will store an entry with the date and time for every successful or failed disconnected attempt. Failed disconnecting from data occurs only when the APP doesn't have root privileges.</p>
            </li>              

              <li><p><b>Q:</b> Will the disconnect time be precise ?</p>
              <p class="answer"><b>A:</b> Relatively, it will be off by a couple of seconds in order to optimize for performance.</p>
            </li>   
            <li><p><b>Q:</b> Is the APP open-source?</p>
              <p class="answer"><b>A:</b> Yes I keep a public git repo on my GitLab accessible at: <a href="https://gitlab.flashsoft.eu/andrei0x309/automatic-data-disconnect">https://gitlab.flashsoft.eu/andrei0x309/automatic-data-disconnect</a> </p>
            </li>  
            <li><p><b>Q:</b> Does the app use services?</p>
                          <p class="answer"><b>A:</b> No. </p>
                        </li>  
            <li><p><b>Q:</b> Does the app works after I reboot my device?</p>
                          <p class="answer"><b>A:</b> Yes. </p>
             </li>
             <li><p><b>Q:</b> On which device this app was tested?</p>
                          <p class="answer"><b>A:</b> Mainly on AVD emulator(Pixel_XL_API_29, Pixel_API_21) and Phone with Android 7.</p>
             </li>
            
            </ul>
            </div>
            
            
        """.trimIndent()
    }


    val htmlData: LiveData<String> = _htmlData
}



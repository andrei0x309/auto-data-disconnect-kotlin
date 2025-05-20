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
.body { 
        width:97%;
        margin-left:auto;
        margin-right:auto;
        padding-bottom:80px;
          overflow-wrap: break-word;
          word-wrap: break-word;
        
          -ms-word-break: break-all;
          /* This is the dangerous one in WebKit, as it breaks things wherever */
          word-break: break-all;
          /* Instead use this non-standard one: */
          word-break: break-word;
        
          /* Adds a hyphen where the word breaks, if supported (No Blink) */
          -ms-hyphens: auto;
          -moz-hyphens: auto;
          -webkit-hyphens: auto;
          hyphens: auto;
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

<li><p><b>Q:</b> What time interval can be set to disconnect data or both wifi and data networks?</p>
  <p class="answer"><b>A:</b> Between 1 minute and 600 minutes</p>
</li>

<li><p><b>Q:</b> What could be some uses cases?</p>
  <p class="answer"><b>A:</b>
Privacy (only allow data connection enabled for a few minutes when you need, and then the phone will always disconnect from networks after that time. If you have a VPN on home Wifi you might want to leave the Wi-Fi network on)
Conserve Battery If you don't use your Phone very often there's no reason to have any network-enabled if you don't care about notifications 
  </p>
</li>
 
<li><p><b>Q:</b>How the app works?</p>
  <p class="answer"><b>A:</b> The app has been rewritten a few times to accommodate new Android requirements. At the moment it uses a combination of: Root commands, a service, and two workers, it should be battery-efficient since it mainly relies on network callbacks.
On older Androids the service will create a service notification that will display when the Network will disconnect. On newer Androids >= 13 it will omit that notification because services can keep running even without that notification, so it's not needed anymore.
    </p>
</li>
<li><p><b>Q:</b> Does the APP collect any data?</p>
  <p class="answer"><b>A:</b> No.</p>
</li>
<li><p><b>Q:</b> What does the enable logs mean?</p>
  <p class="answer"><b>A:</b> It will store an entry with the date and time for every successful or failed disconnected attempt. Failed disconnecting from data occurs only when the APP doesn't have root privileges.
The data is stored in a local plain text file which is linked to the app, ( will be removed on uninstall).
</p>
</li>              

  <li><p><b>Q:</b> Will the disconnect time be precise ?</p>
  <p class="answer"><b>A:</b> Relatively, it will be off by a couple of seconds in order to optimize for performance.</p>
</li>   
<li><p><b>Q:</b> Is the APP open-source?</p>
  <p class="answer"><b>A:</b> Yes I keep a public git repo on my GitLab accessible at: <a href="https://github.com/andrei0x309/auto-data-disconnect-kotlin">https://github.com/andrei0x309/auto-data-disconnect-kotlin</a> </p>
</li>  
<li><p><b>Q:</b> Does the app works after I reboot my device?</p>
              <p class="answer"><b>A:</b> Yes. </p>
 </li>
 <li><p><b>Q:</b> On which device this app was tested?</p>
              <p class="answer"><b>A:</b> Mainly on AVD emulator(API 26 + API 35).</p>
 </li>
</ul>
</div>
            
            
        """.trimIndent()
    }


    val htmlData: LiveData<String> = _htmlData
}



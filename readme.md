# [Root] Auto Data Disconnect
This open-source app was first written around 2018.

The application does not allow Data Connection to stay active for more than a fixed number of minutes ( 1 to 600 ) that the user has set.

It has been rewritten a few times to accommodate many Android restrictions that have been added to newer Android systems.

A rooted device is necessary to shut down your data connection.

It also requires a service that monitors the state of your data connection, manages timers, and issues disconnects if the data connection state changes the timer will be reset, for example, if I set my timer to 4 min and then I turn off my data connection when the connection is available again the 4 minutes timer will restart ensuring that data can only be connected for 4  minutes.

Service stays on very aggressively.
If you run out of resources and the service gets somehow shut down, there's a second monitoring worker restarting the service.

It also starts on device boot. You can disable the service from the application if there's a time when you don't need it.

The service uses system-level network callbacks optimized to be lightweight.

UI is very basic, whereas the logic of the app needs to jump through many hoops to make this functionality available on newer systems; the last SDK compile version was 35.

Play Link: https://play.google.com/store/apps/details?id=eu.flashsoft.automatic_data_disconnect

## Use-cases

- Privacy (only allow data connection enabled for a few minutes when you need, and then the phone will always disconnect from networks after that time. If you have a VPN on your home Wifi, you might want to leave the Wi-Fi network on.
- Conserve Battery. If you don't use your Phone very often, there's no reason to have any network-enabled features if you don't care about notifications

## Demo

https://github.com/user-attachments/assets/a7d3a089-f39f-4396-97ea-c591b57324ce

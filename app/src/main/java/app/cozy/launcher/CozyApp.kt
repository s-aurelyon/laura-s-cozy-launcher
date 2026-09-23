package app.cozy.launcher

import android.app.Application
import app.cozy.launcher.data.Store
import app.cozy.launcher.system.Alarms

class CozyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        Alarms.createChannels(this)
    }
}

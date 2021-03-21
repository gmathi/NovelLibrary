package io.github.gmathi.novellibrary

import android.app.Application
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.DataCenter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uy.kohesive.injekt.api.*

class AppModule(val app: Application) : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingleton(app)
        addSingletonFactory { DBHelper.getInstance(app) }
        addSingletonFactory { DataCenter(app) }
        addSingletonFactory { NetworkHelper(app) }
        addSingletonFactory { SourceManager(app) }//.also { get<ExtensionManager>().init(it) }

        // Asynchronously init expensive components for a faster cold start
        GlobalScope.launch { get<DBHelper>() }
        GlobalScope.launch { get<DataCenter>() }
        GlobalScope.launch { get<NetworkHelper>() }
        GlobalScope.launch { get<SourceManager>() }

    }
}
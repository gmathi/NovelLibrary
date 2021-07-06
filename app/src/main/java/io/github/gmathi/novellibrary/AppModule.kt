package io.github.gmathi.novellibrary

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.DataCenter
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.api.*

class AppModule(val app: Application) : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingleton(app)
        addSingletonFactory { DBHelper.getInstance(app) }
        addSingletonFactory { DataCenter(app) }
        addSingletonFactory { NetworkHelper(app) }
//        addSingletonFactory { JsoupNetworkHelper(app) }
        addSingletonFactory { SourceManager(app).also { get<ExtensionManager>().init(it) } }
        addSingletonFactory { ExtensionManager(app) }

        addSingletonFactory { Gson() }
        addSingletonFactory { Json { ignoreUnknownKeys = true } }

        // Asynchronously init expensive components for a faster cold start
        Handler(Looper.getMainLooper()).post {
            get<DBHelper>()
            get<DataCenter>()
            get<NetworkHelper>()
//            get<JsoupNetworkHelper>()
            get<SourceManager>()
        }
    }
}
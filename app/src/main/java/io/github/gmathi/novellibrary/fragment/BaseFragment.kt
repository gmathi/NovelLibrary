package io.github.gmathi.novellibrary.fragment

import androidx.fragment.app.Fragment
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.DataCenter
import uy.kohesive.injekt.injectLazy


open class BaseFragment : Fragment() {
    val dataCenter: DataCenter by injectLazy()
    val dbHelper: DBHelper by injectLazy()
    val sourceManager: SourceManager by injectLazy()
    val networkHelper: NetworkHelper by injectLazy()
}

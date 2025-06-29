package io.github.gmathi.novellibrary.fragment

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.databinding.ContentRecyclerViewBinding
import io.github.gmathi.novellibrary.databinding.ListitemExtensionCardBinding
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.extension.model.Extension
import io.github.gmathi.novellibrary.extension.model.ExtensionGroupItem
import io.github.gmathi.novellibrary.extension.model.ExtensionItem
import io.github.gmathi.novellibrary.extension.model.InstallStep
import io.github.gmathi.novellibrary.extension.util.getApplicationIcon
import io.github.gmathi.novellibrary.extensions.showEmpty
import io.github.gmathi.novellibrary.extensions.showLoading
import io.github.gmathi.novellibrary.util.ImageLoaderHelper
import io.github.gmathi.novellibrary.util.system.LocaleHelper
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.view.setDefaults
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

private typealias ExtensionTuple =
        Triple<List<Extension.Installed>, List<Extension.Untrusted>, List<Extension.Available>>

class ExtensionsFragment : BaseFragment(), GenericAdapter.Listener<ExtensionItem> {

    companion object {
        fun newInstance() = ExtensionsFragment()
    }

    private val extensionManager: ExtensionManager by injectLazy()

    private lateinit var binding: ContentRecyclerViewBinding
    private lateinit var adapter: GenericAdapter<ExtensionItem>

    private var extensions = emptyList<ExtensionItem>()
    private var currentDownloads = hashMapOf<String, InstallStep>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.content_recycler_view, container, false)
        binding = ContentRecyclerViewBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRecyclerView()
        extensionManager.findAvailableExtensions()
        bindToExtensionsObservable()
        binding.progressLayout.showLoading()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(), layoutResId = R.layout.listitem_extension_card, listener = this)
        binding.run {
            recyclerView.setDefaults(adapter)
            recyclerView.addItemDecoration(CustomDividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            swipeRefreshLayout.setOnRefreshListener {
                extensionManager.findAvailableExtensions()
                bindToExtensionsObservable()
            }
        }
    }

    override fun bind(item: ExtensionItem, itemView: View, position: Int) {
        val binding = ListitemExtensionCardBinding.bind(itemView)
        val extension = item.extension

        //Image
        val iconUrl = (extension as? Extension.Available)?.iconUrl
        ImageLoaderHelper.loadCircleImage(requireContext(), binding.image, iconUrl)

        //Text
        binding.run {
            extTitle.text = extension.name
            version.text = extension.versionName
            lang.text = LocaleHelper.getSourceDisplayName(extension.lang, itemView.context)
            warning.text = when {
                extension is Extension.Untrusted -> itemView.context.getString(R.string.ext_untrusted)
                extension is Extension.Installed && extension.isObsolete -> "${itemView.context.getString(R.string.ext_installed)}: ${itemView.context.getString(R.string.ext_obsolete)}"
                extension is Extension.Installed && extension.isUnofficial -> "${itemView.context.getString(R.string.ext_installed)}: ${itemView.context.getString(R.string.ext_unofficial)}"
                extension.isNsfw && dataCenter.showNSFWSource -> itemView.context.getString(R.string.ext_nsfw_short)
                extension.isNsfw && dataCenter.showNSFWSource && extension is Extension.Installed -> "${itemView.context.getString(R.string.ext_installed)}: ${itemView.context.getString(R.string.ext_nsfw_short)}"
                else -> ""
            }.toUpperCase(Locale.getDefault())
        }

        //Button
        bindButton(item, binding)
        binding.extButton.setOnClickListener {
            when (extension) {
                is Extension.Available -> extensionManager.installExtension(extension).subscribeToInstallUpdate(extension)
                is Extension.Untrusted -> {
                    extensionManager.trustSignature(extension.signatureHash)// Do Nothing //openTrustDialog(extension)
                }
                is Extension.Installed -> {
                    //Do Nothing
                    if (!extension.hasUpdate) {
                        extensionManager.uninstallExtension(extension.pkgName)
                    } else {
                        extensionManager.updateExtension(extension).subscribeToInstallUpdate(extension)
                    }
                }
            }
        }

        itemView.setBackgroundColor(
            if (position % 2 == 0) ContextCompat.getColor(requireContext(), R.color.black_transparent)
            else ContextCompat.getColor(requireContext(), android.R.color.transparent)
        )
    }

    @Suppress("ResourceType")
    fun bindButton(item: ExtensionItem, binding: ListitemExtensionCardBinding) = with(binding.extButton) {
        isEnabled = true
        isClickable = true

        val extension = item.extension

        val installStep = item.installStep
        if (installStep != null) {
            setText(
                when (installStep) {
                    InstallStep.Pending -> R.string.ext_pending
                    InstallStep.Downloading -> R.string.ext_downloading
                    InstallStep.Installing -> R.string.ext_installing
                    InstallStep.Installed -> R.string.ext_uninstall
                    InstallStep.Error -> R.string.try_again
                }
            )
            if (installStep != InstallStep.Error) {
                isEnabled = false
                isClickable = false
            }
        } else if (extension is Extension.Installed) {
            when {
                extension.hasUpdate -> {
                    setText(R.string.ext_update)
                }
                else -> {
                    setText(R.string.ext_uninstall)
                }
            }
        } else if (extension is Extension.Untrusted) {
            setText(R.string.ext_trust)
        } else {
            setText(R.string.ext_install)
        }
    }

    override fun onItemClick(item: ExtensionItem, position: Int) {
        //Do Nothing
    }

    // Processing
    private fun bindToExtensionsObservable(): Subscription {
        val installedObservable = extensionManager.getInstalledExtensionsObservable()
        val untrustedObservable = extensionManager.getUntrustedExtensionsObservable()
        val availableObservable = extensionManager.getAvailableExtensionsObservable()
            .startWith(emptyList<Extension.Available>())

        return Observable.combineLatest(installedObservable, untrustedObservable, availableObservable) { installed, untrusted, available -> Triple(installed, untrusted, available) }
            .debounce(100, TimeUnit.MILLISECONDS)
            .map(::toItems)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                extensions = it
                if (extensions.isEmpty()) {
                    binding.progressLayout.showEmpty(emptyText = getString(R.string.empty_extensions))
                } else {
                    binding.progressLayout.showContent()
                    adapter.updateData(ArrayList(extensions))
                }
                binding.swipeRefreshLayout.isRefreshing = false
            }
    }

    @Synchronized
    private fun toItems(tuple: ExtensionTuple): List<ExtensionItem> {
        val context = Injekt.get<Application>()
//        val activeLangs = preferences.enabledLanguages().get()
        val showNsfwExtensions = dataCenter.showNSFWSource

        val (installed, untrusted, available) = tuple

        val items = mutableListOf<ExtensionItem>()

        val updatesSorted = installed.filter { it.hasUpdate && (showNsfwExtensions || !it.isNsfw) }.sortedBy { it.pkgName }
        val installedSorted = installed.filter { !it.hasUpdate && (showNsfwExtensions || !it.isNsfw) }.sortedWith(compareBy({ !it.isObsolete }, { it.pkgName }))
        val untrustedSorted = untrusted.sortedBy { it.pkgName }
        val availableSorted = available
            // Filter out already installed extensions and disabled languages
            .filter { avail ->
                installed.none { it.pkgName == avail.pkgName } &&
                        untrusted.none { it.pkgName == avail.pkgName } &&
//                        (avail.lang in activeLangs || avail.lang == "all") &&
                        (showNsfwExtensions || !avail.isNsfw)
            }
            .sortedBy { it.pkgName }

        if (updatesSorted.isNotEmpty()) {
            val header = ExtensionGroupItem(context.getString(R.string.ext_updates_pending), updatesSorted.size, true)
            items += updatesSorted.map { extension ->
                ExtensionItem(extension, header, currentDownloads[extension.pkgName])
            }
        }
        if (installedSorted.isNotEmpty() || untrustedSorted.isNotEmpty()) {
            val header = ExtensionGroupItem(context.getString(R.string.ext_installed), installedSorted.size + untrustedSorted.size)
            items += installedSorted.map { extension ->
                ExtensionItem(extension, header, currentDownloads[extension.pkgName])
            }
            items += untrustedSorted.map { extension ->
                ExtensionItem(extension, header)
            }
        }
        if (availableSorted.isNotEmpty()) {
            val availableGroupedByLang = availableSorted
                .groupBy { LocaleHelper.getSourceDisplayName(it.lang, context) }
                .toSortedMap()

            availableGroupedByLang
                .forEach {
                    val header = ExtensionGroupItem(it.key, it.value.size)
                    items += it.value.map { extension ->
                        ExtensionItem(extension, header, currentDownloads[extension.pkgName])
                    }
                }
        }

        this.extensions = items
        return items
    }

    private fun Observable<InstallStep>.subscribeToInstallUpdate(extension: Extension) {
        this.doOnNext { currentDownloads[extension.pkgName] = it }
            .doOnUnsubscribe { currentDownloads.remove(extension.pkgName) }
            .map { state -> updateInstallStep(extension, state) }
            .subscribe {
                it?.let { adapter.updateItem(it) }
            }
    }

    @Synchronized
    private fun updateInstallStep(extension: Extension, state: InstallStep): ExtensionItem? {
        val extensions = extensions.toMutableList()
        val position = extensions.indexOfFirst { it.extension.pkgName == extension.pkgName }

        return if (position != -1) {
            val item = extensions[position].copy(installStep = state)
            extensions[position] = item

            this.extensions = extensions
            item
        } else {
            null
        }
    }

}
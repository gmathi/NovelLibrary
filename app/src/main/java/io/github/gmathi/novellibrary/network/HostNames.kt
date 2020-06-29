package io.github.gmathi.novellibrary.network


import java.util.*

object HostNames {

    const val NOVEL_UPDATES = "novelupdates.com"
    const val ROYAL_ROAD_OLD = "royalroadl.com"
    const val ROYAL_ROAD = "royalroad.com"
    const val KOBATOCHAN = "kobatochan.com"
    const val GRAVITY_TALES = "gravitytales.com"
    const val WUXIA_WORLD = "wuxiaworld.com"
    const val WORD_PRESS = "wordpress.com"
    const val WLN_UPDATES = "wlnupdates.com"
    const val CIRCUS_TRANSLATIONS = "circustranslations.com"
    const val QIDIAN = "webnovel.com"
    const val PRINCE_REVOLUTION = "princerevolution.org"
    const val MOON_BUNNY_CAFE = "moonbunnycafe.com"
    const val LIGHT_NOVEL_TRANSLATIONS = "lightnovelstranslations.com"
    const val BLUE_SILVER_TRANSLATIONS = "bluesilvertranslations.wordpress.com"
    const val GOOGLE_DOCS = "docs.google.com"
    const val TUMBLR = "tumblr.com"
    const val LIBER_SPARK = "liberspark.com"
    const val SKY_WOOD_TRANSLATIONS = "skythewood.blogspot.com"
    const val VOLARE_NOVELS = "volarenovels.com"
    const val SOUSETSUKA = "sousetsuka.com"
    const val FANTASY_BOOKS = "fantasy-books.live"
    const val BAKA_TSUKI = "baka-tsuki.org"
    const val NOVEL_FULL = "novelfull.com"
    const val SCRIBBLE_HUB = "scribblehub.com"
    const val FLYING_LINES = "flying-lines.com"
    const val LNMTL = "lnmtl.com"
    const val WATTPAD = "wattpad.com"

    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 6 Build/LYZ28E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Mobile Safari/537.36"

    private val DEFAULT_ALLOWED_HOST_NAMES_ARRAY = arrayOf(NOVEL_UPDATES, ROYAL_ROAD, KOBATOCHAN, GRAVITY_TALES, WUXIA_WORLD, WORD_PRESS, WLN_UPDATES, CIRCUS_TRANSLATIONS, QIDIAN, LNMTL, WATTPAD,
        //PRINCE_REVOLUTION,
        "patreon.com", "royalroadlupload.blob.core.windows.net", "postimg.org", "lightnovelbastion.com", "fonts.googleapis.com", "ggpht.com", "gravatar.com", "imgur.com", "isohungrytls.com", "bootstrapcdn.com", "CloudFlare.com", "wp.com", "scatterdrift.com",
        "discordapp.com", "chubbycheeksthoughts.com", "omatranslations.com", "www.googleapis.com", "*.googleusercontent.com", "cdn.novelupdates.com", "*.novelupdates.com", "www.novelupdates.com", "www.wuxiaworld.com", "reports.crashlytics.com", "api.crashlytics.com")

    val defaultHostNamesList: ArrayList<String>
        get() = ArrayList(listOf(*DEFAULT_ALLOWED_HOST_NAMES_ARRAY))

    var hostNamesList = ArrayList<String>()
        set(hostNames) {
            hostNamesList.addAll(hostNames)
        }

    fun isVerifiedHost(hostName: String): Boolean {
        return try {
            hostNamesList.isNotEmpty() && hostNamesList.any { it.contains(hostName) }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun addHost(hostName: String): Boolean = hostNamesList.add(hostName)
}

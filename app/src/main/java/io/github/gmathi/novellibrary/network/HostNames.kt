package io.github.gmathi.novellibrary.network


import java.util.*

object HostNames {

    val NOVEL_UPDATES = "novelupdates.com"
    val ROYAL_ROAD = "royalroadl.com"
    val KOBATOCHAN = "kobatochan.com"
    val GRAVITY_TALES = "gravitytales.com"
    val WUXIA_WORLD = "wuxiaworld.com"
    val WORD_PRESS = "wordpress.com"
    val WLN_UPDATES = "wlnupdates.com"
    val CIRCUS_TRANSLATIONS = "circustranslations.com"
    val QIDIAN = "webnovel.com"
    val PRINCE_REVOLUTION = "princerevolution.org"
    val MOON_BUNNY_CAFE = "moonbunnycafe.com"
    val LIGHT_NOVEL_TRANSLATIONS = "lightnovelstranslations.com"
    val BLUE_SILVER_TRANSLATIONS = "bluesilvertranslations.wordpress.com"
    val GOOGLE_DOCS = "docs.google.com"
    val TUMBLR = "tumblr.com"
    val LIBER_SPARK = "liberspark.com"
    val SKY_WOOD_TRANSLATIONS = "skythewood.blogspot.com"
    val VOLARE_NOVELS = "volarenovels.com"
    val SOUSETSUKA = "sousetsuka.com"
    val FANTASY_BOOKS = "fantasy-books.live"
    val BAKA_TSUKI = "baka-tsuki.org"


    val USER_AGENT = "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 6 Build/LYZ28E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Mobile Safari/537.36"

    private val DEFAULT_ALLOWED_HOST_NAMES_ARRAY = arrayOf(NOVEL_UPDATES, ROYAL_ROAD, KOBATOCHAN, GRAVITY_TALES, WUXIA_WORLD, WORD_PRESS, WLN_UPDATES, CIRCUS_TRANSLATIONS, QIDIAN,
        //PRINCE_REVOLUTION,
        "patreon.com", "royalroadlupload.blob.core.windows.net", "postimg.org", "lightnovelbastion.com", "fonts.googleapis.com", "ggpht.com", "gravatar.com", "imgur.com", "isohungrytls.com", "bootstrapcdn.com", "CloudFlare.com", "wp.com", "scatterdrift.com",
        "discordapp.com", "chubbycheeksthoughts.com", "omatranslations.com", "www.googleapis.com", "*.googleusercontent.com" )

    val defaultHostNamesList: ArrayList<String>
        get() = ArrayList(Arrays.asList(*DEFAULT_ALLOWED_HOST_NAMES_ARRAY))

    var hostNamesList = ArrayList<String>()
        set(hostNames) {
            hostNamesList.addAll(hostNames)
        }

    fun isVerifiedHost(hostName: String): Boolean = hostNamesList.any { hostName.contains(it) }

    fun addHost(hostName: String): Boolean = hostNamesList.add(hostName)

    private val DONT_DOWNLOAD_HOSTS = arrayOf("facebook", "google", "twitter", "live.com", "hotmail.com", "gmail.com")

    fun isItDoNotDownloadHost(hostName: String): Boolean = DONT_DOWNLOAD_HOSTS.any { hostName.contains(it) }

}

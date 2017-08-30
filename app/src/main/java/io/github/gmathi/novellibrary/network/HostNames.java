package io.github.gmathi.novellibrary.network;


import java.util.ArrayList;
import java.util.Arrays;

public class HostNames {

    public static final String NOVEL_UPDATES = "novelupdates.com";
    public static final String ROYAL_ROAD = "royalroadl.com";
    public static final String KOBATOCHAN = "kobatochan.com";
    public static final String GRAVITY_TALES = "gravitytales.com";
    public static final String WUXIA_WORLD = "wuxiaworld.com";
    public static final String WORD_PRESS = "wordpress.com";
    public static final String WLN_UPDATES = "wlnupdates.com";
    public static final String CIRCUS_TRANSLATIONS = "circustranslations.com";
    public static final String QIDIAN = "webnovel.com";
    public static final String PRINCE_REVOLUTION = "princerevolution.org";
    public static final String MOON_BUNNY_CAFE = "moonbunnycafe.com";
    public static final String LIGHT_NOVEL_TRANSLATIONS = "lightnovelstranslations.com";
    public static final String BLUE_SILVER_TRANSLATIONS = "bluesilvertranslations.wordpress.com";
    public static final String GOOGLE_DOCS = "docs.google.com";
    public static final String TUMBLR = "tumblr.com";
    public static final String LIBER_SPARK = "liberspark.com";
    public static final String SKY_WOOD_TRANSLATIONS = "skythewood.blogspot.com";
    public static final String VOLARE_NOVELS = "volarenovels.com";
    public static final String SOUSETSUKA = "sousetsuka.com";
    public static final String FANTASY_BOOKS = "fantasy-books.live";
    public static final String BAKA_TSUKI = "baka-tsuki.org";




    @SuppressWarnings("HardcodedFileSeparator")
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 6 Build/LYZ28E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Mobile Safari/537.36";

    private static final String[] DEFAULT_ALLOWED_HOST_NAMES_ARRAY = {
            NOVEL_UPDATES, ROYAL_ROAD, KOBATOCHAN, GRAVITY_TALES, WUXIA_WORLD,
            WORD_PRESS, WLN_UPDATES, CIRCUS_TRANSLATIONS, QIDIAN,
            //PRINCE_REVOLUTION,
            "patreon.com",
            "royalroadlupload.blob.core.windows.net",
            "postimg.org",
            "lightnovelbastion.com",
            "fonts.googleapis.com",
            "ggpht.com",
            "gravatar.com",
            "imgur.com",
            "isohungrytls.com",
            "bootstrapcdn.com",
            "cloudflare.com",
            "wp.com",
            "scatterdrift.com",
            "discordapp.com",
            "chubbycheeksthoughts.com",
            "omatranslations.com"
    };

    public static ArrayList<String> getDefaultHostNamesList() {
        return new ArrayList(Arrays.asList(DEFAULT_ALLOWED_HOST_NAMES_ARRAY));
    }

    private static final ArrayList<String> ALLOWED_HOST_NAMES_ARRAY_LIST = new ArrayList<>();

    public static boolean isVerifiedHost(String hostName) {
        for (String allowedHostname : ALLOWED_HOST_NAMES_ARRAY_LIST)
            if (hostName.contains(allowedHostname)) return true;

        return false;
    }

    public static boolean addHost(String hostName) {
        return ALLOWED_HOST_NAMES_ARRAY_LIST.add(hostName);
    }

    public static ArrayList<String> getHostNamesList() {
        return ALLOWED_HOST_NAMES_ARRAY_LIST;
    }

    public static void setHostNamesList(ArrayList<String> hostNames) {
         ALLOWED_HOST_NAMES_ARRAY_LIST.addAll(hostNames);
    }
}

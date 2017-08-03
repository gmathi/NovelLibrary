package io.github.gmathi.novellibrary.network;


public class HostNames {

    public static final String NOVEL_UPDATES = "novelupdates.com";
    public static final String ROYAL_ROAD = "royalroadl.com";
    public static final String KOBATOCHAN = "kobatochan.com";
    public static final String GRAVITY_TALES = "gravitytales.com";
    public static final String WUXIA_WORLD = "wuxiaworld.com";
    public static final String WORD_PRESS = "wordpress.com";
    public static final String WLN_UPDATES = "wlnupdates.com";
    public static final String CIRCUS_TRANSLATIONS = "circustranslations.com";

    @SuppressWarnings("HardcodedFileSeparator")
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 6 Build/LYZ28E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Mobile Safari/537.36";

    private static final String[] ALLOWED_HOST_NAMES_ARRAY = {
            NOVEL_UPDATES, ROYAL_ROAD, KOBATOCHAN, GRAVITY_TALES, WUXIA_WORLD,
            WORD_PRESS, WLN_UPDATES, CIRCUS_TRANSLATIONS,
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
            "discordapp.com"
    };

    public static boolean isVerifiedHost(String hostName) {
        for (String allowedHostname : ALLOWED_HOST_NAMES_ARRAY)
            if (hostName.contains(allowedHostname)) return true;

        return false;
    }

}

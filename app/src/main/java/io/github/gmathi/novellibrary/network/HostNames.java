package io.github.gmathi.novellibrary.network;


public class HostNames {

    public static final String NOVEL_UPDATES = "novelupdates.com";
    public static final String ROYAL_ROAD = "royalroadl.com";
    public static final String KOBATOCHAN = "kobatochan.com";
    public static final String GRAVITY_TALES = "gravitytales.com";
    public static final String WUXIA_WORLD = "wuxiaworld.com";


    @SuppressWarnings("HardcodedFileSeparator")
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 6 Build/LYZ28E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Mobile Safari/537.36";

    public static final String ALLOWED_HOST_NAMES = " fonts.googleapis.com forum.royalroadl.com yt3.ggpht.com www.gravatar.com i.imgur.com " +
            "cdn.royalroadl.com " +
            "2.gravatar.com " +
            "www.novelupdates.com " +
            "guqintranslations.wordpress.com " +
            "www.royalroadl.com " +
            "www.isohungrytls.com ";

    private static final String[] ALLOWED_HOST_NAMES_ARRAY = {
                    "patreon.com",
                    "royalroadlupload.blob.core.windows.net",
                    "postimg.org",
                    "lightnovelbastion.com",
                    "fonts.googleapis.com",
                    "royalroadl.com",
                    "ggpht.com",
                    "gravatar.com",
                    "imgur.com",
                    "novelupdates.com",
                    "wordpress.com",
                    "isohungrytls.com",
                    "kobatochan.com"
            };

    public static boolean isVerifiedHost(String hostName) {
        for (String allowedHostname : ALLOWED_HOST_NAMES_ARRAY)
            if (hostName.contains(allowedHostname)) return true;

        return false;
    }

}

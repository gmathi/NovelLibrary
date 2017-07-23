package io.github.gmathi.novellibrary.network;


public class HostNames {

    public static final String NOVEL_UPDATES = "novelupdates.com";
    public static final String ROYAL_ROAD = "royalroadl.com";
    public static final String KOBATOCHAN = "kobatochan.com";
    public static final String GRAVITY_TALES = "gravitytales.com";

    @SuppressWarnings("HardcodedFileSeparator")
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76K) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19";

    public static final String ALLOWED_HOST_NAMES = " fonts.googleapis.com forum.royalroadl.com yt3.ggpht.com www.gravatar.com i.imgur.com " +
            "cdn.royalroadl.com " +
            "2.gravatar.com " +
            "www.novelupdates.com " +
            "guqintranslations.wordpress.com " +
            "www.royalroadl.com " +
            "www.isohungrytls.com ";

    private static final String[] ALLOWED_HOST_NAMES_ARRAY = {
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

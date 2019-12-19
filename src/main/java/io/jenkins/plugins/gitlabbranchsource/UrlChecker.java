package io.jenkins.plugins.gitlabbranchsource;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlChecker {

    private static UrlChecker singleton;

    UrlChecker() {

    }

    public static UrlChecker get() {
        if (singleton == null) {
            singleton = new UrlChecker();
        }
        return singleton;
    }

    URL checkURL(String url) {
        try {
            URL anURL = new URL(url);
            if ("localhost".equals(anURL.getHost())) {
                throw new IllegalStateException(
                    "Jenkins URL cannot start with http://localhost \nURL is: " + url);
            }
            if (!anURL.getHost().contains(".")) {
                throw new IllegalStateException(
                    "You must use a fully qualified domain name for Jenkins URL, this is required by GitLab"
                        + "\nURL is: " + url);
            }
            return anURL;
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Bad Jenkins URL\nURL is: " + url);
        }
    }

}

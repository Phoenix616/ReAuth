package reauth;

import cpw.mods.fml.common.versioning.ComparableVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import cpw.mods.fml.common.versioning.VersionParser;
import cpw.mods.fml.common.versioning.VersionRange;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;

class VersionChecker implements Runnable {

    private static final String url = "https://raw.githubusercontent.com/TechnicianLP/ReAuth/master/version";

    private static boolean isLatestVersion = true;
    private static String[] versionInfo = new String[]{"", ""};
    private static boolean isVersionAllowed = true;
    private static long run = 0;

    @Override
    public void run() {
        Main.log.info("Looking for Updates");
        InputStream in;
        try {
            in = new URL(url).openStream();
        } catch (Exception e) {
            Main.log.log(Level.SEVERE, "Looking for Updates - Failed", e);
            return;
        }

        try {
            versionInfo = IOUtils.readLines(in).get(0).split(";");

            ComparableVersion local = new ComparableVersion(Main.meta.version);
            ComparableVersion recommended = new ComparableVersion(versionInfo[0]);
            VersionRange allowed = VersionParser.parseRange(versionInfo[1]);

            isLatestVersion = local.compareTo(recommended) >= 0;
            isVersionAllowed = allowed.containsVersion(new DefaultArtifactVersion(Main.meta.version));
            run = System.currentTimeMillis();

            Main.log.info("Looking for Updates - Finished");
            return;
        } catch (IOException e) {
            Main.log.log(Level.SEVERE, "Looking for Updates - Failed", e);
        } finally {
            IOUtils.closeQuietly(in);
        }

        run = System.currentTimeMillis() - (long) ((1000 * 60 * 60) * 0.75);
    }

    static boolean isLatestVersion() {
        return isLatestVersion;
    }

    static String getLatestVersion() {
        return versionInfo[0];
    }

    static boolean isVersionAllowed() {
        return isVersionAllowed;
    }

    static boolean shouldRun() {
        return System.currentTimeMillis() - run > (1000 * 60 * 60);
    }

    static void update() {
        Thread t = new Thread(new VersionChecker(), "ReAuth-VersionChecker");
        t.setDaemon(true);
        t.start();
    }

}
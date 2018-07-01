package org.webdriver.crawler.helpers;

import org.webdriver.crawler.ApplicationCrawler;
import org.webdriver.crawler.Site;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by ganak on 6/22/2015.
 */
public class ApplicationCrawlerHelper {

    /**
     * Extract from the given url the host name and use it as
     * @param stringUrl
     * @return
     */
    public static String getNameFromURL(String stringUrl) {
        try {
            URL url = new URL(stringUrl);
            return url.getHost();
        } catch (Exception e) {
            System.out.printf("ApplicationCrawlerHelper.getNameFromURL(%s) : Ignoring the exception %s", stringUrl, e.getMessage()).println();
        }
        return stringUrl.replace("http://", "").replace("https://", "").replace("/", "_");
    }

    /**
     * Read file and parse each line to item in list, reads from system property "urls.list", default file name urls4crawling.txt
     * @return
     */
    public static List<Object[]> getListOfSites() {
        String property = System.getProperty("urls.list", "target/classes/client_monitor/urls4crawling.txt");
        List<Object[]> list = new ArrayList<>();
        try {
            File file = new File(System.getProperty("user.dir"), property);
            System.out.println(file.getAbsolutePath());
            List<String> strings = null;
            if (file.isFile()) {
                strings = Files.readAllLines(file.toPath(), Charset.forName("UTF-8"));
            } else {
                strings = Arrays.asList(property.split(";"));
            }
            for (String currentSite : strings) {
                if (currentSite.startsWith("#")) {
                    continue;
                }
                String[] elements = currentSite.split("=", 2);
                if (elements != null && elements.length > 0) {
                    String siteUrl, siteName;

                    if (elements.length == 1) {
                        siteName = getNameFromURL(elements[0]);
                        siteUrl = elements[0];
                    } else {
                        siteName = elements[0];
                        siteUrl = elements[1];
                    }
                    list.add(toArray(new Site(siteName, siteUrl)));
                }
            }
        } catch (Exception e) {
        }
        return list;
    }

    private static Object[] toArray(Object... objects) {
        return objects;
    }

    /**
     * Writes properties safely to the same file
     * @param outputFile
     * @param siteName
     * @param executionTime
     */
    public static void write(String outputFile, String siteName, String executionTime) {
        Properties toBeWrittenProperties = new Properties();
        toBeWrittenProperties.setProperty(siteName, executionTime);
        File lockFile = new File(outputFile + ".lck");
        try {
            while(lockFile.exists()) {
                System.out.println(Thread.currentThread().getName() + ": Lock file " + lockFile.getAbsolutePath() + " exists, waiting..");
                TimeUnit.SECONDS.sleep(2);
            }
            lockFile.createNewFile();
            lockFile.deleteOnExit();
            File file = new File(outputFile);
            if (file.exists()) {
                toBeWrittenProperties.load(new FileInputStream(file));
            }
            toBeWrittenProperties.store(new FileOutputStream(file), ApplicationCrawler.class.getName());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {Files.deleteIfExists(lockFile.toPath());} catch (Exception e) {}
        }
    }

}


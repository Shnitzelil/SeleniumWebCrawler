package org.webdriver.crawler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openqa.selenium.By;
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.webdriver.crawler.executer.SiteExecution;
import org.webdriver.crawler.helpers.ApplicationCrawlerHelper;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.lang.System.currentTimeMillis;

/**
 * Created by ganak on 6/4/2015.
 */
@RunWith(SiteExecution.class)
public class ApplicationCrawler {

    private static final Logger log = Logger.getLogger(ApplicationCrawler.class.getName());
    private static final long FUSE_TIMEOUT = toLong("fuse.timeout.seconds", 250L);
    private static final int FUSE_PATHS = toInt("fuse.paths", 4);
    private static final int FUSE_CLICKS_IN_PATH = toInt("fuse.clicks", 10);

    private static final ThreadLocal<String> threadLocal = new ThreadLocal<>();
    public static final String TARGET_TEST_CLASSES_GENERATED_FLOWS = "target/test-classes/generatedFlows";

    private final DesiredCapabilities desiredCapabilities;
    private final WebCrawlerDriver webCrawlerDriver;
    private final Site site;

    private long startTestTime;

    private static long toLong(String key, long defaultValue) {
        try {
            return Long.parseLong(System.getProperty(key));
        } catch (NumberFormatException nfe) { }
        return defaultValue;
    }

    private static int toInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(System.getProperty(key));
        } catch (NumberFormatException nfe) { }
        return defaultValue;
    }

    public ApplicationCrawler(Site site) {
        this.webCrawlerDriver = new WebCrawlerDriver(this.desiredCapabilities = initCapabilities());
        this.site = site;
    }

    private static DesiredCapabilities initCapabilities() {
        DesiredCapabilities desiredCapabilities = DesiredCapabilities.chrome();
        desiredCapabilities.setPlatform(Platform.WINDOWS);
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("incognito", "test-type", "disable-extensions");
        desiredCapabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
        return desiredCapabilities;
    }

    @Parameterized.Parameters
    public static List<Object[]> sites() {
        List<Object[]> list = ApplicationCrawlerHelper.getListOfSites();
        return list;
    }

    @Before
    public void init() {
        this.startTestTime = currentTimeMillis();
        this.webCrawlerDriver.initialize();
    }

    @Test
    public void crawlingApplication() {
        Set<String> clickedHref = new HashSet<>();
        site.addAction(CrawlerAction.getUrl(site.getUrl())).doIt(webCrawlerDriver).reportSuccessful();
        String domain = webCrawlerDriver.getCurrentDomain();
        Random random = new Random();

        boolean domainWasChangeCounter = false;

        while (true) {
//            Adding the not(contains(@href, 'mailto')) to the xpath to avoid from clicking on "emails addresses" this cause in some machines to open the email application
            List<String> elements = new ArrayList<>();
            int retry = 6;
            while (retry > 0) {
                List<String> temp = webCrawlerDriver.getText(By.xpath("//a[text() and @href and not(contains(@href, 'mailto'))]"));
                if (temp.containsAll(elements) && elements.containsAll(temp)) {
                    break;
                } else {
                    elements = temp;
                    try {retry--; TimeUnit.SECONDS.sleep(5);} catch (Exception e) {return;}
                }
            }
            String currentDomain = webCrawlerDriver.getCurrentDomain();
            if (!domain.equalsIgnoreCase(currentDomain)) {
                if (domainWasChangeCounter) {
                    log.info("This is the second time in a row domain was changed...");
                    break;
                }
                log.info("Domain was changed... need to navigate back");
                domainWasChangeCounter = true;
                this.site.addAction(CrawlerAction.goBack()).doIt(webCrawlerDriver).reportSuccessful();
                continue;
            }
            domainWasChangeCounter = false;
            boolean nothingWasClickedHere = true;
            while (!elements.isEmpty()) {
                int i = random.nextInt(elements.size());
                String text = elements.remove(i);
                By linkLocation =
                        By.xpath(String.format("//a[contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'%s') and @href]", text.toLowerCase()));
                if (!clickedHref.contains(text) &&
                        webCrawlerDriver.isAvailable(linkLocation)) {
                    try {
                        site.addAction(CrawlerAction.clickOn(linkLocation)).doIt(webCrawlerDriver).reportSuccessful();
                        nothingWasClickedHere = false;
                    } catch (Throwable throwable) {
                        log.info("Skipping: Failed to click on " + text);
                        site.removeLastAddAction();
                        continue;
                    }
                    clickedHref.add(text);
                    break;
                }
            }

//            Check run fuse
            if (clickedHref.size() >= FUSE_CLICKS_IN_PATH * FUSE_PATHS) {
                log.info("Reached clicks and paths fuse (" + (FUSE_CLICKS_IN_PATH * FUSE_PATHS) + ")");
                break;
            }

            if (TimeUnit.SECONDS.convert(currentTimeMillis() - startTestTime, TimeUnit.MILLISECONDS) > FUSE_TIMEOUT) {
                log.info("Reached timeout fuse (" + FUSE_TIMEOUT + " seconds)");
                break;
            }

//            End - Check run fuse
            Set<String> windowHandles = webCrawlerDriver.getWrappedDriver().getWindowHandles();
            if (windowHandles.size() > 1) {
                this.site.addAction(CrawlerAction.closeTabs()).doIt(webCrawlerDriver).reportSuccessful();
            }

            if (nothingWasClickedHere) {
                this.site.addAction(CrawlerAction.goBack()).doIt(webCrawlerDriver).reportSuccessful();
            }

            if (clickedHref.size() % FUSE_CLICKS_IN_PATH == 0) {
                site.addAction(CrawlerAction.getUrl(site.getUrl())).doIt(webCrawlerDriver).reportSuccessful();
            }
        }
        this.site.writeStory(TARGET_TEST_CLASSES_GENERATED_FLOWS);
    }

    @After
    public void end() {
        this.webCrawlerDriver.quit();
        this.site.end();
        ApplicationCrawlerHelper.write(TARGET_TEST_CLASSES_GENERATED_FLOWS + "/flowDurations.properties", site.getSiteName(), String.valueOf(currentTimeMillis() - startTestTime));
    }

}



package org.webdriver.crawler;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ganak on 7/7/2015.
 */
public abstract class CrawlerAction {

    private static final Logger logger = Logger.getLogger(CrawlerAction.class.getName());

    private final String step;

    ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("HH:mm:ss.SSS");
        }
    };

    DecimalFormat myFormatter = new DecimalFormat("###,### Milli-Second");
    ThreadLocal<Long> startTimeKeeper = new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            return System.currentTimeMillis();
        }
    };

    protected CrawlerAction(String step) {
        this.step = step;
    }

    public abstract CrawlerAction perform(WebCrawlerDriver webCrawlerDriver);

    public String getStep() {
        return step;
    }

    public CrawlerAction doIt(WebCrawlerDriver webCrawlerDriver) {
        startTimeKeeper.set(System.currentTimeMillis());
        return perform(webCrawlerDriver);
    }

    public void reportSuccessful() {
        logger.log(Level.INFO, formatStep());
    }

    public void reportFailed(Throwable throwable) {
        logger.log(Level.INFO, formatStep(), throwable);
    }

    protected String formatStep() {
        long startTime = startTimeKeeper.get(); startTimeKeeper.remove();
        long stepTime = System.currentTimeMillis() - startTime;
        return dateFormat.get().format(startTime) + "  " + getStep() + " (Duration: " + myFormatter.format(stepTime) + ")";
    }

    public static CrawlerAction clickOn(By by) {
        return new ClickOn(by);
    }

    public static CrawlerAction getUrl(String url) {
        return new GetURL(url);
    }

    public static CrawlerAction goBack() {
        return new GoBack();
    }

    public static CrawlerAction closeTabs() {
        return new CloseTab();
    }

    /**
     * Created by ganak on 7/7/2015.
     */
    public static class ClickOn extends CrawlerAction  {

        private final By by;
    
        public ClickOn(By by) {
            super(String.format("Click on element with locator: '%s'", by.toString().replace("By.xpath: ", "xpath:")));
            this.by = by;
        }
    
        @Override
        public CrawlerAction perform(WebCrawlerDriver webCrawlerDriver) {
            webCrawlerDriver.clickOnElement(by);
            return this;
        }

    }

    /**
     * Created by ganak on 7/7/2015.
     */
    public static class GetURL extends CrawlerAction {
    
        private final String url;
    
        public GetURL(String url) {
            super(String.format("Navigating to url: %s", url));
            this.url = url;
        }
        @Override
        public CrawlerAction perform(WebCrawlerDriver webCrawlerDriver) {
            webCrawlerDriver.get(url);
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (Exception e) {
            }
            return this;
        }

    }

    /**
     * Created by ganak on 7/7/2015.
     */
    public static class GoBack extends CrawlerAction {

        public GoBack() {
            super("Navigating back in browser history");
        }

        @Override
        public CrawlerAction perform(WebCrawlerDriver webCrawlerDriver) {
            webCrawlerDriver.navigateBack();
            return this;
        }

    }

    public static class CloseTab extends CrawlerAction {

        public CloseTab() {
            super("Close all browser tabs/windows except current");
        }

        @Override
        public CrawlerAction perform(WebCrawlerDriver webCrawlerDriver) {
            WebDriver webDriver = webCrawlerDriver.getWrappedDriver();
            String originalWindowHandle = webDriver.getWindowHandle();
            List<String> windowHandles = new ArrayList<String>(webDriver.getWindowHandles());
            if (windowHandles.size() == 1) {
                System.out.println("Skipping this step... as it possible that no new tab/window was opened");
                goBack().doIt(webCrawlerDriver);
                return this;
            }
            windowHandles.remove(originalWindowHandle);
            for (String currentWindow : windowHandles) {
                webDriver.switchTo().window(currentWindow);
                webDriver.close();
            }
            webDriver.switchTo().window(originalWindowHandle);
            return this;
        }
    }
}

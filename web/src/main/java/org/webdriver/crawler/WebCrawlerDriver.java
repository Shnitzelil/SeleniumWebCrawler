package org.webdriver.crawler;

import com.google.common.base.Function;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.internal.WrapsDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.webdriver.crawler.helpers.ClickOnElement;
import org.webdriver.crawler.helpers.FindVisibleElements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ganak on 8/14/2016.
 */
public class WebCrawlerDriver implements WrapsDriver {

    private static final Logger log = Logger.getLogger(WebCrawlerDriver.class.getName());

    private WebDriver webDriver = null;
    private Capabilities desiredCapabilities;

    public WebCrawlerDriver(Capabilities desiredCapabilities) {
        this.desiredCapabilities = desiredCapabilities;
    }

    /**
     * The method is synchronized as a workaround to issue with initiating two webdrivers at the same time.
     */
    public void initialize() {
        long startTime = System.currentTimeMillis();
        try {
            webDriver = (getRemoteWebDriverUrl() != null) ? createRemoteWebDriver() : createLocalWebDriver();
            try {
                webDriver.manage().window().maximize();
            } catch (Exception exception) {
                log.log(Level.INFO, "*********** WebDriver failed to maximize ************", exception);
            }
        } catch (Throwable throwable) {
            log.log(Level.INFO, "*********** WebDriver Initialization Failure ************", throwable);
            throw new RuntimeException("WebDriver Initialization Failure : " + throwable.getMessage(), throwable);
        }
        log.log(Level.INFO,
                "WebDriver initialization took " + TimeUnit.SECONDS.convert(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS) + " seconds");
    }

    private WebDriver createLocalWebDriver() {
        String browserName = desiredCapabilities.getBrowserName();
        switch (browserName.toLowerCase()) {
            case BrowserType.CHROME:
                return new ChromeDriver(getDesiredCapabilities());
            case BrowserType.IE:
                return new InternetExplorerDriver(getDesiredCapabilities());
            case BrowserType.EDGE:
                return new EdgeDriver(getDesiredCapabilities());
            case BrowserType.SAFARI:
                return new SafariDriver(getDesiredCapabilities());
            case BrowserType.OPERA_BLINK:
                return new OperaDriver(getDesiredCapabilities());
            case BrowserType.FIREFOX:
            default:
                return new FirefoxDriver(getDesiredCapabilities());
        }
    }

    private WebDriver createRemoteWebDriver() throws MalformedURLException {
        return new RemoteWebDriver(new URL(getRemoteWebDriverUrl()), desiredCapabilities);
    }

    @Override
    public WebDriver getWrappedDriver() {
        return webDriver;
    }

    public String getRemoteWebDriverUrl() {
        return System.getProperty("webdriver.remote.server");
    }

    public Capabilities getDesiredCapabilities() {
        return desiredCapabilities;
    }

    public void quit() {
        webDriver.quit();
    }

    public boolean isAvailable(final By linkLocation) {
        try {
            List<WebElement> elements = webDriver.findElements(linkLocation);
            if (!elements.isEmpty()) {
                WebElement webElement = elements.get(0);
                Dimension size = webElement.getSize();
                return webElement.isDisplayed() && webElement.isEnabled() && size.getWidth() > 0 && size.getHeight() > 0;
            }
        } catch (Throwable e) {
        }
        return false;
    }

    public List<String> getText(By by) {
        List<WebElement> elements = getElements(by);
        List<String> list = new ArrayList<>();
        for (WebElement element : elements) {
            String text = element.getText();
            if (StringUtils.isNotBlank(text))
                list.add(text);
        }
//        System.out.println("Found " + elements.size() + " from that we have " + list.size() + " with text " + list.toString());
        return list;
    }

    public List<String> getHref(By by) {
        List<WebElement> elements = getElements(by);
        List<String> list = new ArrayList<>();
        for (WebElement element : elements) {
            list.add(element.getAttribute("href"));
        }
        return list;
    }

    public List<WebElement> getElements(By by) {
        return getFluentWaitWithDefault(160, 2000, "Fail to find clickable elements " + by).until(new FindVisibleElements(by, 0));
    }

    public String getElementText(final By by) {
        return getFluentWaitWithDefault(20, 2000, "Fail to get text from element " + by).until(new Function<WebDriver, String>() {
            @Override
            public String apply(WebDriver input) {
                WebElement element = input.findElement(by);
                return element.getText();
            }
        });
    }

    public String getElementAttribute(final By by, final String attributeName) {
        return getFluentWaitWithDefault(20, 2000, "Fail to get attribute from element " + by).until(new Function<WebDriver, String>() {
            @Override
            public String apply(WebDriver input) {
                WebElement element = input.findElement(by);
                String attributeValue = element.getAttribute(attributeName);
                return attributeValue;
            }
        });
    }

    public Wait<WebDriver> getFluentWaitWithDefault(int timeoutInSeconds, int polingInMilli, String message) {
        Collection<Class<? extends Throwable>> c = new ArrayList<Class<? extends Throwable>>(
                Arrays.asList(StaleElementReferenceException.class, NoSuchElementException.class, ElementNotVisibleException.class));
        return new FluentWait<WebDriver>(webDriver)
                .withTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .pollingEvery(polingInMilli, TimeUnit.MILLISECONDS)
                .withMessage(message)
                .ignoreAll(c);
    }

    public String getCurrentDomain() {
        String stringUrl = webDriver.getCurrentUrl();
        try {
            URL url = new URL(stringUrl);
            return url.getHost();
        } catch (Exception e) {
            log.log(Level.INFO, String.format("Ignoring the exception %s", e.getMessage()), e);
        }
        return "Failed To extract domain";
    }

    public void get(String url) {
        webDriver.get(url);
    }

    public void navigateBack() {
        if (webDriver instanceof JavascriptExecutor &&
                desiredCapabilities.getBrowserName().toLowerCase().contains("safari")) {
//            Workaround for: Safari issue #3771 (https://code.google.com/p/selenium/issues/detail?id=3771)
            ((JavascriptExecutor) webDriver).executeScript("history.go(-1)");
        } else {
            webDriver.navigate().back();
        }
    }

    public void clickOnElement(By by) {
        getFluentWaitWithDefault(120, 500, "Fail to click on element " + by).until(new ClickOnElement(by));
    }
}

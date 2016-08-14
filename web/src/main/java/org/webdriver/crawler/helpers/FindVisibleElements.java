package org.webdriver.crawler.helpers;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ganak on 7/7/2015.
 */
public class FindVisibleElements extends FindElements {

    public FindVisibleElements(By by) {
        super(by);
    }

    public FindVisibleElements(By by, int minimumElements) {
        super(by, minimumElements);
    }

    @Override
    public List<WebElement> apply(WebDriver webDriver) {
        List<WebElement> apply = super.apply(webDriver);
        if (apply == null) {
            return null;
        }
        List<WebElement> available = new ArrayList<>();
        for (WebElement currentElement : apply) {
            if (currentElement.isDisplayed() && currentElement.isEnabled() && hasSize(currentElement)) {
                available.add(currentElement);
            }
        }
        return available;
    }

    static boolean hasSize(WebElement webElement) {
        Dimension size = webElement.getSize();
        return size.getHeight() > 0 && size.getWidth() > 0;
    }
}

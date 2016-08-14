package org.webdriver.crawler.helpers;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

import java.util.List;

public class FindElements implements ExpectedCondition<List<WebElement>> {
	
	private int minimumElements;
	private By by;
	private int lastCheck = -1;

	public FindElements(By by) {
		this(by, 1);
	}

	public FindElements(By by, int minimumElements) {
		this.by = by;
		this.minimumElements = minimumElements;
	}

	@Override
	public List<WebElement> apply(WebDriver webDriver) {
		int temp = lastCheck;
		lastCheck = webDriver.findElements(by).size();
		if (lastCheck >= minimumElements && temp >= minimumElements && lastCheck == temp) {
			return webDriver.findElements(by);
		}
		return null;
	}

}

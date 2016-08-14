package org.webdriver.crawler.helpers;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

import java.util.List;

public class ClickOnElement implements ExpectedCondition<Boolean> {
	private By by;

	public ClickOnElement(By by) {
		this.by = by;
	}

	@Override
	public Boolean apply(WebDriver webDriver) {
		List<WebElement> elements = webDriver.findElements(by);
		if (elements != null) {
			for (WebElement currentElement : elements) {
				if(currentElement.isEnabled() &&
						currentElement.isDisplayed() && 
						currentElement.getSize().getHeight() > 0 && 
						currentElement.getSize().getWidth() > 0){
					currentElement.click();
					return Boolean.TRUE;
                }
			}
		}
		return Boolean.FALSE;
	}
}
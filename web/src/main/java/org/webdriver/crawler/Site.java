package org.webdriver.crawler;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ganak on 7/1/2015.
 */
public class Site {

    private final List<CrawlerAction> actions = new ArrayList<>();

    private final String siteName;
    private final String url;

    public Site(String siteName, String url) {
        this.siteName = siteName;
        this.url = url;
    }

    public String getSiteName() {
        return siteName;
    }

    public String getUrl() {
        return url;
    }

    public List<CrawlerAction> getActions(){
        return actions;
    }

    public CrawlerAction addAction(CrawlerAction action) {
        actions.add(action);
        return action;
    }

    public CrawlerAction removeLastAddAction() {
        return actions.remove(actions.size() - 1);
    }

    public void end() {

    }

    public void writeStory(String outputFolder) {
        List<String> lines = new ArrayList<>();
        for (CrawlerAction currentAction : actions) {
            lines.add(currentAction.getStep());
        }
        try {
            File file = new File(outputFolder, siteName + ".txt");
            file.getParentFile().mkdirs();
            FileUtils.writeLines(file, lines);
        } catch (Exception e) {
            System.out.println("Site: Ignoring exception in the writeStory method, cause: " + e.getCause().getMessage());
        }
    }

    public String toString() {
        return getSiteName();
    }

}

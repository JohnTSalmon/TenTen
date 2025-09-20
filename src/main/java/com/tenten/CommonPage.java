package com.tenten;

import com.tenten.locators.Target;
import com.tenten.logger.Logger;
import com.tenten.performable.Performable;
import com.tenten.utils.Colours;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v128.network.Network;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.List;

import static com.tenten.utils.StringUtils.isNotNullOrEmpty;
import static com.tenten.utils.StringUtils.isNullOrEmpty;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.openqa.selenium.devtools.v134.network.Network.responseReceived;


/**
 * {@inheritDoc}
 */

public class CommonPage implements CommonPageInterface {
    private final Logger logger = new Logger();
    private final Integer FOCUS_ELEMENT_PRESENT_TOLERANCE = 60;
    private final Integer PROBE_ELEMENT_PRESENT_TOLERANCE = 10;
    private final Integer MAX_TIMEOUT = 40;
    private final Integer SHORT_WAIT = 5;
    private final Integer PEEK_TIMEOUT = 5;
    private final int CLICK_TIMEOUT = 10;
    private final int CLEAR_ATTEMPTS = 5;
    private final int CLICK_ATTEMPTS = 5;
    private final int COLLECT_POLL_TIMEOUT = 10;
    private final String SCREENSHOTS_DIRECTORY = "screenshots";
    private final String PERFORMABLE = "performable";
    private boolean execute = true;
    // Currently focused element
    protected static WebElement currentElement;
    private List<WebElement> currentElements = new ArrayList<>();
    private Map<String, WebElement> storedElements = new HashMap<>();
    protected static WebDriver driver;
    private static DevTools devTools;
    private final Map<String, String> sessionMap = new HashMap<>();
    private WebDriverWait focusWait = null;
    private WebDriverWait probeWait = null;
    private WebDriverWait shortWait = null;
    private WebDriverWait peekWait = null;
    private final StopWatch stopWatch = new StopWatch();
    private By currentElementLocator;
    private JavascriptExecutor javascriptExecutor;
    // Used for choose : FIRST,LAST,NEXT,PREVIOUS
    private int currentListIndex = 0;
    private static CommonPage instance = null;
    private int getWaitTime = 20;
    private int collectTimeout = COLLECT_POLL_TIMEOUT;
    private int focusTimeout;

    private CommonPage() {
        super();
    }

    public static synchronized CommonPage getInstance() {
        if (instance == null) {
            instance = new CommonPage();
        }
        return instance;
    }

    @Override
    public void go(String url) {
        if (execute) {
            try {
                if (url != null && !url.isEmpty()) {
                    log("get() \t", "URL : ", url);
                    driver.get(url);
                } else {
                    takeScreenShotAndExit("URL is null or empty");
                }
            } catch (WebDriverException wde) {
                takeScreenShotAndExit(wde.getMessage());
            }
        }
    }

    @Override
    public void go(Enums.Direction direction) {
        log("go(Direction) : " + direction.name() + Colours.RESET);
        try {
            switch (direction) {
                case BACK:
                    driver.navigate().back();
                    break;
                case REFRESH:
                    driver.navigate().refresh();
                    break;
                case FORWARD:
                    driver.navigate().forward();
                    break;
            }
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    // Default Chrome
    public void open() {
        open(Enums.Browser.CHROME);
    }

    @Override
    public void open(Enums.Browser browser) {
        switch (browser) {
            // Default
            case CHROME:
                chromeConfig();
                break;
            case FIREFOX:
                driver = new FirefoxDriver();
                break;
            case EDGE:
                driver = new EdgeDriver();
                break;
            case SAFARI:
                driver = new SafariDriver();
                break;
            default:
                break;
        }
        log("open()\t", "New Driver : ", browser.name());
        driverConfig();
    }

    @Override
    public void close() {
        log("close() ", "Current Browser", "Use open() to open another browser");
        try {
            driver.close();
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    @Override
    public void quit() {
        log("quit() ", "All Browser Windows");
        try {
            driver.quit();
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    ///////////
    // FOCUS //
    ///////////

    @Override
    public void focus(Target target) {
        log("focus() ", target.getBy().toString());
        focus(target.getBy());
    }

    @Override
    public void focus(int waitTime) {
        log("focus() ", "waitTime : " + waitTime);
        if (waitTime > 0) {
            focusWait = new WebDriverWait(driver, Duration.ofSeconds(waitTime));
            focusTimeout = waitTime;
        } else {
            throw new CPOException("Invalid wait time : " + waitTime);
        }
    }

    @Override
    public void focus(Target... targets) {
        finder(2, targets);
    }

    @Override
    public void focus(int timeOut, Target... targets) {
        finder(timeOut, targets);
    }

    private void focus(By by) {
        try {
            currentElement = focusWait.until(ExpectedConditions.presenceOfElementLocated(by));
        } catch (TimeoutException toe) {
            throw toe;
        } catch (WebDriverException e) {
            takeScreenShotAndExit(e.getMessage());
        }
        currentElementLocator = by;
    }

    private void finder(int timeOut, Target... targets) {
        boolean found = false;
        WebDriverWait webDriverWait = new WebDriverWait(getDriver(), Duration.ofSeconds(timeOut));
        for (Target target : targets) {
            try {
                webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(target.getBy()));
                focus(target);
                found = true;
                break;
            } catch (TimeoutException toe) {
                // Not present
            }
        }
        if (!found) {
            throw new CPOException("TargetFinder : Target not found from list :" + Arrays.toString(targets));
        }
    }

    ////////////////////////
    // PROBE CONDITIONALS //
    ////////////////////////

    // Default ElementState.VISIBLE
    @Override
    public void probe(Target target) {
        log("probe()   ", target.getBy().toString());
        detect(target.getBy(), Enums.ElementState.VISIBLITY);
    }

    @Override
    public void probe(Target target, Enums.ElementState elementState) {
        log("probe()   ", target.getBy().toString(), " " + elementState.name());
        detect(target.getBy(), elementState);
    }

    private void detect(By by, Enums.ElementState elementState) {
        try {
            switch (elementState) {
                case VISIBLITY ->
                    // Present and visible.  Throws TIMEOUT otherwise
                        currentElement = probeWait.until(ExpectedConditions.visibilityOfElementLocated(by));
                case ABSENCE ->
                    // Either invisible or not present. Will return immediately. Throws TIMEOUT otherwise
                        probeWait.until(ExpectedConditions.invisibilityOfElementLocated(by));
                case PRESENCE ->
                    // Present, not necessarily visible. Throws TIMEOUT otherwise
                        currentElement = probeWait.until(ExpectedConditions.presenceOfElementLocated(by));
                default -> throw new IllegalStateException("Unexpected Element State: " + elementState);
            }
        } catch (WebDriverException wde) {
            // Gets here with TIMEOUT if :
            // VISIBLITY -> Element is either not present or present but not visible.
            // ABSENCE -> Element is present or present and visible
            // PRESENCE -> Element is not present
            execute = false;
        }
    }

    @Override
    public void probe(int waitTime) {
        log("probe() ", "Set Wait Time : " + Integer.toString(waitTime));
        probeWait = new WebDriverWait(driver, Duration.ofSeconds(waitTime));
    }

    @Override
    public void end() {
        execute = true;
        probe(PROBE_ELEMENT_PRESENT_TOLERANCE);
    }

    @Override
    public void origin() {
        log("origin() ", "Document Root");
        try {
            currentElement = driver.findElement((By.tagName("html")));
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    @Override
    public void leaf() {
        log("leaf()", "To leaf");
        while (true) {
            try {
                WebElement nextElement = currentElement.findElement(By.cssSelector(":first-child"));
                currentElement = nextElement;
            } catch (NoSuchElementException e) {
                break;
            } catch (WebDriverException wde) {
                takeScreenShotAndExit(wde.getMessage());
            }
        }

    }

    private void collect(By by) {
        try {
            currentElements = currentElement.findElements(by);
        } catch (TimeoutException timeout) {
            takeScreenShotAndExit("Element list following collect() not fully loaded after seconds : " + collectTimeout + " with locator : " + by.toString());
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
        if (currentElements.isEmpty()) {
            log("Warn : No elements found following collect() : " + by.toString());
        }
    }

    @Override
    public void collect(int waitTime) {
        collectTimeout = waitTime;
    }

    @Override
    public void collect(Target target) {
        log("collect()", target.getBy().toString());
        collect(target.getBy());
        log("collected", Integer.toString(currentElements.size()));
    }

    @Override
    public void collect(List<Target> targets) {
        log("collect()", "Targets : ", "" + targets.size());
        WebElement webElement;
        currentElements.clear();
        try {
            for (Target target : targets) {
                webElement = focusWait.until(ExpectedConditions.presenceOfElementLocated(target.getBy()));
                currentElements.add(webElement);
            }
        } catch (WebDriverException wde) {
            throw new CPOException(wde.getMessage());
        }
        log("collect()", "Targets found : ", "" + targets.size());
    }

    @Override
    public void cognate(Target original, Target required) {
        log("cognate()", "original: " + original.getBy().toString(), "  required: " + required.getBy().toString());
        try {
            WebElement webElement = driver.findElement(original.getBy());
            cognate(webElement, required, 0);
        } catch (NoSuchElementException e) {
            throw new CPOException("Original element not found by locator: " + original.getBy().toString());
        }
    }

    private void cognate(WebElement current, Target required, int level) {
        List<WebElement> targetElements = current.findElements(getRelativeBy(required));
        if (targetElements.isEmpty()) {
            if (current.getTagName().equals("html")) {
                throw new CPOException("Cognate not found");
            } else {
                WebElement parent = current.findElement(By.xpath(".."));
                cognate(parent, required, level + 1);
            }
        } else {
            if (targetElements.size() > 1) {
                log("cognate()", "Warn. Found multiple cognates at " + level + " levels above stem.  Selecting first");
            }
            currentElement = targetElements.getFirst();
            log("cognate()", "Found at/from level : " + level);
        }
    }

    @Override
    public void click() {
        log("click() ");
        click(CLICK_TIMEOUT);
    }

    @Override
    public void dblClick() {
        log("dblClick()");
        try {
            Actions actions = new Actions(driver);
            actions.doubleClick(currentElement);
        } catch (WebDriverException wde) {
            throw new CPOException(wde.getMessage());
        }
    }

    private void click(int waitTime) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitTime));
        int clicks = CLICK_ATTEMPTS;
        boolean isRefocused = false;
        while (true) {
            try {
                if (clicks == CLICK_ATTEMPTS) {
                    wait.until(ExpectedConditions.elementToBeClickable(currentElement)).click();
                } else {
                    currentElement.click();
                }
                break;
            } catch (TimeoutException te) {
                takeScreenShotAndExit("click() Element not clickable");
            } catch (ElementClickInterceptedException e) {
                // This is an exception caused by some element "covering" the element trying to be clicked
                // it won't be detected by elementToBeClickable, so must be dealt with separately - we repeat
                // clicking #(clickTolerance) times waiting between each attempt to approximately match waitTime
                pause(1);
                if (clicks-- == 0) {
                    takeScreenShotAndExit("click() wait time exceeded: " + currentElement.toString());
                }
            } catch (StaleElementReferenceException e) {
                // sometimes a focused element's selenium WebElement reference becomes "stale" and interacting
                // with it throws an error. In this case, we try re-focusing with the given locator only once
                if (!isRefocused) {
                    log("click()\t", "Stale reference: refocusing");
                    focus(currentElementLocator);
                    isRefocused = true;
                } else {
                    takeScreenShotAndExit(e.getMessage());
                }
            } catch (WebDriverException e) {
                takeScreenShotAndExit(e.getMessage());
            }
        }
    }

    @Override
    public void clear() {
        try {
            log("clear() ", "Current Element : " + currentElementLocator.toString());
            currentElement.clear();
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    @Override
    public void choose(int index) {
        try {
            log("choose() ", "Index : ", Integer.toString(index));
            currentElement = currentElements.get(index);
        } catch (IndexOutOfBoundsException | WebDriverException exception) {
            takeScreenShotAndExit(exception.getMessage());
        }
    }

    @Override
    public void choose(String text) {
        log("choose()", "Text : ", text);
        // Strict check by default
        setCurrentElementFromCurrentElementsByText(text, true);
    }

    @Override
    public void choose(String text, boolean strict) {
        setCurrentElementFromCurrentElementsByText(text, strict);
    }

    @Override
    public void choose(Target target) {
        log("choose() ", "Type : " + target.getType(), " " + target.getValue());
        String type = target.getValue();
        switch (target.getType()) {
            case VALUE -> setCurrentElementFromCurrentElementsByValue(type);
            case TEXT -> setCurrentElementFromCurrentElementsByText(type, true);
            case PARTIAL_TEXT -> setCurrentElementFromCurrentElementsByText(type, false);
            case CSS -> setCurrentElementFromCurrentElementsByAttributeKeyPair(target.getKey(), target.getValue());
            default -> throw new CPOException("choose(" + type + "). Not supported.  Request if required.");
        }
    }

    private void setCurrentElementFromCurrentElementsByAttributeKeyPair(String key, String value) {
        for (WebElement webElement : currentElements) {
            String attributeValue = webElement.getAttribute(key);
            if (isNotNullOrEmpty(attributeValue) && attributeValue.equals(value)) {
                currentElement = webElement;
                break;
            }
        }
    }

    @Override
    public void choose(Enums.Index index) {
        log("choose()", index.name());
        choose(index.ordinal());
    }

    @Override
    public void choose(Enums.ListIndex listIndex) {
        log("choose() : ", listIndex.name());
        switch (listIndex) {
            case FIRST:
                currentElement = currentElements.getFirst();
                currentListIndex = 0;
                break;
            case LAST:
                currentListIndex = currentElements.size() - 1;
                currentElement = currentElements.get(currentListIndex);
                break;
            case NEXT:
                // size() is total, list range is  0 -> size() -1
                if (++currentListIndex == currentElements.size()) {
                    currentListIndex--;
                    log("Choose()", "NEXT : ", "List pointer already at the end of collection");
                }
                currentElement = currentElements.get(currentListIndex);
                break;
            case PREVIOUS:
                if (--currentListIndex < 0) {
                    currentListIndex++;
                    log("Choose()", "PREVIOUS : ", "List pointer already at the start of collection");
                }
                currentElement = currentElements.get(currentListIndex);
                break;
        }
    }

    @Override
    public void compose(String keysToSend) {
        String attribute = currentElement.getAttribute("type");
        if (attribute == null) {
            throw new CPOException("Element has no type attribute or is not editable");
        }
        if (!currentElement.getAttribute("type").equals("password")) {
            log("compose() ", "Text : " + keysToSend);
        } else {
            int stringSize = keysToSend.length();
            log("compose() ", "Text : " + "*".repeat(stringSize));
        }
        try {
            click();
            currentElement.sendKeys(keysToSend);
        } catch (StaleElementReferenceException sere) {
            warn("compose() : StaleElementReferenceException : Relocating : By : " + currentElementLocator.toString());
            focus(By.xpath(getXpath(currentElement, "")));
            currentElement.sendKeys(keysToSend);
        } catch (IllegalArgumentException iae) {
            takeScreenShotAndExit("compose() parameters are NULL");
        }
    }

    @Override
    public void compose(Keys keyToSend) {
        log("compose() ", "Text : " + keyToSend.name());
        currentElement.sendKeys(keyToSend);
    }

    @Override
    public void compose(Keys keyToSend, int repeat) {
        log("compose(Keys, repeat ) ", "Text : " + keyToSend.name() + " Repeat : " + repeat);
        for (int i = 0; i < repeat; i++) {
            compose(keyToSend);
        }
    }

    ///////////////
    // SCROLLERS //
    ///////////////

    @Override
    public void scroll(Target target) {
        log("scroll() ", target.getBy().toString());
        focus(target.getBy());
        scrollIntoView();
    }

    @Override
    public void scroll() {
        log("scroll() ", "To currently focused element");
        scrollIntoView();
    }

    private void scrollIntoView() {
        try {
            new Actions(driver)
                    .scrollToElement(currentElement)
                    .perform();
        } catch (WebDriverException wde) {
            takeScreenShotAndExit("scrollIntoView");
        }
    }

    /////////////
    // DESCEND //
    /////////////

    @Override
    public void descend() {
        // To child
        log("descend() ", "To Child");
        try {
            currentElement = currentElement.findElement(By.cssSelector(":first-child"));
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    @Override
    public void descend(Target target) {
        log("descend()", target.getBy().toString());
        descend(target.getBy());
        currentElementLocator = target.getBy();
    }

    private void descend(By by) {
        try {
            currentElement = focusWait.until(ExpectedConditions.presenceOfNestedElementLocatedBy(currentElement, by));
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
        currentElementLocator = by;
    }

    ////////////
    // ASCEND //
    ////////////

    @Override
    public void ascend() {
        // To parent
        log("ascend()", "To Parent");
        ascendBy(1);
    }

    @Override
    public void ascend(int index) {
        // To ancestor by index
        log("ascend() ", "Levels : " + index);
        // Prevent 0
        ascendBy(index == 0 ? 1 : index);
    }

    @Override
    public void ascend(Enums.Index index) {
        // To ancestor by index ordinal + 1
        log("ascend() ", "Levels : " + index.ordinal() + 1);
        ascendBy((index.ordinal() + 1));
    }


    private void ascendBy(int index) {
        // To ancestor by index
        log("ascend() ", "Levels : " + index);
        try {
            currentElement = currentElement.findElement(By.xpath("ancestor::*[" + index + "]"));
            log("ascended", "To : ", currentElement.getTagName());
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }


    public void ascend(Enums.Tag tag) {
        log("ascend()", "Tag: " + tag.name());
        try {
            ascendToTag(currentElement, tag, 0);
        } catch (NoSuchElementException e) {
            throw new CPOException("Original element not found by locator: " + tag.name());
        }
    }

    private void ascendToTag(WebElement current, Enums.Tag tag, int level) {
        if (current.getTagName().equalsIgnoreCase(tag.name())) {
            currentElement = current;
            log("ascend()", "Found tag '" + tag.name() + "' at level: " + level);
        } else if (current.getTagName().equalsIgnoreCase("html")) {
            throw new CPOException("Ancestor tag '" + tag.name() + "' not found");
        } else {
            WebElement parent = current.findElement(By.xpath(".."));
            ascendToTag(parent, tag, level += 1);
        }
    }


    //////////////
    // TRAVERSE //
    //////////////

    @Override
    public void traverse() {
        // To next sibling
        log("traverse() ", "Following Sibling");
        selectFollowingSibling(1);
    }

    @Override
    public void traverse(int index) {
        // To sibling by index
        log("traverse() ", Integer.toString(index));
        selectFollowingSibling(index);
    }

    @Override
    public void traverse(Enums.NodeEnum index) {
        log("traverse() ", index.name());
        selectFollowingSibling(index.getValue());
    }

    private void selectFollowingSibling(int index) {
        try {
            currentElement = currentElement.findElement(By.xpath("following-sibling::*[" + index + "]"));
            log("traversed", "To : ", currentElement.getTagName());
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    //////////////
    // REVERSE //
    //////////////

    @Override
    public void reverse() {
        log("reverse() ", "Previous Sibling");
        selectPreviousSibling(1);
    }

    public void reverse(int index) {
        log("reverse() ", Integer.toString(index));
        selectPreviousSibling(index);
    }

    public void reverse(Enums.NodeEnum index) {
        log("reverse() ", index.name());
        selectPreviousSibling(index.getValue());
    }

    private void selectPreviousSibling(int index) {
        try {
            currentElement = currentElement.findElement(By.xpath("preceding-sibling::*[" + index + "]"));
            log("reversed", "To : ", currentElement.getTagName());
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }


    @Override
    public void matches(String text) {
        try {
            String elementText = currentElement.getText().trim();
            log("matches() ", "Required : " + text, ", Actual : " + elementText);
            if (!elementText.trim().equals(text.trim())) {
                takeScreenShotAndExit("matches() : Required : " + text + " Actual : " + elementText);
            }
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    public void contains(String text) {
        try {
            log("contains() ", text);
            String elementText = currentElement.getText();
            if (!elementText.trim().contains(text.trim())) {
                takeScreenShotAndExit("Text is not present :" + text);
            }
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }


    /////////////////////
    // SESSION STORAGE //
    /////////////////////

    @Override
    public void store(String key, String value) {
        if (isNotNullOrEmpty(key) && isNotNullOrEmpty(value)) {
            sessionMap.put(key, value);
        } else {
            throw new CPOException("store.  Invalid parameters");
        }
    }

    @Override
    public String retrieve(String key) {
        if (isNotNullOrEmpty(key)) {
            return sessionMap.get(key);
        } else {
            throw new CPOException("store Element.  Invalid parameters");
        }
    }

    private void depart(By by) {
        try {
            // Element can still be on the DOM but not visible
            stopWatch.start();
            focusWait.until(ExpectedConditions.invisibilityOfElementLocated(by));
            stopWatch.stop();
            log("depart()    Element departs after " + stopWatch.printElapsed());
        } catch (NoSuchElementException nsee) {
            // Element not present - ignore the exception
            log("depart()   Element departed");
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    @Override
    public void depart(Target target) {
        log("depart() ", target.getBy().toString());
        depart(target.getBy());
    }

    private void absent(By by) {
        log("absent() ", "By : " + by.toString());
        try {
            // Element can still be on the DOM but not visible
            focusWait.until(ExpectedConditions.invisibilityOfElementLocated(by));
        } catch (NoSuchElementException nsee) {
            // Element not present - ignore the exception
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    @Override
    public void absent(Target target) {
        absent(target.getBy());
    }

    @Override
    public void hover() {
        try {
            log("hover() " + currentElementLocator.toString());
            Actions actions = new Actions(driver);
            actions.moveToElement(currentElement).perform();
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    @Override
    public void hover(Target target) {
        focus(target.getBy());
        hover();
    }

    // Drop draggable on dropZone
    @Override
    public void dragDrop(Target draggable, Target dropZone) {
        dragDrop(draggable.getBy(), dropZone.getBy());
    }

    // Drop currently focused element on dropZone
    @Override
    public void drop(Target dropZone) {
        dragDrop(currentElement, focusWait.until(ExpectedConditions.visibilityOfElementLocated(dropZone.getBy())));
    }

    // Drag draggable to currently focused element
    @Override
    public void drag(Target draggable) {
        dragDrop(focusWait.until(ExpectedConditions.visibilityOfElementLocated(draggable.getBy())), currentElement);
    }

    private void dragDrop(By draggable, By dropZone) {
        log("dragDrop :draggable : ", draggable.toString());
        log("dragDrop :dropZone : ", dropZone.toString());
        WebElement drag = focusWait.until(ExpectedConditions.visibilityOfElementLocated(draggable));
        WebElement drop = focusWait.until(ExpectedConditions.visibilityOfElementLocated(dropZone));
        dragDrop(drag, drop);
    }

    private void dragDrop(WebElement draggable, WebElement dropZone) {
        try {
            Actions actions = new Actions(driver);
            actions.dragAndDrop(draggable, dropZone).build().perform();
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    // Driver methods
    public void fullScreen() {
        driver.manage().window().fullscreen();
    }

    public void maximise() {
        driver.manage().window().maximize();
    }

    private void position(int x, int y) {
        driver.manage().window().setPosition(new Point(x, y));
    }

    protected static WebDriver getDriver() {
        return driver;
    }

    @Override
    public <T extends Performable> void perform(T runner) {
        runner.run();
    }

    ////////////////////////////
    // COLLECTION SIZE CHECKS //
    ////////////////////////////

    @Override
    public int size() {
        int currentElementsSize = 0;
        currentElementsSize = getElementsSize();
        log("size() \t", "Returns : ", Integer.toString(currentElementsSize));
        return currentElementsSize;
    }

    private int getElementsSize() {
        if (!currentElements.isEmpty())
            return currentElements.size();
        else
            return 0;
    }

    ///////////////////////
    // COLLECTION CHECKS //
    ///////////////////////

    @Override
    public void present(String text) {
        log("present() ", text);
        if (!isTextPresentInElementList(text))
            takeScreenShotAndExit("present(String) : " + text + " Not found in collection");
    }

    @Override
    public void present(List<String> textList) {
        log("present(List<String>) : ", Arrays.toString(textList.toArray()));
        if (!isStringListPresentInElementList(textList))
            takeScreenShotAndExit("present(List<String> : " + Arrays.toString(textList.toArray()) + " Not found in collection");
    }

    @Override
    public void absent(String text) {
        log("absent()", text);
        if (isTextPresentInElementList(text))
            takeScreenShotAndExit("absent(String) : " + text + " Found in collection");
    }

    @Override
    public void absent(List<String> textList) {
        log("absent(List<String>) : ", Arrays.toString(textList.toArray()));
        if (isStringListPresentInElementList(textList))
            takeScreenShotAndExit("absent(List<String> : " + Arrays.toString(textList.toArray()) + " Found in collection");
    }

    @Override
    public void frame(String idOrName) {
        if (execute) {
            log("frame() ", "idOrName  :", idOrName);
            try {
                driver.switchTo().frame(idOrName);
                log("Switched to :", "Iframe");
            } catch (NoSuchElementException | TimeoutException exception) {
                takeScreenShotAndExit("No Iframe Found : idOrName : " + idOrName);
            } catch (WebDriverException wde) {
                takeScreenShotAndExit(wde.getMessage());
            }
        }
    }

    private void frame(By by) {
        log("frame() ", by.toString());
        try {
            WebElement iframe = focusWait.until(ExpectedConditions.presenceOfElementLocated(by));
            driver.switchTo().frame(iframe);
            log("Switched to :", "Iframe");
        } catch (NoSuchElementException | TimeoutException exception) {
            takeScreenShotAndExit("No Iframe Found :" + by);
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    @Override
    public void frame(Target target) {
        frame(target.getBy());
    }

    @Override
    public void frame() {
        frame(0);
    }

    @Override
    public void frame(int index) {
        log("frame() ", "Index : ", Integer.toString(index));
        try {
            driver.switchTo().frame(index);
            log("Switched to :", "Iframe index : ", "" + index);
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    @Override
    public void deframe() {
        log("unframe() ", "");
        try {
            driver.switchTo().defaultContent();
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    @Override
    public void window(Enums.Window window) {
        log("window()\t", Integer.toString(window.getValue()));
        switchToWindow(window.getValue());
    }

    @Override
    public void window(int index) {
        log("window()\t", Integer.toString(index));
        switchToWindow(index);
    }

    private void switchToWindow(int index) {
        try {
            Object[] windowHandles = driver.getWindowHandles().toArray();
            driver.switchTo().window((String) windowHandles[index]);
        } catch (WebDriverException wde) {
            takeScreenShotAndExit("window()");
        }
    }

    @Override
    public String get(Enums.ElementTrait elementTrait) {
        String val = null;
        log("get()  \t", "Element Attribute : ", elementTrait.name());
        val = getAttribute(elementTrait);
        log("Returning", val);
        return val;
    }

    @Override
    public void get(int waitTime) {
        this.getWaitTime = waitTime;
    }

    @Override
    public String getUrl() {
        String url = null;
        if (execute) {

            try {
                url = driver.getCurrentUrl();
                log("getUrl()", url);
            } catch (WebDriverException wde) {
                takeScreenShotAndExit(wde.getMessage());
            }
        }
        return url;
    }

    @Override
    public String getTitle() {
        String title = null;
        try {
            title = driver.getTitle();
            log("getTitle()", title);
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
        return title;
    }

    @Override
    public void refresh() {
        try {
            driver.navigate().refresh();
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    public void log(String... args) {
        logger.log(args);
    }

    public void log(Colours colour, String text) {
        logger.log(colour, text);
    }

    public void warn(String text) {
        logger.warn(text);
    }

    @Override
    public void printFocused() {
        log("printFocused()");
        printElement(currentElement);
    }

    @Override
    public void printCollection() {
        log("Current Elements : ", Integer.toString(currentElements.size()));
        if (currentElements != null) {
            int index = 1;
            for (WebElement webElement : currentElements) {
                log("===== " + index + " =====");
                printElement(webElement);
                index++;
            }
        } else {
            log("Collection is empty");
        }
    }

    private void printElement(WebElement webElement) {
        log("Tag  \t\t", webElement.getTagName());
        log("Text  \t\t", webElement.getText());
        log("Attributes\t", getAttributes());
        Rectangle rectangle = webElement.getRect();
        log("Coordinate x", "" + rectangle.x);
        log("Coordinate y", "" + rectangle.y);
        log("Height", "\t\t" + rectangle.height);
        log("Width", "\t\t" + rectangle.width);
        log("Enabled", "\t\t" + currentElement.isEnabled());
        log("Displayed", "\t" + currentElement.isDisplayed());
        log("Selected", "\t" + currentElement.isSelected());
    }

    @Override
    public void selected() {
        log("selected() ");
        if (!currentElement.isSelected()) {
            takeScreenShotAndExit(String.format("Element: %s is not selected", currentElement.getText()));
        }
    }

    @Override
    public void unSelected() {
        log("unSelected() ");
        if (currentElement.isSelected()) {
            takeScreenShotAndExit(String.format("Element: %s is selected", currentElement.getText()));
        }
    }

    @Override
    public void enabled() {
        log("enabled() ", currentElementLocator.toString());
        if (!currentElement.isEnabled()) {
            takeScreenShotAndExit(String.format("Element: %s is not enabled", currentElement.getText()));
        }
    }

    @Override
    public void disabled() {
        log("disabled() ", currentElementLocator.toString());
        if (currentElement.isEnabled()) {
            takeScreenShotAndExit(String.format("Element: %s is not disabled", currentElement.getText()));
        }
    }

    @Override
    public void clickable() {
        log("clickable()");
        try {
            shortWait.until(ExpectedConditions.elementToBeClickable(currentElement));
        } catch (TimeoutException te) {
            // Unclickable
            takeScreenShotAndExit(String.format("Element: %s is not clickable", currentElement.getText()));
        } catch (WebDriverException te) {
            takeScreenShotAndExit(String.format(te.getMessage()));
        }
    }

    @Override
    public void unclickable() {
        log("unClickable()");
        try {
            shortWait.until(ExpectedConditions.elementToBeClickable(currentElement));
            takeScreenShotAndExit(String.format("Element: %s is clickable", currentElement.getText()));
        } catch (TimeoutException te) {
            // Unclickable - return
        } catch (WebDriverException te) {
            takeScreenShotAndExit(te.getMessage());
        }
    }

    @Override
    public void visible() {
        log("visible() ", currentElementLocator.toString());
        shortWait.until(ExpectedConditions.visibilityOf(currentElement));
    }

    @Override
    public void hidden() {
        log("hidden() ");
        shortWait.until(ExpectedConditions.invisibilityOf(currentElement));
    }

    @Override
    public void reset() {
        origin();
        currentElements.clear();
        probeWait = new WebDriverWait(driver, Duration.ofSeconds(PROBE_ELEMENT_PRESENT_TOLERANCE));
        focusWait = new WebDriverWait(driver, Duration.ofSeconds(FOCUS_ELEMENT_PRESENT_TOLERANCE));
        focusTimeout = FOCUS_ELEMENT_PRESENT_TOLERANCE;
        collectTimeout = COLLECT_POLL_TIMEOUT;
    }

    @Override
    public boolean peek(Target target) {
        try {
            currentElement = peekWait.until(ExpectedConditions.visibilityOfElementLocated(target.getBy()));
        } catch (TimeoutException wde) {
            return false;
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
        return true;
    }

    @Override
    public void alert(Target.AlertAction action) {
        log("alert()", action.name());
        try {
            Alert alert = driver.switchTo().alert();
            switch (action) {
                case ACCEPT -> alert.accept();
                case DISMISS -> alert.dismiss();
            }
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    @Override
    public void alert(String keysToSend) {
        Alert alert = driver.switchTo().alert();
        log("alert() ", keysToSend);
        try {
            alert.sendKeys(keysToSend);
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    @Override
    public void trigger(Enums.Event event) {
        if (execute) {
            log("trigger() ", event.name());
            switch (event) {
                case CLICK -> javascriptExecutor.executeScript("arguments[0].click();", currentElement);
                case FOCUS -> javascriptExecutor.executeScript("arguments[0].focus();", currentElement);
                case BLUR -> javascriptExecutor.executeScript("arguments[0].blur();", currentElement);
                case DBLCLICK -> javascriptExecutor.executeScript("arguments[0].dblclick();", currentElement);
                case CHANGE -> javascriptExecutor.executeScript("arguments[0].change();", currentElement);
                default -> throw new IllegalArgumentException("Not happening.  This an enum");
            }
        }
    }

    @Override
    public String javascript(String script) {
        String jsOutput = null;
        if (execute) {
            log("javascriptExecutor() ", script);
            jsOutput = (String) javascriptExecutor.executeScript(script);
        }
        return jsOutput;
    }

    ///////////////////////////////////////////////////////////
    // NON-INTERFACE INTERNAL, PRIVATE AND PROTECTED METHODS //
    ///////////////////////////////////////////////////////////

    private String getAttributes() {
        return javascriptExecutor.executeScript("var items = {}; for (index = 0; index < arguments[0].attributes.length; ++index) { items[arguments[0].attributes[index].name] = arguments[0].attributes[index].value }; return items;", currentElement).toString();
    }

    private void setCurrentElementFromCurrentElementsByText(String text, boolean strict) {
        boolean found = false;
        try {
            for (WebElement webElement : currentElements) {
                String s = webElement.getText();
                if ((webElement.getText().trim().equals(text.trim()) && strict) ||
                        (webElement.getText().trim().contains(text.trim()) && !strict)) {
                    currentElement = webElement;
                    found = true;
                    break;
                }
            }
            if (!found) {
                takeScreenShotAndExit("Text not found in collection : " + text);
            }
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    private void setCurrentElementFromCurrentElementsByValue(String value) {
        boolean found = false;
        try {
            for (WebElement webElement : currentElements) {
                if (webElement.getAttribute("value").equals(value)) {
                    currentElement = webElement;
                    found = true;
                    break;
                }
            }
            if (!found) {
                takeScreenShotAndExit("Value  not found in collection : " + value);
            }
        } catch (WebDriverException wde) {
            takeScreenShotAndExit(wde.getMessage());
        }
    }

    void takeScreenShotAndExit(String message) {
        String pathSeparator = File.separator;
        String homeDir = System.getProperty("user.dir");

        File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        String fileName = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss'.png'").format(new Date());
        String directoryPath = homeDir + pathSeparator + SCREENSHOTS_DIRECTORY;
        File directory = new File(directoryPath);

        if (!directory.exists()) {
            directory.mkdir();
        }

        String fullPath = directoryPath + pathSeparator + fileName;
        try {
            FileUtils.moveFile(scrFile, new File(fullPath), REPLACE_EXISTING);
        } catch (FileExistsException fee) {
            // Ignore - file will be overwritten
        } catch (IOException ioe) {
            throw new CPOException(message + "\nScreenShot: " + Path.of(fullPath).toUri());
        }
        throw new CPOException(message);
    }

    private boolean isTextPresentInElementList(String text) {
        boolean found = false;
        for (WebElement webElement : currentElements) {
            if (webElement.getText().trim().equals(text.trim())) {
                found = true;
                break;
            }
        }
        return found;
    }

    private boolean isStringListPresentInElementList(List<String> stringList) {
        int textPresentCount = 0;
        int elementListSize = stringList.size();
        for (WebElement webElement : currentElements) {
            for (String string : stringList) {
                if (webElement.getText().trim().equals(string.trim())) {
                    textPresentCount++;
                }
            }
        }
        return textPresentCount == elementListSize;
    }

    private String getAttribute(Enums.ElementTrait trait) {
        String val;
        int count = 0;
        while (true) {
            switch (trait) {
                case ATTRIBUTES -> val = getAttributes();
                case TAG -> val = currentElement.getTagName();
                case TEXT -> val = currentElement.getText();
                case SELECTED -> val = String.valueOf(currentElement.isSelected()).toUpperCase();
                case ENABLED -> val = String.valueOf(currentElement.isEnabled()).toUpperCase();
                case DISPLAYED -> val = String.valueOf(currentElement.isDisplayed()).toUpperCase();
                default -> val = currentElement.getAttribute(trait.name().toLowerCase());
            }
            if (!isNullOrEmpty(val))
                break;
            else {
                pause(500L);
                count++;
            }
            if (count > (getWaitTime / 2))
                throw new CPOException("No attribute found");
        }
        return val;
    }

    private By getRelativeBy(Target target) {
        String byString = target.getBy().toString();
        if (byString.contains("By.xpath")) {
            String relativeXPath = byString.replaceAll("By\\.xpath: \\.?//", ".//");
            return By.xpath(relativeXPath);
        } else {
            return target.getBy();
        }
    }

    ///////////////
    // INTERNAL. //
    ///////////////

    ////////////////////
    // BROWSER CONFIG //
    ////////////////////

    private void chromeConfig() {
        ChromeOptions chromeOptions = new ChromeOptions();
        boolean headless = System.getProperty("headless", "false").equalsIgnoreCase("true");
        if (headless) {
            chromeOptions.addArguments("--headless=new");
            chromeOptions.addArguments("--window-size=1920,1200");
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.addArguments("--use-fake-device-for-media-stream");
            chromeOptions.addArguments("--use-fake-ui-for-media-stream");
        }
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        driver = new ChromeDriver(chromeOptions);
        devTools = ((HasDevTools) driver).getDevTools();
    }

    private void driverConfig() {
        javascriptExecutor = (JavascriptExecutor) driver;

        driver.manage().window().maximize();
        driver.manage().logs();
        focusWait = new WebDriverWait(driver, Duration.ofSeconds(FOCUS_ELEMENT_PRESENT_TOLERANCE));
        probeWait = new WebDriverWait(driver, Duration.ofSeconds(PROBE_ELEMENT_PRESENT_TOLERANCE));
        shortWait = new WebDriverWait(driver, Duration.ofSeconds(SHORT_WAIT));
        peekWait = new WebDriverWait(driver, Duration.ofSeconds(PEEK_TIMEOUT));
        focusTimeout = FOCUS_ELEMENT_PRESENT_TOLERANCE;
    }

    // Protected access and for use by JUNIT
    protected String getXpath(WebElement childElement, String current) {
        String childTag = childElement.getTagName();
        if (childTag.equals("html")) {
            return "/html[1]" + current;
        }
        WebElement parentElement = childElement.findElement(By.xpath(".."));
        List<WebElement> childrenElements = parentElement.findElements(By.xpath("*"));
        int count = 0;
        for (WebElement child : childrenElements) {
            String childrenElementTag = child.getTagName();
            if (childTag.equals(childrenElementTag)) {
                count++;
            }
            if (childElement.equals(child)) {
                return getXpath(parentElement, "/" + childTag + "[" + count + "]" + current);
            }
        }
        return null;
    }

    protected void pause(int seconds) {
        pause(seconds * 1000L);
    }

    // Protected access for use by JUNIT tests
    protected void pause(long milliSeconds) {
        try {
            Thread.sleep(milliSeconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected File takeScreenshot() {
        File scrFile;
        scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        return scrFile;
    }

    public void networkLogging(String type) {
        log("Network Logging() : Looking for failing calls of type: " + type);
        devTools.createSession();
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
        devTools.addListener(responseReceived(), responseReceivedEvent -> {
            String url = responseReceivedEvent.getResponse().getUrl();
            int status = responseReceivedEvent.getResponse().getStatus();
            String requestType = String.valueOf(responseReceivedEvent.getType());
            if (!(status == 200) && requestType.contains(type)) {
                log("Network Logged : " + url + " - TYPE: " + type);
                log("RESPONSE HEADERS: " + responseReceivedEvent.getResponse().getHeaders());
            }
        });
    }

}

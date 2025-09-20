package com.tenten;

import com.tenten.locators.Target;
import com.tenten.performable.Performable;
import org.openqa.selenium.Keys;

import java.util.List;

interface CommonPageInterface {

    ////////////////////////
    // Chainable commands //
    ////////////////////////

    // Window directives
    void fullScreen();

    void scroll(Target target);

    void scroll();

    void maximise(); // Review

    // Browser directives
    void open();

    void open(Enums.Browser browser);

    void go(String url);

    void go(Enums.Direction direction);

    void refresh();

    void close();

    void quit();

    void focus(Target target);

    void focus(int waitTime);

    void focus(Target... targets);

    void focus(int waitTime, Target... targets);

    void depart(Target target);

    void absent(Target target);

    void origin();

    void leaf();

    ////////////////////////
    // PROBE CONDITIONALS //
    ////////////////////////

    // Default ElementState.VISIBLE
    void probe(Target target);

    void probe(Target target, Enums.ElementState elementState);

    void probe(int waitTime);

    void end();

    // Check element present - returns immediately
    boolean peek(Target target);

    void collect(Target target);

    void collect(List<Target> targets);

    void collect(int waitTime);

    void cognate(Target origin, Target destination);

    void choose(int index);

    void choose(String text);

    void choose(String text, boolean lenient);

    void choose(Target target);

    void choose(Enums.Index index);

    void choose(Enums.ListIndex listIndex);

    void compose(String content);

    void compose(Keys keyToSend);

    void compose(Keys keyToSend, int repeat);

    void alert(Target.AlertAction action);

    void alert(String keysToSend);

    /////////////////
    // COLLECTIONS //
    /////////////////

    int size();

    ///////////////////////////////
    // COLLECTION CONTENT CHECKS //
    ///////////////////////////////

    void present(String text);

    void present(List<String> textList);

    void absent(String text);

    void absent(List<String> textList);

    // Element interaction
    void click();

    void dblClick();

    void clear();

    void hover();

    void hover(Target target);

    void dragDrop(Target draggable, Target dropZone);

    // Drop currently focused / chosen element on dropZone
    void drop(Target dropZone);

    // Drop draggable onto currently focused / chosen element
    void drag(Target draggable);

    // Interactions with return values
    String get(Enums.ElementTrait ElementTrait);

    void get(int getWaitTime);

    String getUrl();

    String getTitle();

    // DOM navigation
    void descend();

    void descend(Target target);

    void ascend();

    void ascend(int levels);

    void ascend(Enums.Index index);

    void ascend(Enums.Tag tag);

    void traverse();
    void traverse(int index);
    void traverse(Enums.NodeEnum index);

    void reverse();
    void reverse(int index);
    void reverse(Enums.NodeEnum index);

    // Perform complex action
    <T extends Performable> void perform(T val);

    // Frames
    void frame(String IdOrName);

    void frame(Target target);

    void frame(int index);

    void frame();

    void deframe();

    // Windows
    void window(Enums.Window window);

    void window(int index);

    // Element text checks

    void matches(String text);

    void contains(String partialText);

    // Element states

    void selected();

    void unSelected();

    void enabled();

    void disabled();

    void clickable();

    void unclickable();

    void visible();

    void hidden();

    // Reset Elements and timers

    void reset();

    // Session storage
    void store(String key, String value);
    String retrieve(String key);

    // Console Output
    void printFocused();

    void printCollection();

    void log(String... args);

    // EVENT GENERATORS
    void trigger( Enums.Event event);

    String javascript(String script);

    void networkLogging(String type);

}

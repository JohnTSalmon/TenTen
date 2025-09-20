package com.tenten.performable;

import com.tenten.CommonPageObject;

public abstract class PerformableExample extends CommonPageObject {

    public void run() {
        getDriver();
    }

    public String description() {
        return "PerformableExample Description";
    }
}

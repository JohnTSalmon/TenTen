package com.tenten.po;

import com.tenten.CommonPageObject;
import com.tenten.Enums;

public class Common extends CommonPageObject {

    public void openBrowser(String browser) {
        open(Enums.Browser.valueOf(browser));
    }
}

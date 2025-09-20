package com.tenten.steps;

import com.tenten.dto.DTOMap;
import com.tenten.dto.Investment;

import io.cucumber.java.DataTableType;
import io.cucumber.java.en.*;

import java.util.Map;


public class Common {

    com.tenten.po.Common common = new com.tenten.po.Common();

    @Given("I open the {word} Browser")
    public void iOpenTheBrowser(String browser) {
        common.openBrowser(browser);
    }

}

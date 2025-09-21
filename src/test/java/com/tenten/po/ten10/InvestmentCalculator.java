package com.tenten.po.ten10;

import com.tenten.CommonPageObject;
import com.tenten.Enums;
import com.tenten.dto.Investment;
import com.tenten.locators.Target;
import com.tenten.utils.InvestmentValidator;

import static com.tenten.Enums.ElementTrait.TEXT;
import static com.tenten.Enums.Tag.*;
import static com.tenten.Enums.Type.PASSWORD;

public class InvestmentCalculator extends CommonPageObject {

    InvestmentValidator investmentValidator = new InvestmentValidator();

    private final String URL = "http://3.8.242.61/";
    private final Target LOGIN = tagWithText(BUTTON, "Login");

    private final Target LOGIN_USERNAME = placeholder("name@example.com");
    private final Target LOGIN_PASSWORD = type(PASSWORD);
    private final Target LOGIN_LOGIN = id("login-submit");

    // TODO - Javascript or Actions class
    private final Target PRINCIPAL_AMOUNT = id("customRange1");

    private final Target DURATION_WEEKLY = attribute("data-value","Monthly");
    private final Target DURATION_MONTHLY = attribute("data-value","Monthly");
    private final Target DURATION_YEARLY = attribute("data-value","Monthly");
    private final Target SELECT_INTEREST_RATE = tagWithText(BUTTON,"Select Interest Rate");
    private final Target INVESTMENT_CALCULATE = tagWithText(BUTTON, "Calculate");
    private final Target INVESTMENT_TOTAL = id("totalAmount");
    private final Target INTEREST_RATE_ITEM = className("dropdown-item");

    public void openCalculator() {
        go(URL);
        if(peek(LOGIN)) {
            focus(LOGIN).click();
            focus(LOGIN_USERNAME).compose("j_salmon@hotmail.com");
            focus(LOGIN_PASSWORD).compose("Ten10123!");
            focus(LOGIN_LOGIN).click();
        }
    }

    public void input(Investment investment) {
        focus(SELECT_INTEREST_RATE).click();
        traverse().collect(INTEREST_RATE_ITEM);
        choose(Math.round(investment.getInterestRate())).descend().click();
        // Click away to close the dropdown
        ascend().click();
        switch (investment.getFrequency()) {
            case WEEKLY -> focus(DURATION_WEEKLY).click();
            case MONTHLY -> focus(DURATION_MONTHLY).click();
            case YEARLY -> focus(DURATION_YEARLY).click();
            default -> throw new IllegalStateException("Unexpected value: " + investment.getFrequency());
        }
        // Consent
        cognate(tagWithText(DIV,"Consent"), tag(INPUT)).click();
        focus(INVESTMENT_CALCULATE).click();
    }

    public boolean validate(Investment investment) {
        // Read result
        String result = focus(INVESTMENT_TOTAL).get(TEXT).replace("Total Amount with Interest: ","");
        Float totalUI = Float.valueOf(result);
        // Validate result
        return investmentValidator.validate(totalUI, investment);
    }
}
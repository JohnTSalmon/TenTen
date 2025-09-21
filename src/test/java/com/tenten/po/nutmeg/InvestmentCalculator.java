package com.tenten.po.nutmeg;

import com.tenten.CommonPageObject;
import com.tenten.locators.Target;
import com.tenten.dto.Investment;
import com.tenten.utils.InvestmentValidator;

import static com.tenten.Enums.ElementTrait.TEXT;
import static com.tenten.Enums.Tag.*;

public class InvestmentCalculator extends CommonPageObject {

    InvestmentValidator investmentValidator = new InvestmentValidator();

    private final String URL = "https://www.nutmeg.com/compound-interest-calculator";
    private final Target INVESTMENT_STARTING = id("initialInvestment");
    private final Target INVESTMENT_MONTHLY = id("contribution");
    private final Target INVESTMENT_YEARS = id("timeframe");
    private final Target INVESTMENT_INTEREST = id("interestRate");
    private final Target INVESTMENT_CALCULATE = attribute("data-qa", "calculate-button");
    private final Target INVESTMENT_TOTAL = tagWithText(P, "Final value");
    private final Target ACCEPT_COOKIES = id("onetrust-accept-btn-handler");


    public void openCalculator() {
        go(URL);
        // Deal with cookies popup
        if(peek(ACCEPT_COOKIES)) {
            click();
        }
    }

    public void input(Investment investment) {
        focus(INVESTMENT_STARTING).clear().compose(investment.getInitial().toString());
        focus(INVESTMENT_MONTHLY).clear().compose(investment.getContribution().toString());
        focus(INVESTMENT_YEARS).clear().compose(investment.getDuration().toString());
        focus(INVESTMENT_INTEREST).clear().compose(investment.getInterestRate().toString());
        focus(INVESTMENT_CALCULATE).click();
    }

    public boolean validate(Investment investment) {
        // Read result
        String result = cognate(INVESTMENT_TOTAL, tag(SPAN)).descend().traverse().get(TEXT).replace(",",".");
        Float totalUI = Float.valueOf(result);
        // Validate result
        return investmentValidator.validate(totalUI, investment);
    }
}

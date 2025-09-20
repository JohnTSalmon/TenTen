package com.tenten.steps;

import com.tenten.dto.DTOMap;
import com.tenten.dto.Investment;
import com.tenten.po.Common;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Map;

public class InvestmentCalculator {

    com.tenten.po.InvestmentCalculator investmentCalculator = new com.tenten.po.InvestmentCalculator();
    Common common = new Common();
    Investment investment;

    @DataTableType
    public Investment decode(Map<String, String> row) {
         return Investment.decode(new DTOMap(row));
    }

    @When("I navigate to the Investment Calculator")
    public void iNavigateToTheInvestmentCaclulator() {
        investmentCalculator.openCalculator();
    }

    @And("I input the following investment parameters")
    public void iInputTheFollowingInvestmentParameters(Investment investment) {
        this.investment = investment;
        investmentCalculator.input(investment);
    }

    @Then("I should see the return on my investment")
    public void iShouldSeeTheReturnOnMyInvestment() {
        investmentCalculator.validate(investment);
    }
}

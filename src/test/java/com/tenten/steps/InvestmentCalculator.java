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

    com.tenten.po.nutmeg.InvestmentCalculator nutmegInvestmentCalculator = new com.tenten.po.nutmeg.InvestmentCalculator();
    com.tenten.po.ten10.InvestmentCalculator ten10InvestmentCalculator = new com.tenten.po.ten10.InvestmentCalculator();

    Common common = new Common();
    Investment investment;

    @DataTableType
    public Investment decode(Map<String, String> row) {
        return Investment.decode(new DTOMap(row));
    }

    @When("I navigate to the Nutmeg Investment Calculator")
    public void iNavigateToTheNutmegInvestmentCaclulator() {
        nutmegInvestmentCalculator.openCalculator();
    }

    @When("I navigate to the Ten10 Investment Calculator")
    public void iNavigateToTheInvestmentCaclulator() {
        ten10InvestmentCalculator.openCalculator();
    }

    @And("I input the following Nutmeg investment parameters")
    public void iInputTheFollowingNutmegInvestmentParameters(Investment investment) {
        this.investment = investment;
        nutmegInvestmentCalculator.input(investment);
    }

    @And("I input the following Ten10 investment parameters")
    public void iInputTheFollowingTen10InvestmentParameters(Investment investment) {
        this.investment = investment;
        ten10InvestmentCalculator.input(investment);
    }

    @Then("I should see the return on my Nutmeg investment")
    public void iShouldSeeTheReturnOnMyNutmegInvestment() {
        nutmegInvestmentCalculator.validate(investment);
    }

    @Then("I should see the return on my Ten10 investment")
    public void iShouldSeeTheReturnOnMyTen10Investment() {
        ten10InvestmentCalculator.validate(investment);
    }

}


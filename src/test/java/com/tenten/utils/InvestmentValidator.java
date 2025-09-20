package com.tenten.utils;

import com.tenten.dto.Investment;

public class InvestmentValidator {

    public boolean validate(Float calculated, Investment investment) {
       // Validate the result string against the investment object
       Float initial = investment.getInitial();
       Float interestRate =investment.getInterestRate();
       Integer duration = investment.getDuration();
       int frequency = investment.getFrequency().getValue();
       // A = P(1 + r/n)nt
        Float total = (float) (initial * Math.pow((1 + (interestRate / 100) / frequency), frequency * duration));
        return calculated.equals(total);
    }
}
// total = P(1 + r/n)nt
// total = final amount
// P = initial principal balance
// r = interest rate
// n = number of times interest is applied per time period
// t = number of time periods elapsed

Feature: Test Ten Ten Investment Calculator

  Background:
    Given I open the CHROME Browser

  Scenario: Calculate and verify the return on investment

    When I navigate to the Nutmeg Investment Calculator
    And I input the following Nutmeg investment parameters
      | initial | interestRate | duration | contribution | frequency |
      | 1000    | 5            | 10       | 0            | MONTHLY    |
    Then I should see the return on my Nutmeg investment

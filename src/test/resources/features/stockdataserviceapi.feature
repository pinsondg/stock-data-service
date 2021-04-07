@API
Feature: API Calls

  Scenario: Client Requests Historic Options Data
    Given a historical option with ticker SPY exists and has price data
    When a successful API call is made to /data/option/SPY with an end date of today
    Then the options chain is contained in the response body
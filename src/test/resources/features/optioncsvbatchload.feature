@BatchJob
Feature: Options CSV Batch Load

  Scenario: Options Load Batch Job is Run
    Given options data for SPY exists in DB
    And a option-csv test file exists in S3
    When the option-csv job is triggered through the API
    Then the option-csv job succeeds within 120 seconds
    And the data for SPY updated in the database
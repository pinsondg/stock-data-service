@BatchJob
Feature: Options CSV Batch Load

  Scenario: Options Load Batch Job is Run
    Given options data for SPY exists in DB
    And test option csv files exist in S3
    When the job is triggered through the API
    Then the job succeeds within 120 seconds
    And the data for SPY updated in the database
@BatchJob
Feature: Fear Greed JSON Batch Load

  Scenario: Fear Greed JSON Batch Load is Run
    Given fear-greed data exists in DB
    And a fear-greed test file exists in S3
    When the fear-greed job is triggered through the API
    Then the fear-greed job succeeds within 60 seconds
    And the fear-greed data is updated in the database


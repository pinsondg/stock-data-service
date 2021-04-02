[![Java CI with Maven](https://github.com/pinsondg/stock-data-service/actions/workflows/master.yml/badge.svg)](https://github.com/pinsondg/stock-data-service/actions/workflows/master.yml)
# Stock-Data-Service
Tired of people charging absurd amounts just to access historic stock and options data?
Well look no further! This is a RESTful, Spring boot service and is your go to for storing and accessing historic
stock and options data. Included in this service is and easy to use API, Yahoo Finance
Options data parsers, and stock data sourced from Tiingo. However, this service is also
easily extensible if you would like to use your own implementation to source the data.
Feel free to fork this repo and implement it how you'd like.

## Tech Stack
Here is an overview of the technologies this service utilizes:
* Java 11
* Spring Boot
* Spring Web
* Postgresql
* H2 Database
* Maven
* Feign API Client
* Jsoup Html Parser
* JUnit
* Mock Server
* Liquibase
* Hibernate/JPA
* And more

## Getting Started
There are multiple profiles this application can run under. It is recommended when starting
to use the `default`/`local` profile. However, there are other profiles as well, and you can also
add your own. The differences between profiles will be described below. To specify a specific profile
when running, add `-Dspring.profiles.active={desired_profile},{another_desired_profile}`. To learn more
about spring profiles, visit the [documentation](https://docs.spring.io/spring-boot/docs/1.2.0.M1/reference/html/boot-features-profiles.html).

### Building and Running Tests
To build the service and run all tests, run the command `mvn clean install`. This will pull in all dependencies
and build a jar under the `target/` directory.

### Local (default) Profile
This is the profile that is used when there is no profile specified. This profile will set up
an in-memory H2 database.  **The database will be destroyed upon application termination** so all data
you have added will be lost.

#### Required Environment Variables
* TIINGO_AUTH_TOKEN: The associated with your tiingo account.

### Local-Save
Running with the local-save profile will create an H2 saved database. This databese will be
preserved when the application is stopped. However, it is only recommended for testing purposes,
as it is not very performant. If you would like to use this instead of setting up your own
database, feel free.

### Local-Mock
This profile is the same as local except it does not make actual API calls to Tiingo or
YahooFinance. This profile utilizes [Mock Server](https://www.mock-server.com/) in order to
mock http/https calls. This profile is mainly used for tests.

### Prod
This is the profile to use for production runs. Using this profile will utalize a PostgreSQL
database. You will need to pass some environemnt variables containing the database-url, username,
and password. [Liquibase](https://www.baeldung.com/liquibase-refactor-schema-of-java-app) 
is used in the prod profile to handle the database schema changes.

#### Required Environment Variables
* DATASOURCE_URL: the url of the datasource
* DATASOURCE_USERNAME: the username to the datasource
* DATASOURCE_PASSWORD: the password to the datasource

## API
There are several api endpoints to interact with this service.

### Get Options Data
Gets historic/live options data based on specified criteria. If either the startDate or endDate
are included, historic data is returned, otherwise only live data is returned. All dates should be
in the ISO-8601 LocalDate format (i.e. `2021-12-30`).

Method: `GET`

Endpoint: `/data/option/{ticker}`

Path Params:
* `ticker`: The stock ticker to look for.

Query Params:
* `expirationDate` - required: `false` -  type: `String`; The expiration date of the options to look for.
* `startDate` - required: `false` - type: `String`; The start date of the options price data to return.
* `endDate` - required: `false` - type: `String`; the end date of the options price data to return.

Returns: A List of [OptionsChains](src/main/java/com/dpgrandslam/stockdataservice/domain/model/options/OptionsChain.java).


### Get All Options Data
Gets all options data (stored and live) for a specified ticker.

Method: `GET`

Endpoint: `/data/option/{ticker}/all`

Path Params:
* `ticker`: The stock ticker to look for.

Returns: A List of [OptionsChains](src/main/java/com/dpgrandslam/stockdataservice/domain/model/options/OptionsChain.java).

### Get End-Of-Day Stock Data
Gets stock data based on specified params. If no params are past, the most recent data is retrieved.

Method: `GET`

Endpoint: `/data/stock/{ticker}`

Path Params:
* `ticker`: The stock ticker to look for.

Query Params:
* `startDate` - required: `false` - type: `String`; The start date of the options price data to return.
* `endDate` - required: `false` - type: `String`; the end date of the options price data to return.

Returns: A list of [EndOfDayStockData](src/main/java/com/dpgrandslam/stockdataservice/domain/model/stock/EndOfDayStockData.java).

### Get Live Stock Data
Get the most recent live stock data.

Method: `GET`

Endpoint: `/data/stock/{ticker}/live`

Path Params:
* `ticker`: The stock ticker to look for.

Returns: A [LiveStockData](src/main/java/com/dpgrandslam/stockdataservice/domain/model/stock/LiveStockData.java).

### Search Stock
Search for a stock on all exchanges. 

Method: `GET`

Endpoint: `/data/stock/search`

Query Params:
* `q` - required: `true`- type: `String`; The query to search for (example: `"apple"`)

Returns: A List of [StockSearchResult](src/main/java/com/dpgrandslam/stockdataservice/domain/model/stock/StockSearchResult.java).

### Get Tracked Stocks
Get all the stocks that are currently being tracked. Tracked stocks are stocks that the
end-of-day options load job will use to add price data to the database.

Method: `GET`

Endpoint: `/data/tracked`

Query Params:
* `activeOnly` - required: `false` - default: `true` - type: `boolean`; Filters return values
by if they are actively being tracked.

Returns: A List of [TrackedStock](src/main/java/com/dpgrandslam/stockdataservice/domain/model/stock/TrackedStock.java)

### Add Tracked Stocks
Adds a list stocks to be tracked by the options load job.

Method: `POST`

Endpoint: `/data/tracked`

Request Body:
```
[
  "TIK",
  "TIK2",
  ...    
]
```

### Update Active Tracked Stocks
Updates the active tracked stocks.

Method: `PUT`

Endpoint: `/data/tracked/active`

Request Body:
```
{
  "TIK" : false,
  "TIK2" : true,
  ...
}
```

## Jobs

### End of Day Options Load Job
This job runs at the end of every trading day (excluding holidays). This job reads data from yahoo finance options
chain and adds the price data to the database. If a read fails (usually due to too many calls to yahoo finance), then
it will be added to a retry queue for retry later. The job reads 2 complete option chains every 5 min. 

## Customizing Data Intake

Custom options data loaders can be added by extending the 
[OptionsChainLoadService](src/main/java/com/dpgrandslam/stockdataservice/domain/service/OptionsChainLoadService.java) abstract
class and implementing the abstract methods.

Custom stock data loaders can be added by implementing the [StockDataLoadService](src/main/java/com/dpgrandslam/stockdataservice/domain/service/StockDataLoadService.java)

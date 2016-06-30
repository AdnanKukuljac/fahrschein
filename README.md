# You need a fahrschein to use the (nakadi event) bus


[![Build Status](https://travis-ci.org/zalando-incubator/fahrschein.svg?branch=master)](https://travis-ci.org/zalando-incubator/fahrschein)
[![Release](https://img.shields.io/github/release/zalando-incubator/fahrschein.svg)](https://github.com/zalando-incubator/fahrschein/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.zalando/fahrschein.svg)](https://maven-badges.herokuapp.com/maven-central/org.zalando/fahrschein)

Fahrschein is a library for consuming events from a nakadi event bus.

## Features

 -

### Installation

Add the following dependency to your project:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>fahrschein</artifactId>
    <version>${fahrschein.version}</version>
</dependency>
```

## Configuration

```java
final URI baseUri = new URI("https://nakadi-sandbox-hila.aruha-test.zalan.do");
final String eventName = "sales-order-service.order-placed";

final ObjectMapper objectMapper = new ObjectMapper();
// Naming strategy of nakadi events is snake_case
objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);

// Problem module is needed to handle error responses from nakadi
objectMapper.registerModule(new ProblemModule());

// Register additional modules that might be used by your event class
objectMapper.registerModule(new JavaTimeModule());
objectMapper.registerModule(new MoneyModule());
objectMapper.registerModule(new Jdk8Module());

// Create and configure a ClientHttpRequestFactory
final SimpleClientHttpRequestFactory requestFactoryDelegate = new SimpleClientHttpRequestFactory();
requestFactoryDelegate.setConnectTimeout(400);
requestFactoryDelegate.setReadTimeout(60*1000);

// Wrap the ClientHttpRequestFactory to automatically handle problem responses
final ProblemHandlingClientHttpRequestFactory problemHandlingRequestFactory = new ProblemHandlingClientHttpRequestFactory(requestFactoryDelegate, objectMapper);

// Wrap it again to add "Authorization: Bearer TOKEN" headers to each request
final ClientHttpRequestFactory requestFactory = new AuthorizedClientHttpRequestFactory(problemHandlingRequestFactory, () -> "MY_ACCESS_TOKEN");

// Create a cursor manager, this can be either in-memory
final CursorManager cursorManager = new InMemoryCursorManager();

// Or persistent in a postgres database
final DataSource dataSource = new HikariDataSource(hikariConfig);
final CursorManager cursorManager = new PersistentCursorManager(dataSource);

// Or managed by the high-level api in nakadi itself
final ManagedCursorManager cursorManager = new ManagedCursorManager(baseUri, requestFactory, objectMapper);

// The retry behaviour can be configured using an ExponentialBackoffStrategy
final ExponentialBackoffStrategy exponentialBackoffStrategy = new ExponentialBackoffStrategy();

// Create a NakadiClient instance
final NakadiClient nakadiClient = new NakadiClient(baseUri, requestFactory, exponentialBackoffStrategy, objectMapper, cursorManager);

// This can be used to get information about available partitions for an event
final List<Partition> partitions = nakadiClient.getPartitions(eventName);

// The cursor manager can be configured to start reading from the oldest available offset in each partition
cursorManager.fromOldestAvailableOffset(eventName, partitions);

// Or from the newest available offset, but this is the same as the default
cursorManager.fromNewestAvailableOffsets(eventName, partitions);

// Or (for a persistent cursor manager) we can start reading from the last offset that we processed if it's still available, and from the oldest available offset otherwise
cursorManager.updatePartitions(eventName, partitions);

// Create a listener for our event
final Listener<SalesOrderPlaced> listener = events -> {
    for (SalesOrderPlaced salesOrderPlaced : events) {
        LOG.info("Received sales order [{}]", salesOrderPlaced.getSalesOrder().getOrderNumber());
    }
};

// StreamParameters configure timeouts and limits of the streaming api
final StreamParameters streamParameters = new StreamParameters().withStreamTimeout(5 * 60);

// Start streaming using the low-level api
nakadiClient.listen(eventName, SalesOrderPlaced.class, listener, streamParameters);

// Using the high-level api, we have to create a subscription first, the api has to be used with a `ManagedCursorManager`
final Subscription subscription = nakadiClient.subscribe("fahrschein-demo2", eventName, "fahrschein-demo-sales-order-placed");
nakadiClient.listen(subscription, SalesOrderPlaced.class, listener, streamParameters);

// The `listen` call above will block and automatically reconnect on `IOException`
```

See `src/test/java/org/zalando/fahrschein/salesorder/Main.java` for an executable version of the above code.


## Fahrschein compared to other nakadi client libraries

|                      | Fahrschein                                                        | Nakadi-Klients        | Straw               |
| -------------------- | ----------------------------------------------------------------- | --------------------- | ------------------- |
| Dependencies         | Spring (http client and jdbc), Jackson, Postgres (optional)       | Scala, Akka, Jackson  | None                |
| Cursor Management    | In-Memory / Persistent (Postgres)                                 | In-Memory             |                     |
| Partition Management | In-Memory / Persistent (Postgres)                                 |                       |                     |
| Error Handling       | Automatic reconnect with exponential backoff                      | Automatic reconnect   | No error handling   |


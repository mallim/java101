### Example for CompletableExecutors

In Spring 3.X, CompletableExecutors will be very useful to avoid error being thrown when using returning CompletableFuture from @ASync method.

Refer to https://geowarin.github.io/completable-futures-with-spring-async/ for more explanation

```
Caused by: java.lang.ClassCastException: java.util.concurrent.FutureTask cannot be cast to java.util.concurrent.CompletableFuture
```

### Required Libraries
* Lombok 1.16.20
* Guava 18.0
* Spring Boot 1.2.4
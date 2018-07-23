# Metrics Library Sample

# About

This sample app uses Guice (https://github.com/google/guice) as a Dependency Injection (DI) framework. This provides the glue for wiring the necessary metric classes and configs.
Don't worry, you can also use the Metrics Library without Guice.

The sample app also uses typesafe (https://github.com/lightbend/config) for storing and retrieving the metric client configurations.
Again, not to worry, you can use the Metrics Library without typesafe too. In which case, you can use the config Java builders to pass the desired configurations (e.g. backend server, port etc.).

# Prerequisites

Here are the prerequisites for running the sample app.

## Create the immutable classes

The project uses immutbales (https://immutables.github.io/) to generate the metrics config files. In order to generate the Java classes for these configs, run the following gradle command:

```
./gradlew build
```


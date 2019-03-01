# Reactor Aeron

`Reactor Aeron` offers non-blocking and backpressure-ready relable `UDP`
clients & servers based on [Aeron](https://github.com/real-logic/aeron) Efficient reliable UDP unicast, UDP multicast, and IPC message transport. it is inspired by [reactor-netty](https://github.com/reactor/reactor-netty)

## Getting it
`Reactor Aeron` requires Java 8 or + to run.

With `Maven` from `Maven Central` repositories (stable releases only):

```xml
<!-- https://mvnrepository.com/artifact/io.scalecube/scalecube-reactor-aeron -->
<dependency>
    <groupId>io.scalecube</groupId>
    <artifactId>scalecube-reactor-aeron</artifactId>
    <version>x.y.z</version>
    <type>pom</type>
</dependency>

```

## Getting Started

Here is a very simple server and the corresponding client example

```java
  AeronResources resources = new AeronResources().useTmpDir().start().block();

  AeronServer.create(resources)
    .options("localhost", 13000, 13001)
    .handle(
        connection ->
            connection
                .inbound()
                .receive()
                .asString()
                .log("receive")
                .then(connection.onDispose()))
    .bind()
    .block()
    .onDispose(resources)
    .onDispose()
    .block();
    
```

```java
    AeronResources resources = new AeronResources().useTmpDir().start().block();

    AeronClient.create(resources)
        .options("localhost", 13000, 13001)
        .handle(
            connection1 -> {
              System.out.println("Handler invoked");
              return connection1
                  .outbound()
                  .sendString(Flux.fromStream(Stream.of("Hello", "world!")).log("send"))
                  .then(connection1.onDispose());
            })
        .connect()
        .block()
        .onDispose(resources)
        .onDispose()
        .block();
```

## Building from Source

```console
$ git clone git@github.com:scalecube/reactor-aeron.git
$ cd reactor-aeron
$ mvn clean install
```

## Performance results

Performance is the key focus. Aeron is designed to be the highest throughput with the lowest and most predictable latency possible of any messaging system.

Benchmark: `reactor-aeron` vs `reactor-netty` vs `pure-aeron` running on `AWS` `C5.xlarge`

- Throughput: https://plot.ly/~ronenhamias/15.embed
- Latency: https://plot.ly/~ronenhamias/17.embed

## Code style

[See the reference on scalecube site](https://github.com/scalecube/scalecube-parent/blob/develop/DEVELOPMENT.md#setting-up-development-environment) 

## License

`Reactor Aeron` is Open Source Software released under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

package reactor.aeron.mdc;

import static reactor.aeron.DefaultFragmentMapper.asString;

import java.time.Duration;
import java.util.function.Function;
import java.util.stream.Stream;
import org.agrona.DirectBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.aeron.AeronDuplex;
import reactor.aeron.BaseAeronTest;
import reactor.aeron.OnDisposable;
import reactor.aeron.SocketUtils;
import reactor.aeron.ThreadWatcher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ReplayProcessor;
import reactor.test.StepVerifier;

class AeronServerTest extends BaseAeronTest {

  private int serverPort;
  private int serverControlPort;
  private AeronResources resources;

  @BeforeEach
  void beforeEach() {
    serverPort = SocketUtils.findAvailableUdpPort();
    serverControlPort = SocketUtils.findAvailableUdpPort();
    resources = new AeronResources().useTmpDir().singleWorker().start().block();
  }

  @AfterEach
  void afterEach() {
    if (resources != null) {
      resources.dispose();
      resources.onDispose().block(TIMEOUT);
    }
  }

  @Test
  public void testServerReceivesData() {
    ReplayProcessor<String> processor = ReplayProcessor.create();

    createServer(
        connection -> {
          connection.inbound().receive().map(asString()).log("receive").subscribe(processor);
          return connection.onDispose();
        });

    createConnection()
        .outbound()
        .sendString(Flux.fromStream(Stream.of("Hello", "world!")).log("send"))
        .then()
        .subscribe();

    StepVerifier.create(processor).expectNext("Hello", "world!").thenCancel().verify();
  }

  @Test
  public void testServerDisconnectsClientsUponShutdown() throws InterruptedException {
    ReplayProcessor<DirectBuffer> processor = ReplayProcessor.create();

    OnDisposable server =
        createServer(
            connection -> {
              connection.inbound().receive().log("receive").subscribe(processor);
              return connection.onDispose();
            });

    createConnection()
        .outbound()
        .sendString(
            Flux.range(1, 100)
                .delayElements(Duration.ofSeconds(1))
                .map(String::valueOf)
                .log("send"))
        .then()
        .subscribe();

    processor.blockFirst();

    server.dispose();

    Assertions.assertTrue(new ThreadWatcher().awaitTerminated(5000, "single-", "parallel-"));
  }

  private AeronDuplex<DirectBuffer> createConnection() {
    return AeronClient.create(resources)
        .options("localhost", serverPort, serverControlPort)
        .connect()
        .block(TIMEOUT);
  }

  private OnDisposable createServer(
      Function<? super AeronDuplex<DirectBuffer>, ? extends Publisher<Void>> handler) {
    return AeronServer.create(resources)
        .options("localhost", serverPort, serverControlPort)
        .handle(handler)
        .bind()
        .block(TIMEOUT);
  }
}

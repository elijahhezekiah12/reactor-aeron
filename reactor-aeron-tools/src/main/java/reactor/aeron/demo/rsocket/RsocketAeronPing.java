package reactor.aeron.demo.rsocket;

import io.aeron.driver.Configuration;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.Frame;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.reactor.aeron.AeronClientTransport;
import io.rsocket.util.ByteBufPayload;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.HdrHistogram.Recorder;
import reactor.aeron.AeronClient;
import reactor.aeron.AeronResources;
import reactor.aeron.demo.Configurations;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

public final class RsocketAeronPing {

  private static final Recorder HISTOGRAM = new Recorder(TimeUnit.SECONDS.toNanos(10), 3);
  private static final Payload PAYLOAD =
      ByteBufPayload.create(ByteBufAllocator.DEFAULT.buffer(Configurations.MESSAGE_LENGTH));

  /**
   * Main runner.
   *
   * @param args program arguments.
   */
  public static void main(String... args) {
    printSettings();

    AeronResources resources =
        new AeronResources()
            .useTmpDir()
            .pollFragmentLimit(Configurations.FRAGMENT_COUNT_LIMIT)
            .singleWorker()
            .workerIdleStrategySupplier(Configurations::idleStrategy)
            .start()
            .block();

    RSocket client =
        RSocketFactory.connect()
            .frameDecoder(Frame::retain)
            .transport(
                () ->
                    new AeronClientTransport(
                        AeronClient.create(resources)
                            .options(
                                Configurations.MDC_ADDRESS,
                                Configurations.MDC_PORT,
                                Configurations.MDC_CONTROL_PORT)))
            .start()
            .block();

    Disposable report = startReport();

    Flux.range(1, (int) Configurations.NUMBER_OF_MESSAGES)
        .flatMap(
            i -> {
              long start = System.nanoTime();
              return client
                  .requestResponse(PAYLOAD.retain())
                  .doOnNext(Payload::release)
                  .doFinally(
                      signalType -> {
                        long diff = System.nanoTime() - start;
                        HISTOGRAM.recordValue(diff);
                      });
            },
            64)
        .doOnError(Throwable::printStackTrace)
        .doOnTerminate(
            () -> System.out.println("Sent " + Configurations.NUMBER_OF_MESSAGES + " messages."))
        .doFinally(s -> report.dispose())
        .then()
        .doFinally(s -> resources.dispose())
        .then(resources.onDispose())
        .block();
  }

  private static Disposable startReport() {
    return Flux.interval(
            Duration.ofSeconds(Configurations.WARMUP_REPORT_DELAY),
            Duration.ofSeconds(Configurations.REPORT_INTERVAL))
        .doOnNext(RsocketAeronPing::report)
        .doFinally(RsocketAeronPing::report)
        .subscribe();
  }

  private static void report(Object ignored) {
    System.out.println("---- PING/PONG HISTO ----");
    HISTOGRAM.getIntervalHistogram().outputPercentileDistribution(System.out, 5, 1000.0, false);
    System.out.println("---- PING/PONG HISTO ----");
  }

  private static void printSettings() {
    System.out.println(
        "address: "
            + Configurations.MDC_ADDRESS
            + ", port: "
            + Configurations.MDC_PORT
            + ", controlPort: "
            + Configurations.MDC_CONTROL_PORT);
    System.out.println("MediaDriver THREADING_MODE: " + Configuration.THREADING_MODE_DEFAULT);
    System.out.println("Message length of " + Configurations.MESSAGE_LENGTH + " bytes");
    System.out.println("pollFragmentLimit of " + Configurations.FRAGMENT_COUNT_LIMIT);
    System.out.println(
        "Using worker idle strategy "
            + Configurations.idleStrategy().getClass()
            + "("
            + Configurations.IDLE_STRATEGY
            + ")");
  }
}

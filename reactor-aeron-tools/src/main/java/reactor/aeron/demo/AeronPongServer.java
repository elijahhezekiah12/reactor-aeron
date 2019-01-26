package reactor.aeron.demo;

import io.aeron.driver.ThreadingMode;
import java.util.function.Supplier;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import reactor.aeron.AeronResources;
import reactor.aeron.AeronServer;

public final class AeronPongServer {

  static {
    // see .../aeron/aeron-driver/src/main/resources/low-latency.properties
    System.setProperty("agrona.disable.bounds.checks", "true");
    System.setProperty("aeron.socket.so_sndbuf", "2m");
    System.setProperty("aeron.socket.so_rcvbuf", "2m");
    System.setProperty("aeron.rcv.initial.window.length", "2m");
  }

  /**
   * Main runner.
   *
   * @param args program arguments.
   */
  public static void main(String... args) {

    Supplier<IdleStrategy> idleStrategySupplier = () -> new BackoffIdleStrategy(1, 1, 1, 1);
    //    Supplier<IdleStrategy> idleStrategySupplier = () -> new BusySpinIdleStrategy();
    //    Supplier<IdleStrategy> idleStrategySupplier = () -> null;

    AeronResources resources =
        new AeronResources()
            .useTmpDir()
            .pollFragmentLimit(4)
            .singleWorker()
            .media(
                ctx ->
                    ctx.threadingMode(ThreadingMode.DEDICATED)
                        .conductorIdleStrategy(idleStrategySupplier.get())
                        .receiverIdleStrategy(idleStrategySupplier.get())
                        .senderIdleStrategy(idleStrategySupplier.get())
                        .termBufferSparseFile(false))
            .start()
            .block();

    AeronServer.create(resources)
        .options("localhost", 13000, 13001)
        .handle(
            connection ->
                connection
                    .outbound()
                    .send(connection.inbound().receive())
                    .then(connection.onDispose()))
        .bind()
        .block()
        .onDispose(resources)
        .onDispose()
        .block();
  }
}

package reactor.ipc.aeron.demo;

import io.aeron.driver.AeronResources;
import java.time.Duration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.aeron.AeronUtils;
import reactor.ipc.aeron.server.AeronServer;

public class ServerServerSends {

  /**
   * Main runner.
   *
   * @param args program arguments.
   */
  public static void main(String[] args) throws Exception {
    try (AeronResources aeronResources = AeronResources.start()) {

      AeronServer server =
          AeronServer.create(
              "server",
              aeronResources,
              options -> {
                options.serverChannel("aeron:udp?endpoint=localhost:13000");
              });
      server
          .newHandler(
              (inbound, outbound) -> {
                outbound
                    .send(
                        Flux.range(1, 10000)
                            .delayElements(Duration.ofMillis(250))
                            .map(i -> AeronUtils.stringToByteBuffer("" + i))
                            .log("send"))
                    .then()
                    .subscribe();
                return Mono.never();
              })
          .block();

      System.out.println("main finished");
      Thread.currentThread().join();
    }
  }
}

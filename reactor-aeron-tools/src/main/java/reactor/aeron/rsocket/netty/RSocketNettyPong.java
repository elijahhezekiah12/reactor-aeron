package reactor.aeron.rsocket.netty;

import io.netty.channel.ChannelOption;
import io.rsocket.AbstractRSocket;
import io.rsocket.Frame;
import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.aeron.Configurations;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpServer;

public final class RSocketNettyPong {

  /**
   * Main runner.
   *
   * @param args program arguments.
   */
  public static void main(String... args) {
    System.out.println(
        "address: " + Configurations.MDC_ADDRESS + ", port: " + Configurations.MDC_PORT);

    LoopResources loopResources = LoopResources.create("rsocket-netty");

    TcpServer tcpServer =
        TcpServer.create()
            .runOn(loopResources)
            .host(Configurations.MDC_ADDRESS)
            .port(Configurations.MDC_PORT)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.SO_REUSEADDR, true);

    RSocketFactory.receive()
        .frameDecoder(Frame::retain)
        .acceptor(
            (setupPayload, rsocket) ->
                Mono.just(
                    new AbstractRSocket() {
                      @Override
                      public Mono<Payload> requestResponse(Payload payload) {
                        return Mono.just(payload);
                      }
                    }))
        .transport(() -> TcpServerTransport.create(tcpServer))
        .start()
        .block()
        .onClose()
        .block();
  }
}

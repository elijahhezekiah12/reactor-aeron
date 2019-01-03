package reactor.aeron.client;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.aeron.AeronChannelUri;
import reactor.aeron.AeronResources;
import reactor.aeron.Connection;

public class AeronClientSettings {

  private AeronResources resources;
  private Function<? super Connection, ? extends Publisher<Void>> handler;
  private AeronChannelUri inboundUri = new AeronChannelUri();
  private AeronChannelUri outboundUri = new AeronChannelUri();
  private Duration ackTimeout = Duration.ofSeconds(10);
  private Duration connectTimeout = Duration.ofSeconds(5);
  private Duration backpressureTimeout = Duration.ofSeconds(5);

  public AeronClientSettings() {}

  public AeronClientSettings(AeronClientSettings other) {
    this.resources = other.resources;
    this.handler = other.handler;
    this.inboundUri = other.inboundUri;
    this.outboundUri = other.outboundUri;
    this.ackTimeout = other.ackTimeout;
    this.connectTimeout = other.connectTimeout;
    this.backpressureTimeout = other.backpressureTimeout;
  }

  public AeronResources resources() {
    return resources;
  }

  public Function<? super Connection, ? extends Publisher<Void>> handler() {
    return handler;
  }

  public AeronChannelUri inboundUri() {
    return inboundUri;
  }

  public AeronChannelUri outboundUri() {
    return outboundUri;
  }

  public Duration ackTimeout() {
    return ackTimeout;
  }

  public Duration connectTimeout() {
    return connectTimeout;
  }

  public Duration backpressureTimeout() {
    return backpressureTimeout;
  }

  public AeronClientSettings resources(AeronResources resources) {
    return set(s -> s.resources = resources);
  }

  public AeronClientSettings handler(
      Function<? super Connection, ? extends Publisher<Void>> handler) {
    return set(s -> s.handler = handler);
  }

  public AeronClientSettings inboundUri(AeronChannelUri inboundUri) {
    return set(s -> s.inboundUri = inboundUri);
  }

  public AeronClientSettings outboundUri(AeronChannelUri outboundUri) {
    return set(s -> s.outboundUri = outboundUri);
  }

  public AeronClientSettings ackTimeout(Duration ackTimeout) {
    return set(s -> s.ackTimeout = ackTimeout);
  }

  public AeronClientSettings connectTimeout(Duration connectTimeout) {
    return set(s -> s.connectTimeout = connectTimeout);
  }

  public AeronClientSettings backpressureTimeout(Duration backpressureTimeout) {
    return set(s -> s.backpressureTimeout = backpressureTimeout);
  }

  private AeronClientSettings set(Consumer<AeronClientSettings> c) {
    AeronClientSettings s = new AeronClientSettings(this);
    c.accept(s);
    return s;
  }
}

package reactor.aeron;

import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.SignalType;

/**
 * Aeron <i>connection</i> interface.
 *
 * <p>Configured and established connection makes available operations such as {@link #inbound()}
 * for reading and {@link #outbound()} for writing data. AeronConnection interface comes with {@link
 * OnDisposable} and {@link #disposeSubscriber()} function for convenient resource cleanup.
 */
public interface AeronDuplex<I> extends OnDisposable {

  /**
   * Return the {@link AeronInbound} read API from this connection. If {@link AeronDuplex} has not
   * been configured, receive operations will be unavailable.
   *
   * @return {@code AeronInbound} instance
   */
  AeronInbound<I> inbound();

  /**
   * Return the {@link AeronOutbound} write API from this connection. If {@link AeronDuplex} has not
   * been configured, send operations will be unavailable.
   *
   * @return {@code AeronOutbound} instance
   */
  AeronOutbound outbound();

  /**
   * Return a {@link CoreSubscriber} that will dispose on complete or error.
   *
   * @return a {@code CoreSubscriber} that will dispose on complete or error
   */
  default CoreSubscriber<Void> disposeSubscriber() {
    return new ConnectionDisposer(this);
  }

  final class ConnectionDisposer extends BaseSubscriber<Void> {
    final OnDisposable onDisposable;

    public ConnectionDisposer(OnDisposable onDisposable) {
      this.onDisposable = onDisposable;
    }

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
      request(Long.MAX_VALUE);
      onDisposable.onDispose(this);
    }

    @Override
    protected void hookFinally(SignalType type) {
      if (type != SignalType.CANCEL) {
        onDisposable.dispose();
      }
    }
  }
}

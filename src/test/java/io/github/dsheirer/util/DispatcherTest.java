package io.github.dsheirer.util;

import io.github.dsheirer.sample.CapturingListener;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class DispatcherTest {

  @Test
  void shouldCallListenerWithSuppliedElements() {
    // given
    final var dispatcher = new Dispatcher<>(3, "some thread name", "");
    final var listener = new CapturingListener<String>();
    dispatcher.setListener(listener);

    // when
    dispatcher.start();
    dispatcher.receive("1");
    dispatcher.receive("2");
    dispatcher.flushAndStop();

    // then
    await()
        .atMost(1, SECONDS)
        .untilAsserted(() -> assertThat(listener.capturedValues()).containsExactly("1", "2"));
  }

  @Test
  void shouldCallListenerOnDedicatedThread() {
    // given
    final var dispatcher = new Dispatcher<>(3, "some thread name", "");
    final var threadCaptor = new AtomicReference<Thread>();
    dispatcher.setListener(e -> threadCaptor.set(Thread.currentThread()));

    // when
    dispatcher.start();
    dispatcher.receive("1");
    dispatcher.receive("2");
    dispatcher.flushAndStop();

    // then
    await()
        .atMost(1, SECONDS)
        .untilAsserted(() -> assertThat(threadCaptor.get()).isNotNull());

    // and
    final var thread = threadCaptor.get();
    assertThat(thread).isNotEqualTo(Thread.currentThread());
    assertThat(thread.getName()).isEqualTo("some thread name");
  }

  @Test
  void shouldDropElementsWhenQueueIsFull() {
    // given
    final var dispatcher = new Dispatcher<>(1, "some thread name", "");
    final var listener = new CapturingListener<String>();
    dispatcher.setListener(listener);

    final var elements = IntStream.range(1, 100).mapToObj(String::valueOf).toList();

    // when
    dispatcher.start();
    for (final var element : elements) dispatcher.receive(element);
    dispatcher.flushAndStop();

    // then
    await()
        .atMost(1, SECONDS)
        .untilAsserted(() -> assertThat(listener.capturedValues()).doesNotContainSequence(elements));
  }
}

package io.github.dsheirer.module.decode.event.filter;

import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class EventFilterTest {

  @Test
  public void shouldProcessSpecifiedDecodeEventTypesAndAllowThemByDefault() {
    // given
    final var types = List.of(DecodeEventType.CALL, DecodeEventType.PAGE);
    final var filter = new EventFilter("some name", types);

    // expect
    for (final var type : types) {
      final var decodeEvent = DecodeEvent.builder(0).eventType(type).build();
      assertThat(filter.canProcess(decodeEvent)).isTrue();
      assertThat(filter.passes(decodeEvent)).isTrue();
    }
  }

  @Test
  public void shouldNotProcessOrAllowOtherDecodeEventTypes() {
    // given
    final var filter = new EventFilter("some name", List.of(DecodeEventType.CALL, DecodeEventType.PAGE));
    final var decodeEvent = DecodeEvent.builder(0).eventType(DecodeEventType.ANNOUNCEMENT).build();

    // expect
    assertThat(filter.canProcess(decodeEvent)).isFalse();
    assertThat(filter.passes(decodeEvent)).isFalse();
  }

  @Test
  public void shouldFilterOutSpecifiedDecodeEventTypesWhenDisabled() {
    // given
    final var types = List.of(DecodeEventType.CALL, DecodeEventType.PAGE);
    final var filter = new EventFilter("some name", types);

    // when
    filter.setEnabled(false);

    // then
    for (final var type : types) {
      final var decodeEvent = DecodeEvent.builder(0).eventType(type).build();
      assertThat(filter.canProcess(decodeEvent)).isTrue();
      assertThat(filter.passes(decodeEvent)).isFalse();
    }
  }

  @Test
  public void shouldAllowFilteringOutBySpecificDecodeEventType() {
    // given
    final var filter = new EventFilter("some name", List.of(DecodeEventType.CALL, DecodeEventType.PAGE));
    final var decodeEvent = DecodeEvent.builder(0).eventType(DecodeEventType.CALL).build();

    // when
    filter.getFilterElements().stream()
        .filter(f -> f.getElement().equals(decodeEvent.getEventType()))
        .forEach(f -> f.setEnabled(false));

    // then
    assertThat(filter.canProcess(decodeEvent)).isTrue();
    assertThat(filter.passes(decodeEvent)).isFalse();
  }
}

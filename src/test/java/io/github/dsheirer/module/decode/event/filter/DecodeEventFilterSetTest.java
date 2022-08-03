package io.github.dsheirer.module.decode.event.filter;

import com.google.common.collect.Sets;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class DecodeEventFilterSetTest {

  @Test
  public void shouldContainDisjunctiveFilters() {
    // given
    final var filters = new DecodeEventFilterSet().getFilters();

    for (final var filter1 : filters) {
      for (final var filter2 : filters) {
        if (filter1 != filter2) {
          // when
          final var intersection = Sets.intersection(
              Set.copyOf(((EventFilter) filter1).getFilterElements()),
              Set.copyOf(((EventFilter) filter2).getFilterElements())
          );

          // then
          assertThat(intersection)
              .as("Decode event filters must be disjoint")
              .isEmpty();
        }
      }
    }
  }

  @Test
  public void shouldCoverDecodeEventsWithAllTypes() {
    // given
    final var types = DecodeEventType.values();
    final var filters = new DecodeEventFilterSet().getFilters();

    // when
    for (final var type : types) {
      final var capableFilters =
          filters.stream().filter(f -> {
            final var someDecodeEvent = DecodeEvent.builder(0).eventType(type).build();
            return f.canProcess(someDecodeEvent);
          });

      // then
      assertThat(capableFilters)
          .as("Decode event with type " + type + " must be handled by a filter")
          .isNotEmpty();
    }
  }

  @Test
  public void shouldHandleDecodeEventsWithoutType() {
    // given
    final var filterSet = new DecodeEventFilterSet();
    final var decodeEventWithoutType = DecodeEvent.builder(0).build();

    // expect
    assertThat(filterSet.canProcess(decodeEventWithoutType))
        .as("Decode event without type must be handled")
        .isTrue();
  }

  @Test
  public void shouldPassAllDecodeEventTypesByDefault() {
    // given
    final var filterSet = new DecodeEventFilterSet();

    // expect
    for (final var type : DecodeEventType.values()) {
      final var someDecodeEvent = DecodeEvent.builder(0).eventType(type).build();
      assertThat(filterSet.passes(someDecodeEvent)).isTrue();
    }
  }

  @Test
  public void shouldAllowFilteringOutEverything() {
    // given
    final var filterSet = new DecodeEventFilterSet();
    final var someDecodeEvent = DecodeEvent.builder(0).eventType(DecodeEventType.CALL).build();

    // when
    filterSet.setEnabled(false);

    // then
    assertThat(filterSet.passes(someDecodeEvent)).isFalse();
  }

  @Test
  public void shouldAllowFilteringOutByDecodeEventTypeGroup() {
    // given
    final var filterSet = new DecodeEventFilterSet();
    final var someDecodeEvent = DecodeEvent.builder(0).eventType(DecodeEventType.CALL).build();

    // when
    filterSet.getFilters().stream()
        .filter(f -> f.canProcess(someDecodeEvent))
        .forEach(f -> f.setEnabled(false));

    // then
    assertThat(filterSet.passes(someDecodeEvent)).isFalse();
  }
}

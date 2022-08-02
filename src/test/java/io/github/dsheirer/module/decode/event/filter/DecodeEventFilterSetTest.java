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
    final var filters = new DecodeEventFilterSet().getFilters();
    final var someDecodeEvent = DecodeEvent.builder(0).build();

    // expect
    assertThat(filters)
        .filteredOn(f -> f.canProcess(someDecodeEvent))
        .as("Decode event without type must be handled")
        .isNotEmpty();
  }
}

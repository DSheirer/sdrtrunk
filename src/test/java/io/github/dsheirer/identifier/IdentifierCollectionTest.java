package io.github.dsheirer.identifier;

import io.github.dsheirer.CommonFixtures;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IdentifierCollectionTest implements CommonFixtures {

  @Test
  public void shouldImplementHashCodeAndEquals() {
    // expect
    assertThat(someIdentifiers()).isEqualTo(someIdentifiers());
    assertThat(someIdentifiers().hashCode()).isEqualTo(someIdentifiers().hashCode());
  }

  @Test
  public void shouldSetIdentifiersAndTimeslot() {
    // given
    final var identifiers = someIdentifiers().mIdentifiers;
    final var collection = new IdentifierCollection(identifiers, 8);

    // expect
    final var softly = new SoftAssertions();
    softly.assertThat(collection.mIdentifiers).isEqualTo(identifiers);
    softly.assertThat(collection.getTimeslot()).isEqualTo(8);
    softly.assertAll();
  }
}

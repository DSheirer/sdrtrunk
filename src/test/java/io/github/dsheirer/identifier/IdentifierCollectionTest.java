package io.github.dsheirer.identifier;

import io.github.dsheirer.CommonFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IdentifierCollectionTest implements CommonFixtures {

  @Test
  public void shouldImplementHashCodeAndEquals() {
    // expect
    assertThat(someIdentifiers()).isEqualTo(someIdentifiers());
    assertThat(someIdentifiers().hashCode()).isEqualTo(someIdentifiers().hashCode());
  }
}

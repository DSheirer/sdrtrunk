/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

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

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

package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.CommonFixtures;
import io.github.dsheirer.protocol.Protocol;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class DecodeEventTest implements CommonFixtures {

  @Test
  public void builderShouldHonorSetValues() {
    // given
    final var decodeEvent = DecodeEvent.builder(123L)
        .channel(someChannel())
        .eventType(DecodeEventType.CALL)
        .eventDescription("some description")
        .identifiers(someIdentifiers())
        .details("some details")
        .protocol(Protocol.DMR)
        .timeslot(2)
        .build();

    // expect
    final var softly = new SoftAssertions();
    softly.assertThat(decodeEvent.getTimeStart()).isEqualTo(123L);
    softly.assertThat(decodeEvent.getChannelDescriptor()).isEqualTo(someChannel());
    softly.assertThat(decodeEvent.getEventType()).isEqualTo(DecodeEventType.CALL);
    softly.assertThat(decodeEvent.getEventDescription()).isEqualTo("some description");
    softly.assertThat(decodeEvent.getIdentifierCollection()).isEqualTo(someIdentifiers());
    softly.assertThat(decodeEvent.getDetails()).isEqualTo("some details");
    softly.assertThat(decodeEvent.getProtocol()).isEqualTo(Protocol.DMR);
    softly.assertThat(decodeEvent.getTimeslot()).isEqualTo(2);
    softly.assertAll();
  }
}

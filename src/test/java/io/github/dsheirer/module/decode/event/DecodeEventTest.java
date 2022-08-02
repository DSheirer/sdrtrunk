package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.CommonFixtures;
import io.github.dsheirer.protocol.Protocol;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class DecodeEventTest implements CommonFixtures {

  @Test
  public void builderShouldHonorSetValues() {
    final var decodeEvent = DecodeEvent.builder(123L)
        .channel(someChannel())
        .eventType(DecodeEventType.CALL)
        .eventDescription("some description")
        .identifiers(someIdentifiers())
        .details("some details")
        .protocol(Protocol.DMR)
        .timeslot(2)
        .build();

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

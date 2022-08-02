package io.github.dsheirer;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.decode.dmr.channel.DMRLogicalChannel;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;

import java.util.List;

public interface CommonFixtures {

  default IChannelDescriptor someChannel() {
    return new DMRLogicalChannel(1, 1);
  }

  default IdentifierCollection someIdentifiers() {
    return new IdentifierCollection(List.of(
        new DMRTalkgroup(1, Role.FROM),
        new DMRTalkgroup(2, Role.TO)
    ));
  }
}

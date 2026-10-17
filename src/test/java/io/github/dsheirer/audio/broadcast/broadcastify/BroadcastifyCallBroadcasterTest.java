package io.github.dsheirer.audio.broadcast.broadcastify;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.audio.broadcast.AudioRecording;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNTalkerAliasIdentifier;
import io.github.dsheirer.protocol.Protocol;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BroadcastifyCallBroadcasterTest
{
    @Test
    void resolvesOverTheAirAndStoredPlaylistAliasesIndependently()
    {
        AliasModel aliasModel = new AliasModel();
        Alias stored = new Alias("Stored Unit Name");
        stored.setAliasListName("Metro");
        stored.addAliasID(new Radio(Protocol.DMR, 1201));
        aliasModel.addAlias(stored);

        List<Identifier> identifiers = List.of(AliasListConfigurationIdentifier.create("Metro"),
            DMRRadio.createFrom(1201), new NXDNTalkerAliasIdentifier("OTA Unit Name"));
        AudioRecording recording = new AudioRecording(Path.of("call.mp3"), List.of(),
            new IdentifierCollection(identifiers), 1L, 1_000L);
        BroadcastifyCallBroadcaster broadcaster = new BroadcastifyCallBroadcaster(
            new BroadcastifyCallConfiguration(), null, null, aliasModel);

        assertEquals("OTA Unit Name", BroadcastifyCallBroadcaster.getOverTheAirFromAlias(recording));
        assertEquals("Stored Unit Name", broadcaster.getPlaylistFromAlias(recording));
    }
}

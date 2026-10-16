package io.github.dsheirer.module.decode.analog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.decoder.DecoderLogicalChannelNameIdentifier;
import io.github.dsheirer.module.decode.am.AMDecoderState;
import io.github.dsheirer.module.decode.am.DecodeConfigAM;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.decode.nbfm.NBFMDecoderState;
import io.github.dsheirer.protocol.Protocol;
import org.junit.jupiter.api.Test;

class AnalogDecoderChannelNameTest
{
    @Test
    void nbfmPublishesDecoderChannelNameForNowPlaying()
    {
        assertChannelName(new NBFMDecoderState("County Fire", new DecodeConfigNBFM()),
            "County Fire", Protocol.NBFM);
    }

    @Test
    void amPublishesDecoderChannelNameForNowPlaying()
    {
        assertChannelName(new AMDecoderState("Airband Tower", new DecodeConfigAM()),
            "Airband Tower", Protocol.AM);
    }

    private static void assertChannelName(AnalogDecoderState state, String expectedName, Protocol expectedProtocol)
    {
        state.start();
        Identifier identifier = state.getIdentifierCollection().getIdentifier(
            IdentifierClass.DECODER, Form.CHANNEL_NAME, Role.BROADCAST);
        DecoderLogicalChannelNameIdentifier channelName =
            assertInstanceOf(DecoderLogicalChannelNameIdentifier.class, identifier);
        assertEquals(expectedName, channelName.getValue());
        assertEquals(expectedProtocol, channelName.getProtocol());
    }
}

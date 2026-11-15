/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.decode.p25.P25TrafficChannelManager;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCGroupVoiceChannelUser;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.HarrisTalkerAliasAssembler;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisTalkerAliasBase;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisTalkerAliasBlock1;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisTalkerAliasBlock2;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisTalkerAliasBlock3;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisTalkerAliasBlock4;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisTalkerAliasComplete;
import java.util.ArrayList;
import java.util.List;
import io.github.dsheirer.module.decode.p25.phase1.message.tdu.TDUMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests that P25 Phase 1 talker aliases are assigned to the right radio, including L3Harris talker aliases that
 * complete after the call ends, and that the FROM radio and talker alias don't carry over to the next call.
 */
public class P25P1DecoderStateTalkerAliasTest
{
    private static final int NAC = 0x293;
    private static final int RADIO_A = 1001;
    private static final int RADIO_B = 2002;
    private static final int TALKGROUP = 100;
    private P25P1DecoderState mDecoderState;
    private P25TrafficChannelManager mTrafficChannelManager;
    private final MutableIdentifierCollection mIdentifiers = new MutableIdentifierCollection();
    private final List<IDecodeEvent> mEvents = new ArrayList<>();
    private long mTimestamp = 1_700_000_000_000L;

    @BeforeEach
    public void setup()
    {
        Channel channel = new Channel("Test");
        channel.setDecodeConfiguration(new DecodeConfigP25Phase1());
        mTrafficChannelManager = new P25TrafficChannelManager(channel);
        mTrafficChannelManager.addDecodeEventListener(event -> {
            if(!mEvents.contains(event))
            {
                mEvents.add(event);
            }
        });
        mDecoderState = new P25P1DecoderState(channel, mTrafficChannelManager);
        mDecoderState.setDecoderStateListener(event -> {});
        //Mirrors the decoder state identifier collection, the same as the channel metadata and audio modules
        mDecoderState.setIdentifierUpdateListener(mIdentifiers);
        mDecoderState.start();
    }

    /**
     * Call from the radio: group voice channel user link control messages, as carried in the LDU1s.
     */
    private void talk(int radio)
    {
        //A call carries the channel user link control in each LDU1
        for(int x = 0; x < 3; x++)
        {
            CorrectedBinaryMessage message = new CorrectedBinaryMessage(72);
            message.load(32, 16, TALKGROUP); //Group address
            message.load(48, 24, radio);     //Source address
            ChannelUser lcw = new ChannelUser(message, mTimestamp);
            lcw.setValid(true);
            mDecoderState.receive(lcw);
            mTimestamp += 360;
        }
    }

    private void endCall()
    {
        mDecoderState.receive(new TDUMessage(new CorrectedBinaryMessage(28), NAC, mTimestamp));
        mTimestamp += 500;
    }

    /**
     * Completed L3Harris talker alias, reassembled from link control blocks that carry 7 characters each.
     */
    private void harrisTalkerAlias(String alias)
    {
        mDecoderState.receive(assemble(alias, mTimestamp));
        mTimestamp += 180;
    }

    /**
     * Creates the L3Harris talker alias blocks for the alias and reassembles them with the assembler.
     * @param alias of up to 28 characters
     * @param timestamp for the blocks
     * @return reassembled talker alias
     */
    static LCHarrisTalkerAliasComplete assemble(String alias, long timestamp)
    {
        HarrisTalkerAliasAssembler assembler = new HarrisTalkerAliasAssembler();
        LCHarrisTalkerAliasComplete complete = null;
        int blocks = Math.max(2, (alias.length() + 6) / 7);

        for(int block = 0; block < blocks; block++)
        {
            CorrectedBinaryMessage message = new CorrectedBinaryMessage(72);
            message.load(2, 6, 50 + block); //L3Harris talker alias block opcode
            message.load(8, 8, 164);        //L3Harris vendor

            for(int x = 0; x < 7 && block * 7 + x < alias.length(); x++)
            {
                message.load(16 + x * 8, 8, alias.charAt(block * 7 + x));
            }

            LCHarrisTalkerAliasBase lcw = switch(block)
            {
                case 0 -> new LCHarrisTalkerAliasBlock1(message);
                case 1 -> new LCHarrisTalkerAliasBlock2(message);
                case 2 -> new LCHarrisTalkerAliasBlock3(message);
                default -> new LCHarrisTalkerAliasBlock4(message);
            };
            lcw.setValid(true);
            LCHarrisTalkerAliasComplete assembled = assembler.process(lcw, timestamp);

            if(assembled != null)
            {
                complete = assembled;
            }
        }

        return complete;
    }

    private Identifier getFromRadio()
    {
        return mIdentifiers.getIdentifier(IdentifierClass.USER, Form.RADIO, Role.FROM);
    }

    private String getTalkerAlias()
    {
        Identifier alias = mIdentifiers.getIdentifier(IdentifierClass.USER, Form.TALKER_ALIAS, Role.FROM);
        return alias != null ? alias.getValue().toString() : null;
    }

    /**
     * Group voice channel user link control as a message, the same way that the decoder state receives the
     * reassembled link control messages.
     */
    private static class ChannelUser extends LCGroupVoiceChannelUser implements IMessage
    {
        private final long mMessageTimestamp;

        ChannelUser(CorrectedBinaryMessage message, long timestamp)
        {
            super(message, timestamp, false);
            mMessageTimestamp = timestamp;
        }

        @Override
        public long getTimestamp()
        {
            return mMessageTimestamp;
        }

        @Override
        public Protocol getProtocol()
        {
            return Protocol.APCO25;
        }

        @Override
        public int getTimeslot()
        {
            return P25P1Message.TIMESLOT_1;
        }
    }

    @Test
    public void harrisAliasAfterCallEndIsStoredForThatCallsTalker()
    {
        talk(RADIO_A);
        assertEquals(RADIO_A, getFromRadio().getValue());
        endCall();
        assertNull(getFromRadio(), "FROM radio is cleared at the end of the call");

        //Alias completes after the call ended
        harrisTalkerAlias("ENGINE 7");
        assertNull(getTalkerAlias(), "No call in progress, so the alias isn't shown");

        //A different talker doesn't get the alias
        talk(RADIO_B);
        assertEquals(RADIO_B, getFromRadio().getValue());
        assertNull(getTalkerAlias());
        endCall();

        //The talker's next call shows the alias
        talk(RADIO_A);
        assertEquals("ENGINE 7", getTalkerAlias());
        endCall();
        assertNull(getTalkerAlias(), "Talker alias is cleared at the end of the call");
    }

    @Test
    public void harrisAliasDuringCallIsShownAndStored()
    {
        talk(RADIO_A);
        harrisTalkerAlias("ENGINE 7");
        assertEquals("ENGINE 7", getTalkerAlias());
        endCall();

        talk(RADIO_A);
        assertEquals("ENGINE 7", getTalkerAlias());
    }

    @Test
    public void aliasIsReplacedWhenTheTalkerChangesWithoutACallEnd()
    {
        talk(RADIO_A);
        harrisTalkerAlias("ENGINE 7");
        talk(RADIO_B);
        assertEquals(RADIO_B, getFromRadio().getValue());
        assertNull(getTalkerAlias());
    }

    private static String getEventTalkerAlias(IDecodeEvent event)
    {
        Identifier alias = event.getIdentifierCollection().getIdentifier(IdentifierClass.USER, Form.TALKER_ALIAS,
            Role.FROM);
        return alias != null ? alias.getValue().toString() : null;
    }

    private static Identifier getEventRadio(IDecodeEvent event)
    {
        return event.getIdentifierCollection().getIdentifier(IdentifierClass.USER, Form.RADIO, Role.FROM);
    }

    @Test
    public void harrisAliasAfterCallEndIsAddedToTheEndedCallEvent()
    {
        talk(RADIO_A);
        endCall();
        talk(RADIO_B);
        endCall();
        assertEquals(2, mEvents.size());
        long duration = mEvents.get(1).getDuration();

        //Alias completes after radio B's call ended
        harrisTalkerAlias("ENGINE 7");
        assertEquals(2, mEvents.size(), "No new event");
        assertNull(getEventTalkerAlias(mEvents.get(0)), "Radio A's call event isn't changed");
        assertEquals(RADIO_B, getEventRadio(mEvents.get(1)).getValue());
        assertEquals("ENGINE 7", getEventTalkerAlias(mEvents.get(1)));
        assertEquals(duration, mEvents.get(1).getDuration(), "Event duration isn't changed");
    }

    @Test
    public void spaceAtAFragmentBoundaryIsKept()
    {
        //Characters 7 and 8 are spaces, at the end of block 1 and the start of block 2
        assertEquals("ENGINE  7", assemble("ENGINE  7", 0).getTalkerAlias().getValue());
        assertEquals("ENGINE 7", assemble("ENGINE 7", 0).getTalkerAlias().getValue());
        assertEquals("BATTALION CHIEF 1", assemble("BATTALION CHIEF 1", 0).getTalkerAlias().getValue());
        //Trailing padding is removed
        assertEquals("A", assemble("A", 0).getTalkerAlias().getValue());
    }
}

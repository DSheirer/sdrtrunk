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

package io.github.dsheirer.module.decode.cw;

import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.pager.PagerSignalGenerator;
import io.github.dsheirer.module.decode.pager.flex.FlexMode;
import io.github.dsheirer.protocol.Protocol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the Morse code (CW) station ID decoder with synthesized audio.
 */
public class CWDecoderTest
{
    private static List<CWMessage> decode(float[] samples)
    {
        CWDecoder decoder = new CWDecoder();
        List<CWMessage> messages = new ArrayList<>();
        decoder.setMessageListener(message -> messages.add((CWMessage)message));
        decoder.start();

        for(int x = 0; x < samples.length; x += 512)
        {
            decoder.receive(Arrays.copyOfRange(samples, x, Math.min(samples.length, x + 512)));
        }

        decoder.stop();
        return messages;
    }

    @Test
    public void speedsAndTones()
    {
        for(int wpm: new int[]{10, 15, 25, 46, 60})
        {
            for(int tone: new int[]{600, 1000, 1200, 1800})
            {
                float[] samples = new CWSignalGenerator().silence(0.5).morse("WQUH413", wpm, tone).silence(1.5)
                    .getSamples();
                List<CWMessage> messages = decode(samples);
                String label = wpm + " WPM " + tone + " Hz";
                assertEquals(1, messages.size(), label + ": " + messages);
                assertEquals("WQUH413", messages.get(0).getText(), label);
                assertEquals(tone, messages.get(0).getToneFrequency(), 15, label);
                assertEquals(wpm, messages.get(0).getWordsPerMinute(), Math.max(2, wpm * 0.1), label);
            }
        }
    }

    @Test
    public void wordsAndCallSign()
    {
        float[] samples = new CWSignalGenerator().silence(0.5).morse("DE WQUH413/R", 20, 800).silence(1.5)
            .getSamples();
        List<CWMessage> messages = decode(samples);
        assertEquals(1, messages.size(), "Messages: " + messages);
        assertEquals("DE WQUH413/R", messages.get(0).getText());
        assertEquals("WQUH413/R", messages.get(0).getCallSign());
    }

    @Test
    public void guardToneSweepAndNoise()
    {
        //Like a tone remote controlled transmitter: guard tone, a sweep before the ID, then two IDs in separate
        //transmissions
        float[] samples = new CWSignalGenerator().noise(0.05).guardTone(0.1).silence(0.3).sweep(2100, 700, 0.5)
            .silence(0.4).morse("WQUH413", 46, 1200).silence(2.0).guardTone(0.0).silence(1.0).guardTone(0.1)
            .morse("WQUH413", 46, 1200).silence(1.5).getSamples();
        List<CWMessage> messages = decode(samples);
        assertEquals(2, messages.size(), "Messages: " + messages);
        assertEquals("WQUH413", messages.get(0).getText());
        assertEquals("WQUH413", messages.get(1).getText());
    }

    @Test
    public void squelchCloseDecodesImmediately()
    {
        CWDecoder decoder = new CWDecoder();
        List<IMessage> messages = new ArrayList<>();
        decoder.setMessageListener(messages::add);
        decoder.receive(new CWSignalGenerator().silence(0.2).morse("WQUH413", 30, 1000).silence(0.1).getSamples());
        assertTrue(messages.isEmpty());

        //NBFM decoder reports idle when the squelch closes
        decoder.getDecoderStateListener().receive(new DecoderStateEvent(this, DecoderStateEvent.Event.CONTINUATION,
            State.IDLE));
        assertEquals(1, messages.size());
        assertEquals("WQUH413", ((CWMessage)messages.get(0)).getText());
    }

    @Test
    public void noFalseDecodes()
    {
        assertTrue(decode(new CWSignalGenerator().noise(0.2).silence(60.0).getSamples()).isEmpty(), "Noise");
        assertTrue(decode(new CWSignalGenerator().guardTone(0.3).silence(10.0).getSamples()).isEmpty(), "Guard tone");
        assertTrue(decode(new CWSignalGenerator().sweep(300, 2000, 3.0).getSamples()).isEmpty(), "Sweep");

        //A steady tone (e.g. a tone remote control function tone or an alert tone) is not Morse code
        assertTrue(decode(new CWSignalGenerator().silence(0.2).morse("T", 1, 1000).silence(1.5).getSamples())
            .isEmpty(), "Steady tone");

        //Paging signals
        List<List<Integer>> pages = List.of(PagerSignalGenerator.pocsagAlphaPage(1999998, 3, "Test page"));
        List<Integer> bits = PagerSignalGenerator.pocsagTransmission(pages, List.of(1999998));
        assertTrue(decode(new PagerSignalGenerator().silence(0.5).afsk(bits, 512, 2200, 1200)
            .afsk(bits, 1200, 2200, 1200).silence(1.5).getSamples()).isEmpty(), "POCSAG AFSK");
        assertTrue(decode(new PagerSignalGenerator().silence(0.5)
            .symbols(PagerSignalGenerator.bitsLowIsOne(bits), 512).silence(1.5).getSamples()).isEmpty(),
            "POCSAG");
        PagerSignalGenerator flex = new PagerSignalGenerator().silence(0.5);
        flex.flexFrame(FlexMode.FLEX_1600_2, 1, 1, new int[4][]);
        assertTrue(decode(flex.silence(1.5).getSamples()).isEmpty(), "FLEX");
    }

    @Test
    public void parserRejectsInconsistentTiming()
    {
        //Element durations that don't cluster at 1 and 3 units
        List<MorseCodeParser.KeyingRun> runs = new ArrayList<>();

        for(int x = 0; x < 10; x++)
        {
            runs.add(new MorseCodeParser.KeyingRun(true, 40 + x * 25));
            runs.add(new MorseCodeParser.KeyingRun(false, 40));
        }

        runs.removeLast();
        assertNull(MorseCodeParser.parse(runs));
    }

    @Test
    public void decoderStateStationIdEvent()
    {
        CWDecoderState state = new CWDecoderState();
        List<IDecodeEvent> events = new ArrayList<>();
        List<DecoderStateEvent> stateEvents = new ArrayList<>();
        state.addDecodeEventListener(events::add);
        state.setDecoderStateListener(stateEvents::add);

        state.receive(new CWMessage(0, "WQUH413", 1200, 46));

        assertEquals(1, events.size());
        assertEquals(DecodeEventType.STATION_ID, events.get(0).getEventType());
        assertEquals(Protocol.CW, events.get(0).getProtocol());
        assertTrue(events.get(0).getDetails().contains("WQUH413"));
        List<Identifier> from = events.get(0).getIdentifierCollection().getIdentifiers(Role.FROM);
        assertEquals(1, from.size());
        assertEquals("WQUH413", from.get(0).getValue());
        assertEquals(Form.SITE, from.get(0).getForm());
        assertTrue(stateEvents.isEmpty(), "CW decoder state does not change the channel state");
        assertTrue(state.getActivitySummary().contains("WQUH413"));
    }
}

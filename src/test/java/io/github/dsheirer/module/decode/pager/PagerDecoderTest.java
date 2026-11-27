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

package io.github.dsheirer.module.decode.pager;

import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.pager.flex.FlexMessage;
import io.github.dsheirer.module.decode.pager.flex.FlexMode;
import io.github.dsheirer.module.decode.pager.pocsag.PocsagDecoder;
import io.github.dsheirer.module.decode.pager.pocsag.PocsagMessage;
import io.github.dsheirer.protocol.Protocol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests of the pager decoder with synthesized POCSAG and FLEX discriminator audio.
 *
 * These tests verify the decoder against the encoder in {@link PagerSignalGenerator}, which follows the same
 * understanding of the protocols as the decoder, so they verify the DSP, framing and parsing but not conformance with
 * real paging transmissions.
 */
public class PagerDecoderTest
{
    private static final String ALPHA_TEXT = "Engine 7 respond to 123 Main St, structure fire. Cross: Oak Ave";

    /**
     * Decodes the audio samples in 512 sample buffers and returns the decoded pager messages.
     */
    private static List<PagerMessage> decode(float[] samples)
    {
        return decodeAll(samples).stream().filter(PagerMessage.class::isInstance).map(PagerMessage.class::cast)
            .toList();
    }

    /**
     * Decodes the audio samples in 512 sample buffers and returns all decoded messages, including sync (idle)
     * messages.
     */
    private static List<IMessage> decodeAll(float[] samples)
    {
        PagerDecoder decoder = new PagerDecoder();
        List<IMessage> messages = new ArrayList<>();
        decoder.setMessageListener(messages::add);
        decoder.start();

        for(int x = 0; x < samples.length; x += 512)
        {
            decoder.receive(Arrays.copyOfRange(samples, x, Math.min(samples.length, x + 512)));
        }

        decoder.stop();
        return messages;
    }

    private static List<PagerIdleMessage> idleMessages(List<IMessage> messages)
    {
        return messages.stream().filter(PagerIdleMessage.class::isInstance).map(PagerIdleMessage.class::cast)
            .toList();
    }

    @Test
    public void pocsagAllRatesAndPolarities()
    {
        for(int bitRate: new int[]{512, 1200, 2400})
        {
            pocsagAlphaAndNumeric(bitRate, false);
            pocsagAlphaAndNumeric(bitRate, true);
        }
    }

    private void pocsagAlphaAndNumeric(int bitRate, boolean inverted)
    {
        int alphaCapcode = 1234567;
        int numericCapcode = 2001234;
        int toneCapcode = 8;

        List<List<Integer>> pages = new ArrayList<>();
        pages.add(PagerSignalGenerator.pocsagAlphaPage(alphaCapcode, 3, ALPHA_TEXT));
        pages.add(PagerSignalGenerator.pocsagNumericPage(numericCapcode, 0, "555-0123"));
        pages.add(List.of(PagerSignalGenerator.pocsagNumericPage(toneCapcode, 1, "").get(0)));

        List<Integer> bits = PagerSignalGenerator.pocsagTransmission(pages,
            List.of(alphaCapcode, numericCapcode, toneCapcode));

        float[] samples = new PagerSignalGenerator()
            .inverted(inverted)
            .silence(0.5)
            .symbols(PagerSignalGenerator.bitsLowIsOne(bits), bitRate)
            .silence(0.5)
            .getSamples();

        List<PagerMessage> messages = decode(samples);
        List<PocsagMessage> pocsag = messages.stream().filter(PocsagMessage.class::isInstance)
            .map(PocsagMessage.class::cast).toList();

        assertEquals(3, pocsag.size(), bitRate + (inverted ? " inverted" : "") + " POCSAG pages decoded: " + messages);

        assertEquals(alphaCapcode, pocsag.get(0).getCapcode());
        assertEquals(3, pocsag.get(0).getFunction());
        assertEquals(PagerMessageType.ALPHANUMERIC, pocsag.get(0).getMessageType());
        assertEquals(ALPHA_TEXT, pocsag.get(0).getContent());
        assertEquals(bitRate, pocsag.get(0).getBitRate());
        assertTrue(pocsag.get(0).isComplete());

        assertEquals(numericCapcode, pocsag.get(1).getCapcode());
        assertEquals(PagerMessageType.NUMERIC, pocsag.get(1).getMessageType());
        assertEquals("555-0123", pocsag.get(1).getContent());

        assertEquals(toneCapcode, pocsag.get(2).getCapcode());
        assertEquals(PagerMessageType.TONE, pocsag.get(2).getMessageType());
        assertEquals(1, pocsag.get(2).getFunction());
    }

    @Test
    public void pocsagAfsk()
    {
        for(int bitRate: new int[]{512, 1200})
        {
            for(boolean swapTones: new boolean[]{false, true})
            {
                int alphaCapcode = 1999998;
                int numericCapcode = 2001234;
                List<List<Integer>> pages = new ArrayList<>();
                pages.add(PagerSignalGenerator.pocsagAlphaPage(alphaCapcode, 3, ALPHA_TEXT));
                pages.add(PagerSignalGenerator.pocsagNumericPage(numericCapcode, 0, "555-0123"));
                List<Integer> bits = PagerSignalGenerator.pocsagTransmission(pages, List.of(alphaCapcode, numericCapcode));

                float[] samples = new PagerSignalGenerator().noise(0.1)
                    .silence(0.5)
                    .afsk(bits, bitRate, swapTones ? 1200 : 2200, swapTones ? 2200 : 1200)
                    .silence(0.5)
                    .getSamples();

                String label = "AFSK " + bitRate + (swapTones ? " swapped tones" : "");
                List<PagerMessage> messages = decode(samples);
                assertEquals(2, messages.size(), label + " pages decoded: " + messages);

                PocsagMessage alpha = (PocsagMessage)messages.get(0);
                assertTrue(alpha.isAfsk(), label);
                assertEquals(bitRate, alpha.getBitRate(), label);
                assertEquals(alphaCapcode, alpha.getCapcode(), label);
                assertEquals(ALPHA_TEXT, alpha.getContent(), label);

                assertEquals(numericCapcode, messages.get(1).getCapcode(), label);
                assertEquals("555-0123", messages.get(1).getContent(), label);
            }
        }
    }

    @Test
    public void pocsagUnreadableContentIsBinary()
    {
        //Message codewords from a live AFSK page whose content is neither numeric nor text (likely encrypted)
        PocsagMessage message = io.github.dsheirer.module.decode.pager.pocsag.PocsagMessageParser.parse(0, 1999998, 0,
            List.of(0xE82AE, 0x895F2, 0x2CFB4, 0x3FA2F, 0x08EA3), 512, true);
        assertEquals(PagerMessageType.BINARY, message.getMessageType());
        assertEquals("E82AE 895F2 2CFB4 3FA2F 08EA3", message.getContent());
    }

    @Test
    public void pocsagCorrectsBitErrors()
    {
        int capcode = 1234567;
        List<Integer> page = new ArrayList<>(PagerSignalGenerator.pocsagAlphaPage(capcode, 3, "Test message"));

        //Two bit errors in the address and one in a message codeword
        page.set(0, page.get(0) ^ 0x00100400);
        page.set(1, page.get(1) ^ 0x00000800);

        List<Integer> bits = PagerSignalGenerator.pocsagTransmission(List.of(page), List.of(capcode));
        float[] samples = new PagerSignalGenerator().silence(0.2)
            .symbols(PagerSignalGenerator.bitsLowIsOne(bits), 1200).silence(0.2).getSamples();

        List<PagerMessage> messages = decode(samples);
        assertEquals(1, messages.size(), "Messages: " + messages);
        assertEquals(capcode, messages.get(0).getCapcode());
        assertEquals("Test message", messages.get(0).getContent());
    }

    @Test
    public void noiseProducesNoMessages()
    {
        float[] samples = new PagerSignalGenerator().silence(60.0).getSamples();
        List<IMessage> messages = decodeAll(samples);
        assertTrue(messages.isEmpty(), "Messages (including idle) decoded from noise: " + messages);
    }

    @Test
    public void pocsagIdleMessagePerBatchSync()
    {
        //Two pages spanning 2 batches, sent as direct FSK at 1200 and as AFSK at 512
        List<List<Integer>> pages = new ArrayList<>();
        pages.add(PagerSignalGenerator.pocsagAlphaPage(1999998, 3, ALPHA_TEXT));
        pages.add(PagerSignalGenerator.pocsagNumericPage(2001234, 0, "555-0123"));
        List<Integer> bits = PagerSignalGenerator.pocsagTransmission(pages, List.of(1999998, 2001234));
        int batches = (bits.size() - 576) / (17 * 32);

        List<IMessage> messages = decodeAll(new PagerSignalGenerator().silence(0.3)
            .symbols(PagerSignalGenerator.bitsLowIsOne(bits), 1200).silence(0.3)
            .afsk(bits, 512, 2200, 1200).silence(0.3).getSamples());

        List<PagerIdleMessage> idle = idleMessages(messages);
        assertEquals(2 * batches, idle.size(), "Idle messages: " + idle);

        for(int x = 0; x < batches; x++)
        {
            assertEquals(Protocol.POCSAG, idle.get(x).getProtocol());
            assertEquals(1200, idle.get(x).getBitRate());
            assertFalse(idle.get(x).isAfsk());
            assertEquals("POCSAG 1200 IDLE", idle.get(x).toString());

            PagerIdleMessage afsk = idle.get(batches + x);
            assertEquals(512, afsk.getBitRate());
            assertTrue(afsk.isAfsk());
            assertEquals("POCSAG 512 AFSK IDLE", afsk.toString());
        }

        //The idle message for a batch precedes the pages completed within that batch
        assertTrue(messages.get(0) instanceof PagerIdleMessage);
        assertEquals(4, decode(new PagerSignalGenerator().silence(0.3)
            .symbols(PagerSignalGenerator.bitsLowIsOne(bits), 1200).silence(0.3)
            .afsk(bits, 512, 2200, 1200).silence(0.3).getSamples()).size());
    }

    @Test
    public void flexIdleMessagePerFrameSync()
    {
        PagerSignalGenerator generator = new PagerSignalGenerator().silence(0.3);
        generator.flexFrame(FlexMode.FLEX_6400_4, 5, 64, new int[4][]);
        generator.flexFrame(FlexMode.FLEX_6400_4, 5, 65, new int[4][]);
        generator.silence(0.3);

        List<IMessage> messages = decodeAll(generator.getSamples());
        List<PagerIdleMessage> idle = idleMessages(messages);
        assertEquals(2, idle.size(), "Messages: " + messages);
        assertEquals(messages.size(), idle.size(), "Only idle messages expected: " + messages);
        assertEquals(Protocol.FLEX, idle.get(0).getProtocol());
        assertEquals(6400, idle.get(0).getBitRate());
        assertEquals("FLEX 6400 IDLE MODE:6400/4 CYCLE:5 FRAME:64", idle.get(0).toString());
        assertEquals("FLEX 6400 IDLE MODE:6400/4 CYCLE:5 FRAME:65", idle.get(1).toString());
    }

    @Test
    public void messageFilterAcceptsIdleMessages()
    {
        PagerMessageFilter filter = new PagerMessageFilter();
        PagerIdleMessage idle = new PagerIdleMessage(0, Protocol.POCSAG, 512, true, null);
        assertTrue(filter.canProcess(idle));
        assertEquals(PagerMessageType.IDLE, filter.getKeyExtractor().apply(idle));
    }

    @Test
    public void flexAllModesAndPolarities()
    {
        for(FlexMode mode: new FlexMode[]{FlexMode.FLEX_1600_2, FlexMode.FLEX_3200_2, FlexMode.FLEX_3200_4,
            FlexMode.FLEX_6400_4})
        {
            flexFrames(mode, false);
            flexFrames(mode, true);
        }
    }

    private void flexFrames(FlexMode mode, boolean inverted)
    {
        int[][] phases = new int[4][];
        phases[0] = new PagerSignalGenerator.FlexPhase()
            .alpha(1500123, ALPHA_TEXT)
            .numeric(1500124, "8005551212")
            .tone(1500125)
            .build();

        if(mode.getPhaseCount() > 1)
        {
            //Second active phase: B for 3200/4, C for 3200/2
            int second = mode.getLevels() == 4 ? 1 : 2;
            phases[second] = new PagerSignalGenerator.FlexPhase().alpha(1600001, "Phase two page").build();
        }

        if(mode.getPhaseCount() == 4)
        {
            phases[2] = new PagerSignalGenerator.FlexPhase().alpha(1700001, "Phase C page").build();
            phases[3] = new PagerSignalGenerator.FlexPhase().numeric(1700002, "911").build();
        }

        PagerSignalGenerator generator = new PagerSignalGenerator().inverted(inverted).silence(0.3);
        generator.flexFrame(mode, 5, 64, phases);
        generator.flexFrame(mode, 5, 65, new int[][]{new PagerSignalGenerator.FlexPhase().alpha(1500123,
            "Second frame").build()});
        generator.silence(0.3);

        List<PagerMessage> messages = decode(generator.getSamples());
        List<FlexMessage> flex = messages.stream().filter(FlexMessage.class::isInstance)
            .map(FlexMessage.class::cast).toList();

        int expected = 3 + (mode.getPhaseCount() == 2 ? 1 : 0) + (mode.getPhaseCount() == 4 ? 3 : 0) + 1;
        assertEquals(expected, flex.size(), mode + (inverted ? " inverted" : "") + " FLEX pages decoded: " + messages);

        FlexMessage alpha = flex.get(0);
        assertEquals(1500123, alpha.getCapcode());
        assertEquals(PagerMessageType.ALPHANUMERIC, alpha.getMessageType());
        assertEquals(ALPHA_TEXT, alpha.getContent());
        assertEquals(mode, alpha.getMode());
        assertEquals(5, alpha.getCycle());
        assertEquals(64, alpha.getFrame());
        assertEquals('A', alpha.getPhase());
        assertTrue(alpha.isComplete());

        FlexMessage numeric = flex.get(1);
        assertEquals(1500124, numeric.getCapcode());
        assertEquals(PagerMessageType.NUMERIC, numeric.getMessageType());
        assertEquals("8005551212", numeric.getContent());

        FlexMessage tone = flex.get(2);
        assertEquals(1500125, tone.getCapcode());
        assertEquals(PagerMessageType.TONE, tone.getMessageType());

        if(mode.getPhaseCount() == 2)
        {
            assertEquals("Phase two page", flex.get(3).getContent());
            assertEquals(mode.getLevels() == 4 ? 'B' : 'C', flex.get(3).getPhase());
        }
        else if(mode.getPhaseCount() == 4)
        {
            assertEquals("Phase two page", flex.get(3).getContent());
            assertEquals('B', flex.get(3).getPhase());
            assertEquals("Phase C page", flex.get(4).getContent());
            assertEquals('C', flex.get(4).getPhase());
            assertEquals("911", flex.get(5).getContent());
            assertEquals('D', flex.get(5).getPhase());
        }

        FlexMessage last = flex.get(flex.size() - 1);
        assertEquals("Second frame", last.getContent());
        assertEquals(65, last.getFrame());
    }

    @Test
    public void decoderStateBroadcastsPageEventsWithoutStateChanges()
    {
        PagerDecoderState state = new PagerDecoderState();
        List<DecoderStateEvent> events = new ArrayList<>();
        List<IDecodeEvent> decodeEvents = new ArrayList<>();
        state.setDecoderStateListener(events::add);
        state.addDecodeEventListener(decodeEvents::add);

        IMessage page = new PocsagMessage(System.currentTimeMillis(), 1234567, 3, PagerMessageType.ALPHANUMERIC,
            "Hello", 1200, 0, false);
        state.receive(page);

        assertEquals(1, decodeEvents.size());
        assertTrue(decodeEvents.get(0).getDetails().contains("Hello"));
        assertEquals(Protocol.POCSAG, decodeEvents.get(0).getProtocol());

        //The NBFM decoder controls the channel state
        assertTrue(events.isEmpty(), "Pager decoder state broadcast state events: " + events);

        assertTrue(state.getActivitySummary().contains("1234567"));
    }

    @Test
    public void pocsagSyncAndIdleAreValidCodewords()
    {
        assertEquals(PocsagDecoder.SYNC_CODEWORD & 0xFFFFFFFFL, PocsagDecoder.correct(PocsagDecoder.SYNC_CODEWORD));
        assertEquals(PocsagDecoder.IDLE_CODEWORD & 0xFFFFFFFFL, PocsagDecoder.correct(PocsagDecoder.IDLE_CODEWORD));
        assertFalse(PocsagDecoder.correct(0x7A89C197 ^ 0x70000000) == (0x7A89C197 & 0xFFFFFFFFL));
    }
}

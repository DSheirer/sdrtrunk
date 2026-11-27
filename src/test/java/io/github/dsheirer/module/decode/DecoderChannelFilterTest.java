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
package io.github.dsheirer.module.decode;

import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.amTones;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.assertEquivalentAtFilterRate;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.assertFlagIgnoredWhenDecimating;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.assertUnflaggedEventRestoresFilter;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.bytes;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.dqpsk;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.floats;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.fmTones;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.fsk4;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.getInterfererOffset;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.impair;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.of;
import static io.github.dsheirer.module.decode.ChannelFilterTestSupport.run;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.dsp.filter.ChannelFilterDesigner;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.module.decode.ChannelFilterTestSupport.DecoderUnderTest;
import io.github.dsheirer.module.decode.am.AMDecoder;
import io.github.dsheirer.module.decode.am.DecodeConfigAM;
import io.github.dsheirer.module.decode.analog.DecodeConfigAnalog.Bandwidth;
import io.github.dsheirer.module.decode.dmr.DMRDecoder;
import io.github.dsheirer.module.decode.dmr.DecodeConfigDMR;
import io.github.dsheirer.module.decode.ltrnet.DecodeConfigLTRNet;
import io.github.dsheirer.module.decode.ltrstandard.DecodeConfigLTRStandard;
import io.github.dsheirer.module.decode.mpt1327.DecodeConfigMPT1327;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.decode.nbfm.NBFMDecoder;
import io.github.dsheirer.module.decode.nxdn.DecodeConfigNXDN;
import io.github.dsheirer.module.decode.nxdn.NXDNDecoder;
import io.github.dsheirer.module.decode.nxdn.layer3.type.TransmissionMode;
import io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2;
import io.github.dsheirer.module.decode.p25.phase2.P25P2DecoderHDQPSK;
import io.github.dsheirer.module.decode.passport.DecodeConfigPassport;
import io.github.dsheirer.module.demodulate.fm.FMDemodulatorModule;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * Decoder channel filters (ChannelSpecification.getChannelFilter()) for the DMR, NXDN, P25 Phase 2, NBFM and AM
 * decoders and the FM demodulator of the LTR, LTR-Net, MPT1327 and Passport decoders:
 * <ul>
 * <li>at the filter's sample rate, a decoder given the channel pre-filtered with the published taps and the channel
 * filter flag produces exactly the same output as the decoder filtering the raw channel itself;</li>
 * <li>at a sample rate where the decoder decimates, the flag changes nothing;</li>
 * <li>the published taps are the decoder's own design, and the decode configurations publish the filter.</li>
 * </ul>
 * The P25 Phase 1 decoders are covered by P25P1ChannelFilterTest.
 */
public class DecoderChannelFilterTest
{
    private static final double DIGITAL_SECONDS = 3.0;
    private static final double ANALOG_SECONDS = 2.0;
    private static final double NOISE = 0.05;

    /**
     * Checks equivalence at the filter's rate, that a later unflagged sample rate change restores the decoder's filter,
     * and the flag at twice the filter's rate, where every decoder here decimates.
     */
    private static void check(String label, Function<List<String>, DecoderUnderTest> factory,
                              ChannelSpecification.ChannelFilter filter, DoubleFunction<float[][]> signal,
                              double interfererAmplitude)
    {
        double rate = filter.sampleRate();
        float[][] raw = impair(signal.apply(rate), rate, getInterfererOffset(filter, rate), interfererAmplitude, NOISE,
            1);
        assertEquivalentAtFilterRate(label, factory, raw, filter);
        assertUnflaggedEventRestoresFilter(label, factory, raw, filter);
        double decimatingRate = rate * 2.0;
        assertFlagIgnoredWhenDecimating(label, factory, impair(signal.apply(decimatingRate), decimatingRate,
            getInterfererOffset(filter, rate), interfererAmplitude, NOISE, 2), decimatingRate);
    }

    private static DecoderUnderTest dmr(List<String> output)
    {
        DMRDecoder decoder = new DMRDecoder(new DecodeConfigDMR(), false);
        decoder.setMessageListener(message -> output.add("M" + message));
        decoder.setBufferListener(buffer -> output.add(bytes(buffer)));
        decoder.start();
        return of(decoder.getSourceEventListener(), decoder::receive, decoder::stop);
    }

    private static Function<List<String>, DecoderUnderTest> nxdn(TransmissionMode mode)
    {
        return output -> {
            DecodeConfigNXDN config = new DecodeConfigNXDN();
            config.setTransmissionMode(mode);
            NXDNDecoder decoder = new NXDNDecoder(config);
            decoder.setMessageListener(message -> output.add("M" + message));
            decoder.setBufferListener(buffer -> output.add(bytes(buffer)));
            decoder.start();
            return of(decoder.getSourceEventListener(), decoder::receive, decoder::stop);
        };
    }

    private static DecoderUnderTest p25Phase2(List<String> output)
    {
        P25P2DecoderHDQPSK decoder = new P25P2DecoderHDQPSK(null);
        decoder.setMessageListener(message -> output.add("M" + message));
        decoder.setBufferListener(buffer -> output.add(bytes(buffer)));
        decoder.start();
        return of(decoder.getSourceEventListener(), decoder::receive, decoder::stop);
    }

    private static Function<List<String>, DecoderUnderTest> nbfm(Bandwidth bandwidth)
    {
        return output -> {
            DecodeConfigNBFM config = new DecodeConfigNBFM();
            config.setBandwidth(bandwidth);
            NBFMDecoder decoder = new NBFMDecoder(config);
            decoder.setBufferListener(audio -> output.add(floats("A", audio)));
            decoder.setDecoderStateListener(event -> output.add("S" + event.getEvent() + " " + event.getState()));
            decoder.start();
            return of(decoder.getSourceEventListener(), decoder::receive, decoder::stop);
        };
    }

    private static Function<List<String>, DecoderUnderTest> am(Bandwidth bandwidth)
    {
        return output -> {
            DecodeConfigAM config = new DecodeConfigAM();
            config.setBandwidth(bandwidth);
            AMDecoder decoder = new AMDecoder(config);
            decoder.setBufferListener(audio -> output.add(floats("A", audio)));
            decoder.setDecoderStateListener(event -> output.add("S" + event.getEvent() + " " + event.getState()));
            decoder.setSourceEventListener(event -> output.add("E" + event.getEvent() + " " + event.getValue()));
            decoder.start();
            return of(decoder.getSourceEventListener(), decoder::receive, decoder::stop);
        };
    }

    private static DecoderUnderTest fmDemodulator(List<String> output)
    {
        FMDemodulatorModule demodulator = new FMDemodulatorModule(FMDemodulatorModule.DEFAULT_CHANNEL_BANDWIDTH);
        demodulator.setBufferListener(audio -> output.add(floats("A", audio)));
        demodulator.setSourceEventListener(event -> output.add("E" + event.getEvent() + " " + event.getValue()));
        demodulator.start();
        return of(demodulator.getSourceEventListener(), demodulator::receive, demodulator::stop);
    }

    @Test
    void dmr()
    {
        check("DMR", DecoderChannelFilterTest::dmr, DMRDecoder.getChannelFilter(),
            rate -> fsk4(rate, 4800.0, 1944.0, DIGITAL_SECONDS, 11), 1.0);
    }

    @Test
    void nxdn()
    {
        for(TransmissionMode mode : new TransmissionMode[]{TransmissionMode.M4800, TransmissionMode.M9600})
        {
            DecodeConfigNXDN config = new DecodeConfigNXDN();
            config.setTransmissionMode(mode);
            check("NXDN " + mode, nxdn(mode), config.getChannelSpecification().getChannelFilter(),
                rate -> fsk4(rate, mode.getSymbolRate(), mode.getOuterSymbolDeviation(), DIGITAL_SECONDS, 12), 1.0);
        }
    }

    @Test
    void p25Phase2()
    {
        check("P25 Phase 2", DecoderChannelFilterTest::p25Phase2, P25P2DecoderHDQPSK.getChannelFilter(),
            rate -> dqpsk(rate, 6000.0, DIGITAL_SECONDS, 13), 1.0);
    }

    /**
     * NBFM at each bandwidth.  The audio must flow (squelch open), so that the comparison covers the demodulator.
     */
    @Test
    void nbfm()
    {
        for(Bandwidth bandwidth : Bandwidth.FM_BANDWIDTHS)
        {
            DecodeConfigNBFM config = new DecodeConfigNBFM();
            config.setBandwidth(bandwidth);
            ChannelSpecification.ChannelFilter filter = config.getChannelSpecification().getChannelFilter();
            double deviation = bandwidth.getValue() / 5.0;
            check("NBFM " + bandwidth, nbfm(bandwidth), filter, rate -> fmTones(rate, deviation, ANALOG_SECONDS), 1.0);

            double rate = filter.sampleRate();
            List<String> output = run(nbfm(bandwidth), impair(fmTones(rate, deviation, ANALOG_SECONDS), rate,
                getInterfererOffset(filter, rate), 1.0, NOISE, 1), rate, true, filter.getTaps(rate));
            assertTrue(output.stream().anyMatch(item -> item.startsWith("A")), "NBFM " + bandwidth + " audio");
        }
    }

    /**
     * AM at each bandwidth.  The audio must flow (squelch open), so that the comparison covers the demodulator.
     */
    @Test
    void am()
    {
        for(Bandwidth bandwidth : Bandwidth.AM_BANDWIDTHS)
        {
            DecodeConfigAM config = new DecodeConfigAM();
            config.setBandwidth(bandwidth);
            ChannelSpecification.ChannelFilter filter = config.getChannelSpecification().getChannelFilter();
            check("AM " + bandwidth, am(bandwidth), filter, rate -> amTones(rate, ANALOG_SECONDS), 0.5);

            double rate = filter.sampleRate();
            List<String> output = run(am(bandwidth), impair(amTones(rate, ANALOG_SECONDS), rate,
                getInterfererOffset(filter, rate), 0.5, NOISE, 1), rate, true, filter.getTaps(rate));
            assertTrue(output.stream().anyMatch(item -> item.startsWith("A")), "AM " + bandwidth + " audio");
        }
    }

    @Test
    void fmDemodulator()
    {
        check("FM demodulator", DecoderChannelFilterTest::fmDemodulator,
            FMDemodulatorModule.getChannelFilter(FMDemodulatorModule.DEFAULT_CHANNEL_BANDWIDTH),
            rate -> fmTones(rate, 2500.0, ANALOG_SECONDS), 1.0);
    }

    /**
     * A sample rate that the decoder can't use leaves it unconfigured, exactly as without the channel filter hook: the
     * rate change throws, receive() then fails the same way (the FM demodulator's guard, a missing filter in the NBFM
     * and AM decoders), and nothing is demodulated, with or without the flag.
     */
    @Test
    void unsupportedSampleRateLeavesTheDecoderUnconfigured()
    {
        for(boolean flag : new boolean[]{false, true})
        {
            List<String> output = new ArrayList<>();
            DecoderUnderTest fm = fmDemodulator(output);
            assertUnconfigured("FM demodulator", fm, 24000.0, flag, IllegalStateException.class);

            DecoderUnderTest am = am(Bandwidth.BW_15_0).apply(output);
            assertUnconfigured("AM 15 kHz", am, 12500.0, flag, NullPointerException.class);

            DecoderUnderTest nbfm = nbfm(Bandwidth.BW_12_5).apply(output);
            assertUnconfigured("NBFM 12.5 kHz", nbfm, 20000.0, flag, NullPointerException.class);

            assertTrue(output.stream().noneMatch(item -> item.startsWith("A")), "audio " + output.size());
        }
    }

    private static void assertUnconfigured(String label, DecoderUnderTest decoder, double sampleRate, boolean flag,
                                           Class<? extends Throwable> receiveFailure)
    {
        SourceEvent event = SourceEvent.sampleRateChange(sampleRate, flag, "Test");
        assertThrows(IllegalStateException.class, () -> decoder.getSourceEventListener().receive(event),
            label + " rate change");
        float[][] signal = fmTones(sampleRate, 2500.0, 0.1);
        ComplexSamples samples = new ComplexSamples(Arrays.copyOf(signal[0], 2048), Arrays.copyOf(signal[1], 2048), 0);
        assertThrows(receiveFailure, () -> decoder.receive(samples), label + " receive");
    }

    /**
     * A failed rate change after a flagged one leaves the FM demodulator with its configured filter, which it then
     * applies, exactly as after an unflagged one.
     */
    @Test
    void failedRateChangeAfterFlaggedOneStillFilters()
    {
        float[][] signal = fmTones(25000.0, 2500.0, ANALOG_SECONDS);
        List<String> expected = null;

        for(boolean flag : new boolean[]{false, true})
        {
            List<String> output = new ArrayList<>();
            DecoderUnderTest fm = fmDemodulator(output);
            fm.getSourceEventListener().receive(SourceEvent.sampleRateChange(25000.0, flag, "Test"));
            assertThrows(IllegalStateException.class,
                () -> fm.getSourceEventListener().receive(SourceEvent.sampleRateChange(24000.0, false, "Test")));

            for(int offset = 0; offset + ChannelFilterTestSupport.BUFFER_SIZE <= signal[0].length;
                offset += ChannelFilterTestSupport.BUFFER_SIZE)
            {
                fm.receive(new ComplexSamples(Arrays.copyOfRange(signal[0], offset, offset +
                    ChannelFilterTestSupport.BUFFER_SIZE), Arrays.copyOfRange(signal[1], offset, offset +
                    ChannelFilterTestSupport.BUFFER_SIZE), 0));
            }

            if(expected == null)
            {
                expected = output;
                assertTrue(output.stream().anyMatch(item -> item.startsWith("A")), "audio");
            }
            else
            {
                ChannelFilterTestSupport.assertSame("FM demodulator after a failed rate change", expected, output);
            }
        }
    }

    /**
     * The published taps are each decoder's own design at the filter's sample rate and at another rate, and each
     * filter's sample rate is one where the decoder doesn't decimate.
     */
    @Test
    void tapsAreTheDecodersOwnDesign()
    {
        for(double rate : new double[]{25000.0, 37500.0})
        {
            assertArrayEquals(FilterFactory.getRemezLowPass((float)rate, 5100.0, 6500.0),
                DMRDecoder.getChannelFilter().getTaps(rate), "DMR at " + rate);
            assertArrayEquals(FilterFactory.getRemezLowPass((float)rate, 6500.0, 7800.0),
                P25P2DecoderHDQPSK.getChannelFilter().getTaps(rate), "P25 Phase 2 at " + rate);
        }

        assertEquals(25000.0, DMRDecoder.getChannelFilter().sampleRate());
        assertEquals(25000.0, P25P2DecoderHDQPSK.getChannelFilter().sampleRate());

        //NXDN designs from its channel specification's pass and stop frequencies
        for(TransmissionMode mode : TransmissionMode.values())
        {
            DecodeConfigNXDN config = new DecodeConfigNXDN();
            config.setTransmissionMode(mode);
            ChannelSpecification specification = config.getChannelSpecification();
            ChannelSpecification.ChannelFilter filter = specification.getChannelFilter();
            assertEquals(specification.getMinimumSampleRate(), filter.sampleRate(), mode.name());
            assertArrayEquals(FilterFactory.getRemezLowPass((float)filter.sampleRate(),
                specification.getPassFrequency(), specification.getStopFrequency()),
                filter.getTaps(filter.sampleRate()), mode.name());
        }

        for(Bandwidth bandwidth : Bandwidth.FM_BANDWIDTHS)
        {
            DecodeConfigNBFM config = new DecodeConfigNBFM();
            config.setBandwidth(bandwidth);
            ChannelSpecification.ChannelFilter filter = config.getChannelSpecification().getChannelFilter();
            assertEquals(bandwidth.getValue() > 12500 ? 50000.0 : 25000.0, filter.sampleRate(), bandwidth.name());
            assertArrayEquals(ChannelFilterDesigner.design(filter.sampleRate(), bandwidth.getValue()),
                filter.getTaps(filter.sampleRate()), bandwidth.name());
        }

        //AM: the rate that the decoder decimates the specification's minimum sample rate to
        double[] amRates = {6250.0, 12500.0, 12500.0, 25000.0, 30000.0, 50000.0};
        int pointer = 0;

        for(Bandwidth bandwidth : Bandwidth.AM_BANDWIDTHS)
        {
            DecodeConfigAM config = new DecodeConfigAM();
            config.setBandwidth(bandwidth);
            ChannelSpecification.ChannelFilter filter = config.getChannelSpecification().getChannelFilter();
            assertEquals(amRates[pointer++], filter.sampleRate(), bandwidth.name());
            assertEquals(0, AMDecoder.getDecimation(filter.sampleRate(), bandwidth.getValue()), bandwidth.name());
            assertTrue(AMDecoder.getDecimation(filter.sampleRate() * 2.0, bandwidth.getValue()) > 0,
                bandwidth.name());
            assertArrayEquals(ChannelFilterDesigner.design(filter.sampleRate(), bandwidth.getValue()),
                filter.getTaps(filter.sampleRate()), bandwidth.name());
        }

        ChannelSpecification.ChannelFilter fm =
            FMDemodulatorModule.getChannelFilter(FMDemodulatorModule.DEFAULT_CHANNEL_BANDWIDTH);
        assertEquals(25000.0, fm.sampleRate());
        assertEquals(5000.0, fm.passFrequency());
        assertEquals(6250.0, fm.stopFrequency());
        assertArrayEquals(FMDemodulatorModule.designChannelFilter(25000.0, 12500.0), fm.getTaps(25000.0));
    }

    /**
     * Each decode configuration publishes its decoder's channel filter.
     */
    @Test
    void configurationsPublishTheFilter()
    {
        assertSameFilter(DMRDecoder.getChannelFilter(), new DecodeConfigDMR().getChannelSpecification(), "DMR");
        assertSameFilter(P25P2DecoderHDQPSK.getChannelFilter(), new DecodeConfigP25Phase2().getChannelSpecification(),
            "P25 Phase 2");

        ChannelSpecification.ChannelFilter fm =
            FMDemodulatorModule.getChannelFilter(FMDemodulatorModule.DEFAULT_CHANNEL_BANDWIDTH);
        assertSameFilter(fm, new DecodeConfigLTRNet().getChannelSpecification(), "LTR-Net");
        assertSameFilter(fm, new DecodeConfigLTRStandard().getChannelSpecification(), "LTR Standard");
        assertSameFilter(fm, new DecodeConfigMPT1327().getChannelSpecification(), "MPT1327");
        assertSameFilter(fm, new DecodeConfigPassport().getChannelSpecification(), "Passport");

        for(Bandwidth bandwidth : Bandwidth.FM_BANDWIDTHS)
        {
            DecodeConfigNBFM config = new DecodeConfigNBFM();
            config.setBandwidth(bandwidth);
            assertSameFilter(NBFMDecoder.getChannelFilter((int)bandwidth.getValue()), config.getChannelSpecification(),
                "NBFM " + bandwidth);
        }

        for(Bandwidth bandwidth : Bandwidth.AM_BANDWIDTHS)
        {
            DecodeConfigAM config = new DecodeConfigAM();
            config.setBandwidth(bandwidth);
            ChannelSpecification specification = config.getChannelSpecification();
            assertSameFilter(AMDecoder.getChannelFilter(bandwidth.getValue(), specification.getMinimumSampleRate()),
                specification, "AM " + bandwidth);
        }
    }

    /**
     * A specification without a filter has none, and a sample rate change without the flag isn't channel filtered.
     */
    @Test
    void noFilterAndNoFlagByDefault()
    {
        ChannelSpecification specification = new ChannelSpecification(25000.0, 12500, 6000.0, 7000.0);
        assertNull(specification.getChannelFilter());
        assertFalse(specification.hasChannelFilter());
        assertFalse(SourceEvent.sampleRateChange(25000.0).isChannelFiltered());
        assertFalse(SourceEvent.sampleRateChange(25000.0, "Test").isChannelFiltered());
        assertFalse(SourceEvent.sampleRateChange(25000.0, false, "Test").isChannelFiltered());

        SourceEvent flagged = SourceEvent.sampleRateChange(25000.0, true, "Test");
        assertTrue(flagged.isChannelFiltered());
        assertEquals(SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE, flagged.getEvent());
        assertEquals(25000.0, flagged.getValue().doubleValue());
    }

    private static void assertSameFilter(ChannelSpecification.ChannelFilter expected,
                                         ChannelSpecification specification, String label)
    {
        ChannelSpecification.ChannelFilter actual = specification.getChannelFilter();
        assertNotNull(actual, label);
        assertTrue(specification.hasChannelFilter(), label);
        assertEquals(expected.sampleRate(), actual.sampleRate(), label);
        assertEquals(expected.passFrequency(), actual.passFrequency(), label);
        assertEquals(expected.stopFrequency(), actual.stopFrequency(), label);
        assertArrayEquals(expected.getTaps(expected.sampleRate()), actual.getTaps(expected.sampleRate()), label);
    }
}

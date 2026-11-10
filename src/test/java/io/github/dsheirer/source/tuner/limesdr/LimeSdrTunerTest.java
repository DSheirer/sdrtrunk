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

package io.github.dsheirer.source.tuner.limesdr;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dsheirer.dsp.filter.channelizer.ComplexPolyphaseChannelizerM2;
import io.github.dsheirer.limesdr.DeviceEntry;
import io.github.dsheirer.limesdr.LogLevel;
import io.github.dsheirer.limesdr.Range;
import io.github.dsheirer.limesdr.StreamConfig;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerFactory;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for the parts of the LimeSDR tuner that don't require a device.
 */
class LimeSdrTunerTest
{
    private static final DeviceEntry USB_A = DeviceEntry.parse("LimeSDR-USB, media=USB3.0, addr=1d50:6108, serial=0009060B00462008");
    private static final DeviceEntry USB_B = DeviceEntry.parse("LimeSDR-USB, media=USB3.0, addr=1d50:6108, serial=0009060B00471B22");
    private static final DeviceEntry MINI = DeviceEntry.parse("LimeSDR-Mini, media=USB3.0, addr=0403:601f, serial=1D3AC9A5C7B4D2");

    @Test
    void lookupFindsEachUsbProduct()
    {
        assertEquals(TunerClass.LIMESDR, TunerClass.lookup((short)0x1D50, (short)0x6108));
        assertEquals(TunerClass.LIMESDR, TunerClass.lookup((short)0x0403, (short)0x601F));
        assertTrue(TunerClass.LIMESDR.isSupportedUsbTuner());
        assertTrue(TunerType.LIMESDR.isSupportedUsbTuner());
    }

    @Test
    void scalesSamplesAndRemovesHalfBitOffset()
    {
        short[] samples = {2047, -2048, -1, 0};

        assertArrayEquals(new float[]{2047.5f / 2048, -2047.5f / 2048, -0.5f / 2048, 0.5f / 2048},
                LimeSdrTunerController.scale(samples));
    }

    @Test
    void sampleRatesAreLimitedByTheDevice()
    {
        //LimeSDR-USB on USB 3.0
        assertEquals(List.of(LimeSdrSampleRate.values()), LimeSdrSampleRate.getRates(100_000, 61_440_000));
        //LimeSDR Mini
        assertEquals(LimeSdrSampleRate.RATE_30_72, LimeSdrSampleRate.getRates(100_000, 30_720_000).getLast());
    }

    @Test
    void usb2LimitsTheSampleRate()
    {
        Range range = new Range(100_000, 61_440_000, 0);

        assertEquals(61_440_000, LimeSdrTunerController.getMaximumSampleRate(range, "USB3.0"));
        double usb2 = LimeSdrTunerController.getMaximumSampleRate(range, "USB2.0");
        assertEquals(LimeSdrSampleRate.RATE_10_0, LimeSdrSampleRate.getRates(range.min(), usb2).getLast());
    }

    @Test
    void savedRateAboveTheDeviceMaximumIsReduced()
    {
        List<LimeSdrSampleRate> rates = LimeSdrSampleRate.getRates(100_000, 30_720_000);

        assertEquals(LimeSdrSampleRate.RATE_30_72, LimeSdrSampleRate.getClosest(rates, LimeSdrSampleRate.RATE_61_44.getRate()));
        assertEquals(LimeSdrSampleRate.RATE_1_25, LimeSdrSampleRate.getClosest(rates, 1));
    }

    @Test
    void sampleRatesDivideIntoPolyphaseChannels()
    {
        for(LimeSdrSampleRate rate : LimeSdrSampleRate.values())
        {
            int channelCount = ComplexPolyphaseChannelizerM2.getChannelCount(rate.getRate());
            assertEquals(0, channelCount % 2, rate.toString());
            assertTrue((double)rate.getRate() / channelCount >= 25_000.0, rate.toString());
        }
    }

    @Test
    void maximumUsbRateDoesNotOverflow()
    {
        assertTrue(LimeSdrSampleRate.RATE_61_44.getRate() * 24L <= Integer.MAX_VALUE);
    }

    @Test
    void antennaListExcludesNoneAndLoopbackPorts()
    {
        List<String> ports = List.of("None", "LNAH", "LNAL", "LNAW", "LB1", "LB2");
        List<String> antennas = LimeSdrTunerController.getSelectableAntennas(ports);

        assertEquals(List.of("LNAH", "LNAL", "LNAW"), antennas);
        assertEquals("LNAW", LimeSdrTunerController.getDefaultAntenna(antennas));
        assertEquals("LNAH", LimeSdrTunerController.getDefaultAntenna(List.of("LNAH")));
    }

    @Test
    void deviceIsFoundBySerialNumber() throws SourceException
    {
        LimeSdrTunerController.UsbIdentity identity = new LimeSdrTunerController.UsbIdentity(0x1D50, 0x6108,
                "0009060B00471B22");

        assertEquals(USB_B, LimeSdrTunerController.findDevice(identity, List.of(MINI, USB_A, USB_B)));
    }

    @Test
    void deviceWithoutSerialIsFoundWhenItIsTheOnlyOneOfItsKind() throws SourceException
    {
        LimeSdrTunerController.UsbIdentity mini = new LimeSdrTunerController.UsbIdentity(0x0403, 0x601F, null);
        LimeSdrTunerController.UsbIdentity usb = new LimeSdrTunerController.UsbIdentity(0x1D50, 0x6108, null);

        assertEquals(MINI, LimeSdrTunerController.findDevice(mini, List.of(USB_A, MINI)));
        assertThrows(SourceException.class, () -> LimeSdrTunerController.findDevice(usb, List.of(USB_A, USB_B, MINI)));
    }

    @Test
    void deviceThatLimeSuiteCantSeeIsAnError()
    {
        LimeSdrTunerController.UsbIdentity identity = new LimeSdrTunerController.UsbIdentity(0x1D50, 0x6108,
                "0009060B00462008");

        assertThrows(SourceException.class, () -> LimeSdrTunerController.findDevice(identity, List.of(MINI)));
        assertThrows(SourceException.class, () -> LimeSdrTunerController.findDevice(identity, List.of(USB_B)));
    }

    @Test
    void configurationSurvivesSerialization() throws Exception
    {
        LimeSdrTunerConfiguration configuration = new LimeSdrTunerConfiguration("test");
        configuration.setSampleRate(LimeSdrSampleRate.RATE_5_0.getRate());
        configuration.setGain(55);
        configuration.setAntenna("LNAH");

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String json = mapper.writeValueAsString(configuration);
        TunerConfiguration restored = mapper.readValue(json, TunerConfiguration.class);

        LimeSdrTunerConfiguration limeSdr = assertInstanceOf(LimeSdrTunerConfiguration.class, restored);
        assertEquals(TunerType.LIMESDR, limeSdr.getTunerType());
        assertEquals(LimeSdrSampleRate.RATE_5_0.getRate(), limeSdr.getSampleRate());
        assertEquals(55, limeSdr.getGain());
        assertEquals("LNAH", limeSdr.getAntenna());
        assertEquals("test", limeSdr.getUniqueID());
    }

    @Test
    void factoryCreatesTheConfiguration()
    {
        assertInstanceOf(LimeSdrTunerConfiguration.class, TunerFactory.getTunerConfiguration(TunerType.LIMESDR, "id"));
    }

    @Test
    void fifoHoldsAQuarterSecondOfSamples()
    {
        assertEquals(StreamConfig.DEFAULT_FIFO_SIZE, LimeSdrTunerController.getFifoSize(LimeSdrSampleRate.RATE_1_25));
        assertEquals(10_000_000, LimeSdrTunerController.getFifoSize(LimeSdrSampleRate.RATE_40_0));
    }

    @Test
    void debugMessagesAreDiscarded()
    {
        LimeSdrTunerController.log(LogLevel.DEBUG, "Loss: pkt:4622555 exp: 000000017a863dc0, got: 000000017a864860");
        LimeSdrTunerController.log(LogLevel.VERBOSE, "Set Rx LO frequency (450100180 Hz)");
    }

    @Test
    void droppedSamplesAreFoundFromTimestampGaps()
    {
        assertEquals(0, LimeSdrTunerController.getTimestampGap(-1, 5_000_000));
        assertEquals(0, LimeSdrTunerController.getTimestampGap(65536, 65536));
        assertEquals(718_080, LimeSdrTunerController.getTimestampGap(32_309_248, 33_027_328));
        assertEquals(0, LimeSdrTunerController.getTimestampGap(32_309_248, 0));
    }
}

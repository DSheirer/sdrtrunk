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

package io.github.dsheirer.source.tuner.signalhound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.signalhound.DeviceDescriptor;
import io.github.dsheirer.signalhound.Diagnostics;
import io.github.dsheirer.signalhound.IqBlock;
import io.github.dsheirer.signalhound.IqBuffer;
import io.github.dsheirer.signalhound.IqConfig;
import io.github.dsheirer.signalhound.IqDataType;
import io.github.dsheirer.signalhound.IqStreamInfo;
import io.github.dsheirer.signalhound.SignalHoundApi;
import io.github.dsheirer.signalhound.SignalHoundDevice;
import io.github.dsheirer.signalhound.SignalHoundException;
import io.github.dsheirer.signalhound.Sweep;
import io.github.dsheirer.signalhound.SweepConfig;
import io.github.dsheirer.signalhound.SweepInfo;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerFactory;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Signal Hound tuner, using a simulated device since the tests can't assume Signal Hound hardware.
 */
class SignalHoundTunerTest
{
    private static final DeviceDescriptor SM200 = new DeviceDescriptor(SignalHoundApi.SM, 1234, "SM200B");

    @Test
    void sampleRatesAreTheBaseRateDecimatedByPowersOfTwo()
    {
        List<SignalHoundSampleRate> bb60 = SignalHoundSampleRate.getRates(SignalHoundFamily.BB60);
        assertEquals(new SignalHoundSampleRate(1, 40_000_000), bb60.getFirst());
        assertEquals(new SignalHoundSampleRate(64, 625_000), bb60.getLast());
        assertEquals(new SignalHoundSampleRate(64, 781_250), SignalHoundSampleRate.getRates(SignalHoundFamily.SM).getLast());
        assertEquals(new SignalHoundSampleRate(64, 960_000), SignalHoundSampleRate.getRates(SignalHoundFamily.SP145).getLast());

        for(SignalHoundFamily family : SignalHoundFamily.values())
        {
            for(SignalHoundSampleRate rate : SignalHoundSampleRate.getRates(family))
            {
                assertEquals(family.getBaseSampleRate(), (long)rate.rate() * rate.decimation(), "Rate " + rate);
            }
        }
    }

    @Test
    void closestRateDoesNotExceedTheRequestedRate()
    {
        List<SignalHoundSampleRate> bb60 = SignalHoundSampleRate.getRates(SignalHoundFamily.BB60);
        assertEquals(10_000_000, SignalHoundSampleRate.getClosest(bb60, 10_000_000).rate());
        assertEquals(6_250_000, SignalHoundSampleRate.getClosest(SignalHoundSampleRate.getRates(SignalHoundFamily.SM),
                10_000_000).rate());
        assertEquals(7_680_000, SignalHoundSampleRate.getClosest(
                SignalHoundSampleRate.getRates(SignalHoundFamily.SP145), 10_000_000).rate());
        assertEquals(625_000, SignalHoundSampleRate.getClosest(bb60, 1).rate());
        assertEquals("1.5625 MHz", new SignalHoundSampleRate(32, 1_562_500).toString());
        assertEquals("10.00 MHz", new SignalHoundSampleRate(4, 10_000_000).toString());
    }

    @Test
    void familyLimits()
    {
        assertEquals(SignalHoundFamily.SM, SignalHoundFamily.of(SignalHoundApi.SM));
        assertEquals(20_600_000_000L, SignalHoundFamily.SM.getMaximumFrequency("SM200B"));
        assertEquals(44_200_000_000L, SignalHoundFamily.SM.getMaximumFrequency("SM435C"));
        assertEquals(6_400_000_000L, SignalHoundFamily.BB60.getMaximumFrequency("BB60D"));
        assertTrue(SignalHoundFamily.BB60.hasGain());
        assertFalse(SignalHoundFamily.SM.hasGain());
        assertEquals(SignalHoundTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ,
                SignalHoundFamily.SM.getMaximumFrequency("SM435B"));
    }

    @Test
    void readsAreAbout10MillisecondsAsAPowerOfTwo()
    {
        assertEquals(65536, SignalHoundTunerController.getBufferSampleCount(10_000_000));
        assertEquals(8192, SignalHoundTunerController.getBufferSampleCount(625_000));
        assertEquals(524288, SignalHoundTunerController.getBufferSampleCount(61_440_000));
        assertEquals(8192, SignalHoundTunerController.getBufferSampleCount(0));
    }

    @Test
    void referenceLevelIsFullScale()
    {
        //A -20 dBm signal has an amplitude of sqrt(0.01 mW) = 0.1
        assertEquals(10.0f, SignalHoundTunerController.getSampleScale(-20), 1e-5f);
        assertEquals(1.0f, SignalHoundTunerController.getSampleScale(0), 1e-6f);
        assertEquals(0.1f, SignalHoundTunerController.getSampleScale(20), 1e-6f);
    }

    @Test
    void usableBandwidthComesFromTheStreamBandwidth()
    {
        assertEquals(0.75, SignalHoundTunerController.getUsableBandwidthPercent(
                new IqStreamInfo(40e6, 30e6, IqDataType.FLOAT32)), 1e-9);
        assertEquals(0.9, SignalHoundTunerController.getUsableBandwidthPercent(
                new IqStreamInfo(10e6, 9.9e6, IqDataType.FLOAT32)), 1e-9);
        assertEquals(0.5, SignalHoundTunerController.getUsableBandwidthPercent(
                new IqStreamInfo(10e6, 1e6, IqDataType.FLOAT32)), 1e-9);
    }

    @Test
    void invalidSettingsAreAutomatic()
    {
        assertEquals(3, SignalHoundTunerController.getValidSetting(3, 3));
        assertEquals(SignalHoundTunerController.AUTOMATIC, SignalHoundTunerController.getValidSetting(4, 3));
        assertEquals(SignalHoundTunerController.AUTOMATIC, SignalHoundTunerController.getValidSetting(-5, 3));
    }

    @Test
    void idUsesTheFamilyAndSerialNumber()
    {
        assertEquals("Signal Hound SM SER#1234", DiscoveredSignalHoundTuner.getId(SM200));
        assertEquals("Signal Hound SP145 SER#99", DiscoveredSignalHoundTuner.getId(
                new DeviceDescriptor(SignalHoundApi.SP, 99, "SP145")));
    }

    @Test
    void configurationRoundTrips() throws Exception
    {
        SignalHoundTunerConfiguration configuration = new SignalHoundTunerConfiguration("test");
        configuration.setSampleRate(5_000_000);
        configuration.setReferenceLevel(-40);
        configuration.setAttenuation(2);
        configuration.setGain(1);
        configuration.setPreselector(true);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        TunerConfiguration restored = mapper.readValue(mapper.writeValueAsString(configuration),
                TunerConfiguration.class);

        SignalHoundTunerConfiguration signalHound = assertInstanceOf(SignalHoundTunerConfiguration.class, restored);
        assertEquals(TunerType.SIGNAL_HOUND, signalHound.getTunerType());
        assertEquals(5_000_000, signalHound.getSampleRate());
        assertEquals(-40, signalHound.getReferenceLevel());
        assertEquals(2, signalHound.getAttenuation());
        assertEquals(1, signalHound.getGain());
        assertTrue(signalHound.isPreselector());
        assertEquals("test", signalHound.getUniqueID());
        assertInstanceOf(SignalHoundTunerConfiguration.class,
                TunerFactory.getTunerConfiguration(TunerType.SIGNAL_HOUND, "id"));
    }

    @Test
    void streamsScaledSamplesAndReconfiguresForChanges() throws Exception
    {
        FakeDevice device = new FakeDevice(0.01f);
        ErrorListener errors = new ErrorListener();
        SignalHoundTunerController controller = new SignalHoundTunerController(SM200, errors, _ -> device);
        controller.start();

        assertEquals("SM200B", controller.getDeviceInfo().model());
        assertEquals(20_600_000_000L, controller.getMaximumTunableFrequency());
        assertEquals(6_250_000, controller.getSignalHoundSampleRate().rate());
        assertFalse(controller.isPreselectorSupported(), "Only SmDevice and SP145B have a preselector");
        assertTrue(device.configs.isEmpty(), "The stream isn't configured until there's a listener");

        //Small, since each buffer is up to 512 KB.  Buffers are dropped while it is full.
        BlockingQueue<INativeBuffer> buffers = new ArrayBlockingQueue<>(16);
        Listener<INativeBuffer> listener = buffers::offer;
        controller.addBufferListener(listener);

        //0.01 sqrt(mW) at the default -20 dBm reference level is 0.1 of full scale
        assertEquals(0.1f, firstSample(buffers), 1e-6f);
        IqConfig first = device.configs.getLast();
        assertEquals(8, first.decimation());
        assertEquals(-20.0, first.referenceLevel());
        assertEquals(IqDataType.FLOAT32, first.dataType());

        controller.setFrequency(460_000_000L);
        assertEquals(460_000_000.0, device.configs.getLast().centerFrequency());

        controller.setReferenceLevel(-40);
        assertEquals(-40.0, device.configs.getLast().referenceLevel());
        //A block read before the change can still be in flight to the listeners
        awaitSample(buffers, 1.0f);

        controller.setSampleRate(controller.getSampleRates().get(2));
        assertEquals(4, device.configs.getLast().decimation());
        assertEquals(12_500_000.0, controller.getSampleRate());
        awaitSampleCount(buffers, 65536);

        controller.removeBufferListener(listener);
        assertFalse(device.streaming, "The stream stops without listeners");
        assertTrue(device.stops > 0);

        controller.stop();
        assertTrue(device.closed);
        assertTrue(errors.messages.isEmpty(), "Errors: " + errors.messages);
    }

    @Test
    void settingsAreAppliedWhenTheStreamStarts() throws Exception
    {
        FakeDevice device = new FakeDevice(0.01f);
        SignalHoundTunerController controller = new SignalHoundTunerController(SM200, new ErrorListener(),
                _ -> device);
        controller.start();

        SignalHoundTunerConfiguration configuration = new SignalHoundTunerConfiguration("id");
        configuration.setFrequency(851_000_000L);
        configuration.setSampleRate(25_000_000);
        configuration.setReferenceLevel(-100);
        configuration.setAttenuation(9);
        configuration.setGain(2);
        configuration.setPreselector(true);
        controller.apply(configuration);

        //Settings that the device doesn't support are corrected in the configuration
        assertEquals(SignalHoundTunerController.REFERENCE_LEVEL_MINIMUM, configuration.getReferenceLevel());
        assertEquals(SignalHoundTunerController.AUTOMATIC, configuration.getAttenuation());
        assertEquals(SignalHoundTunerController.AUTOMATIC, configuration.getGain());
        assertFalse(configuration.isPreselector());
        assertTrue(device.configs.isEmpty());

        Listener<INativeBuffer> listener = _ -> {};
        controller.addBufferListener(listener);
        IqConfig config = device.configs.getLast();
        assertEquals(851_000_000.0, config.centerFrequency());
        assertEquals(2, config.decimation());
        assertEquals(-60.0, config.referenceLevel());
        assertEquals(1, device.configs.size(), "Configured once when the stream starts");

        assertThrows(IllegalArgumentException.class, () -> controller.setGain(1), "The SM doesn't have gain");
        assertThrows(SourceException.class, () -> controller.setPreselector(true));
        controller.stop();
    }

    @Test
    void readFailureStopsTheTunerWithAnError() throws Exception
    {
        FakeDevice device = new FakeDevice(0.01f);
        ErrorListener errors = new ErrorListener();
        SignalHoundTunerController controller = new SignalHoundTunerController(SM200, errors, _ -> device);
        controller.start();
        controller.addBufferListener(_ -> {});

        device.readFailure = new SignalHoundException(SignalHoundApi.SM, "smGetIQ", -11, "Device disconnected");
        String message = errors.messages.poll(5, TimeUnit.SECONDS);

        assertNotNull(message);
        assertTrue(message.contains("Device disconnected"), message);
        controller.stop();
        assertTrue(device.closed);
    }

    @Test
    void startFailureClosesTheDevice()
    {
        SignalHoundTunerController controller = new SignalHoundTunerController(SM200, new ErrorListener(), _ -> {
            throw new SignalHoundException(SignalHoundApi.SM, "smOpenDeviceBySerial", -1, "Device not found");
        });

        SourceException exception = assertThrows(SourceException.class, controller::start);
        assertTrue(exception.getMessage().contains("Device not found"), exception.getMessage());
    }

    /**
     * Waits for a buffer and returns its first I sample.
     */
    private static float firstSample(BlockingQueue<INativeBuffer> buffers) throws InterruptedException
    {
        INativeBuffer buffer = buffers.poll(5, TimeUnit.SECONDS);
        assertNotNull(buffer, "No samples received");
        return buffer.iteratorInterleaved().next().samples()[0];
    }

    /**
     * Waits for a buffer whose first I sample is the expected value.
     */
    private static void awaitSample(BlockingQueue<INativeBuffer> buffers, float expected) throws InterruptedException
    {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        float sample = Float.NaN;

        while(System.nanoTime() < end)
        {
            sample = firstSample(buffers);

            if(Math.abs(sample - expected) < 1e-5f)
            {
                return;
            }
        }

        assertEquals(expected, sample, 1e-5f);
    }

    /**
     * Waits for a buffer with the expected sample count.
     */
    private static void awaitSampleCount(BlockingQueue<INativeBuffer> buffers, int expected) throws InterruptedException
    {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        int count = 0;

        while(System.nanoTime() < end)
        {
            INativeBuffer buffer = buffers.poll(5, TimeUnit.SECONDS);
            assertNotNull(buffer, "No samples received");
            count = buffer.sampleCount();

            if(count == expected)
            {
                return;
            }
        }

        assertEquals(expected, count);
    }

    private static class ErrorListener implements ITunerErrorListener
    {
        private final BlockingQueue<String> messages = new ArrayBlockingQueue<>(10);

        @Override
        public void setErrorMessage(String errorMessage)
        {
            messages.offer(errorMessage);
        }

        @Override
        public void tunerRemoved()
        {
        }
    }

    /**
     * Simulated SM200 that streams a constant sample value, reading as fast as the test reads.
     */
    private static class FakeDevice implements SignalHoundDevice
    {
        private final float mValue;
        private final List<IqConfig> configs = new CopyOnWriteArrayList<>();
        private volatile boolean streaming;
        private volatile boolean closed;
        private volatile int stops;
        private volatile SignalHoundException readFailure;
        private volatile RuntimeException firmwareFailure;

        FakeDevice(float value)
        {
            mValue = value;
        }

        @Override
        public SignalHoundApi api()
        {
            return SignalHoundApi.SM;
        }

        @Override
        public int serialNumber()
        {
            return 1234;
        }

        @Override
        public String model()
        {
            return "SM200B";
        }

        @Override
        public String firmwareVersion()
        {
            if(firmwareFailure != null)
            {
                throw firmwareFailure;
            }

            return "8.1.0";
        }

        @Override
        public Diagnostics diagnostics()
        {
            return new Diagnostics(40.0f, 5.0f, 1.2f);
        }

        @Override
        public synchronized IqStreamInfo startIq(IqConfig config)
        {
            configs.add(config);
            streaming = true;
            double rate = SignalHoundFamily.SM.getBaseSampleRate() / (double)config.decimation();
            return new IqStreamInfo(rate, Math.min(config.bandwidth(), rate * 0.8), config.dataType());
        }

        @Override
        public synchronized IqBlock readIq(IqBuffer buffer, boolean purge) throws SignalHoundException
        {
            if(!streaming)
            {
                throw new IllegalStateException("Not streaming");
            }

            if(readFailure != null)
            {
                throw readFailure;
            }

            buffer.segment().fill((byte)0);

            for(int x = 0; x < buffer.capacity() * 2; x++)
            {
                buffer.segment().setAtIndex(ValueLayout.JAVA_FLOAT, x, mValue);
            }

            try
            {
                Thread.sleep(1);
            }
            catch(InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }

            return new IqBlock(buffer.capacity(), System.currentTimeMillis() * 1_000_000L, false, 0, null);
        }

        @Override
        public float iqCorrection()
        {
            return 1.0f;
        }

        @Override
        public SweepInfo startSweep(SweepConfig config)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Sweep readSweep()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public synchronized void stop()
        {
            streaming = false;
            stops++;
        }

        @Override
        public synchronized boolean isClosed()
        {
            return closed;
        }

        @Override
        public synchronized void close()
        {
            streaming = false;
            closed = true;
        }
    }
}

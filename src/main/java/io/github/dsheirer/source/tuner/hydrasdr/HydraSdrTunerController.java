/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
 * Copyright (C) 2026 Benjamin Vernoux
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
package io.github.dsheirer.source.tuner.hydrasdr;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HydraSDR native tuner controller using libhydrasdr via JNI.
 *
 * Extends TunerController directly (not USBTunerController) because libhydrasdr
 * manages USB internally through its own HAL layer.
 */
public class HydraSdrTunerController extends TunerController implements HydraSdrNative.SampleCallback
{
	private static final Logger mLog = LoggerFactory.getLogger(HydraSdrTunerController.class);

	/* Pre-open frequency bounds used only to seed the TunerController base class
	 * before the device is opened. Once start() runs, the authoritative min/max
	 * are read from HydraSdrDeviceInfo (libhydrasdr-reported capabilities) and
	 * override these values. Do not rely on these constants for any tuner logic. */
	public static final long FALLBACK_MIN_FREQUENCY_HZ = 1_000_000;
	public static final long FALLBACK_MAX_FREQUENCY_HZ = 6_000_000_000L;
	public static final long FREQUENCY_DEFAULT = 101_100_000;
	public static final int DEFAULT_SAMPLE_RATE = 10_000_000;
	public static final double USABLE_BANDWIDTH_PERCENT = 0.90;

	/* Gain mode constants matching HydraSdrTunerConfiguration.gainMode */
	public static final int GAIN_MODE_LINEARITY = 0;
	public static final int GAIN_MODE_SENSITIVITY = 1;
	public static final int GAIN_MODE_CUSTOM = 2;

	/* Gain defaults (used when device doesn't report ranges) */
	public static final int LNA_GAIN_DEFAULT = 7;
	public static final int MIXER_GAIN_DEFAULT = 9;
	public static final int VGA_GAIN_DEFAULT = 9;

	private long mDeviceHandle;
	private long mSerialNumber;
	private HydraSdrDeviceInfo mDeviceInfo;
	private HydraSdrNativeBufferFactory mNativeBufferFactory;
	private int mSampleRate = DEFAULT_SAMPLE_RATE;
	private volatile boolean mStreaming = false;
	private List<HydraSdrSampleRate> mSampleRates = new ArrayList<>();
	private volatile long mLastDroppedSamples = 0;

	/* Streaming performance counters */
	private volatile long mCallbackCount = 0;
	private volatile long mTotalSamples = 0;
	private volatile long mCallbackTimeNs = 0;
	private volatile long mFactoryTimeNs = 0;
	private volatile long mBroadcastTimeNs = 0;
	private volatile long mStatsStartTime = 0;
	private volatile long mLastStatsLog = 0;
	private static final long STATS_LOG_INTERVAL_MS = 10_000;

	/**
	 * Constructs an instance.
	 * @param serialNumber device serial number (0 to open first available)
	 * @param tunerErrorListener to receive error notifications
	 */
	public HydraSdrTunerController(long serialNumber, ITunerErrorListener tunerErrorListener)
	{
		super(tunerErrorListener);
		mSerialNumber = serialNumber;
		mNativeBufferFactory = new HydraSdrNativeBufferFactory(DEFAULT_SAMPLE_RATE);

		setMinimumFrequency(FALLBACK_MIN_FREQUENCY_HZ);
		setMaximumFrequency(FALLBACK_MAX_FREQUENCY_HZ);
		setMiddleUnusableHalfBandwidth(0);
		setUsableBandwidthPercentage(USABLE_BANDWIDTH_PERCENT);
	}

	@Override
	public TunerType getTunerType()
	{
		return TunerType.HYDRASDR;
	}

	@Override
	public int getBufferSampleCount()
	{
		return 65536;
	}

	@Override
	public void start() throws SourceException
	{
		if(!HydraSdrNative.isLoaded())
		{
			throw new SourceException("HydraSDR native library (hydrasdr_jni) is not loaded");
		}

		if(mSerialNumber != 0)
		{
			mDeviceHandle = HydraSdrNative.open(mSerialNumber);
		}
		else
		{
			mDeviceHandle = HydraSdrNative.openAny();
		}

		if(mDeviceHandle == 0)
		{
			throw new SourceException("Unable to open HydraSDR device" +
				(mSerialNumber != 0 ? " serial=" + Long.toHexString(mSerialNumber) : ""));
		}

		mLog.info("Opened HydraSDR device, handle=0x" + Long.toHexString(mDeviceHandle));

		/* Query device info and update frequency limits */
		mDeviceInfo = HydraSdrNative.getDeviceInfo(mDeviceHandle);
		if(mDeviceInfo != null)
		{
			mLog.info("HydraSDR device: " + mDeviceInfo.getBoardName() +
				" serial=" + mDeviceInfo.getSerialNumber() +
				" fw=" + mDeviceInfo.getFirmwareVersion() +
				" freq=" + mDeviceInfo.getMinFrequency() + "-" + mDeviceInfo.getMaxFrequency() + " Hz" +
				" caps=0x" + Integer.toHexString(mDeviceInfo.getCapabilities()));

			if(mDeviceInfo.getMinFrequency() > 0)
			{
				setMinimumFrequency(mDeviceInfo.getMinFrequency());
			}
			if(mDeviceInfo.getMaxFrequency() > 0)
			{
				setMaximumFrequency(mDeviceInfo.getMaxFrequency());
			}
		}
		else
		{
			mLog.warn("Failed to query HydraSDR device info");
		}

		/* Request float32 IQ samples from the library. The streaming callback path
		 * (JNI deinterleave + onSamples float[]) assumes FLOAT32_IQ — any other
		 * sample type would deliver garbage to downstream DSP. Fail fast. */
		int result = HydraSdrNative.setSampleType(mDeviceHandle, HydraSdrNative.SAMPLE_FLOAT32_IQ);
		if(result != HydraSdrNative.SUCCESS)
		{
			HydraSdrNative.close(mDeviceHandle);
			mDeviceHandle = 0;
			throw new SourceException("HydraSDR rejected FLOAT32_IQ sample type: " +
				HydraSdrNative.errorName(result));
		}

		/* Query available sample rates */
		determineAvailableSampleRates();

		/* Set default frequency */
		setFrequency(FREQUENCY_DEFAULT);

		/* Set default sample rate */
		if(!mSampleRates.isEmpty())
		{
			setSampleRate(mSampleRates.get(0));
		}
		else
		{
			setSampleRateHz(DEFAULT_SAMPLE_RATE);
		}
	}

	@Override
	public void stop()
	{
		if(mStreaming)
		{
			stopStreaming();
		}

		if(mDeviceHandle != 0)
		{
			HydraSdrNative.close(mDeviceHandle);
			mLog.info("Closed HydraSDR device");
			mDeviceHandle = 0;
		}
	}

	/**
	 * Starts streaming. Called when the first buffer listener is added.
	 */
	private void startStreaming()
	{
		if(!mStreaming && mDeviceHandle != 0)
		{
			int result = HydraSdrNative.startRx(mDeviceHandle, this);
			if(result == HydraSdrNative.SUCCESS)
			{
				mStreaming = true;
				mLastDroppedSamples = 0;
				resetPerformanceStats();
				mLog.info("HydraSDR streaming started");
			}
			else
			{
				mLog.error("Failed to start HydraSDR streaming: " + HydraSdrNative.errorName(result));
			}
		}
	}

	/**
	 * Stops streaming. Called when the last buffer listener is removed.
	 */
	private void stopStreaming()
	{
		if(mStreaming && mDeviceHandle != 0)
		{
			int result = HydraSdrNative.stopRx(mDeviceHandle);
			if(result == HydraSdrNative.SUCCESS)
			{
				mLog.info("HydraSDR streaming stopped");
			}
			else
			{
				mLog.error("Failed to stop HydraSDR streaming: " + HydraSdrNative.errorName(result));
			}
			mStreaming = false;
		}
	}

	/**
	 * Native sample callback from libhydrasdr.
	 * Called on the native USB streaming thread.
	 */
	@Override
	public void onSamples(float[] iSamples, float[] qSamples, int sampleCount, long droppedSamples)
	{
		long t0 = System.nanoTime();

		if(droppedSamples > mLastDroppedSamples)
		{
			mLog.warn("HydraSDR dropped samples: " + (droppedSamples - mLastDroppedSamples) +
				" (total: " + droppedSamples + ")");
			mLastDroppedSamples = droppedSamples;
		}

		long tFactory = System.nanoTime();
		List<HydraSdrNativeBuffer> buffers = mNativeBufferFactory.get(iSamples, qSamples,
			sampleCount, System.currentTimeMillis());
		mFactoryTimeNs += (System.nanoTime() - tFactory);

		long tBroadcast = System.nanoTime();
		for(HydraSdrNativeBuffer buffer : buffers)
		{
			mNativeBufferBroadcaster.broadcast(buffer);
		}
		mBroadcastTimeNs += (System.nanoTime() - tBroadcast);

		mCallbackCount++;
		mTotalSamples += sampleCount;
		mCallbackTimeNs += (System.nanoTime() - t0);

		/* Periodic stats + streaming health check */
		long now = System.currentTimeMillis();
		if(now - mLastStatsLog >= STATS_LOG_INTERVAL_MS)
		{
			mLastStatsLog = now;
			mLog.info("HydraSDR perf: " + getPerformanceStats().replace('\n', ' '));

			/* Check if native side stopped unexpectedly (USB unplug, error) */
			if(mStreaming && mDeviceHandle != 0 && !HydraSdrNative.isStreaming(mDeviceHandle))
			{
				mLog.error("HydraSDR streaming stopped unexpectedly");
				mStreaming = false;
				setErrorMessage("HydraSDR streaming stopped unexpectedly (device error or USB disconnect)");
			}
		}
	}

	/**
	 * Returns streaming performance statistics.
	 * @return formatted string with throughput, callback rate, and timing
	 */
	public String getPerformanceStats()
	{
		long callbacks = mCallbackCount;
		long samples = mTotalSamples;
		long timeNs = mCallbackTimeNs;
		long factoryNs = mFactoryTimeNs;
		long broadcastNs = mBroadcastTimeNs;
		long elapsed = System.currentTimeMillis() - mStatsStartTime;

		if(callbacks == 0 || elapsed == 0)
		{
			return "No streaming data yet";
		}

		double elapsedSec = elapsed / 1000.0;
		double msps = samples / elapsedSec / 1e6;
		double callbacksPerSec = callbacks / elapsedSec;
		double avgCallbackUs = (timeNs / callbacks) / 1000.0;
		double avgFactoryUs = (factoryNs / callbacks) / 1000.0;
		double avgBroadcastUs = (broadcastNs / callbacks) / 1000.0;
		double avgSamplesPerCallback = (double)samples / callbacks;
		double javaOverheadPercent = (timeNs / 1e6) / elapsed * 100.0;

		return String.format(
			"Throughput: %.2f MSps\n" +
			"Callbacks: %.0f/sec (%.0f samples/cb)\n" +
			"Java total: %.1f us [factory: %.1f us, broadcast: %.1f us]\n" +
			"Java overhead: %.2f%%\n" +
			"Duration: %.1f sec\n" +
			"Total: %,d samples, %,d callbacks",
			msps, callbacksPerSec, avgSamplesPerCallback,
			avgCallbackUs, avgFactoryUs, avgBroadcastUs,
			javaOverheadPercent,
			elapsedSec, samples, callbacks);
	}

	/**
	 * Resets the performance counters.
	 */
	public void resetPerformanceStats()
	{
		mCallbackCount = 0;
		mTotalSamples = 0;
		mCallbackTimeNs = 0;
		mFactoryTimeNs = 0;
		mBroadcastTimeNs = 0;
		mStatsStartTime = System.currentTimeMillis();
	}

	@Override
	public void addBufferListener(Listener<INativeBuffer> listener)
	{
		getLock().lock();
		try
		{
			if(!hasBufferListeners())
			{
				startStreaming();
			}
			super.addBufferListener(listener);
		}
		finally
		{
			getLock().unlock();
		}
	}

	@Override
	public void removeBufferListener(Listener<INativeBuffer> listener)
	{
		getLock().lock();
		try
		{
			super.removeBufferListener(listener);
			if(!hasBufferListeners())
			{
				stopStreaming();
			}
		}
		finally
		{
			getLock().unlock();
		}
	}

	@Override
	public long getTunedFrequency() throws SourceException
	{
		return mFrequencyController.getTunedFrequency();
	}

	@Override
	public synchronized void setTunedFrequency(long frequency) throws SourceException
	{
		if(mDeviceHandle == 0)
		{
			throw new SourceException("Device not open");
		}

		int result = HydraSdrNative.setFrequency(mDeviceHandle, frequency);
		if(result != HydraSdrNative.SUCCESS)
		{
			throw new SourceException("Error setting frequency [" + frequency + "]: " +
				HydraSdrNative.errorName(result));
		}
	}

	@Override
	public double getCurrentSampleRate()
	{
		return mSampleRate;
	}

	@Override
	public void apply(TunerConfiguration tunerConfiguration) throws SourceException
	{
		super.apply(tunerConfiguration);

		if(tunerConfiguration instanceof HydraSdrTunerConfiguration config)
		{
			int sampleRate = config.getSampleRate();
			HydraSdrSampleRate rate = getSampleRate(sampleRate);

			if(rate == null)
			{
				if(!mSampleRates.isEmpty())
				{
					rate = mSampleRates.get(0);
				}
			}

			try
			{
				if(rate != null)
				{
					setSampleRate(rate);
				}
				else
				{
					setSampleRateHz(sampleRate > 0 ? sampleRate : DEFAULT_SAMPLE_RATE);
				}
			}
			catch(Exception e)
			{
				throw new SourceException("Couldn't set sample rate", e);
			}

			try
			{
				setBiasT(config.isBiasT());

				int gainMode = config.getGainMode();

				if(gainMode == GAIN_MODE_CUSTOM)
				{
					/* Custom mode: apply individual gains */
					setGain(HydraSdrNative.GAIN_TYPE_LNA_AGC, config.isLnaAgc() ? 1 : 0);
					setGain(HydraSdrNative.GAIN_TYPE_MIXER_AGC, config.isMixerAgc() ? 1 : 0);

					if(config.getLnaGain() >= 0)
					{
						setGain(HydraSdrNative.GAIN_TYPE_LNA, config.getLnaGain());
					}
					if(config.getMixerGain() >= 0)
					{
						setGain(HydraSdrNative.GAIN_TYPE_MIXER, config.getMixerGain());
					}
					if(config.getVgaGain() >= 0)
					{
						setGain(HydraSdrNative.GAIN_TYPE_VGA, config.getVgaGain());
					}
				}
				else if(gainMode == GAIN_MODE_SENSITIVITY && config.getSensitivityGain() > 0)
				{
					/* Sensitivity mode: single preset value sets all gains */
					setGain(HydraSdrNative.GAIN_TYPE_SENSITIVITY, config.getSensitivityGain());
				}
				else
				{
					/* Linearity mode (default): single preset value sets all gains */
					int lin = config.getLinearityGain();
					setGain(HydraSdrNative.GAIN_TYPE_LINEARITY, lin > 0 ? lin : 14);
				}
			}
			catch(Exception e)
			{
				throw new SourceException("Couldn't apply HydraSDR gain settings", e);
			}
		}
	}

	/* ==================== Sample Rate ==================== */

	/**
	 * Sets sample rate from a HydraSdrSampleRate descriptor.
	 */
	public void setSampleRate(HydraSdrSampleRate rate) throws SourceException
	{
		setSampleRateHz(rate.getRate());
	}

	/**
	 * Sets sample rate in Hz.
	 */
	public void setSampleRateHz(int rateHz) throws SourceException
	{
		if(mDeviceHandle == 0)
		{
			throw new SourceException("Device not open");
		}

		int result = HydraSdrNative.setSampleRate(mDeviceHandle, rateHz);
		if(result != HydraSdrNative.SUCCESS)
		{
			throw new SourceException("Error setting sample rate [" + rateHz + "]: " +
				HydraSdrNative.errorName(result));
		}

		mSampleRate = rateHz;
		mFrequencyController.setSampleRate(mSampleRate);
		mNativeBufferFactory.setSampleRate(mSampleRate);

		updateUsableBandwidthForRate(mSampleRate);
	}

	/**
	 * Updates usable bandwidth percentage based on sample rate.
	 */
	private void updateUsableBandwidthForRate(int rateHz)
	{
		if(rateHz >= 10_000_000)
		{
			setUsableBandwidthPercentage(0.90);
		}
		else if(rateHz >= 6_000_000)
		{
			setUsableBandwidthPercentage(0.83);
		}
		else if(rateHz >= 3_000_000)
		{
			setUsableBandwidthPercentage(0.66);
		}
		else
		{
			setUsableBandwidthPercentage(0.60);
		}
	}

	/**
	 * Returns available sample rates.
	 */
	public List<HydraSdrSampleRate> getSampleRates()
	{
		return mSampleRates;
	}

	/**
	 * Returns the sample rate matching the given Hz value, or null.
	 */
	public HydraSdrSampleRate getSampleRate(int rateHz)
	{
		for(HydraSdrSampleRate rate : mSampleRates)
		{
			if(rate.getRate() == rateHz)
			{
				return rate;
			}
		}
		return null;
	}

	/**
	 * Queries the device for available sample rates.
	 */
	private void determineAvailableSampleRates()
	{
		mSampleRates.clear();

		if(mDeviceHandle != 0)
		{
			int[] rates = HydraSdrNative.getSampleRates(mDeviceHandle);
			if(rates != null)
			{
				for(int i = 0; i < rates.length; i++)
				{
					mSampleRates.add(new HydraSdrSampleRate(i, rates[i],
						formatSampleRate(rates[i])));
				}
			}
		}

		if(mSampleRates.isEmpty())
		{
			mSampleRates.add(new HydraSdrSampleRate(0, DEFAULT_SAMPLE_RATE,
				formatSampleRate(DEFAULT_SAMPLE_RATE)));
		}
	}

	/* ==================== Gain Control ==================== */

	/**
	 * Sets a gain value using the unified gain API.
	 * @param gainType one of HydraSdrNative.GAIN_TYPE_*
	 * @param value gain value
	 */
	public void setGain(int gainType, int value) throws SourceException
	{
		if(mDeviceHandle == 0)
		{
			throw new SourceException("Device not open");
		}

		int result = HydraSdrNative.setGain(mDeviceHandle, gainType, value);
		if(result != HydraSdrNative.SUCCESS)
		{
			mLog.warn("Failed to set gain type " + gainType + " to " + value + ": " +
				HydraSdrNative.errorName(result));
		}
	}

	/**
	 * Queries gain information for a specific type.
	 * @param gainType one of HydraSdrNative.GAIN_TYPE_*
	 * @return int[6] = {value, min, max, step, default, flags}, or null
	 */
	public int[] getGainInfo(int gainType)
	{
		if(mDeviceHandle == 0)
		{
			return null;
		}
		return HydraSdrNative.getGainInfo(mDeviceHandle, gainType);
	}

	/**
	 * Returns the device capability bitmask.
	 */
	public int getCapabilities()
	{
		if(mDeviceHandle == 0)
		{
			return 0;
		}
		return HydraSdrNative.getCapabilities(mDeviceHandle);
	}

	/* ==================== RF Control ==================== */

	/**
	 * Enables/disables Bias-T.
	 */
	public void setBiasT(boolean enabled) throws SourceException
	{
		if(mDeviceHandle == 0)
		{
			throw new SourceException("Device not open");
		}

		int result = HydraSdrNative.setBiasT(mDeviceHandle, enabled);
		if(result != HydraSdrNative.SUCCESS)
		{
			mLog.warn("Failed to set Bias-T: " + HydraSdrNative.errorName(result));
		}
	}

	/**
	 * Reads the device temperature.
	 * @return temperature in Celsius, or Float.NaN if not available
	 */
	public float getTemperature()
	{
		if(mDeviceHandle == 0)
		{
			return Float.NaN;
		}
		return HydraSdrNative.getTemperature(mDeviceHandle);
	}

	/**
	 * Sets bandwidth in Hz (0 for auto).
	 */
	public void setBandwidth(int bandwidthHz) throws SourceException
	{
		if(mDeviceHandle == 0)
		{
			throw new SourceException("Device not open");
		}

		int result = HydraSdrNative.setBandwidth(mDeviceHandle, bandwidthHz);
		if(result != HydraSdrNative.SUCCESS)
		{
			mLog.warn("Failed to set bandwidth: " + HydraSdrNative.errorName(result));
		}
	}

	/**
	 * Sets the decimation mode.
	 */
	public void setDecimationMode(int mode) throws SourceException
	{
		if(mDeviceHandle == 0)
		{
			throw new SourceException("Device not open");
		}

		int result = HydraSdrNative.setDecimationMode(mDeviceHandle, mode);
		if(result != HydraSdrNative.SUCCESS)
		{
			mLog.warn("Failed to set decimation mode: " + HydraSdrNative.errorName(result));
		}
	}

	/* ==================== Device Info ==================== */

	/**
	 * Returns device information.
	 */
	public HydraSdrDeviceInfo getDeviceInfo()
	{
		if(mDeviceInfo == null && mDeviceHandle != 0)
		{
			mDeviceInfo = HydraSdrNative.getDeviceInfo(mDeviceHandle);
		}
		return mDeviceInfo;
	}

	/**
	 * Indicates if the native library is available.
	 */
	public static boolean isNativeAvailable()
	{
		return HydraSdrNative.isLoaded();
	}

	/**
	 * Lists available HydraSDR device serial numbers.
	 */
	public static long[] listDevices()
	{
		if(!HydraSdrNative.isLoaded())
		{
			return new long[0];
		}
		long[] serials = HydraSdrNative.listDevices();
		return serials != null ? serials : new long[0];
	}

	/* Track which serial numbers have been assigned to controllers */
	private static final AtomicInteger sNextDeviceIndex = new AtomicInteger(0);

	/**
	 * Finds the serial number for a HydraSDR device at the given USB bus/port.
	 *
	 * Since libhydrasdr enumerates devices via libusb internally, the discovery
	 * order matches the USB enumeration order. Multiple HydraSDR devices are
	 * assigned serial numbers in the order they are discovered by TunerManager.
	 *
	 * @param bus USB bus number (from sdrtrunk discovery)
	 * @param portAddress USB port address (from sdrtrunk discovery)
	 * @return serial number for this device, or 0 to open first available
	 */
	public static long findSerialForUsbPort(int bus, String portAddress)
	{
		long[] serials = listDevices();

		if(serials.length == 0)
		{
			return 0;
		}

		/* Single device: return its serial directly */
		if(serials.length == 1)
		{
			return serials[0];
		}

		/* Multiple devices: assign by discovery order.
		 * Both sdrtrunk USB enum and libhydrasdr libusb enum iterate in the
		 * same bus/port order, so index N in sdrtrunk maps to index N here. */
		int idx = sNextDeviceIndex.getAndIncrement();
		if(idx < serials.length)
		{
			mLog.info("Multi-device: assigned serial 0x" + Long.toHexString(serials[idx]) +
				" to USB Bus " + bus + " Port " + portAddress +
				" (device " + (idx + 1) + "/" + serials.length + ")");
			return serials[idx];
		}

		/* Fallback: more sdrtrunk discoveries than native devices */
		mLog.warn("More HydraSDR USB devices discovered than native library reports");
		return 0;
	}

	/**
	 * Resets the device assignment counter. Called at the start of USB enumeration.
	 */
	public static void resetDeviceAssignment()
	{
		sNextDeviceIndex.set(0);
	}

	/**
	 * Formats a sample rate value in Hz for display.
	 */
	public static String formatSampleRate(int rateHz)
	{
		return new java.text.DecimalFormat("#.00 MHz").format((double)rateHz / 1E6d);
	}
}

/*
 * *****************************************************************************
 * Copyright (C) 2026 Benjamin VERNOUX
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

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JNI bridge to libhydrasdr native library.
 *
 * Provides hardware-agnostic access to HydraSDR devices through the
 * libhydrasdr C API. All device-specific protocol details are handled
 * by the native library's HAL (Hardware Abstraction Layer).
 */
public class HydraSdrNative
{
	private static final Logger mLog = LoggerFactory.getLogger(HydraSdrNative.class);
	private static boolean sLoaded = false;
	private static boolean sLoadedFromPath = false;
	private static String sLibraryPath;

	/* Capability flags matching hydrasdr_capability_t */
	public static final int CAP_LNA_GAIN             = (1 << 0);
	public static final int CAP_RF_GAIN              = (1 << 1);
	public static final int CAP_MIXER_GAIN           = (1 << 2);
	public static final int CAP_FILTER_GAIN          = (1 << 3);
	public static final int CAP_VGA_GAIN             = (1 << 4);
	public static final int CAP_LNA_AGC              = (1 << 5);
	public static final int CAP_RF_AGC               = (1 << 6);
	public static final int CAP_MIXER_AGC            = (1 << 7);
	public static final int CAP_FILTER_AGC           = (1 << 8);
	public static final int CAP_LINEARITY_GAIN       = (1 << 9);
	public static final int CAP_SENSITIVITY_GAIN     = (1 << 10);
	public static final int CAP_BIAS_TEE             = (1 << 11);
	public static final int CAP_PACKING              = (1 << 12);
	public static final int CAP_RF_PORT_SELECT       = (1 << 13);
	public static final int CAP_GPIO                 = (1 << 14);
	public static final int CAP_SPIFLASH             = (1 << 15);
	public static final int CAP_CLOCKGEN             = (1 << 16);
	public static final int CAP_RF_FRONTEND          = (1 << 17);
	public static final int CAP_BANDWIDTH            = (1 << 18);
	public static final int CAP_TEMPERATURE_SENSOR   = (1 << 19);
	public static final int CAP_RX                   = (1 << 20);
	public static final int CAP_EXTENDED_SAMPLERATES = (1 << 21);
	public static final int CAP_EXTENDED_GAIN        = (1 << 22);

	/* Gain type indices matching hydrasdr_gain_type_t */
	public static final int GAIN_TYPE_LNA         = 0;
	public static final int GAIN_TYPE_RF          = 1;
	public static final int GAIN_TYPE_MIXER       = 2;
	public static final int GAIN_TYPE_FILTER      = 3;
	public static final int GAIN_TYPE_VGA         = 4;
	public static final int GAIN_TYPE_LINEARITY   = 5;
	public static final int GAIN_TYPE_SENSITIVITY = 6;
	public static final int GAIN_TYPE_LNA_AGC     = 7;
	public static final int GAIN_TYPE_RF_AGC      = 8;
	public static final int GAIN_TYPE_MIXER_AGC   = 9;
	public static final int GAIN_TYPE_FILTER_AGC  = 10;
	public static final int GAIN_TYPE_COUNT       = 11;

	/* Sample type indices matching hydrasdr_sample_type */
	public static final int SAMPLE_FLOAT32_IQ   = 0;
	public static final int SAMPLE_FLOAT32_REAL = 1;
	public static final int SAMPLE_INT16_IQ     = 2;
	public static final int SAMPLE_INT16_REAL   = 3;
	public static final int SAMPLE_UINT16_REAL  = 4;
	public static final int SAMPLE_RAW          = 5;
	public static final int SAMPLE_INT8_IQ      = 6;
	public static final int SAMPLE_UINT8_IQ     = 7;
	public static final int SAMPLE_INT8_REAL    = 8;
	public static final int SAMPLE_UINT8_REAL   = 9;

	/* Decimation mode matching hydrasdr_decimation_mode */
	public static final int DEC_MODE_LOW_BANDWIDTH   = 0;
	public static final int DEC_MODE_HIGH_DEFINITION = 1;

	/* Error codes matching hydrasdr_error */
	public static final int SUCCESS                   = 0;
	public static final int ERROR_INVALID_PARAM       = -2;
	public static final int ERROR_NOT_FOUND           = -5;
	public static final int ERROR_BUSY                = -6;
	public static final int ERROR_NO_MEM              = -11;
	public static final int ERROR_UNSUPPORTED         = -12;
	public static final int ERROR_LIBUSB              = -1000;
	public static final int ERROR_THREAD              = -1001;
	public static final int ERROR_STREAMING_THREAD    = -1002;
	public static final int ERROR_STREAMING_STOPPED   = -1003;
	public static final int ERROR_OTHER               = -9999;

	/* Gain info array indices returned by getGainInfo() */
	public static final int GAIN_INFO_VALUE    = 0;
	public static final int GAIN_INFO_MIN      = 1;
	public static final int GAIN_INFO_MAX      = 2;
	public static final int GAIN_INFO_STEP     = 3;
	public static final int GAIN_INFO_DEFAULT  = 4;
	public static final int GAIN_INFO_FLAGS    = 5;

	static
	{
		try
		{
			System.loadLibrary("hydrasdr_jni");
			sLoaded = true;
			mLog.info("Loaded hydrasdr_jni native library via System.loadLibrary");
		}
		catch(UnsatisfiedLinkError e)
		{
			mLog.debug("hydrasdr_jni not on java.library.path, searching platform paths");
			sLoaded = loadFromPlatformPath();
		}
	}

	private static boolean loadFromPlatformPath()
	{
		String os = System.getProperty("os.name", "").toLowerCase();
		String arch = System.getProperty("os.arch", "").toLowerCase();
		String[][] searchDirs;
		String jniName;
		String[] depNames;

		if(os.contains("win"))
		{
			String programFiles = System.getenv("ProgramFiles");
			String userDir = System.getProperty("user.dir");
			jniName = "hydrasdr_jni.dll";
			/* Both names listed: MinGW builds the libhydrasdr core as
			 * libhydrasdr.dll (with the lib prefix), MSVC builds it as
			 * hydrasdr.dll (no prefix). The loop tolerates missing files
			 * via depFile.exists(), so listing both is safe on both toolchains. */
			depNames = new String[] {"libusb-1.0.dll", "libhydrasdr.dll", "hydrasdr.dll"};
			searchDirs = new String[][] {
				{userDir + "\\jni\\build"},
				{userDir + "\\lib\\native"},
				{userDir + "\\..\\lib\\native"},
				{userDir},
				{programFiles + "\\HydraSDR"},
				{programFiles + "\\HydraSDR\\bin"},
			};
		}
		else if(os.contains("linux"))
		{
			String userDir = System.getProperty("user.dir");
			jniName = "libhydrasdr_jni.so";
			depNames = new String[] {"libusb-1.0.so", "libhydrasdr.so"};
			searchDirs = new String[][] {
				{userDir + "/jni/build"},
				{userDir + "/lib/native"},
				{userDir + "/../lib/native"},
				{"/usr/local/lib"},
				{"/usr/lib"},
				{"/usr/lib/" + arch + "-linux-gnu"},
			};
		}
		else if(os.contains("mac"))
		{
			String userDir = System.getProperty("user.dir");
			jniName = "libhydrasdr_jni.dylib";
			depNames = new String[] {"libusb-1.0.dylib", "libhydrasdr.dylib"};
			searchDirs = new String[][] {
				{userDir + "/jni/build"},
				{userDir + "/lib/native"},
				{userDir + "/../lib/native"},
				{"/usr/local/lib"},
				{"/opt/homebrew/lib"},
			};
		}
		else
		{
			searchDirs = new String[0][];
			jniName = "";
			depNames = new String[0];
		}

		for(String[] dirEntry : searchDirs)
		{
			String dir = dirEntry[0];
			File jniFile = new File(dir, jniName);
			if(jniFile.exists())
			{
				try
				{
					/* Load dependency DLLs first so the JNI DLL can resolve them */
					for(String dep : depNames)
					{
						File depFile = new File(dir, dep);
						if(depFile.exists())
						{
							System.load(depFile.getAbsolutePath());
							mLog.info("Pre-loaded dependency: " + depFile.getAbsolutePath());
						}
					}

					System.load(jniFile.getAbsolutePath());
					sLoadedFromPath = true;
					sLibraryPath = jniFile.getAbsolutePath();
					mLog.info("Loaded hydrasdr_jni from: " + sLibraryPath);
					return true;
				}
				catch(UnsatisfiedLinkError e)
				{
					mLog.warn("Failed to load from: " + dir + " - " + e.getMessage());
				}
			}
		}

		mLog.error("Failed to load hydrasdr_jni native library from any path");
		return false;
	}

	/**
	 * Indicates if the native library was loaded successfully.
	 */
	public static boolean isLoaded()
	{
		return sLoaded;
	}

	/**
	 * Indicates if the native library was loaded from an explicit path.
	 */
	public static boolean isLoadedFromPath()
	{
		return sLoadedFromPath;
	}

	/**
	 * Returns the explicit path the library was loaded from, or null.
	 */
	public static String getLibraryPath()
	{
		return sLibraryPath;
	}

	/* ==================== Device Management ==================== */

	/**
	 * Returns the library version as {major, minor, revision}.
	 */
	public static native int[] getLibVersion();

	/**
	 * Lists serial numbers of available HydraSDR devices.
	 * @return array of serial numbers, or empty array if none found
	 */
	public static native long[] listDevices();

	/**
	 * Opens a device by serial number.
	 * @param serialNumber device serial number from listDevices()
	 * @return native device handle (pointer), or 0 on error
	 */
	public static native long open(long serialNumber);

	/**
	 * Opens the first available device.
	 * @return native device handle (pointer), or 0 on error
	 */
	public static native long openAny();

	/**
	 * Closes a device and releases resources.
	 * @param handle native device handle
	 */
	public static native void close(long handle);

	/* ==================== Configuration ==================== */

	/**
	 * Sets the center frequency.
	 * @param handle native device handle
	 * @param freqHz frequency in Hz
	 * @return HYDRASDR_SUCCESS or error code
	 */
	public static native int setFrequency(long handle, long freqHz);

	/**
	 * Sets the sample rate.
	 * @param handle native device handle
	 * @param rateHz sample rate in Hz
	 * @return HYDRASDR_SUCCESS or error code
	 */
	public static native int setSampleRate(long handle, int rateHz);

	/**
	 * Sets the bandwidth.
	 * @param handle native device handle
	 * @param bwHz bandwidth in Hz (0 for auto)
	 * @return HYDRASDR_SUCCESS or error code
	 */
	public static native int setBandwidth(long handle, int bwHz);

	/**
	 * Sets the output sample type (e.g., SAMPLE_FLOAT32_IQ).
	 * @param handle native device handle
	 * @param type sample type constant
	 * @return HYDRASDR_SUCCESS or error code
	 */
	public static native int setSampleType(long handle, int type);

	/**
	 * Sets the decimation mode.
	 * @param handle native device handle
	 * @param mode DEC_MODE_LOW_BANDWIDTH or DEC_MODE_HIGH_DEFINITION
	 * @return HYDRASDR_SUCCESS or error code
	 */
	public static native int setDecimationMode(long handle, int mode);

	/**
	 * Queries available sample rates.
	 * @param handle native device handle
	 * @return array of sample rates in Hz, or null on error
	 */
	public static native int[] getSampleRates(long handle);

	/**
	 * Queries available bandwidths.
	 * @param handle native device handle
	 * @return array of bandwidths in Hz, or null on error
	 */
	public static native int[] getBandwidths(long handle);

	/* ==================== Gain Control ==================== */

	/**
	 * Sets a gain value using the unified gain API.
	 * @param handle native device handle
	 * @param type gain type (GAIN_TYPE_*)
	 * @param value gain value
	 * @return HYDRASDR_SUCCESS or error code
	 */
	public static native int setGain(long handle, int type, int value);

	/**
	 * Queries gain information for a specific gain type.
	 * @param handle native device handle
	 * @param type gain type (GAIN_TYPE_*)
	 * @return int[6] = {value, min, max, step, default, flags}, or null on error
	 */
	public static native int[] getGainInfo(long handle, int type);

	/**
	 * Queries the device capability bitmask.
	 * @param handle native device handle
	 * @return capability bitmask (CAP_* flags)
	 */
	public static native int getCapabilities(long handle);

	/* ==================== Streaming ==================== */

	/**
	 * Starts RX streaming. Samples are delivered via the callback.
	 * The callback runs on a native thread; implementations must be fast.
	 * @param handle native device handle
	 * @param callback receives float32 IQ sample blocks
	 * @return HYDRASDR_SUCCESS or error code
	 */
	public static native int startRx(long handle, SampleCallback callback);

	/**
	 * Stops RX streaming.
	 * @param handle native device handle
	 * @return HYDRASDR_SUCCESS or error code
	 */
	public static native int stopRx(long handle);

	/**
	 * Checks if the device is currently streaming.
	 * @param handle native device handle
	 * @return true if streaming
	 */
	public static native boolean isStreaming(long handle);

	/* ==================== Device Information ==================== */

	/**
	 * Retrieves comprehensive device information.
	 * @param handle native device handle
	 * @return HydraSdrDeviceInfo populated from native hydrasdr_device_info_t, or null on error
	 */
	public static native HydraSdrDeviceInfo getDeviceInfo(long handle);

	/* ==================== RF Control ==================== */

	/**
	 * Enables/disables Bias-T power for active antennas.
	 * @param handle native device handle
	 * @param enable true to enable
	 * @return HYDRASDR_SUCCESS or error code
	 */
	public static native int setBiasT(long handle, boolean enable);

	/**
	 * Selects the RF port.
	 * @param handle native device handle
	 * @param port RF port index (0-31)
	 * @return HYDRASDR_SUCCESS or error code
	 */
	public static native int setRfPort(long handle, int port);

	/**
	 * Reads the device temperature.
	 * @param handle native device handle
	 * @return temperature in Celsius, or Float.NaN on error
	 */
	public static native float getTemperature(long handle);

	/**
	 * Resets the device.
	 * @param handle native device handle
	 * @return HYDRASDR_SUCCESS or error code
	 */
	public static native int reset(long handle);

	/* ==================== Utility ==================== */

	/**
	 * Returns a human-readable error name.
	 * @param errorCode error code constant
	 * @return error name string
	 */
	public static native String errorName(int errorCode);

	/**
	 * Callback interface for receiving IQ samples from the native library.
	 * Called on a native streaming thread - implementations must process
	 * samples quickly and not block.
	 */
	public interface SampleCallback
	{
		/**
		 * Called when a block of float32 IQ samples is available.
		 * Samples are pre-split into separate I and Q arrays by the JNI layer
		 * using SIMD-optimized native de-interleaving, avoiding a Java-side copy.
		 * @param iSamples in-phase float samples (length = sampleCount)
		 * @param qSamples quadrature float samples (length = sampleCount)
		 * @param sampleCount number of complex samples
		 * @param droppedSamples cumulative count of dropped samples
		 */
		void onSamples(float[] iSamples, float[] qSamples, int sampleCount, long droppedSamples);
	}
}

/*
 * *****************************************************************************
 * Copyright (C) 2024-2025 Benjamin VERNOUX
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

/**
 * Device information populated from native hydrasdr_device_info_t structure.
 * Created by the JNI layer via HydraSdrNative.getDeviceInfo().
 */
public class HydraSdrDeviceInfo
{
	/* Board identification */
	private int mBoardId;
	private String mBoardName;
	private String mFirmwareVersion;
	private String mSerialNumber;
	private String mPartNumber;

	/* Feature capabilities (bitmask of HydraSdrNative.CAP_* flags) */
	private int mCapabilities;

	/* Frequency range */
	private long mMinFrequency;
	private long mMaxFrequency;

	/* RF ports */
	private int mRfPortCount;

	/* Sample types supported (bitmask) */
	private int mSampleTypes;

	/* GPIO count */
	private int mGpioCount;

	/* Power & thermal */
	private float mMaxSafeTemperature;

	/* Current device state */
	private int mCurrentSampleRate;
	private int mCurrentBandwidth;
	private int mCurrentHwSampleRate;
	private int mCurrentDecimationFactor;

	/**
	 * Default constructor for JNI population.
	 */
	public HydraSdrDeviceInfo()
	{
	}

	public int getBoardId()
	{
		return mBoardId;
	}

	public void setBoardId(int boardId)
	{
		mBoardId = boardId;
	}

	public String getBoardName()
	{
		return mBoardName != null ? mBoardName : "Unknown";
	}

	public void setBoardName(String boardName)
	{
		mBoardName = boardName;
	}

	public String getFirmwareVersion()
	{
		return mFirmwareVersion != null ? mFirmwareVersion : "Unknown";
	}

	public void setFirmwareVersion(String firmwareVersion)
	{
		mFirmwareVersion = firmwareVersion;
	}

	public String getSerialNumber()
	{
		return mSerialNumber != null ? mSerialNumber : "Unknown";
	}

	public void setSerialNumber(String serialNumber)
	{
		mSerialNumber = serialNumber;
	}

	public String getPartNumber()
	{
		return mPartNumber != null ? mPartNumber : "Unknown";
	}

	public void setPartNumber(String partNumber)
	{
		mPartNumber = partNumber;
	}

	public int getCapabilities()
	{
		return mCapabilities;
	}

	public void setCapabilities(int capabilities)
	{
		mCapabilities = capabilities;
	}

	/**
	 * Checks if the device supports a specific capability.
	 * @param cap one of HydraSdrNative.CAP_* constants
	 * @return true if the capability is supported
	 */
	public boolean hasCapability(int cap)
	{
		return (mCapabilities & cap) != 0;
	}

	public long getMinFrequency()
	{
		return mMinFrequency;
	}

	public void setMinFrequency(long minFrequency)
	{
		mMinFrequency = minFrequency;
	}

	public long getMaxFrequency()
	{
		return mMaxFrequency;
	}

	public void setMaxFrequency(long maxFrequency)
	{
		mMaxFrequency = maxFrequency;
	}

	public int getRfPortCount()
	{
		return mRfPortCount;
	}

	public void setRfPortCount(int rfPortCount)
	{
		mRfPortCount = rfPortCount;
	}

	public int getSampleTypes()
	{
		return mSampleTypes;
	}

	public void setSampleTypes(int sampleTypes)
	{
		mSampleTypes = sampleTypes;
	}

	public int getGpioCount()
	{
		return mGpioCount;
	}

	public void setGpioCount(int gpioCount)
	{
		mGpioCount = gpioCount;
	}

	public float getMaxSafeTemperature()
	{
		return mMaxSafeTemperature;
	}

	public void setMaxSafeTemperature(float maxSafeTemperature)
	{
		mMaxSafeTemperature = maxSafeTemperature;
	}

	public int getCurrentSampleRate()
	{
		return mCurrentSampleRate;
	}

	public void setCurrentSampleRate(int currentSampleRate)
	{
		mCurrentSampleRate = currentSampleRate;
	}

	public int getCurrentBandwidth()
	{
		return mCurrentBandwidth;
	}

	public void setCurrentBandwidth(int currentBandwidth)
	{
		mCurrentBandwidth = currentBandwidth;
	}

	public int getCurrentHwSampleRate()
	{
		return mCurrentHwSampleRate;
	}

	public void setCurrentHwSampleRate(int currentHwSampleRate)
	{
		mCurrentHwSampleRate = currentHwSampleRate;
	}

	public int getCurrentDecimationFactor()
	{
		return mCurrentDecimationFactor;
	}

	public void setCurrentDecimationFactor(int currentDecimationFactor)
	{
		mCurrentDecimationFactor = currentDecimationFactor;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("HydraSDR Device Information\n");
		sb.append("Board: ").append(getBoardName()).append("\n");
		sb.append("Part Number: ").append(getPartNumber()).append("\n");
		sb.append("Serial Number: ").append(getSerialNumber()).append("\n");
		sb.append("Firmware: ").append(getFirmwareVersion()).append("\n");
		sb.append("Frequency Range: ").append(mMinFrequency / 1e6).append(" - ")
			.append(mMaxFrequency / 1e6).append(" MHz\n");
		sb.append("Capabilities: 0x").append(Integer.toHexString(mCapabilities));
		return sb.toString();
	}
}

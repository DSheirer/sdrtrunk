package io.github.dsheirer.source.tuner.hydrasdr;

import io.github.dsheirer.hydrasdr.DeviceInfo;

/**
 * HydraSDR device information, formatted for display and for identifying the tuner.
 */
public class HydraSdrDeviceInformation
{
	private final DeviceInfo mDeviceInfo;

	/**
	 * Constructs an instance
	 * @param deviceInfo from the device
	 */
	public HydraSdrDeviceInformation(DeviceInfo deviceInfo)
	{
		mDeviceInfo = deviceInfo;
	}

	public String getBoardName()
	{
		return mDeviceInfo.boardName();
	}

	public String getVersion()
	{
		return mDeviceInfo.firmwareVersion().replace("HydraSDR ", "");
	}

	/**
	 * Serial number, formatted as the identifier that tuner configurations are saved under.
	 */
	public String getSerialNumber()
	{
		long serial = mDeviceInfo.serialNumber();
		return String.format("%08X-%08X", serial >>> 32, serial & 0xFFFFFFFFL);
	}

	public String getPartNumber()
	{
		return String.format("%016X", mDeviceInfo.partId());
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("HydraSDR Device Information\n");
		sb.append("Board: ");
		sb.append(getBoardName());
		sb.append("\nPart Number: ");
		sb.append(getPartNumber());
		sb.append("\nSerial Number: ");
		sb.append(getSerialNumber());
		sb.append("\nFirmware: ");
		sb.append(getVersion());

		return sb.toString();
	}
}

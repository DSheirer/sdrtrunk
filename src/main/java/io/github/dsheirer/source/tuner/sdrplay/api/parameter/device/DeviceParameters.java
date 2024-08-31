/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.device;

import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_DevParamsT;
import java.lang.foreign.MemorySegment;

/**
 * Device Parameters structure (sdrplay_api_DevParamsT)
 */
public abstract class DeviceParameters
{
    private final MemorySegment mDevParams;
    private final SamplingFrequency mSamplingFrequency;
    private final SynchronousUpdate mSynchronousUpdate;
    private final ResetFlags mResetFlags;

    /**
     * Constructs an instance.
     * @param devParams for the DeviceParameters structure
     */
    public DeviceParameters(MemorySegment devParams)
    {
        mDevParams = devParams;
        mSamplingFrequency = new SamplingFrequency(sdrplay_api_DevParamsT.fsFreq(devParams));
        mSynchronousUpdate = new SynchronousUpdate(sdrplay_api_DevParamsT.syncUpdate(devParams));
        mResetFlags = new ResetFlags(sdrplay_api_DevParamsT.resetFlags(devParams));
    }

    /**
     * Foreign memory segment for this device parameters instance
     */
    protected MemorySegment getDevParams()
    {
        return mDevParams;
    }

    /**
     * Parts Per Million (ppm) center frequency correction value.
     * @return ppm
     */
    public double getPPM()
    {
        return sdrplay_api_DevParamsT.ppm(getDevParams());
    }

    /**
     * Sets the parts per million (ppm) center frequency correction value
     * @param ppm parts per million
     */
    public void setPPM(double ppm)
    {
        sdrplay_api_DevParamsT.ppm(getDevParams(), ppm);
    }

    /**
     * Sampling frequency structure.
     */
    public SamplingFrequency getSamplingFrequency()
    {
        return mSamplingFrequency;
    }

    /**
     * Synchronous update structure
     */
    public SynchronousUpdate getSynchronousUpdate()
    {
        return mSynchronousUpdate;
    }

    /**
     * Reset request flags
     */
    public ResetFlags getResetFlags()
    {
        return mResetFlags;
    }

    /**
     * USB Transfer mode currently used by the device
     */
    public TransferMode getTransferMode()
    {
        return TransferMode.fromValue(sdrplay_api_DevParamsT.mode(getDevParams()));
    }

    /**
     * Sets the USB transfer mode used by the device
     */
    public void setTransferMode(TransferMode transferMode)
    {
        if(transferMode != TransferMode.UNKNOWN)
        {
            sdrplay_api_DevParamsT.mode(getDevParams(), transferMode.getValue());
        }
    }

    public long getSamplesPerPacket()
    {
        return sdrplay_api_DevParamsT.samplesPerPkt(getDevParams());
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\tDevice Parameters").append("\n");
        sb.append("\t\tPPM: ").append(getPPM()).append("\n");
        sb.append("\t\tSample Rate: ").append(getSamplingFrequency()).append("\n");
        sb.append("\t\tSamples Per Packet: ").append(getSamplesPerPacket()).append("\n");
        sb.append("\t\tSync Update: ").append(getSynchronousUpdate()).append("\n");
        sb.append("\t\tReset Flags: ").append(getResetFlags()).append("\n");
        sb.append("\t\tTransfer Mode: ").append(getTransferMode()).append("\n");

        return sb.toString();
    }
}

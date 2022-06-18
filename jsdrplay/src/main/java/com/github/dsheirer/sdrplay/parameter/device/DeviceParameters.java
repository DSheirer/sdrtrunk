/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package com.github.dsheirer.sdrplay.parameter.device;

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_DevParamsT;

import java.lang.foreign.MemorySegment;

/**
 * Device Parameters structure (sdrplay_api_DevParamsT)
 */
public abstract class DeviceParameters
{
    private MemorySegment mMemorySegment;
    private SamplingFrequency mSamplingFrequency;
    private SynchronousUpdate mSynchronousUpdate;
    private ResetFlags mResetFlags;

    public DeviceParameters(MemorySegment memorySegment)
    {
        mMemorySegment = memorySegment;
        mSamplingFrequency = new SamplingFrequency(sdrplay_api_DevParamsT.fsFreq$slice(memorySegment));
        mSynchronousUpdate = new SynchronousUpdate(sdrplay_api_DevParamsT.syncUpdate$slice(memorySegment));
        mResetFlags = new ResetFlags(sdrplay_api_DevParamsT.resetFlags$slice(memorySegment));
    }

    /**
     * Foreign memory segment for this device parameters instance
     */
    protected MemorySegment getMemorySegment()
    {
        return mMemorySegment;
    }

    /**
     * Parts Per Million (ppm) center frequency correction value.
     * @return ppm
     */
    public double getPPM()
    {
        return sdrplay_api_DevParamsT.ppm$get(getMemorySegment());
    }

    /**
     * Sets the parts per million (ppm) center frequency correction value
     * @param ppm parts per million
     */
    public void setPPM(double ppm)
    {
        sdrplay_api_DevParamsT.ppm$set(getMemorySegment(), ppm);
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
        return TransferMode.fromValue(sdrplay_api_DevParamsT.mode$get(getMemorySegment()));
    }

    /**
     * Sets the USB transfer mode used by the device
     */
    public void setTransferMode(TransferMode transferMode)
    {
        if(transferMode != TransferMode.UNKNOWN)
        {
            sdrplay_api_DevParamsT.mode$set(getMemorySegment(), transferMode.getValue());
        }
    }

    public long getSamplesPerPacket()
    {
        return sdrplay_api_DevParamsT.samplesPerPkt$get(getMemorySegment());
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

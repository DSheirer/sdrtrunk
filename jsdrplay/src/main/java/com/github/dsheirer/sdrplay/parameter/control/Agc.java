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

package com.github.dsheirer.sdrplay.parameter.control;

import com.github.dsheirer.sdrplay.api.v3_07.sdrplay_api_AgcT;
import com.github.dsheirer.sdrplay.util.Flag;

import java.lang.foreign.MemorySegment;

/**
 * AGC structure (sdrplay_api_AgcT)
 */
public class Agc
{
    private MemorySegment mMemorySegment;

    /**
     * Constructs an instance from the foreign memory segment
     */
    public Agc(MemorySegment memorySegment)
    {
        mMemorySegment = memorySegment;
    }

    /**
     * Foreign memory segment for this structure
     */
    private MemorySegment getMemorySegment()
    {
        return mMemorySegment;
    }

    /**
     * AGC Control (mode)
     */
    public AgcMode getAgcMode()
    {
        return AgcMode.fromValue(sdrplay_api_AgcT.enable$get(getMemorySegment()));
    }

    /**
     * Sets AGC Control (mode)
     */
    public void setAgcMode(AgcMode mode)
    {
        sdrplay_api_AgcT.enable$set(getMemorySegment(), mode.getValue());
    }

    /**
     * Set point dBfs
     */
    public int getSetPointDbfs()
    {
        return sdrplay_api_AgcT.setPoint_dBfs$get(getMemorySegment());
    }

    /**
     * Sets the set point dBfs
     */
    public void setSetPointDbfs(int setPointDbfs)
    {
        sdrplay_api_AgcT.setPoint_dBfs$set(getMemorySegment(), setPointDbfs);
    }

    /**
     * Attack rate in milliseconds
     */
    public int getAttackMs()
    {
        return sdrplay_api_AgcT.attack_ms$get(getMemorySegment());
    }

    /**
     * Sets the attack rate in milliseconds
     */
    public void setAttackMs(int attackMs)
    {
        sdrplay_api_AgcT.attack_ms$set(getMemorySegment(), (short)attackMs);
    }

    /**
     * Decay rate in milliseconds
     */
    public int getDecayMs()
    {
        return sdrplay_api_AgcT.decay_ms$get(getMemorySegment());
    }

    /**
     * Sets the decay rate in milliseconds
     */
    public void setDecayMs(int decayMs)
    {
        sdrplay_api_AgcT.decay_ms$set(getMemorySegment(), (short)decayMs);
    }

    /**
     * Decay delay rate in milliseconds
     */
    public int getDecayDelayMs()
    {
        return sdrplay_api_AgcT.decay_delay_ms$get(getMemorySegment());
    }

    /**
     * Sets the decay delay rate in milliseconds
     */
    public void setDecayDelayMs(int decayDelayMs)
    {
        sdrplay_api_AgcT.decay_delay_ms$set(getMemorySegment(), (short)decayDelayMs);
    }

    /**
     * Decay threshold in dB
     */
    public int getDecayThresholdDb()
    {
        return sdrplay_api_AgcT.decay_threshold_dB$get(getMemorySegment());
    }

    /**
     * Sets the decay threshold in dB
     */
    public void setDecayThresholdDb(int decayThresholdDb)
    {
        sdrplay_api_AgcT.decay_threshold_dB$set(getMemorySegment(), (short)decayThresholdDb);
    }

    /**
     * Sets the changes to be applied as a synchronous update
     */
    public void setSynchronousUpdate(boolean syncUpdate)
    {
        //Note: this is supposed to be an integer value ... does it represent a boolean (0 or 1) or something else??
        sdrplay_api_AgcT.syncUpdate$set(getMemorySegment(), Flag.of(syncUpdate));
    }
}

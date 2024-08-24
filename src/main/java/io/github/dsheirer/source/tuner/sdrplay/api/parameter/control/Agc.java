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

package io.github.dsheirer.source.tuner.sdrplay.api.parameter.control;

import io.github.dsheirer.source.tuner.sdrplay.api.util.Flag;
import io.github.dsheirer.source.tuner.sdrplay.api.v3_07.sdrplay_api_AgcT;
import java.lang.foreign.MemorySegment;

/**
 * AGC structure (sdrplay_api_AgcT)
 */
public class Agc
{
    private final MemorySegment mMemorySegment;

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
        return AgcMode.fromValue(sdrplay_api_AgcT.enable(getMemorySegment()));
    }

    /**
     * Sets AGC Control (mode)
     */
    public void setAgcMode(AgcMode mode)
    {
        sdrplay_api_AgcT.enable(getMemorySegment(), mode.getValue());
    }

    /**
     * Set point dBfs
     */
    public int getSetPointDbfs()
    {
        return sdrplay_api_AgcT.setPoint_dBfs(getMemorySegment());
    }

    /**
     * Sets the set point dBfs
     */
    public void setSetPointDbfs(int setPointDbfs)
    {
        sdrplay_api_AgcT.setPoint_dBfs(getMemorySegment(), setPointDbfs);
    }

    /**
     * Attack rate in milliseconds
     */
    public int getAttackMs()
    {
        return sdrplay_api_AgcT.attack_ms(getMemorySegment());
    }

    /**
     * Sets the attack rate in milliseconds
     */
    public void setAttackMs(int attackMs)
    {
        sdrplay_api_AgcT.attack_ms(getMemorySegment(), (short)attackMs);
    }

    /**
     * Decay rate in milliseconds
     */
    public int getDecayMs()
    {
        return sdrplay_api_AgcT.decay_ms(getMemorySegment());
    }

    /**
     * Sets the decay rate in milliseconds
     */
    public void setDecayMs(int decayMs)
    {
        sdrplay_api_AgcT.decay_ms(getMemorySegment(), (short)decayMs);
    }

    /**
     * Decay delay rate in milliseconds
     */
    public int getDecayDelayMs()
    {
        return sdrplay_api_AgcT.decay_delay_ms(getMemorySegment());
    }

    /**
     * Sets the decay delay rate in milliseconds
     */
    public void setDecayDelayMs(int decayDelayMs)
    {
        sdrplay_api_AgcT.decay_delay_ms(getMemorySegment(), (short)decayDelayMs);
    }

    /**
     * Decay threshold in dB
     */
    public int getDecayThresholdDb()
    {
        return sdrplay_api_AgcT.decay_threshold_dB(getMemorySegment());
    }

    /**
     * Sets the decay threshold in dB
     */
    public void setDecayThresholdDb(int decayThresholdDb)
    {
        sdrplay_api_AgcT.decay_threshold_dB(getMemorySegment(), (short)decayThresholdDb);
    }

    /**
     * Sets the changes to be applied as a synchronous update
     */
    public void setSynchronousUpdate(boolean syncUpdate)
    {
        //Note: this is supposed to be an integer value ... does it represent a boolean (0 or 1) or something else??
        sdrplay_api_AgcT.syncUpdate(getMemorySegment(), Flag.of(syncUpdate));
    }
}

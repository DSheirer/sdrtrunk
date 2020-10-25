/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.source.tuner.fcd.proplusV2;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.MixerTunerType;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.fcd.FCDCommand;
import io.github.dsheirer.source.tuner.fcd.FCDTunerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;

import javax.sound.sampled.TargetDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FCD2TunerController extends FCDTunerController
{
    private final static Logger mLog = LoggerFactory.getLogger(FCD2TunerController.class);

    public static final int MINIMUM_TUNABLE_FREQUENCY = 150000;
    public static final int MAXIMUM_TUNABLE_FREQUENCY = 2050000000;
    public static final int SAMPLE_RATE = 192000;

    public FCD2TunerController(TargetDataLine mixerTDL, Device device, DeviceDescriptor descriptor)
    {
        super(MixerTunerType.FUNCUBE_DONGLE_PRO_PLUS, mixerTDL, device, descriptor,
            MINIMUM_TUNABLE_FREQUENCY, MAXIMUM_TUNABLE_FREQUENCY);
    }

    public void init() throws SourceException
    {
        super.init();

        mFrequencyController.setSampleRate(SAMPLE_RATE);

        try
        {
            setFCDMode(Mode.APPLICATION);
        }
        catch(Exception e)
        {
            throw new SourceException("error setting Mode to APPLICATION", e);
        }
    }

    public double getCurrentSampleRate()
    {
        return SAMPLE_RATE;
    }

    @Override
    public TunerClass getTunerClass()
    {
        return TunerClass.FUNCUBE_DONGLE_PRO_PLUS;
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.FUNCUBE_DONGLE_PRO_PLUS;
    }

    public void setLNAGain(boolean enabled) throws SourceException
    {
        mLock.lock();

        try
        {
            send(FCDCommand.APP_SET_LNA_GAIN, enabled ? 1 : 0);
        }
        catch(Exception e)
        {
            throw new SourceException("error while setting LNA Gain", e);
        }
        finally
        {
            mLock.unlock();
        }
    }

    public void setMixerGain(boolean enabled) throws SourceException
    {
        mLock.lock();

        try
        {
            send(FCDCommand.APP_SET_MIXER_GAIN, enabled ? 1 : 0);
        }
        catch(Exception e)
        {
            throw new SourceException("error while setting Mixer Gain", e);
        }
        finally
        {
            mLock.unlock();
        }
    }

    @Override
    public void apply(TunerConfiguration config) throws SourceException
    {
        if(config instanceof FCD2TunerConfiguration)
        {
            FCD2TunerConfiguration fcd2 = (FCD2TunerConfiguration)config;

            setFrequencyCorrection(fcd2.getFrequencyCorrection());

            long frequency = MINIMUM_TUNABLE_FREQUENCY;

            if(fcd2.getFrequency() >= MINIMUM_TUNABLE_FREQUENCY &&
                fcd2.getFrequency() <= MAXIMUM_TUNABLE_FREQUENCY)
            {
                frequency = fcd2.getFrequency();
            }

            setFrequency(frequency);
            setLNAGain(fcd2.getGainLNA());
            setMixerGain(fcd2.getGainMixer());

            try
            {
                setFrequency(fcd2.getFrequency());
            }
            catch(SourceException se)
            {
                //Do nothing, we couldn't set the frequency
            }
        }
    }

    public int getDCCorrection()
    {
        int dcCorrection = -999;

        try
        {
            ByteBuffer buffer = send(FCDCommand.APP_GET_DC_CORRECTION);

            buffer.order(ByteOrder.LITTLE_ENDIAN);

            return buffer.getInt(2);
        }
        catch(Exception e)
        {
            mLog.error("error getting dc correction value", e);
        }

        return dcCorrection;
    }

    public void setDCCorrection(int value)
    {
        mLock.lock();

        try
        {
            send(FCDCommand.APP_SET_DC_CORRECTION, value);
        }
        catch(Exception e)
        {
            mLog.error("error setting dc correction to [" + value + "]", e);
        }
        finally
        {
            mLock.unlock();
        }
    }

    public int getIQCorrection()
    {
        int iqCorrection = -999;

        try
        {
            ByteBuffer buffer = send(FCDCommand.APP_GET_IQ_CORRECTION);

            buffer.order(ByteOrder.LITTLE_ENDIAN);

            return buffer.getInt(2);
        }
        catch(Exception e)
        {
            mLog.error("error reading IQ correction value", e);
        }

        return iqCorrection;
    }

    public void setIQCorrection(int value)
    {
        mLock.lock();

        try
        {
            send(FCDCommand.APP_SET_IQ_CORRECTION, value);
        }
        catch(Exception e)
        {
            mLog.error("error setting IQ correction to [" + value + "]", e);
        }
        finally
        {
            mLock.unlock();
        }
    }
}
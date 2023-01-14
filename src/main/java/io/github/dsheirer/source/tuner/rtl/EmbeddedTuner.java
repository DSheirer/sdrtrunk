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

package io.github.dsheirer.source.tuner.rtl;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import org.usb4java.LibUsbException;

import javax.usb.UsbException;

/**
 * Tuner embedded in an RTL-2832 USB tuner.
 *
 * Note: since the RTL2832TunerController instance is constructed without knowing the embedded tuner type, we construct
 * an instance of the actual tuner once the type is discovered and we use an adapter to access the non-public USB
 * controls and interfaces needed by the embedded tuner.  This access is through the RtlTunerControllerAdapter.
 */
public abstract class EmbeddedTuner
{
    private RTL2832TunerController.ControllerAdapter mAdapter;

    /**
     * Constructs an instance
     * @param adapter exposing the interfaces of the parent RTL2832TunerController
     */
    public EmbeddedTuner(RTL2832TunerController.ControllerAdapter adapter)
    {
        mAdapter = adapter;
    }

    /**
     * Access the interfaces of the parent RTL tuner controller via the adapter
     * @return
     */
    protected RTL2832TunerController.ControllerAdapter getAdapter()
    {
        return mAdapter;
    }

    /**
     * Initialize this embedded tuner.  The calling method should ensure that the I2C repeater is enabled before
     * calling this method.
     */
    protected abstract void initTuner() throws UsbException;

    /**
     * Type of embedded tuner
     */
    public abstract TunerType getTunerType();

    /**
     * Apply the tuner configuration settings to this embedded tuner
     * @param tunerConfig containing settings
     * @throws SourceException if there is an error in applying the settings
     */
    public abstract void apply(TunerConfiguration tunerConfig) throws SourceException;

    /**
     * Minimum frequency supported by this tuner
     */
    public abstract long getMinimumFrequencySupported();

    /**
     * Maximum frequency supported by this tuner
     */
    public abstract long getMaximumFrequencySupported();

    /**
     * Bandwidth of the DC spike that is unusable for this tuner
     */
    //TODO: can we get rid of this now?
    public abstract int getDcSpikeHalfBandwidth();

    /**
     * Percentage of the bandwidth that is usable for this tuner
     */
    public abstract double getUsableBandwidthPercent();

    /**
     * Sets the center tuned frequency
     * @param frequency in Hertz
     * @throws SourceException if there is an error
     */
    public abstract void setTunedFrequency(long frequency) throws SourceException;

    /**
     * Sets the sample mode for the tuner
     * @param mode to set
     * @throws LibUsbException
     */
    public abstract void setSamplingMode(RTL2832TunerController.SampleMode mode) throws LibUsbException;

    /**
     * Sets the filters according to the specified sample rate
     * @param sampleRate for filter setup
     * @throws SourceException if there is an error
     */
    public abstract void setSampleRateFilters(int sampleRate) throws SourceException;
}

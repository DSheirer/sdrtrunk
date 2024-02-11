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
package io.github.dsheirer.source.tuner.rtl.e4k;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.EmbeddedTuner;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import javax.usb.UsbException;

/**
 * Elonic E4000 Tuner
 */
public class E4KEmbeddedTuner extends EmbeddedTuner
{
    private final static Logger mLog = LoggerFactory.getLogger(E4KEmbeddedTuner.class);

    public static final long MINIMUM_TUNABLE_FREQUENCY_HZ = 52000000;
    public static final long MAXIMUM_TUNABLE_FREQUENCY_HZ = 2200000000l;
    public static final double USABLE_BANDWIDTH_PERCENT = 0.95;
    public static final int DC_SPIKE_AVOID_BUFFER = 15000;

    /**
     * The local oscillator is defined by whole (integer) units of the oscillator frequency and fractional units
     * representing 1/65536th of the oscillator frequency, meaning we can only tune the local oscillator in units of
     * 439.453125 hertz.
     */
    public static final long E4K_PLL_Y = 65536l; /* 16-bit fractional register */
    public static final byte MASTER1_RESET = (byte) 0x01;
    public static final byte MASTER1_NORM_STBY = (byte) 0x02;
    public static final byte MASTER1_POR_DET = (byte) 0x04;

    public static final byte SYNTH1_PLL_LOCK = (byte) 0x01;
    public static final byte SYNTH1_BAND_SHIF = (byte) 0x01;
    public static final byte SYNTH7_3PHASE_EN = (byte) 0x08;
    public static final byte SYNTH8_VCOCAL_UPD = (byte) 0x04;

    public static final byte FILT3_MASK = (byte) 0x20;
    public static final byte FILT3_ENABLE = (byte) 0x00;
    public static final byte FILT3_DISABLE = (byte) 0x20;

    public static final byte MIXER_FILTER_MASK = (byte) 0xF0;
    public static final byte IF_CHANNEL_FILTER_MASK = (byte) 0x1F;
    public static final byte IF_RC_FILTER_MASK = (byte) 0x0F;

    public static final byte AGC1_LIN_MODE = (byte) 0x10;
    public static final byte AGC1_LNA_UPDATE = (byte) 0x20;
    public static final byte AGC1_LNA_G_LOW = (byte) 0x40;
    public static final byte AGC1_LNA_G_HIGH = (byte) 0x80;
    public static final byte AGC1_MOD_MASK = (byte) 0xF;

    public static final byte GAIN1_MOD_MASK = (byte) 0xF;
    public static final byte IF_GAIN_MODE_SWITCHING_MASK = (byte) 0x1;

    public static final byte AGC6_LNA_CAL_REQ = (byte) 0x10;

    public static final byte AGC7_MIXER_GAIN_MASK = (byte) 0x01;
    public static final byte AGC7_MIX_GAIN_MANUAL = (byte) 0x00;
    public static final byte AGC7_MIX_GAIN_AUTO = (byte) 0x01;
    public static final byte ENH_GAIN_MOD_MASK = (byte) 0x07;
    public static final byte MIXER_GAIN_MASK = (byte) 0x01;
    public static final byte DC5_RANGE_DETECTOR_ENABLED_MASK = (byte) 0x04; //DC Offset Detector Enabled
    public static final byte DC5_RANGE_DETECTOR_ENABLED = (byte) 0x04; //DC Offset Detector Enabled

    public E4KEmbeddedTuner(RTL2832TunerController.ControllerAdapter adapter)
    {
        super(adapter);
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.ELONICS_E4000;
    }

    @Override
    public long getMinimumFrequencySupported()
    {
        return MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumFrequencySupported()
    {
        return MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public int getDcSpikeHalfBandwidth()
    {
        return DC_SPIKE_AVOID_BUFFER;
    }

    @Override
    public double getUsableBandwidthPercent()
    {
        return USABLE_BANDWIDTH_PERCENT;
    }

    @Override
    public void setSamplingMode(RTL2832TunerController.SampleMode mode) throws LibUsbException
    {
        switch(mode)
        {
            case QUADRATURE:
                /* Set intermediate frequency to 0 Hz */
                getAdapter().setIFFrequency(0);

                /* Enable I/Q ADC Input */
                getAdapter().writeDemodRegister(RTL2832TunerController.Page.ZERO, (short) 0x08, (short) 0xCD, 1);

                /* Enable zero-IF mode */
                getAdapter().writeDemodRegister(RTL2832TunerController.Page.ONE, (short) 0xB1, (short) 0x1B, 1);

                /* Set default i/q path */
                getAdapter().writeDemodRegister(RTL2832TunerController.Page.ZERO, (short) 0x06, (short) 0x80, 1);
                break;
            case DIRECT:
            default:
                throw new LibUsbException("QUADRATURE mode is the only mode currently supported", LibUsb.ERROR_NOT_SUPPORTED);
        }
    }


    @Override
    public void apply(TunerConfiguration config) throws SourceException
    {
        if(config instanceof E4KTunerConfiguration e4kConfig)
        {
            try
            {
                E4KGain masterGain = e4kConfig.getMasterGain();
                setGain(masterGain, true);

                if(masterGain == E4KGain.MANUAL)
                {
                    E4KMixerGain mixerGain = e4kConfig.getMixerGain();
                    setMixerGain(mixerGain, true);

                    E4KLNAGain lnaGain = e4kConfig.getLNAGain();
                    setLNAGain(lnaGain, true);

                    IFGain ifGain = e4kConfig.getIFGain();
                    setIFGain(ifGain, true);
                }
            }
            catch(UsbException e)
            {
                throw new SourceException("Usb error while applying tuner config", e);
            }
        }
        else
        {
            throw new IllegalArgumentException("Unrecognized tuner config [" + config.getClass() + "]");
        }
    }

    /**
     * Sets the IF filters (mixer, channel and RC) to the correct filter setting
     * for the selected bandwidth/sample rate
     */
    @Override
    public void setSampleRateFilters(int bandwidth) throws SourceException
    {
        /* Determine repeater state so we can restore it when done */
        boolean i2CRepeaterEnabled = getAdapter().isI2CRepeaterEnabled();

        if(!i2CRepeaterEnabled)
        {
            getAdapter().enableI2CRepeater();
        }

        boolean controlI2CRepeater = false;

        MixerFilter mixer = MixerFilter.getFilter(bandwidth);
        setMixerFilter(mixer, controlI2CRepeater);

        ChannelFilter channel = ChannelFilter.getFilter(bandwidth);
        setChannelFilter(channel, controlI2CRepeater);

        RCFilter rc = RCFilter.getFilter(bandwidth);
        setRCFilter(rc, controlI2CRepeater);

        if(!i2CRepeaterEnabled)
        {
            getAdapter().disableI2CRepeater();
        }
    }

    public long getTunedFrequency() throws SourceException
    {
        try
        {
            /* Determine repeater state so we can restore it when done */
            boolean i2CRepeaterEnabled = getAdapter().isI2CRepeaterEnabled();

            if(!i2CRepeaterEnabled)
            {
                getAdapter().enableI2CRepeater();
            }

            boolean controlI2CRepeater = false;
            byte z = (byte) readE4KRegister(Register.SYNTH3, controlI2CRepeater);
            int xHigh = readE4KRegister(Register.SYNTH4, controlI2CRepeater);
            int xLow = readE4KRegister(Register.SYNTH5, controlI2CRepeater);
            int x = (Integer.rotateLeft(xHigh, 8)) | xLow;
            int pllSetting = readE4KRegister(Register.SYNTH7, controlI2CRepeater);
            PLL pll = PLL.fromSetting(pllSetting);

            /* Return the repeater to its previous state */
            if(!i2CRepeaterEnabled)
            {
                getAdapter().disableI2CRepeater();
            }

            return calculateActualFrequency(pll, z, x);
        }
        catch(LibUsbException e)
        {
            throw new SourceException("E4K tuner controller - couldn't get " + "tuned frequency", e);
        }
    }

    @Override
    public synchronized void setTunedFrequency(long frequency) throws SourceException
    {
        /* Get the phase locked loop setting */
        PLL pll = PLL.fromFrequency(frequency);

        /* Z is an integer representing the number of scaled oscillator frequency
         * increments the multiplied frequency. */
        byte z = (byte) (frequency / pll.getScaledOscillator());

        /* remainder is just as it describes.  It is what is left over after we
         * carve out scaled oscillator frequency increments (z) from the desired
         * frequency. */
        int remainder = (int) (frequency - ((z & 0xFF) * pll.getScaledOscillator()));

        /* X is a 16-bit representation of the remainder */
        int x = (int) ((double) remainder / (double) pll.getScaledOscillator() * E4K_PLL_Y);

        /* Calculate the exact (tunable) frequency and apply that to the tuner */
        long actualFrequency = calculateActualFrequency(pll, z, x);

        /**
         * Hack: if we're trying to set the minimum frequency for the E4K, due
         * to rounding errors, 52 mhz becomes 51.999993, so we need to adjust
         * x to get 52.000003 mhz ... otherwise the PLL won't lock on the freq
         */
        if(actualFrequency < getMinimumFrequencySupported())
        {
            x++;
            actualFrequency = calculateActualFrequency(pll, z, x);
        }

        getAdapter().getLock().lock();

        try
        {
            /* Apply the actual frequency */
            getAdapter().enableI2CRepeater();
            boolean controlI2CRepeater = false;
            /* Write the PLL setting */
            writeE4KRegister(Register.SYNTH7, pll.getIndex(), controlI2CRepeater);
            /* Write z (integral) value */
            writeE4KRegister(Register.SYNTH3, z, controlI2CRepeater);
            /* Write the x (fractional) value high-order byte to synth4 register */
            writeE4KRegister(Register.SYNTH4, (byte) (x & 0xFF), controlI2CRepeater);
            /* Write the x (fractional) value low-order byte to synth5 register */
            writeE4KRegister(Register.SYNTH5, (byte) ((Integer.rotateRight(x, 8)) & 0xFF), controlI2CRepeater);
            /* Set the band for the new frequency */
            setBand(actualFrequency, controlI2CRepeater);
            /* Set the filter */
            setRFFilter(actualFrequency, controlI2CRepeater);
            /* Check for PLL lock */
            int lock = readE4KRegister(Register.SYNTH1, controlI2CRepeater);

            if(!((lock & 0x1) == 0x1))
            {
                throw new SourceException("E4K tuner - couldn't achieve PLL lock for frequency [" + actualFrequency +
                        "] lock value [" + lock + "]");
            }

            getAdapter().disableI2CRepeater();
        }
        catch(UsbException e)
        {
            throw new SourceException("E4K tuner controller - error tuning frequency [" + frequency + "]", e);
        }
        finally
        {
            getAdapter().getLock().unlock();
        }
    }

    private long calculateActualFrequency(PLL pll, byte z, int x)
    {
        long whole = pll.getScaledOscillator() * (z & 0xFF);

        int fractional = (int) (pll.getScaledOscillator() *
                ((double) x / (double) E4K_PLL_Y));

        return whole + fractional;
    }

    protected void initTuner() throws UsbException
    {
        getAdapter().enableI2CRepeater();

        boolean i2CRepeaterControl = false;

        /* Perform dummy read */
        readE4KRegister(Register.DUMMY, i2CRepeaterControl);
        /* Reset everything and clear POR indicator. NOTE: register value remains 0010 even after we write 0111 to it */
        writeE4KRegister(Register.MASTER1, (byte) (MASTER1_RESET | MASTER1_NORM_STBY | MASTER1_POR_DET), i2CRepeaterControl);
        /* Configure clock input */
        writeE4KRegister(Register.CLK_INP, (byte) 0x00, i2CRepeaterControl);
        /* Disable clock output */
        writeE4KRegister(Register.REF_CLK, (byte) 0x00, i2CRepeaterControl);
        writeE4KRegister(Register.CLKOUT_PWDN, (byte) 0x96, i2CRepeaterControl);
        //Make the magic happen ...
        magicInit(i2CRepeaterControl);

        /* Initialize DC offset lookup tables */
        generateDCOffsetTables(i2CRepeaterControl);

        /* Enable time variant DC correction */
        writeE4KRegister(Register.DCTIME1, (byte) 0x01, i2CRepeaterControl);
        writeE4KRegister(Register.DCTIME2, (byte) 0x01, i2CRepeaterControl);

        /* Set LNA Mode */
        writeE4KRegister(Register.AGC4, (byte) 0x10, i2CRepeaterControl); //High Threshold
        writeE4KRegister(Register.AGC5, (byte) 0x04, i2CRepeaterControl); //Low Threshold
        writeE4KRegister(Register.AGC6, (byte) 0x1A, i2CRepeaterControl); //LNA calibrate request + slowest loop rate

        //Temp - manual set lna mode to manual
        writeMaskedE4KRegister(Register.AGC1, AGC1_MOD_MASK, AGCMode.SERIAL.getValue(), false);
        //Temp - set mixer gain to manual
        writeMaskedE4KRegister(Register.AGC7, AGC7_MIXER_GAIN_MASK, AGC7_MIX_GAIN_MANUAL, false);

        /* Use automatic gain as a default */
        setLNAGain(E4KLNAGain.AUTOMATIC, i2CRepeaterControl);
        setMixerGain(E4KMixerGain.AUTOMATIC, i2CRepeaterControl);
        setEnhanceGain(E4KEnhanceGain.GAIN_5, i2CRepeaterControl); //Setting of 5 is recommended in datasheet

        /* Set IF gain stages */
        setIFGain(IFGain.LINEARITY_8, i2CRepeaterControl);

        /* Set the narrowest filter we can possibly use */
        setMixerFilter(MixerFilter.BW_1M9, i2CRepeaterControl);
        setRCFilter(RCFilter.BW_1M0, i2CRepeaterControl);
        setChannelFilter(ChannelFilter.BW_2M15, i2CRepeaterControl);
        setChannelFilterEnabled(true, i2CRepeaterControl);

		/* Disable DC detector */
		setDCRangeDetectorEnabled( false, i2CRepeaterControl );
        setDCOffsetLookupTablesEnabled(false, i2CRepeaterControl);

        /* Disable time variant DC correction */
        writeMaskedE4KRegister(Register.DC5, (byte) 0x03, (byte) 0, i2CRepeaterControl);
        writeMaskedE4KRegister(Register.DCTIME1, (byte) 0x03, (byte) 0, i2CRepeaterControl);
        writeMaskedE4KRegister(Register.DCTIME2, (byte) 0x03, (byte) 0, i2CRepeaterControl);

        getAdapter().disableI2CRepeater();
    }

    /**
     * Requests a DC offset calibration for the tuner
     */
    private void requestDcOffsetCalibration(boolean controlI2CRepeater) throws LibUsbException
    {
        if(controlI2CRepeater)
        {
            getAdapter().enableI2CRepeater();
        }

        writeMaskedE4KRegister(Register.DC1, (byte) 0x1, (byte) 0x1, false);

        if(controlI2CRepeater)
        {
            getAdapter().disableI2CRepeater();
        }
    }

    /**
     * Calibrates the DC offset lookup table values for tuner initialization
     */
    private void generateDCOffsetTables(boolean controlI2CRepeater) throws UsbException
    {
        if(controlI2CRepeater)
        {
            getAdapter().enableI2CRepeater();
        }

        boolean i2CRepeaterControl = false;

        //Set all IF gain stages (1-6) to maximum values.  During calibration we'll change IF stage 1 gain
        setIFGain(IFGain.LINEARITY_60, i2CRepeaterControl);

        //Enable DC range detector
        setDCRangeDetectorEnabled(true, i2CRepeaterControl);

        //Enable DC offset lookup tables
        setDCOffsetLookupTablesEnabled(true, i2CRepeaterControl);

        //Step 0
        setIFStage1Gain(IFStage1Gain.GAIN_MINUS3, i2CRepeaterControl);
        setMixerGain(E4KMixerGain.GAIN_4, i2CRepeaterControl);
        requestDcOffsetCalibration(i2CRepeaterControl);
        byte i = getDcOffsetI(i2CRepeaterControl);
        byte q = getDcOffsetQ(i2CRepeaterControl);
        writeE4KRegister(Register.ILUT0, i, i2CRepeaterControl);
        writeE4KRegister(Register.QLUT0, q, i2CRepeaterControl);

        //Step 1
        setMixerGain(E4KMixerGain.GAIN_12, i2CRepeaterControl);
        requestDcOffsetCalibration(i2CRepeaterControl);
        i = getDcOffsetI(i2CRepeaterControl);
        q = getDcOffsetQ(i2CRepeaterControl);
        writeE4KRegister(Register.ILUT1, i, i2CRepeaterControl);
        writeE4KRegister(Register.QLUT1, q, i2CRepeaterControl);

        //Step 2
        setIFStage1Gain(IFStage1Gain.GAIN_PLUS6, i2CRepeaterControl);
        setMixerGain(E4KMixerGain.GAIN_4, i2CRepeaterControl);
        requestDcOffsetCalibration(i2CRepeaterControl);
        i = getDcOffsetI(i2CRepeaterControl);
        q = getDcOffsetQ(i2CRepeaterControl);
        writeE4KRegister(Register.ILUT2, i, i2CRepeaterControl);
        writeE4KRegister(Register.QLUT2, q, i2CRepeaterControl);

        //Step 3
        setMixerGain(E4KMixerGain.GAIN_12, i2CRepeaterControl);
        requestDcOffsetCalibration(i2CRepeaterControl);
        i = getDcOffsetI(i2CRepeaterControl);
        q = getDcOffsetQ(i2CRepeaterControl);
        writeE4KRegister(Register.ILUT3, i, i2CRepeaterControl);
        writeE4KRegister(Register.QLUT3, q, i2CRepeaterControl);

        if(controlI2CRepeater)
        {
            getAdapter().disableI2CRepeater();
        }
    }

    private byte getDcOffsetI(boolean i2CRepeaterControl)
    {
        byte offset = (byte) (readE4KRegister(Register.DC2, i2CRepeaterControl) & 0x3F);
        byte range = (byte) ((readE4KRegister(Register.DC4, i2CRepeaterControl) & 0x3) << 6);
        return (byte) (range | offset);
    }

    private byte getDcOffsetQ(boolean i2CRepeaterControl)
    {
        byte offset = (byte) (readE4KRegister(Register.DC3, i2CRepeaterControl) & 0x3F);
        byte range = (byte) (readE4KRegister(Register.DC4, i2CRepeaterControl) << 2);
        return (byte) (range | offset);
    }

    /**
     * Enables or disables the DC offset lookup tables.
     *
     * @param enabled
     * @param controlI2CRepeater
     * @throws LibUsbException
     */
    public void setDCOffsetLookupTablesEnabled(boolean enabled, boolean controlI2CRepeater) throws LibUsbException
    {
        byte value = enabled ? (byte) 0x3 : (byte) 0x0;
        writeMaskedE4KRegister(Register.DC5, value, (byte) 0x3, controlI2CRepeater);
    }

    /**
     * Enables or disables the DC range detector
     */
    public void setDCRangeDetectorEnabled(boolean enabled, boolean controlI2CRepeater) throws LibUsbException
    {
        byte value = enabled ? DC5_RANGE_DETECTOR_ENABLED : (byte) 0x0;
        writeMaskedE4KRegister(Register.DC5, DC5_RANGE_DETECTOR_ENABLED_MASK, value, controlI2CRepeater);
    }

    /**
     * Sets the LNA gain within the E4K Tuner
     * <p>
     * Note: requires I2C repeater enabled
     *
     * @param gain
     * @throws UsbException
     */
    public void setLNAGain(E4KLNAGain gain, boolean controlI2CRepeater) throws UsbException
    {
        getAdapter().getLock().lock();

        try
        {
            if(controlI2CRepeater)
            {
                getAdapter().enableI2CRepeater();
            }

            if(gain == E4KLNAGain.AUTOMATIC)
            {
                writeMaskedE4KRegister(Register.AGC1, AGC1_MOD_MASK, AGCMode.IF_SERIAL_LNA_AUTON.getValue(), false);
            }
            else
            {
                writeMaskedE4KRegister(Register.AGC1, AGC1_MOD_MASK, AGCMode.SERIAL.getValue(), false);
                writeMaskedE4KRegister(E4KLNAGain.getRegister(), E4KLNAGain.getMask(), gain.getValue(), false);
            }

            if(controlI2CRepeater)
            {
                getAdapter().disableI2CRepeater();
            }
        }
        finally
        {
            getAdapter().getLock().unlock();
        }
    }

    /**
     * Reads LNA gain from E4K tuner
     * <p>
     * Note: requires I2C repeater enabled
     */
    public E4KLNAGain getLNAGain(boolean controlI2CRepeater) throws UsbException
    {
        return E4KLNAGain.fromRegisterValue(readE4KRegister(E4KLNAGain.getRegister(), controlI2CRepeater));
    }

    /**
     * Sets enhanced gain for E4K repeater.
     * <p>
     * Note: requires I2C repeater enabled
     *
     * @param gain
     * @throws UsbException
     */
    public void setEnhanceGain(E4KEnhanceGain gain, boolean controlI2CRepeater) throws UsbException
    {
        writeMaskedE4KRegister(E4KEnhanceGain.getRegister(), E4KEnhanceGain.getMask(), gain.getValue(), controlI2CRepeater);
    }

    /**
     * Gets the enhanced gain setting in the E4K tuner.
     * <p>
     * Note: requires I2C repeater
     *
     * @return
     * @throws UsbException
     */
    public E4KEnhanceGain getEnhanceGain(boolean controlI2CRepeater) throws UsbException
    {
        return E4KEnhanceGain.fromRegisterValue(readE4KRegister(E4KEnhanceGain.getRegister(), controlI2CRepeater));
    }

    /**
     * Sets the mixer gain
     *
     * @param gain to apply
     * @param controlI2CRepeater set to true to turn on/off repeater within this method or false if already enabled.
     * @throws UsbException on error
     */
    public void setMixerGain(E4KMixerGain gain, boolean controlI2CRepeater) throws UsbException
    {
        getAdapter().getLock().lock();

        try
        {
            if(controlI2CRepeater)
            {
                getAdapter().enableI2CRepeater();
            }

            boolean repeaterControl = false;

            if(gain == E4KMixerGain.AUTOMATIC)
            {
                writeMaskedE4KRegister(Register.AGC7, AGC7_MIXER_GAIN_MASK, AGC7_MIX_GAIN_AUTO, repeaterControl);
            }
            else
            {
                writeMaskedE4KRegister(Register.AGC7, AGC7_MIXER_GAIN_MASK, AGC7_MIX_GAIN_MANUAL, repeaterControl);
                /* Set the desired manual gain setting */
                writeMaskedE4KRegister(E4KMixerGain.getRegister(), E4KMixerGain.getMask(), gain.getValue(), repeaterControl);
            }

            if(controlI2CRepeater)
            {
                getAdapter().disableI2CRepeater();
            }
        }
        finally
        {
            getAdapter().getLock().unlock();
        }
    }

    public E4KMixerGain getMixerGain(boolean controlI2CRepeater) throws UsbException
    {
        byte autoOrManual = readMaskedE4KRegister(Register.AGC7, AGC7_MIXER_GAIN_MASK, controlI2CRepeater);

        if(autoOrManual == AGC7_MIX_GAIN_AUTO)
        {
            return E4KMixerGain.AUTOMATIC;
        }
        else
        {
            int register = readE4KRegister(E4KMixerGain.getRegister(), controlI2CRepeater);
            return E4KMixerGain.fromRegisterValue(register);
        }
    }

    /**
     * Sets IF Gains 1 - 6 using a set of linear gain values defined in the IFGain enumeration
     *
     * @param gain to apply
     * @param controlI2CRepeater set false if I2C repeater is already enabled or true if the I2C repeater should
     * be turned on and off within this method.
     * @throws LibUsbException if the write operation doesn't complete successfully
     */
    public void setIFGain(IFGain gain, boolean controlI2CRepeater) throws LibUsbException
    {
        getAdapter().getLock().lock();

        try
        {
            byte linearityMode = gain.isLinearity() ? (byte) 0x00 : (byte) 0x10;
            writeMaskedE4KRegister(Register.AGC1, gain.getLinearityModeMask(), linearityMode, controlI2CRepeater);
            writeMaskedE4KRegister(Register.GAIN3, gain.getGain3Mask(), gain.getGain3(), controlI2CRepeater);
            writeMaskedE4KRegister(Register.GAIN4, gain.getGain4Mask(), gain.getGain4(), controlI2CRepeater);
        }
        finally
        {
            getAdapter().getLock().unlock();
        }
    }

    /**
     * Sets the IF Stage 1 gain value.
     */
    public void setIFStage1Gain(IFStage1Gain gain, boolean controlI2CRepeater)
    {
        if(controlI2CRepeater)
        {
            getAdapter().enableI2CRepeater();
        }

        writeMaskedE4KRegister(Register.GAIN3, gain.getValue(), gain.getMask(), false);

        if(controlI2CRepeater)
        {
            getAdapter().disableI2CRepeater();
        }
    }

    public void setMixerFilter(MixerFilter filter, boolean controlI2CRepeater) throws LibUsbException
    {
        writeMaskedE4KRegister(MixerFilter.getRegister(), MixerFilter.getMask(), filter.getValue(), controlI2CRepeater);
    }

    public MixerFilter getMixerFilter(boolean controlI2CRepeater) throws UsbException
    {
        int value = readE4KRegister(MixerFilter.getRegister(), controlI2CRepeater);
        return MixerFilter.fromRegisterValue(value);
    }

    public void setRCFilter(RCFilter filter, boolean controlI2CRepeater) throws LibUsbException
    {
        writeMaskedE4KRegister(RCFilter.getRegister(), RCFilter.getMask(), filter.getValue(), controlI2CRepeater);
    }

    public RCFilter getRCFilter(boolean controlI2CRepeater) throws UsbException
    {
        int value = readE4KRegister(RCFilter.getRegister(), controlI2CRepeater);
        return RCFilter.fromRegisterValue(value);
    }

    public void setChannelFilter(ChannelFilter filter, boolean controlI2CRepeater) throws LibUsbException
    {
        writeMaskedE4KRegister(ChannelFilter.getRegister(), ChannelFilter.getMask(), filter.getValue(), controlI2CRepeater);
    }

    public ChannelFilter getChannelFilter(boolean controlI2CRepeater) throws UsbException
    {
        int value = readE4KRegister(ChannelFilter.getRegister(), controlI2CRepeater);
        return ChannelFilter.fromRegisterValue(value);
    }

    public void setChannelFilterEnabled(boolean enabled, boolean controlI2CRepeater) throws UsbException
    {
        if(enabled)
        {
            writeMaskedE4KRegister(Register.FILT3, FILT3_MASK, FILT3_ENABLE, controlI2CRepeater);
        }
        else
        {
            writeMaskedE4KRegister(Register.FILT3, FILT3_MASK, FILT3_DISABLE, controlI2CRepeater);
        }
    }

    /**
     * Enables or disables VCO auto-calibration.
     */
    public void setVcoAutoCalibration(boolean enabled, boolean controlI2CRepeater)
    {
        byte value = enabled ? (byte) 0x01 : (byte) 0x00;
        byte mask = (byte) 0x3;
        writeMaskedE4KRegister(Register.SYNTH8, mask, value, controlI2CRepeater);
    }

    public void setBand(long frequency, boolean controlI2CRepeater) throws UsbException
    {
        if(controlI2CRepeater)
        {
            getAdapter().enableI2CRepeater();
        }

        FrequencyBand frequencyBand = FrequencyBand.fromFrequency(frequency);

        /* Set the bias */
        switch(frequencyBand)
        {
            case UHF:
            case VHF2:
            case VHF3:
                writeE4KRegister(Register.BIAS, (byte) 0x3, false);
                break;
            case L_BAND:
            default:
                writeE4KRegister(Register.BIAS, (byte) 0x0, false);
                break;

        }

        /**
         * Workaround - reset register (set value to 0) before writing to it, to
         * avoid a gap around 325-350 MHz
         */
        writeMaskedE4KRegister(Register.SYNTH1, FrequencyBand.getMask(), (byte) 0x0, false);
        writeMaskedE4KRegister(Register.SYNTH1, FrequencyBand.getMask(), frequencyBand.getValue(), false);

        if(controlI2CRepeater)
        {
            getAdapter().disableI2CRepeater();
        }
    }

    private void setRFFilter(long frequency, boolean controlI2CRepeater) throws UsbException
    {
        RFFilter filter = RFFilter.fromFrequency(frequency);
        writeMaskedE4KRegister(Register.FILT1, RFFilter.getMask(), filter.getValue(), controlI2CRepeater);
    }

    /**
     * Sets the master gain combination setting.
     * Note: does not respond to the MANUAL setting.  Use the manual setting to configure gui components to expose the
     * individual Mixer, LNA and Enhance gain settings, to allow the user to change those settings individually.
     *
     * @param gain - requested gain setting
     * @throws UsbException if there are any errors
     */
    public void setGain(E4KGain gain, boolean controlI2CRepeater) throws UsbException
    {
        getAdapter().getLock().lock();

        try
        {
            if(gain != E4KGain.MANUAL)
            {
                if(controlI2CRepeater)
                {
                    getAdapter().enableI2CRepeater();
                }

                boolean i2CRepeaterControl = false;

                setLNAGain(gain.getLNAGain(), i2CRepeaterControl);
                setMixerGain(gain.getMixerGain(), i2CRepeaterControl);

                if(controlI2CRepeater)
                {
                    getAdapter().disableI2CRepeater();
                }
            }
        }
        finally
        {
            getAdapter().getLock().unlock();
        }
    }

    /*
     * Performs magic initialization ... and that's all we need to know, I guess
     */
    private void magicInit(boolean controlI2CRepeater) throws UsbException
    {
        if(controlI2CRepeater)
        {
            getAdapter().enableI2CRepeater();
        }

        writeE4KRegister(Register.MAGIC_1, (byte) 0x01, false);
        writeE4KRegister(Register.MAGIC_2, (byte) 0xFE, false);
        writeE4KRegister(Register.MAGIC_3, (byte) 0x00, false);
        writeE4KRegister(Register.MAGIC_4, (byte) 0x50, false); //Polarity A
        writeE4KRegister(Register.MAGIC_5, (byte) 0x20, false);
        writeE4KRegister(Register.MAGIC_6, (byte) 0x01, false);
        writeE4KRegister(Register.MAGIC_7, (byte) 0x7F, false);
        writeE4KRegister(Register.MAGIC_8, (byte) 0x07, false);

        if(controlI2CRepeater)
        {
            getAdapter().disableI2CRepeater();
        }
    }

    private byte readMaskedE4KRegister(Register register, byte mask, boolean controlI2CRepeater) throws UsbException
    {
        int temp = readE4KRegister(register, controlI2CRepeater);
        return (byte) (temp & mask);
    }

    private void writeMaskedE4KRegister(Register register, byte mask, byte value, boolean controlI2CRepeater) throws LibUsbException
    {
        int temp = readE4KRegister(register, controlI2CRepeater);

        /* If the register is not set to the masked value, then change it */
        if((byte) (temp & mask) != value)
        {
            writeE4KRegister(register, (byte) ((temp & ~mask) | (value & mask)), controlI2CRepeater);
            readE4KRegister(register, controlI2CRepeater);
        }
    }

    private int readE4KRegister(Register register, boolean controlI2CRepeater) throws LibUsbException
    {
        return getAdapter().readI2CRegister(Register.I2C_REGISTER.getValue(), register.getValue(), controlI2CRepeater);
    }


    private void writeE4KRegister(Register register, byte value, boolean controlI2CRepeater) throws LibUsbException
    {
        getAdapter().writeI2CRegister(Register.I2C_REGISTER.getValue(), register.getValue(), value, controlI2CRepeater);
    }

    /**
     * Frequency bands enumeration
     */
    public enum FrequencyBand
    {
        VHF2(0), //64-108 MHz
        VHF3(2), //170-240 MHz
        UHF(4),  //470-858 MHz
        L_BAND(6); //1452-1680 MHz

        private int mValue;

        FrequencyBand(int value)
        {
            mValue = value;
        }

        public static byte getMask()
        {
            return (byte) 0x06;
        }

        public byte getValue()
        {
            return (byte) mValue;
        }

        public String getLabel()
        {
            return toString();
        }

        public static FrequencyBand fromFrequency(long frequency)
        {
            if(frequency < 140000000) //140 MHz
            {
                return VHF2;
            }
            else if(frequency < 350000000) //350 MHz
            {
                return VHF3;
            }
            else if(frequency < 1135000000) //1135 MHz
            {
                return UHF;
            }
            else
            {
                return L_BAND;
            }
        }
    }

    /**
     * PLL enumeration for SYNTH7 (0x0D) Register to control the 'R' VCO output divider and 3-phase mixing.
     * <p>
     * Note: 3-phase mixing (1-bit) and R index (3-bits) are combined in the index value that is written to the register.
     */
    public enum PLL
    {
        PLL_72M4(0x0F, 72400000, 48, 600000, true, "72.4 MHz"),
        PLL_81M2(0x0E, 81200000, 40, 720000, true, "81.2 MHz"),
        PLL_108M3(0x0D, 108300000, 32, 900000, true, "108.3 MHz"),
        PLL_162M5(0x0C, 162500000, 24, 1200000, true, "162.5 MHz"),
        PLL_216M6(0x0B, 216600000, 16, 1800000, true, "216.6 MHz"),
        PLL_325M0(0x0A, 325000000, 12, 2400000, true, "325.0 MHz"),
        PLL_350M0(0x09, 350000000, 8, 3600000, true, "350.0 MHz"),
        PLL_432M0(0x03, 432000000, 8, 3600000, false, "432.0 MHz"),
        PLL_667M0(0x02, 667000000, 6, 4800000, false, "667.0 MHz"),
        PLL_1200M0(0x01, 1200000000, 4, 7200000, false, "1200.0 MHz");

        private int mIndex;
        private long mFrequency;
        private int mMultiplier;
        private int mScaledOscillator;
        private boolean mRequires3PhaseMixing;
        private String mLabel;

        PLL(int index, long frequency, int multiplier, int scaledOscillator, boolean requires3Phase, String label)
        {
            mIndex = index;
            mFrequency = frequency;
            mMultiplier = multiplier;
            mScaledOscillator = scaledOscillator;
            mRequires3PhaseMixing = requires3Phase;
            mLabel = label;
        }

        public byte getIndex()
        {
            return (byte) mIndex;
        }

        public long getFrequency()
        {
            return mFrequency;
        }

        public byte getMultiplier()
        {
            return (byte) mMultiplier;
        }

        public int getScaledOscillator()
        {
            return mScaledOscillator;
        }

        public boolean requires3PhaseMixing()
        {
            return mRequires3PhaseMixing;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public String toString()
        {
            return mLabel;
        }

        /**
         * Returns the PLL setting with the closest frequency that is greater than the frequency argument
         */
        public static PLL fromFrequency(long frequency)
        {
            for(PLL pll : values())
            {
                if(frequency < pll.getFrequency())
                {
                    return pll;
                }
            }

            //Default
            return PLL.PLL_72M4;
        }

        public static PLL fromSetting(int setting)
        {
            int value = setting & 0xF;

            for(PLL pll : values())
            {
                if(value == pll.getIndex())
                {
                    return pll;
                }
            }

            //Default
            return PLL.PLL_72M4;
        }
    }

    /**
     * RF filters - UHF Band.  Each enum entry defines the center frequency for
     * the filter and the applicable frequency minimums and maximums for the
     * filter.
     * <p>
     * Note: max frequency overlaps with the next minimum frequency, so valid
     * frequencies should be less than the maximum frequency defined for the
     * filter.
     */
    public enum RFFilter
    {
        NO_FILTER(0, 0, 0),

        //VHF-II Filters (0.0 - 140.0) Optimal (64-108)
        LP268(0, MINIMUM_TUNABLE_FREQUENCY_HZ, 86_000_000),  //Values 0 - 7 are all the same
        LP299(8, 86_000_000, 140_000_000), //Values 8 - 15 are all the same

        //VHF-III Filters (140.0 - 350.0) Optimal (170-240)
        LP509(0, 140_000_000, 245_000_000), //Values 0 - 7 are all the same
        LP656(8, 245_000_000, 350_000_000), //Values 8 - 15 are all the same

        //UHF Filters (350.0 - 1135) Optimal (470-858)
        BP360(0, 350000000, 370000000),
        BP380(1, 370000000, 392500000),
        BP405(2, 392500000, 417500000),
        BP425(3, 417500000, 437500000),
        BP450(4, 437500000, 462500000),
        BP475(5, 462500000, 490000000),
        BP505(6, 490000000, 522500000),
        BP540(7, 522500000, 557500000),
        BP575(8, 557500000, 595000000),
        BP615(9, 595000000, 642500000),
        BP670(10, 642500000, 695000000),
        BP720(11, 695000000, 740000000),
        BP760(12, 740000000, 800000000),
        BP840(13, 800000000, 865000000),
        BP890(14, 865000000, 930000000),
        BP970(15, 930000000, 1135000000),

        //L Band Filters (1135.0 - 2147.0) Optimal (1452-1680)
        BP1300(0, 1135000000, 1310000000),
        BP1320(1, 1310000000, 1340000000),
        BP1360(2, 1340000000, 1385000000),
        BP1410(3, 1385000000, 1427500000),
        BP1445(4, 1427500000, 1452500000),
        BP1460(5, 1452500000, 1475000000),
        BP1490(6, 1475000000, 1510000000),
        BP1530(7, 1510000000, 1545000000),
        BP1560(8, 1545000000, 1575000000),
        BP1590(9, 1575000000, 1615000000),
        BP1640(10, 1615000000, 1650000000),
        BP1660(11, 1650000000, 1670000000),
        BP1680(12, 1670000000, 1690000000),
        BP1700(13, 1690000000, 1710000000),
        BP1720(14, 1710000000, 1735000000),
        BP1750(15, 1735000000, MAXIMUM_TUNABLE_FREQUENCY_HZ);

        private int mValue;
        private long mMinFrequency;
        private long mMaxFrequency;

        RFFilter(int value, long minFrequency, long maxFrequency)
        {
            mValue = value;
            mMinFrequency = minFrequency;
            mMaxFrequency = maxFrequency;
        }

        public static Register getRegister()
        {
            return Register.FILT1;
        }

        public static byte getMask()
        {
            return (byte) 0xF;
        }

        public byte getValue()
        {
            return (byte) mValue;
        }

        /**
         * Minimum frequency for this filter.
         */
        public long getMinimumFrequency()
        {
            return mMinFrequency;
        }

        /**
         * Maximum frequency for this filter is less than this value, but does
         * not include this value.
         */
        public long getMaximumFrequency()
        {
            return mMaxFrequency;
        }

        /**
         * Filter ranges by frequency band
         */
        public static EnumSet<RFFilter> VHFII_FILTERS = EnumSet.of(LP268, LP299);
        public static EnumSet<RFFilter> VHFIII_FILTERS = EnumSet.of(LP509, LP656);
        public static EnumSet<RFFilter> UHF_FILTERS = EnumSet.range(BP360, BP970);
        public static EnumSet<RFFilter> LBAND_FILTERS = EnumSet.of(BP1300, BP1750);

        /**
         * Selects the appropriate filter for the frequency
         *
         * @param frequency
         * @return selected filter
         * @throws IllegalArgumentException if the frequency is outside the
         * max value (2200 MHz)
         */
        public static RFFilter fromFrequency(long frequency)
        {
            switch(FrequencyBand.fromFrequency(frequency))
            {
                case VHF2:
                    for(RFFilter filter : VHFII_FILTERS)
                    {
                        if(filter.getMinimumFrequency() <= frequency && frequency < filter.getMaximumFrequency())
                        {
                            return filter;
                        }
                    }
                    break;
                case VHF3:
                    for(RFFilter filter : VHFIII_FILTERS)
                    {
                        if(filter.getMinimumFrequency() <= frequency && frequency < filter.getMaximumFrequency())
                        {
                            return filter;
                        }
                    }
                    break;
                case UHF:
                    for(RFFilter filter : UHF_FILTERS)
                    {
                        if(filter.getMinimumFrequency() <= frequency && frequency < filter.getMaximumFrequency())
                        {
                            return filter;
                        }
                    }
                    break;
                case L_BAND:
                default:
                    for(RFFilter filter : LBAND_FILTERS)
                    {
                        if(filter.getMinimumFrequency() <= frequency && frequency < filter.getMaximumFrequency())
                        {
                            return filter;
                        }
                    }
                    break;
            }

            return NO_FILTER;
        }
    }

    public enum MixerFilter
    {
        BW_27M0(0x00, 27000000, 4800000, 28800000, "27.0 MHz"),
        BW_4M6(0x80, 4600000, 4400000, 4800000, "4.6 MHz"),
        BW_4M2(0x90, 4200000, 4000000, 4400000, "4.2 MHz"),
        BW_3M8(0xA0, 3800000, 3600000, 4000000, "3.8 MHz"),
        BW_3M4(0xB0, 3400000, 3200000, 3600000, "3.4 MHz"),
        BW_3M0(0xC0, 3000000, 2850000, 3200000, "3.0 MHz"),
        BW_2M7(0xD0, 2700000, 2500000, 2850000, "2.7 MHz"),
        BW_2M3(0xE0, 2300000, 2100000, 2500000, "2.3 MHz"),
        BW_1M9(0xF0, 1900000, 0, 2100000, "1.9 MHz");

        private int mValue;
        private int mBandwidth;
        private int mMinBandwidth;
        private int mMaxBandwidth;
        private String mLabel;

        MixerFilter(int value, int bandwidth, int minimumBandwidth, int maximumBandwidth, String label)
        {
            mValue = value;
            mBandwidth = bandwidth;
            mMinBandwidth = minimumBandwidth;
            mMaxBandwidth = maximumBandwidth;
            mLabel = label;
        }

        public static Register getRegister()
        {
            return Register.FILT2;
        }

        public static byte getMask()
        {
            return MIXER_FILTER_MASK;
        }

        public byte getValue()
        {
            return (byte) mValue;
        }

        public int getBandwidth()
        {
            return mBandwidth;
        }

        public int getMinimumBandwidth()
        {
            return mMinBandwidth;
        }

        public int getMaximumBandwidth()
        {
            return mMaxBandwidth;
        }

        public String getLabel()
        {
            return mLabel;
        }

        /**
         * Returns the correct filter to use for the bandwidth/sample rate
         *
         * @param bandwidth - current sample rate
         * @return - correct filter to use for the bandwidth/sample rate
         */
        public static MixerFilter getFilter(int bandwidth)
        {
            for(MixerFilter filter : values())
            {
                if(filter.getMinimumBandwidth() <= bandwidth && bandwidth < filter.getMaximumBandwidth())
                {
                    return filter;
                }
            }

            /* default */
            return MixerFilter.BW_27M0;
        }

        public static MixerFilter fromRegisterValue(int registerValue)
        {
            int value = registerValue & getMask();

            for(MixerFilter filter : values())
            {
                if(value == filter.getValue())
                {
                    return filter;
                }
            }

            throw new IllegalArgumentException("E4KTunerController - unrecognized mixer filter value [" + value + "]");
        }
    }

    public enum RCFilter
    {
        BW_21M4(0x00, 21400000, 21200000, 28800000, "21.4 MHz"),
        BW_21M0(0x01, 21000000, 19300000, 21200000, "21.0 MHz"),
        BW_17M6(0x02, 17600000, 16150000, 19300000, "17.6 MHz"),
        BW_14M7(0x03, 14700000, 13550000, 16150000, "14.7 MHz"),
        BW_12M4(0x04, 12400000, 11500000, 13550000, "12.4 MHz"),
        BW_10M6(0x05, 10600000, 9800000, 11500000, "10.6 MHz"),
        BW_9M0(0x06, 9000000, 8350000, 9800000, "9.0 MHz"),
        BW_7M7(0x07, 7700000, 7050000, 8350000, "7.7 MHz"),
        BW_6M4(0x08, 6400000, 5805000, 7050000, "6.4 MHz"),
        BW_5M3(0x09, 5300000, 4850000, 5805000, "5.3 MHz"),
        BW_4M4(0x0A, 4400000, 3900000, 4850000, "4.4 MHz"),
        BW_3M4(0x0B, 3400000, 3000000, 3900000, "3.4 MHz"),
        BW_2M6(0x0C, 2600000, 2200000, 3000000, "2.6 MHz"),
        BW_1M8(0x0D, 1800000, 1500000, 2200000, "1.8 MHz"),
        BW_1M2(0x0E, 1200000, 1100000, 1500000, "1.2 MHz"),
        BW_1M0(0x0F, 1000000, 0, 1100000, "1.0 MHz");

        private int mValue;
        private int mBandwidth;
        private int mMinBandwidth;
        private int mMaxBandwidth;
        private String mLabel;

        RCFilter(int value, int bandwidth, int minimumBandwidth, int maximumBandwidth, String label)
        {
            mValue = value;
            mBandwidth = bandwidth;
            mMinBandwidth = minimumBandwidth;
            mMaxBandwidth = maximumBandwidth;
            mLabel = label;
        }

        public static Register getRegister()
        {
            return Register.FILT2;
        }

        public static byte getMask()
        {
            return IF_RC_FILTER_MASK;
        }

        public byte getValue()
        {
            return (byte) mValue;
        }

        public int getBandwidth()
        {
            return mBandwidth;
        }

        public int getMinimumBandwidth()
        {
            return mMinBandwidth;
        }

        public int getMaximumBandwidth()
        {
            return mMaxBandwidth;
        }

        public String getLabel()
        {
            return mLabel;
        }

        /**
         * Returns the correct filter to use for the bandwidth/sample rate
         *
         * @param bandwidth - current sample rate
         * @return - correct filter to use for the bandwidth/sample rate
         */
        public static RCFilter getFilter(int bandwidth)
        {
            for(RCFilter filter : values())
            {
                if(filter.getMinimumBandwidth() <= bandwidth && bandwidth < filter.getMaximumBandwidth())
                {
                    return filter;
                }
            }

            /* default */
            return RCFilter.BW_21M4;
        }

        public static RCFilter fromRegisterValue(int registerValue)
        {
            int value = registerValue & getMask();

            for(RCFilter filter : values())
            {
                if(value == filter.getValue())
                {
                    return filter;
                }
            }

            throw new IllegalArgumentException("E4KTunerController - unrecognized rc filter value [" + value + "]");
        }
    }

    public enum ChannelFilter
    {
        BW_5M50(0x00, 5500000, 5500000, 28800000, "5.50 MHz"),
        BW_5M30(0x01, 5300000, 5300000, 5500000, "5.30 MHz"),
        BW_5M00(0x02, 5000000, 5000000, 5300000, "5.00 MHz"),
        BW_4M80(0x03, 4800000, 4800000, 5000000, "4.80 MHz"),
        BW_4M60(0x04, 4600000, 4600000, 4800000, "4.60 MHz"),
        BW_4M40(0x05, 4400000, 4400000, 4600000, "4.40 MHz"),
        BW_4M30(0x06, 4300000, 4300000, 4400000, "4.30 MHz"),
        BW_4M10(0x07, 4100000, 4100000, 4300000, "4.10 MHz"),
        BW_3M90(0x08, 3900000, 3900000, 4100000, "3.90 MHz"),
        BW_3M80(0x09, 3800000, 3800000, 3900000, "3.80 MHz"),
        BW_3M70(0x0A, 3700000, 3700000, 3800000, "3.70 MHz"),
        BW_3M60(0x0B, 3600000, 3600000, 3700000, "3.60 MHz"),
        BW_3M40(0x0C, 3400000, 3400000, 3600000, "3.40 MHz"),
        BW_3M30(0x0D, 3300000, 3300000, 3400000, "3.30 MHz"),
        BW_3M20(0x0E, 3200000, 3200000, 3300000, "3.20 MHz"),
        BW_3M10(0x0F, 3100000, 3100000, 3200000, "3.10 MHz"),
        BW_3M00(0x10, 3000000, 3000000, 3100000, "3.00 MHz"),
        BW_2M95(0x11, 2950000, 2950000, 3000000, "2.95 MHz"),
        BW_2M90(0x12, 2900000, 2900000, 2950000, "2.90 MHz"),
        BW_2M80(0x13, 2800000, 2800000, 2900000, "2.80 MHz"),
        BW_2M75(0x14, 2750000, 2750000, 2800000, "2.75 MHz"),
        BW_2M70(0x15, 2700000, 2700000, 2750000, "2.70 MHz"),
        BW_2M60(0x16, 2600000, 2600000, 2700000, "2.60 MHz"),
        BW_2M55(0x17, 2550000, 2550000, 2600000, "2.55 MHz"),
        BW_2M50(0x18, 2500000, 2500000, 2550000, "2.50 MHz"),
        BW_2M45(0x19, 2450000, 2450000, 2500000, "2.45 MHz"),
        BW_2M40(0x1A, 2400000, 2400000, 2450000, "2.40 MHz"),
        BW_2M30(0x1B, 2300000, 2300000, 2400000, "2.30 MHz"),
        BW_2M28(0x1C, 2280000, 2280000, 2300000, "2.28 MHz"),
        BW_2M24(0x1D, 2240000, 2240000, 2280000, "2.24 MHz"),
        BW_2M20(0x1E, 2200000, 2200000, 2240000, "2.20 MHz"),
        BW_2M15(0x1F, 2150000, 0, 2150000, "2.15 MHz");

        private int mValue;
        private int mBandwidth;
        private int mMinBandwidth;
        private int mMaxBandwidth;
        private String mLabel;

        ChannelFilter(int value, int bandwidth, int minimumBandwidth, int maximumBandwidth, String label)
        {
            mValue = value;
            mBandwidth = bandwidth;
            mMinBandwidth = minimumBandwidth;
            mMaxBandwidth = maximumBandwidth;
            mLabel = label;
        }

        public static Register getRegister()
        {
            return Register.FILT3;
        }

        public static byte getMask()
        {
            return IF_CHANNEL_FILTER_MASK;
        }

        public byte getValue()
        {
            return (byte) mValue;
        }

        public int getBandwidth()
        {
            return mBandwidth;
        }

        public int getMinimumBandwidth()
        {
            return mMinBandwidth;
        }

        public int getMaximumBandwidth()
        {
            return mMaxBandwidth;
        }

        public String getLabel()
        {
            return mLabel;
        }

        /**
         * Returns the correct filter to use for the bandwidth/sample rate
         *
         * @param bandwidth - current sample rate
         * @return - correct filter to use for the bandwidth/sample rate
         */
        public static ChannelFilter getFilter(int bandwidth)
        {
            for(ChannelFilter filter : values())
            {
                if(filter.getMinimumBandwidth() <= bandwidth && bandwidth < filter.getMaximumBandwidth())
                {
                    return filter;
                }
            }

            /* default */
            return ChannelFilter.BW_5M50;
        }

        public static ChannelFilter fromRegisterValue(int registerValue)
        {
            int value = registerValue & getMask();

            for(ChannelFilter filter : values())
            {
                if(value == filter.getValue())
                {
                    return filter;
                }
            }

            throw new IllegalArgumentException("E4KTunerController - unrecognized channel filter value [" + value + "]");
        }
    }

    public enum E4KLNAGain
    {
        AUTOMATIC(-1, "Auto"),
        GAIN_MINUS_50(0, "-5.0 db"),
        GAIN_MINUS_25(1, "-2.5 db"),
        GAIN_PLUS_0(4, "0.0 db"),
        GAIN_PLUS_25(5, "2.5 db"),
        GAIN_PLUS_50(6, "5.0 db"),
        GAIN_PLUS_75(7, "7.5 db"),
        GAIN_PLUS_100(8, "10.0 db"),
        GAIN_PLUS_125(9, "12.5 db"),
        GAIN_PLUS_150(10, "15.0 db"),
        GAIN_PLUS_175(11, "17.5 db"),
        GAIN_PLUS_200(12, "20.0 db"),
        GAIN_PLUS_250(13, "25.0 db"),
        GAIN_PLUS_300(14, "30.0 db");

        private int mValue;
        private String mLabel;

        E4KLNAGain(int value, String label)
        {
            mValue = value;
            mLabel = label;
        }

        public static Register getRegister()
        {
            return Register.GAIN1;
        }

        public static byte getMask()
        {
            return GAIN1_MOD_MASK;
        }

        public byte getValue()
        {
            return (byte) mValue;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public String toString()
        {
            return mLabel;
        }

        public static E4KLNAGain fromRegisterValue(int registerValue)
        {
            int value = registerValue & getMask();

            for(E4KLNAGain setting : values())
            {
                if(value == setting.getValue())
                {
                    return setting;
                }
            }

            throw new IllegalArgumentException("E4KTunerController - unrecognized LNA Gain value [" + value + "]");
        }
    }

    /**
     * Defines the default gain settings that are exposed to the user.
     * <p>
     * Manual is an override that keeps each of the settings intact and allows
     * the user to change each of them individually
     */
    public enum E4KGain
    {
        AUTOMATIC("Auto", E4KMixerGain.AUTOMATIC, E4KLNAGain.AUTOMATIC),
        MANUAL("Manual", null, null),
        MINUS_10("-1.0 db", E4KMixerGain.GAIN_4, E4KLNAGain.GAIN_MINUS_50),
        PLUS_15("1.5 db", E4KMixerGain.GAIN_4, E4KLNAGain.GAIN_MINUS_25),
        PLUS_40("4.0 db", E4KMixerGain.GAIN_4, E4KLNAGain.GAIN_PLUS_0),
        PLUS_65("6.5 db", E4KMixerGain.GAIN_4, E4KLNAGain.GAIN_PLUS_25),
        PLUS_90("9.0 db", E4KMixerGain.GAIN_4, E4KLNAGain.GAIN_PLUS_50),
        PLUS_115("11.5 db", E4KMixerGain.GAIN_4, E4KLNAGain.GAIN_PLUS_75),
        PLUS_140("14.0 db", E4KMixerGain.GAIN_4, E4KLNAGain.GAIN_PLUS_100),
        PLUS_165("16.5 db", E4KMixerGain.GAIN_4, E4KLNAGain.GAIN_PLUS_125),
        PLUS_190("19.0 db", E4KMixerGain.GAIN_4, E4KLNAGain.GAIN_PLUS_150),
        PLUS_215("21.5 db", E4KMixerGain.GAIN_4, E4KLNAGain.GAIN_PLUS_175),
        PLUS_240("24.0 db", E4KMixerGain.GAIN_4, E4KLNAGain.GAIN_PLUS_200),
        PLUS_290("29.0 db", E4KMixerGain.GAIN_4, E4KLNAGain.GAIN_PLUS_250),
        PLUS_340("34.0 db", E4KMixerGain.GAIN_4, E4KLNAGain.GAIN_PLUS_300),
        PLUS_420("42.0 db", E4KMixerGain.GAIN_12, E4KLNAGain.GAIN_PLUS_300);

        private String mLabel;
        private E4KMixerGain mMixerGain;
        private E4KLNAGain mLNAGain;

        E4KGain(String label, E4KMixerGain mixerGain, E4KLNAGain lnaGain)
        {
            mLabel = label;
            mMixerGain = mixerGain;
            mLNAGain = lnaGain;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public String toString()
        {
            return mLabel;
        }

        public E4KMixerGain getMixerGain()
        {
            return mMixerGain;
        }

        public E4KLNAGain getLNAGain()
        {
            return mLNAGain;
        }
    }

    public enum E4KEnhanceGain
    {
        AUTOMATIC(0, "Auto"),
        GAIN_1(1, "10 db"),
        GAIN_3(3, "30 db"),
        GAIN_5(5, "50 db"), //Recommended
        GAIN_7(7, "70 db");

        private int mValue;
        private String mLabel;

        E4KEnhanceGain(int value, String label)
        {
            mValue = value;
            mLabel = label;
        }

        public static Register getRegister()
        {
            return Register.AGC11;
        }

        public static byte getMask()
        {
            return ENH_GAIN_MOD_MASK;
        }

        public byte getValue()
        {
            return (byte) mValue;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public String toString()
        {
            return mLabel;
        }

        public static E4KEnhanceGain fromRegisterValue(int registerValue)
        {
            int value = registerValue & getMask();

            for(E4KEnhanceGain setting : values())
            {
                if(value == setting.getValue())
                {
                    return setting;
                }
            }

            throw new IllegalArgumentException("E4KTunerController - unrecognized Enhance Gain value [" + value + "]");
        }
    }

    public enum DCGainCombination
    {
        LOOKUP_TABLE_0(Register.QLUT0, Register.ILUT0, E4KMixerGain.GAIN_4, IFStage1Gain.GAIN_MINUS3),
        LOOKUP_TABLE_1(Register.QLUT1, Register.ILUT1, E4KMixerGain.GAIN_4, IFStage1Gain.GAIN_PLUS6),
        LOOKUP_TABLE_2(Register.QLUT2, Register.ILUT2, E4KMixerGain.GAIN_12, IFStage1Gain.GAIN_MINUS3),
        LOOKUP_TABLE_3(Register.QLUT3, Register.ILUT3, E4KMixerGain.GAIN_12, IFStage1Gain.GAIN_PLUS6);

        private Register mQRegister;
        private Register mIRegister;
        private E4KMixerGain mMixerGain;
        private IFStage1Gain mIFStage1Gain;

        DCGainCombination(Register qRegister, Register iRegister, E4KMixerGain mixer, IFStage1Gain stage1)
        {
            mQRegister = qRegister;
            mIRegister = iRegister;
            mMixerGain = mixer;
            mIFStage1Gain = stage1;
        }

        public Register getQRegister()
        {
            return mQRegister;
        }

        public Register getIRegister()
        {
            return mIRegister;
        }

        public E4KMixerGain getMixerGain()
        {
            return mMixerGain;
        }

        public IFStage1Gain getIFStage1Gain()
        {
            return mIFStage1Gain;
        }
    }


    public enum E4KMixerGain
    {
        AUTOMATIC(-1, "Auto"),
        GAIN_4(0, "4 db"),
        GAIN_12(1, "12 db");

        private int mValue;
        private String mLabel;

        E4KMixerGain(int value, String label)
        {
            mValue = value;
            mLabel = label;
        }

        public static Register getRegister()
        {
            return Register.GAIN2;
        }

        public static byte getMask()
        {
            return MIXER_GAIN_MASK;
        }

        public byte getValue()
        {
            return (byte) mValue;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public String toString()
        {
            return mLabel;
        }

        public static E4KMixerGain fromRegisterValue(int registerValue)
        {
            int value = registerValue & getMask();

            for(E4KMixerGain setting : values())
            {
                if(value == setting.getValue())
                {
                    return setting;
                }
            }

            throw new IllegalArgumentException("E4KTunerController - unrecognized Mixer Gain value [" + value + "]");
        }
    }

    /**
     * Combines IF Gains 1 - 4 (register 0x16h GAIN3) and IF Gains 5 - 6 (register 0x17h GAIN4) into a contiguous
     * range if IF gains, per data sheet table page 38, for both Linearity and Sensitivity modes.
     */
    public enum IFGain
    {
        LINEARITY_6((byte) 0x00, (byte) 0x00, "6"),
        LINEARITY_7((byte) 0x20, (byte) 0x00, "7"),
        LINEARITY_8((byte) 0x40, (byte) 0x00, "8"),
        LINEARITY_9((byte) 0x00, (byte) 0x08, "9"),
        LINEARITY_10((byte) 0x20, (byte) 0x08, "10"),
        LINEARITY_11((byte) 0x40, (byte) 0x08, "11"),
        LINEARITY_12((byte) 0x00, (byte) 0x10, "12"),
        LINEARITY_13((byte) 0x20, (byte) 0x10, "13"),
        LINEARITY_14((byte) 0x40, (byte) 0x10, "14"),
        LINEARITY_15((byte) 0x00, (byte) 0x18, "15"),
        LINEARITY_16((byte) 0x20, (byte) 0x18, "16"),
        LINEARITY_17((byte) 0x40, (byte) 0x18, "17"),
        LINEARITY_18((byte) 0x00, (byte) 0x20, "18"),
        LINEARITY_19((byte) 0x20, (byte) 0x20, "19"),
        LINEARITY_20((byte) 0x40, (byte) 0x20, "20"),
        LINEARITY_21((byte) 0x00, (byte) 0x21, "21"),
        LINEARITY_22((byte) 0x20, (byte) 0x21, "22"),
        LINEARITY_23((byte) 0x40, (byte) 0x21, "23"),
        LINEARITY_24((byte) 0x00, (byte) 0x22, "24"),
        LINEARITY_25((byte) 0x20, (byte) 0x22, "25"),
        LINEARITY_26((byte) 0x40, (byte) 0x22, "26"),
        LINEARITY_27((byte) 0x00, (byte) 0x23, "27"),
        LINEARITY_28((byte) 0x20, (byte) 0x23, "28"),
        LINEARITY_29((byte) 0x40, (byte) 0x23, "29"),
        LINEARITY_30((byte) 0x00, (byte) 0x24, "30"),
        LINEARITY_31((byte) 0x20, (byte) 0x24, "31"),
        LINEARITY_32((byte) 0x40, (byte) 0x24, "32"),
        LINEARITY_33((byte) 0x08, (byte) 0x24, "33"),
        LINEARITY_34((byte) 0x28, (byte) 0x24, "34"),
        LINEARITY_35((byte) 0x48, (byte) 0x24, "35"),
        LINEARITY_36((byte) 0x10, (byte) 0x24, "36"),
        LINEARITY_37((byte) 0x30, (byte) 0x24, "37"),
        LINEARITY_38((byte) 0x50, (byte) 0x24, "38"),
        LINEARITY_39((byte) 0x18, (byte) 0x24, "39"),
        LINEARITY_40((byte) 0x38, (byte) 0x24, "40"),
        LINEARITY_41((byte) 0x58, (byte) 0x24, "41"),
        LINEARITY_42((byte) 0x1A, (byte) 0x24, "42"),
        LINEARITY_43((byte) 0x3A, (byte) 0x24, "43"),
        LINEARITY_44((byte) 0x5A, (byte) 0x24, "44"),
        LINEARITY_45((byte) 0x1C, (byte) 0x24, "45"),
        LINEARITY_46((byte) 0x3C, (byte) 0x24, "46"),
        LINEARITY_47((byte) 0x5C, (byte) 0x24, "47"),
        LINEARITY_48((byte) 0x19, (byte) 0x24, "48"),
        LINEARITY_49((byte) 0x39, (byte) 0x24, "49"),
        LINEARITY_50((byte) 0x59, (byte) 0x24, "50"),
        LINEARITY_51((byte) 0x1B, (byte) 0x24, "51"),
        LINEARITY_52((byte) 0x3B, (byte) 0x24, "52"),
        LINEARITY_53((byte) 0x5B, (byte) 0x24, "53"),
        LINEARITY_54((byte) 0x1D, (byte) 0x24, "54"),
        LINEARITY_55((byte) 0x3D, (byte) 0x24, "55"),
        LINEARITY_56((byte) 0x5D, (byte) 0x24, "56"),
        LINEARITY_57((byte) 0x1F, (byte) 0x24, "57"),
        LINEARITY_58((byte) 0x3F, (byte) 0x24, "58"),
        LINEARITY_59((byte) 0x5F, (byte) 0x24, "59"),
        LINEARITY_60((byte) 0x7F, (byte) 0x24, "60"),

        SENSITIVITY_6((byte) 0x00, (byte) 0x00, "6"),
        SENSITIVITY_7((byte) 0x20, (byte) 0x00, "7"),
        SENSITIVITY_8((byte) 0x40, (byte) 0x00, "8"),
        SENSITIVITY_9((byte) 0x02, (byte) 0x00, "9"),
        SENSITIVITY_10((byte) 0x22, (byte) 0x00, "10"),
        SENSITIVITY_11((byte) 0x44, (byte) 0x00, "11"),
        SENSITIVITY_12((byte) 0x04, (byte) 0x00, "12"),
        SENSITIVITY_13((byte) 0x24, (byte) 0x00, "13"),
        SENSITIVITY_14((byte) 0x44, (byte) 0x00, "14"),
        SENSITIVITY_15((byte) 0x01, (byte) 0x00, "15"),
        SENSITIVITY_16((byte) 0x21, (byte) 0x00, "16"),
        SENSITIVITY_17((byte) 0x41, (byte) 0x00, "17"),
        SENSITIVITY_18((byte) 0x03, (byte) 0x00, "18"),
        SENSITIVITY_19((byte) 0x23, (byte) 0x00, "19"),
        SENSITIVITY_20((byte) 0x43, (byte) 0x00, "20"),
        SENSITIVITY_21((byte) 0x05, (byte) 0x00, "21"),
        SENSITIVITY_22((byte) 0x25, (byte) 0x00, "22"),
        SENSITIVITY_23((byte) 0x45, (byte) 0x00, "23"),
        SENSITIVITY_24((byte) 0x07, (byte) 0x00, "24"),
        SENSITIVITY_25((byte) 0x27, (byte) 0x00, "25"),
        SENSITIVITY_26((byte) 0x47, (byte) 0x00, "26"),
        SENSITIVITY_27((byte) 0x0F, (byte) 0x00, "27"),
        SENSITIVITY_28((byte) 0x2F, (byte) 0x00, "28"),
        SENSITIVITY_29((byte) 0x4F, (byte) 0x00, "29"),
        SENSITIVITY_30((byte) 0x17, (byte) 0x00, "30"),
        SENSITIVITY_31((byte) 0x37, (byte) 0x00, "31"),
        SENSITIVITY_32((byte) 0x57, (byte) 0x00, "32"),
        SENSITIVITY_33((byte) 0x1F, (byte) 0x00, "33"),
        SENSITIVITY_34((byte) 0x3F, (byte) 0x00, "34"),
        SENSITIVITY_35((byte) 0x5F, (byte) 0x00, "35"),
        SENSITIVITY_36((byte) 0x1F, (byte) 0x01, "36"),
        SENSITIVITY_37((byte) 0x3F, (byte) 0x01, "37"),
        SENSITIVITY_38((byte) 0x5F, (byte) 0x01, "38"),
        SENSITIVITY_39((byte) 0x1F, (byte) 0x02, "39"),
        SENSITIVITY_40((byte) 0x3F, (byte) 0x02, "40"),
        SENSITIVITY_41((byte) 0x5F, (byte) 0x02, "41"),
        SENSITIVITY_42((byte) 0x1F, (byte) 0x03, "42"),
        SENSITIVITY_43((byte) 0x3F, (byte) 0x03, "43"),
        SENSITIVITY_44((byte) 0x5F, (byte) 0x03, "44"),
        SENSITIVITY_45((byte) 0x1F, (byte) 0x04, "45"),
        SENSITIVITY_46((byte) 0x3F, (byte) 0x04, "46"),
        SENSITIVITY_47((byte) 0x5F, (byte) 0x04, "47"),
        SENSITIVITY_48((byte) 0x1F, (byte) 0x0C, "48"),
        SENSITIVITY_49((byte) 0x3F, (byte) 0x0C, "49"),
        SENSITIVITY_50((byte) 0x5F, (byte) 0x0C, "50"),
        SENSITIVITY_51((byte) 0x1F, (byte) 0x14, "51"),
        SENSITIVITY_52((byte) 0x3F, (byte) 0x14, "52"),
        SENSITIVITY_53((byte) 0x5F, (byte) 0x14, "53"),
        SENSITIVITY_54((byte) 0x1F, (byte) 0x1C, "54"),
        SENSITIVITY_55((byte) 0x3F, (byte) 0x1C, "55"),
        SENSITIVITY_56((byte) 0x5F, (byte) 0x1C, "56"),
        SENSITIVITY_57((byte) 0x1F, (byte) 0x24, "57"),
        SENSITIVITY_58((byte) 0x3F, (byte) 0x24, "58"),
        SENSITIVITY_59((byte) 0x5F, (byte) 0x24, "59"),
        SENSITIVITY_60((byte) 0x7F, (byte) 0x24, "60");

        private byte mGain3;
        private byte mGain4;
        private String mLabel;

        IFGain(byte gain3, byte gain4, String label)
        {
            mGain3 = gain3;
            mGain4 = gain4;
            mLabel = label;
        }

        /**
         * Value to apply to AGC3 register
         */
        public byte getGain3()
        {
            return mGain3;
        }

        /**
         * Value to apply to AGC4 register
         */
        public byte getGain4()
        {
            return mGain4;
        }

        /**
         * Register mask for AGC3
         */
        public byte getGain3Mask()
        {
            return (byte) 0x7F;
        }

        /**
         * Register mask for AGC4
         */
        public byte getGain4Mask()
        {
            return (byte) 0x3F;
        }

        /**
         * Linearity mode mask for AGC1 (0x1A) register
         */
        public byte getLinearityModeMask()
        {
            return (byte) 0x10;
        }

        /**
         * Set of linearity gains
         */
        public static EnumSet<IFGain> LINEARITY_GAINS = EnumSet.range(LINEARITY_6, LINEARITY_60);

        /**
         * Indicates if this is a linearity or sensitivity mode
         *
         * @return true if linearity or false if sensitivity mode
         */
        public boolean isLinearity()
        {
            return LINEARITY_GAINS.contains(this);
        }

        @Override
        public String toString()
        {
            if(isLinearity())
            {
                return "Linearity " + mLabel + " dB";
            }
            else
            {
                return "Sensitivity " + mLabel + " dB";
            }
        }
    }

    public enum IFStage1Gain
    {
        GAIN_MINUS3(0x0, "-3 db"),
        GAIN_PLUS6(0x1, "6 db");

        private int mValue;
        private String mLabel;

        IFStage1Gain(int value, String label)
        {
            mValue = value;
            mLabel = label;
        }

        public static Register getRegister()
        {
            return Register.GAIN3;
        }

        public byte getMask()
        {
            return (byte) 0x1;
        }

        public byte getValue()
        {
            return (byte) mValue;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public String toString()
        {
            return mLabel;
        }
    }

    public enum IFStage2Gain
    {
        GAIN_PLUS0(0x0, "0 db"),
        GAIN_PLUS3(0x2, "3 db"),
        GAIN_PLUS6(0x4, "6 db"),
        GAIN_PLUS9(0x6, "9 db");

        private int mValue;
        private String mLabel;

        IFStage2Gain(int value, String label)
        {
            mValue = value;
            mLabel = label;
        }

        public static Register getRegister()
        {
            return Register.GAIN3;
        }

        public static byte getMask()
        {
            return (byte) 0x6;
        }

        public byte getValue()
        {
            return (byte) mValue;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public String toString()
        {
            return mLabel;
        }

        public static IFStage2Gain fromRegisterValue(int registerValue)
        {
            int value = registerValue & getMask();

            for(IFStage2Gain setting : values())
            {
                if(value == setting.getValue())
                {
                    return setting;
                }
            }

            throw new IllegalArgumentException("E4KTunerController - unrecognized IF Gain Stage 2 value [" + value + "]");
        }
    }

    public enum IFStage3Gain
    {
        GAIN_PLUS0(0x00, "0 db"),
        GAIN_PLUS3(0x08, "3 db"),
        GAIN_PLUS6(0x10, "6 db"),
        GAIN_PLUS9(0x30, "9 db");

        private int mValue;
        private String mLabel;

        IFStage3Gain(int value, String label)
        {
            mValue = value;
            mLabel = label;
        }

        public static Register getRegister()
        {
            return Register.GAIN3;
        }

        public static byte getMask()
        {
            return (byte) 0x18;
        }

        public byte getValue()
        {
            return (byte) mValue;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public String toString()
        {
            return mLabel;
        }

        public static IFStage3Gain fromRegisterValue(int registerValue)
        {
            int value = registerValue & getMask();

            for(IFStage3Gain setting : values())
            {
                if(value == setting.getValue())
                {
                    return setting;
                }
            }

            throw new IllegalArgumentException("E4KTunerController - unrecognized IF Gain Stage 3 value [" + value + "]");
        }
    }

    public enum IFStage4Gain
    {
        GAIN_PLUS0(0x00, "0 db"),
        GAIN_PLUS1(0x20, "1 db"),
        GAIN_PLUS2A(0x40, "2 db"),
        GAIN_PLUS2B(0x60, "2 db");

        private int mValue;
        private String mLabel;

        IFStage4Gain(int value, String label)
        {
            mValue = value;
            mLabel = label;
        }

        public static Register getRegister()
        {
            return Register.GAIN3;
        }

        public static byte getMask()
        {
            return (byte) 0x60;
        }

        public byte getValue()
        {
            return (byte) mValue;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public String toString()
        {
            return mLabel;
        }

        public static IFStage4Gain fromRegisterValue(int registerValue)
        {
            int value = registerValue & getMask();

            for(IFStage4Gain setting : values())
            {
                if(value == setting.getValue())
                {
                    return setting;
                }
            }

            throw new IllegalArgumentException("E4KTunerController - unrecognized IF Gain Stage 4 value [" + value + "]");
        }
    }

    public enum IFStage5Gain
    {
        GAIN_PLUS3(0x0, "3 db"),
        GAIN_PLUS6(0x1, "6 db"),
        GAIN_PLUS9(0x2, "9 db"),
        GAIN_PLUS12(0x3, "12 db"),
        GAIN_PLUS15A(0x4, "15 db"),
        GAIN_PLUS15B(0x5, "15 db"),
        GAIN_PLUS15C(0x6, "15 db"),
        GAIN_PLUS15D(0x7, "15 db");

        private int mValue;
        private String mLabel;

        IFStage5Gain(int value, String label)
        {
            mValue = value;
            mLabel = label;
        }

        public static Register getRegister()
        {
            return Register.GAIN4;
        }

        public static byte getMask()
        {
            return (byte) 0x7;
        }

        public byte getValue()
        {
            return (byte) mValue;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public String toString()
        {
            return mLabel;
        }

        public static IFStage5Gain fromRegisterValue(int registerValue)
        {
            int value = registerValue & getMask();

            for(IFStage5Gain setting : values())
            {
                if(value == setting.getValue())
                {
                    return setting;
                }
            }

            throw new IllegalArgumentException("E4KTunerController - unrecognized IF Gain Stage 5 value [" + value + "]");
        }

    }

    public enum IFStage6Gain
    {
        GAIN_PLUS3(0x00, "3 db"),
        GAIN_PLUS6(0x08, "6 db"),
        GAIN_PLUS9(0x10, "9 db"),
        GAIN_PLUS12(0x18, "12 db"),
        GAIN_PLUS15A(0x20, "15 db"),
        GAIN_PLUS15B(0x28, "15 db"),
        GAIN_PLUS15C(0x30, "15 db"),
        GAIN_PLUS15D(0x38, "15 db");

        private int mValue;
        private String mLabel;

        IFStage6Gain(int value, String label)
        {
            mValue = value;
            mLabel = label;
        }

        public static Register getRegister()
        {
            return Register.GAIN4;
        }

        public static byte getMask()
        {
            return (byte) 0x38;
        }

        public byte getValue()
        {
            return (byte) mValue;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public String toString()
        {
            return mLabel;
        }

        public static IFStage6Gain fromRegisterValue(int registerValue)
        {
            int value = registerValue & getMask();

            for(IFStage6Gain setting : values())
            {
                if(value == setting.getValue())
                {
                    return setting;
                }
            }

            throw new IllegalArgumentException("E4KTunerController - unrecognized IF Gain Stage 6 value [" + value + "]");
        }
    }

    public enum IFFilter
    {
        MIX("Mix"),
        CHAN("Channel"),
        RC("RC");

        private String mLabel;

        IFFilter(String label)
        {
            mLabel = label;
        }

        public String getLabel()
        {
            return mLabel;
        }
    }

    public enum AGCMode
    {
        SERIAL((byte) 0x0),
        IF_PWM_LNA_SERIAL((byte) 0x1),
        IF_PWM_LNA_AUTONL((byte) 0x2),
        IF_PWM_LNA_SUPERV((byte) 0x3),
        IF_SERIAL_LNA_PWM((byte) 0x4),
        IF_PWM_LNA_PWM((byte) 0x5),
        IF_DIG_LNA_SERIAL((byte) 0x6),
        IF_DIG_LNA_AUTON((byte) 0x7),
        IF_DIG_LNA_SUPERV((byte) 0x8),
        IF_SERIAL_LNA_AUTON((byte) 0x9),
        IF_SERIAL_LNA_SUPERV((byte) 0xA);

        private byte mValue;

        AGCMode(byte value)
        {
            mValue = value;
        }

        public byte getValue()
        {
            return mValue;
        }
    }

    public enum Register
    {
        DUMMY(0x00),
        MASTER1(0x00),
        MASTER2(0x01),
        MASTER3(0x02),
        MASTER4(0x03),
        MASTER5(0x04),
        CLK_INP(0x05),
        REF_CLK(0x06),
        SYNTH1(0x07),
        SYNTH2(0x08),
        SYNTH3(0x09),
        SYNTH4(0x0A),
        SYNTH5(0x0B),
        SYNTH6(0x0C),
        SYNTH7(0x0D),
        SYNTH8(0x0E),
        SYNTH9(0x0F),
        FILT1(0x10),
        FILT2(0x11),
        FILT3(0x12),
        GAIN1(0x14),
        GAIN2(0x15),
        GAIN3(0x16),
        GAIN4(0x17),
        AGC1(0x1A),
        AGC2(0x1B),
        AGC3(0x1C),
        AGC4(0x1D),
        AGC5(0x1E),
        AGC6(0x1F),
        AGC7(0x20),
        AGC8(0x21),
        AGC11(0x24),
        AGC12(0x25),
        DC1(0x29),
        DC2(0x2A),
        DC3(0x2B),
        DC4(0x2C),
        DC5(0x2D),
        DC6(0x2E),
        DC7(0x2F),
        DC8(0x30),
        QLUT0(0x50),
        QLUT1(0x51),
        QLUT2(0x52),
        QLUT3(0x53),
        ILUT0(0x60),
        ILUT1(0x61),
        ILUT2(0x62),
        ILUT3(0x63),
        DCTIME1(0x70),
        DCTIME2(0x71),
        DCTIME3(0x72),
        DCTIME4(0x73),
        PWM1(0x74),
        PWM2(0x75),
        PWM3(0x76),
        PWM4(0x77),
        BIAS(0x78),
        CLKOUT_PWDN(0x7A),
        CHFILT_CALIB(0x7B),
        I2C_REG_ADDR(0x7D),
        MAGIC_1(0x7E),
        MAGIC_2(0x7F),
        MAGIC_3(0x82),
        MAGIC_4(0x86),
        MAGIC_5(0x87),
        MAGIC_6(0x88),
        MAGIC_7(0x9F),
        MAGIC_8(0xA0),
        I2C_REGISTER(0xC8);

        private int mValue;

        Register(int value)
        {
            mValue = value;
        }

        public byte getValue()
        {
            return (byte) mValue;
        }
    }
}
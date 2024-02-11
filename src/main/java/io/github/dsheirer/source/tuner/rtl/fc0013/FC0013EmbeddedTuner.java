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
package io.github.dsheirer.source.tuner.rtl.fc0013;

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.EmbeddedTuner;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;
import java.text.DecimalFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.LibUsbException;

/**
 * Fitipower FC0013 Tuner
 * <p>
 * Ported from: https://github.com/osmocom/rtl-sdr/blob/master/src/tuner_fc0013.c
 *
 * Register: Original Contents / Shadow Contents - Comments
 * 0x00: .. / 00 - Dummy register for 0-index
 * 0x01: 05 / 09
 * 0x02: 10 / 16
 * 0x03: 00 / 00
 * 0x04: 00 / 00
 * 0x05: 0A / 17
 * 0x06: 0A / 02 - Low Pass Filter Bandwidth
 * 0x07: 00 / 2A - 0x2x setup for 28.8MHz crystal
 * 0x08: B0 / FF - AGC clock divide by 256, AGC gain 1/256, Loop bw 1/8
 * 0x09: 6E / 6E - Disable loop through, enable loop through: 0x6F
 * 0x0A: B8 / B8 - Disable LO Test Buffer
 * 0x0B: 82 / 82
 * 0x0C: 7C / FC - Depending on AGC Up-Down mode, may need 0xF8
 * 0x0D: 00 / 01 - AGC not forcing & LNA forcing, we may need 0x02
 * 0x0E: 00 / 00
 * 0x0F: 00 / 00
 * 0x10: 00 / 00
 * 0x11: 08 / 00
 * 0x12: 00 / 00
 * 0x13: 00 / 00
 * 0x14: C0 / 50 - DVB-t=High Gain, UHF=middle gain (0x48), or low gain (0x40)
 * 0x15: 01 / 01
 */
public class FC0013EmbeddedTuner extends EmbeddedTuner
{
    private final static Logger mLog = LoggerFactory.getLogger(FC0013EmbeddedTuner.class);
    private DecimalFormat FREQUENCY_FORMAT = new DecimalFormat("0.000000");
    public static final long MINIMUM_TUNABLE_FREQUENCY_HZ = 13_500_000;
    public static final long MAXIMUM_TUNABLE_FREQUENCY_HZ = 1_907_999_890l;
    private static final double USABLE_BANDWIDTH_PERCENT = 0.95;
    private static final int DC_SPIKE_AVOID_BUFFER = 15000;
    //Hardware I2C address
    private static final byte I2C_ADDRESS = (byte) 0xC6;
    //Hardware crystal oscillator frequency - used by the frequency divider
    private static final int XTAL_FREQUENCY = 28_800_000;
    private static final int XTAL_FREQUENCY_DIVIDED_BY_2 = XTAL_FREQUENCY / 2;
    //Initial register settings to apply to the tuner
    private static byte[] REGISTERS = {(byte) 0x00, (byte) 0x09, (byte) 0x16, (byte) 0x00, (byte) 0x00, (byte) 0x17,
            (byte) 0x02, (byte) 0x2A, (byte) 0xFF, (byte) 0x6E, (byte) 0xB8, (byte) 0x82, (byte) 0xFE, (byte) 0x01,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x50, (byte) 0x01};
    private long mTunedFrequency = MINIMUM_TUNABLE_FREQUENCY_HZ;

    /**
     * Constructs an instance
     *
     * @param adapter for controlling the RTL2832
     */
    public FC0013EmbeddedTuner(RTL2832TunerController.ControllerAdapter adapter)
    {
        super(adapter);
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.FITIPOWER_FC0013;
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

    /**
     * Applies the tuner configuration settings.
     * @param tunerConfig containing settings to apply
     * @throws SourceException if there is an error.
     */
    @Override
    public void apply(TunerConfiguration tunerConfig) throws SourceException
    {
        if(tunerConfig instanceof FC0013TunerConfiguration config)
        {
            try
            {
                setGain(config.getAGC(), config.getLnaGain());
            }
            catch(Exception e)
            {
                throw new SourceException("Error while applying tuner config", e);
            }
        }
        else
        {
            throw new IllegalArgumentException("Unrecognized tuner config [" + tunerConfig.getClass() + "]");
        }
    }

    /**
     * Writes the value to the FC0013 register and optionally controls the I2C repeater
     *
     * @param register to write
     * @param value to write
     * @param controlI2CRepeater true to turn on the I2C repeater before and after the write operation.  If this is
     * set to false, you need to have previously enabled the I2C repeater before invoking this method call.
     */
    private void write(Register register, byte value, boolean controlI2CRepeater)
    {
        getAdapter().writeI2CRegister(I2C_ADDRESS, register.byteValue(), value, controlI2CRepeater);
    }

    /**
     * Writes the value to the FC0013 register and optionally controls the I2C repeater
     *
     * @param register to write
     * @param value to write
     * @param mask to use when reading the full register byte that has ones in the bits that should be untouched and
     * zeros in the bits that should be cleared prior to applying (or'ing) the value into the register.
     * @param controlI2CRepeater true to turn on the I2C repeater before and after the write operation.  If this is
     * set to false, you need to have previously enabled the I2C repeater before invoking this method call.
     */
    public void writeMaskedRegister(Register register, byte value, byte mask, boolean controlI2CRepeater)
    {
        byte content = (byte) (readRegister(register, controlI2CRepeater) & 0xFF);
        content &= mask;
        content |= value;
        write(register, content, controlI2CRepeater);
    }

    /**
     * Writes the value to a field within an FC0013 register and optionally controls the I2C repeater
     *
     * @param field to write
     * @param value to write
     * @param controlI2CRepeater true to turn on the I2C repeater before and after the write operation.  If this is
     * set to false, you need to have previously enabled the I2C repeater before invoking this method call.
     */
    private void write(Field field, byte value, boolean controlI2CRepeater)
    {
        writeMaskedRegister(field.getRegister(), value, field.getMask(), controlI2CRepeater);
    }

    /**
     * Reads the FC0013 tuner register value.
     *
     * @param register to read
     * @param controlI2CRepeater true to turn on the I2C repeater before and after the write operation.  If this is
     * set to false, you need to have previously enabled the I2C repeater before invoking this method call.
     * @return register value.
     */
    private int readRegister(Register register, boolean controlI2CRepeater)
    {
        return getAdapter().readI2CRegister(I2C_ADDRESS, register.byteValue(), controlI2CRepeater);
    }

    @Override
    public void setSamplingMode(RTL2832TunerController.SampleMode mode) throws LibUsbException
    {
        //No-op
    }

    @Override
    public void setSampleRateFilters(int sampleRate) throws SourceException
    {
        //No-op
    }

    /**
     * Currently tuned frequency.
     * @return frequency
     * @throws SourceException never
     */
    public long getTunedFrequency() throws SourceException
    {
        return mTunedFrequency;
    }

    /**
     * Sets the frequency value to the tuner.  Note: this is managed by the frequency controller along with PPM adjust.
     * @param frequency in Hertz to apply to the tuner
     * @throws SourceException if there is an error.
     */
    @Override
    public synchronized void setTunedFrequency(long frequency) throws SourceException
    {
        getAdapter().getLock().lock();

        try
        {
            //Capture current I2C repeater state and enable if necessary.
            boolean i2CEnabledState = getAdapter().isI2CRepeaterEnabled();
            if(!i2CEnabledState)
            {
                getAdapter().enableI2CRepeater();
            }

            setVhfTracking(frequency);
            setFilters(frequency);

            FrequencyDivider divider = FrequencyDivider.fromFrequency(frequency);
            boolean vcoSelect = ((double)frequency / (double)divider.getIntegralFrequencyMultiplier()) >= 212.5;
            int integral = (int)(frequency / divider.getIntegralFrequencyMultiplier());
            int pm = integral / 8; //pm is units of 8x integrals
            pm = Math.max(pm, 11);
            pm = Math.min(pm, 31);

            int am = integral - (pm * 8); //am is units of 1x integrals
            if(am < 2)
            {
                am += 8;
                pm--;
            }
            am = Math.min(am, 15);
            integral = pm * 8 + am;

            int residual = (int)(frequency - (integral * divider.getIntegralFrequencyMultiplier()));

            //fractional is units of 1/65536th of 2x integrals
            int fractional = (int)Math.round(residual / divider.getFractionalFrequencyMultiplier());

            if(pm < 11 || pm > 31 || am < 2 || am > 15 || fractional < 0 || fractional > 65535)
            {
                String message = "FC0013 - no valid PLL combination for frequency [" + frequency + "] using divider [" +
                        divider + "] pm [" + pm + "] am [" + am + "] fractional [" + fractional + "]";
                mLog.error(message);
                throw new SourceException(message);
            }

            setFrequencyValues(divider, pm, am, fractional, vcoSelect);

            //Restore the I2C repeater to previous state
            if(!i2CEnabledState)
            {
                getAdapter().disableI2CRepeater();
            }
        }
        catch(LibUsbException e)
        {
            mLog.error("FC0013 tuner error while setting tuned frequency [" + frequency + "]", e);
        }
        finally
        {
            getAdapter().getLock().unlock();
        }

        mTunedFrequency = frequency;
    }

    /**
     * Apply initial settings to the tuner
     */
    protected void initTuner()
    {
        getAdapter().enableI2CRepeater();
        boolean i2CRepeaterControl = false;

        //Write initial register settings to the tuner
        for(int x = 1; x < REGISTERS.length; x++)
        {
            write(Register.values()[x], REGISTERS[x], i2CRepeaterControl);
        }

        write(Field.R0D_AGC, (byte)0x08, false);
        write(Register.R13, (byte)0x0A, false);
        getAdapter().disableI2CRepeater();
    }

    /**
     * Applies the frequency settings and calibrates the PLL.  Note: I2C repeater must be enabled before invoking.
     * @param divider to use
     * @param pm units of 8x integrals
     * @param am units of 1x integrals
     * @param fractional value.
     * @param vcoSelect to enable (true) or disable (false)
     * @throws SourceException if the settings are invalid or the PLL can't be calibrated with the settings.
     * @return true if the frequency settings were applied successfully.
     */
    private boolean setFrequencyValues(FrequencyDivider divider, int pm, int am, int fractional, boolean vcoSelect)
            throws SourceException
    {
        if(pm < 11 || pm > 31)
        {
            throw new IllegalArgumentException("PM value [" + pm + "] must be in range 11-31");
        }
        if(am < 2 || am > 15)
        {
            throw new IllegalArgumentException("AM value [" + am + "] must be in range 2-15");
        }
        if(fractional < 0 || fractional > 65535)
        {
            throw new IllegalArgumentException("Fractional value [" + fractional + "] must be in range 0-65,535");
        }
        double exactFrequency = divider.calculate(pm, am, fractional);
        long frequency = (long)exactFrequency;
        byte register5 = divider.getRegister5();
        register5 |= 0x07; //modified for Realtek demod

        byte register6 = divider.getRegister6();
        if(vcoSelect)
        {
            register6 |= 0x08;
        }

        //Write the integral units to the PLL
        write(Register.R01, (byte)(am & 0xFF), false);
        write(Register.R02, (byte)(pm & 0xFF), false);

        //Write the fractional units to the PLL
        write(Register.R03, (byte)((fractional >> 8) & 0xFF), false);
        write(Register.R04, (byte)(fractional & 0xFF), false);

        //Set the frequency divider, bandwidth, and enable the clock output
        write(Register.R05, register5, false);
        write(Register.R06, register6, false);

        //Whatever register 11 does ... seems like it's a gain thing.
        int tmp = readRegister(Register.R11, false);
        if(divider == FrequencyDivider.D64)
        {
            write(Register.R11, (byte)(tmp | 0x04), false);
        }
        else
        {
            write(Register.R11, (byte)(tmp & 0xFB), false);
        }

        //Perform VCO Calibration
        write(Register.R0E, (byte)0x80, false);
        write(Register.R0E, (byte)0x00, false);
        write(Register.R0E, (byte)0x00, false);
        int calibration = readRegister(Register.R0E, false) & 0x3F;

        boolean recalibrateRequired = false;
        if(vcoSelect && calibration > 0x3C)
        {
            register6 &= ~0x08;
            recalibrateRequired = true;
        }
        else if(!vcoSelect && calibration < 0x02)
        {
            register6 |= 0x08;
            recalibrateRequired = true;
        }

        if(recalibrateRequired)
        {
            write(Register.R06, register6, false);
            write(Register.R0E, (byte)0x80, false);
            write(Register.R0E, (byte)0x00, false);
            write(Register.R0E, (byte)0x00, false);
            calibration = readRegister(Register.R0E, false) & 0x3F;
            if((!vcoSelect & calibration < 0x02) || (vcoSelect & calibration > 0x3C))
            {
                String msg = "Unable to tune frequency [" + fractional + "] PLL calibration [" + Integer.toHexString(calibration).toUpperCase() + "] out of limits [02-3C]";
                mLog.error(msg);
                throw new SourceException(msg);
            }
        }

        return true;
    }

    /**
     * Sets the tuner filters according to the tuned frequency. Note: I2C repeater must be enabled before invoking.
     * @param frequency of the tuner
     */
    private void setFilters(long frequency)
    {
        byte vhf = (byte)0x00; //default to disabled
        byte uhf = (byte)0x00; //default to disabled

        if(frequency < 300_000_000)
        {
            vhf = (byte)0x10; //enable
        }
        else if(frequency < 862_000_000)
        {
            uhf = (byte)0x40;
        }
        else
        {
            //The original source code seems to target a GPS filter, but it's using the same uhf filter setting.
            uhf = (byte)0x40;
        }

        write(Field.R07_VHF_FILTER, vhf, false);
        write(Field.R14_UHF_FILTER, uhf, false);
    }

    /**
     * Sets VHF tracking according to the tuned frequency. Note: I2C repeater must be enabled before invoking.
     * @param frequency for the tuner
     */
    private void setVhfTracking(long frequency)
    {
        int tmp = readRegister(Register.R1D, false);

        //Clear the middle 3x bits that we're going to set and leave the remaining bits untouched.
        tmp &= 0xE3;

        if(frequency <= 177_500_000)
        {
            tmp |= 0x1C; //VHF Track 7
        }
        else if(frequency <= 184_500_000)
        {
            tmp |= 0x18; //VHF Track 6
        }
        else if(frequency <= 191_500_000)
        {
            tmp |= 0x14; //VHF Track 5
        }
        else if(frequency <= 198_500_000)
        {
            tmp |= 0x10; //VHF Track 4
        }
        else if(frequency <= 205_500_000)
        {
            tmp |= 0x0C; //VHF Track 3
        }
        else if(frequency <= 219_500_000)
        {
            tmp |= 0x08; //VHF Track 2
        }
        else if(frequency <= 300_000_000)
        {
            tmp |= 0x04; //VHF Track 1
        }
        else
        {
            tmp |= 0x1C; //UHF & GPS
        }

        write(Register.R1D, (byte) tmp, false);
    }

    /**
     * Sets the gain for the tuner.  Note: if agc=true, the manual gain setting is ignored.
     * @param agc automatic gain control.
     * @param manualGain setting.
     */
    public void setGain(boolean agc, LNAGain manualGain)
    {
        getAdapter().getLock().lock();

        try
        {
            boolean repeaterState = getAdapter().isI2CRepeaterEnabled();
            if(!repeaterState)
            {
                getAdapter().enableI2CRepeater();
            }

            if(agc)
            {
                write(Field.R0D_AGC, (byte)0x00, false);
                write(Register.R13, (byte)0x0A, false);
            }
            else
            {
                write(Field.R14_LNA_GAIN, manualGain.byteValue(), false);
                write(Field.R0D_AGC, (byte)0x08, false);
                write(Register.R13, (byte)0x0A, false);
            }

            if(!repeaterState)
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
     * Enumeration of FC0013 registers.  Provides convenience method for accessing the entry's ordinal position as a
     * byte value for reading/writing.
     */
    private enum Register
    {
        R00, R01, R02, R03, R04, R05, R06, R07, R08, R09, R0A, R0B, R0C, R0D, R0E, R0F, R10, R11, R12, R13, R14, R15,
        R16, R17, R18, R19, R1A, R1B, R1C, R1D;

        /**
         * Returns the ordinal position of the entry within the enumeration as a byte value.
         */
        public byte byteValue()
        {
            return (byte) ordinal();
        }
    }

    /**
     * Masked tuner register fields enumeration
     */
    private enum Field
    {
        R06_BANDWIDTH(Register.R06, 0xC0),
        R07_VHF_FILTER(Register.R07, 0xEF),
        R0D_AGC(Register.R0D, 0xF7),
        R14_LNA_GAIN(Register.R14, 0xE0),
        R14_UHF_FILTER(Register.R14, 0x1F);

        private Register mRegister;
        private byte mMask;

        /**
         * Constructs an instance
         * @param register for the field
         * @param mask for the field
         */
        Field(Register register, int mask)
        {
            mRegister = register;
            mMask = (byte) mask;
        }

        /**
         * Register for the field.
         * @return register
         */
        public Register getRegister()
        {
            return mRegister;
        }

        /**
         * Bit field mask for the register that masks out the target field bits prior to applying a value.
         * @return mask value.
         */
        public byte getMask()
        {
            return mMask;
        }
    }

    /**
     * LNA gain values enumeration
     */
    public enum LNAGain
    {
        G00("0 LOW", 0x02),
        G01("1", 0x03),
        G02("2", 0x05),
        G03("3", 0x04),
        G04("4", 0x00),
        G05("5", 0x07),
        G06("6", 0x01),
        G07("7", 0x06),

        G08("8", 0x0F),
        G09("9", 0x0E),
        G10("10", 0x0D),
        G11("11", 0x0C),
        G12("12", 0x0B),
        G13("13", 0x0A),
        G14("14", 0x09),
        G15("15", 0x08),

        G16("16", 0x17),
        G17("17", 0x16),
        G18("18", 0x15),
        G19("19", 0x14),
        G20("20", 0x13),
        G21("21", 0x12),
        G22("22", 0x11),
        G23("23 HIGH", 0x10);

        private String mLabel;
        private byte mValue;

        /**
         * Constructs an instance
         *
         * @param label to show to the user
         * @param value to apply to the register
         */
        LNAGain(String label, int value)
        {
            mLabel = label;
            mValue = (byte) value;
        }

        /**
         * Byte value for the gain setting.
         *
         * @return byte value.
         */
        public byte byteValue()
        {
            return mValue;
        }

        @Override
        public String toString()
        {
            return mLabel;
        }
    }

    /**
     * SDM PLL Frequency Divider and Supported Frequency Ranges
     *
     * Note: each divider frequency range is overlapping with other divider frequency range(s) and also within each
     * frequency divider there are overlapping frequency range across each PM value.  Each unit of PM is 8x the integral
     * and each unit of AM is 1x the integral.  You can specify a range of 2-15 units of AM which causes overlap when
     * each unit of PM represents 8x units of AM.
     *
     * So, for a requested frequency, there are likely multiple combinations of Divider/PM/AM/Fractional that will
     * tune the frequency, each with varying degrees of correctness.
     *
     * For each divider range, minimum and maximum frequency is calculated as:
     * Min: PM = 11, AM = 2, FRACTIONAL = 0
     * Max: PM = 31, AM = 15, FRACTIONAL = 65,535, or the highest setting that can be calibrated by the VCO.
     *
     * integral = XTAL_FREQ / 2 / divider
     *
     * Where: (integral * ((PM * 8) + AM)) + (2 * integral / 65,536 * fractional)
     *
     * Minimum tunable frequency step size is (2 * integral / 65,536).
     */
    public enum FrequencyDivider
    {
        D96(96, true, 0x82, 13_500_000, 39_749_997),
        D64(64, false, 0x02, 20_250_000, 59_624_996), //Special one, why?
        D48(48, true, 0x42, 27_000_000, 79_499_995),
        D32(32, false, 0x82, 40_500_000, 119_249_993),
        D24(24, true, 0x22, 54_000_000, 158_999_990),
        D16(16, false, 0x42, 81_000_000, 238_499_986),
        D12(12, true, 0x12, 108_000_000, 317_999_981),
        D08(8, false, 0x22, 162_000_000, 476_999_972),
        D06(6, true, 0x0A, 235_200_000, 635_999_963),
        D04(4, false, 0x12, 514_900_000, 953_999_945),
        D02(2, false, 0x0A, 648_000_000, 1_907_999_890);

        private int mDivider;
        private boolean mIs3xMode;
        private int mRegister5;
        private long mMinimumFrequency;
        private long mMaximumFrequency;

        /**
         * Constructs an instance
         * @param divider quantity
         * @param is3xMode indicates 3x divider (true) or 2x divider (false)
         * @param register5 setting
         * @param minimumFrequency supported by the divider
         * @param maximumFrequency supported by the divider
         */
        FrequencyDivider(int divider, boolean is3xMode, int register5, long minimumFrequency, long maximumFrequency)
        {
            mDivider = divider;
            mIs3xMode = is3xMode;
            mRegister5 = register5;
            mMinimumFrequency = minimumFrequency;
            mMaximumFrequency = maximumFrequency;
        }

        /**
         * Register 5 frequency multiplier setting
         */
        public byte getRegister5()
        {
            return (byte)(mRegister5 & 0xFF);
        }

        /**
         * Register 6 multiplier mode setting (2x or 3x)
         */
        public byte getRegister6()
        {
            //0xAx OR Enable clock (0x20) OR bandwidth set to 6 MHz (0x80).
            return is3xMode() ? (byte)0xA0 : (byte)0xA2;
        }

        /**
         * Calculates the frequency for this frequency divider.
         * @param pm - 8x integral units
         * @param am - 1x integral units
         * @param fractional integral units
         * @param vcoSelect to indicate if the frequency requires VCO select (ie AM + 2)
         * @return frequency
         */
        public double getFrequency(int pm, int am, int fractional, boolean vcoSelect)
        {
            if(vcoSelect)
            {
                return calculate(pm, am, fractional);
            }
            else
            {
                return calculate(pm, am - 2, fractional);
            }
        }

        /**
         * Indicates if the frequency requires VCO select for this divider.
         * @param frequency to evaluate
         * @return true if vco select
         */
        public boolean isVcoSelect(double frequency)
        {
            return (frequency * getDivider()) > 3_060_000_000l;
        }

        private double calculate(int pm, int am, int fractional)
        {
            return (((8l * pm) + am) * getIntegralFrequencyMultiplier()) + (getFractionalFrequencyMultiplier() * fractional);
        }

        /**
         * Integral unit of frequency multiplier supported by the PLL for the specified frequency divider.
         * @return integral frequency multiplier.
         */
        public int getIntegralFrequencyMultiplier()
        {
            return XTAL_FREQUENCY_DIVIDED_BY_2 / mDivider;
        }

        /**
         * Fractional (1/65536th) unit of frequency of 2x integrals that is supported by the 16-bit fractional PLL.
         * @return fractional frequency multiplier
         */
        public double getFractionalFrequencyMultiplier()
        {
            return XTAL_FREQUENCY / mDivider / 65_536d;
        }

        /**
         * Indicates if this divider is mode 2x (false) or mode 3x (true) as a divider of the base crystal frequency.
         * @return true if 3x mode.
         */
        public boolean is3xMode()
        {
            return mIs3xMode;
        }

        /**
         * Divider value (of the base crystal frequency).
         * @return divider
         */
        public int getDivider()
        {
            return mDivider;
        }

        /**
         * Minimum frequency supported by this divider.
         * @return minimum frequency (Hz).
         */
        public long getMinimumFrequency()
        {
            return mMinimumFrequency;
        }

        /**
         * Maximum frequency supported by this divider.
         * @return maximum frequency (Hz).
         */
        public long getMaximumFrequency()
        {
            return mMaximumFrequency;
        }

        /**
         * Indicates if the frequency is contained by the min/max values of the current frequency divider entry.
         * @param frequency to test
         * @return true if contained.
         */
        public boolean contains(long frequency)
        {
            return mMinimumFrequency <= frequency && frequency <= mMaximumFrequency;
        }

        /**
         * Returns the matching frequency divider for the specified frequency, or returns divider 16x, if the frequency
         * is outside any supported frequency ranges.
         *
         * @param frequency - desired frequency
         * @return - FrequencyDivider to use for the specified frequency
         */
        public static FrequencyDivider fromFrequency(long frequency)
        {
            for(FrequencyDivider divider : FrequencyDivider.values())
            {
                if(divider.contains(frequency))
                {
                    return divider;
                }
            }

            return FrequencyDivider.D16;
        }
    }
}
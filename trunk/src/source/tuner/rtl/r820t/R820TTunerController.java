/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 *     
 *     Java port of librtlsdr <https://github.com/steve-m/librtlsdr>
 *     
 *     Copyright (C) 2013 Mauro Carvalho Chehab <mchehab@redhat.com>
 *     Copyright (C) 2013 Steve Markgraf <steve@steve-m.de>
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package source.tuner.rtl.r820t;

import javax.swing.JPanel;
import javax.usb.UsbException;

import log.Log;
import source.SourceException;
import source.tuner.TunerConfiguration;
import source.tuner.TunerType;
import source.tuner.rtl.RTL2832TunerController;
import source.tuner.usb.USBTunerDevice;
import controller.ResourceManager;

public class R820TTunerController extends RTL2832TunerController
{
	public static final int MIN_FREQUENCY = 24000000;
	public static final int MAX_FREQUENCY = 1766000000;
	public static final int R820T_IF_FREQUENCY = 3570000;
	public static final byte VERSION = (byte)49;
	
	public static final byte[] BIT_REV_LOOKUP_TABLE = 
		{ (byte)0x0, (byte)0x8, (byte)0x4, (byte)0xC, 
		  (byte)0x2, (byte)0xA, (byte)0x6, (byte)0xE,
		  (byte)0x1, (byte)0x9, (byte)0x5, (byte)0xD, 
		  (byte)0x3, (byte)0xB, (byte)0x7, (byte)0xF }; 
	
	/* R820T I2C address */
	private byte mI2CAddress = (byte)0x34;
	
	/* Shadow register is used to keep a cached (in-memory) copy of all 
	 * registers, so that we don't have to read a full byte from a register in 
	 * order to apply a masked value and then re-write the full byte.  With the 
	 * shadow register, we can apply the masked value to the cached value, and 
	 * then just write the masked byte, skipping the need to read the byte 
	 * first. */
	private int[] mShadowRegister = 
	    { 0x00, 0x00, 0x00, 0x00, 0x00, 0x83, 0x32, 0x75,
	      0xC0, 0x40, 0xD6, 0x6C, 0xF5, 0x63, 0x75, 0x68,
	      0x6C, 0x83, 0x80, 0x00, 0x0F, 0x00, 0xC0, 0x30,
	      0x48, 0xCC, 0x60, 0x00, 0x54, 0xAE, 0x4A, 0xC0 };
	
	private JPanel mEditor;
	
	public R820TTunerController( USBTunerDevice device ) throws SourceException
	{
	    super( device, MIN_FREQUENCY, MAX_FREQUENCY );
	}

	@Override
    public TunerType getTunerType()
    {
	    return TunerType.RAFAELMICRO_R820T;
    }
	
	@Override
    public void setSampleRateFilters( int sampleRate ) throws UsbException
    {
		//TODO: why is this being forced as an abstract method on sub classes?
    }
	
	@Override
    public void apply( TunerConfiguration tunerConfig ) throws SourceException
    {
		if( tunerConfig != null && 
			tunerConfig instanceof R820TTunerConfiguration )
		{
			R820TTunerConfiguration config = (R820TTunerConfiguration)tunerConfig;
			
			try
			{
				SampleRate sampleRate = config.getSampleRate();
				setSampleRate( sampleRate );
				
				double correction = config.getFrequencyCorrection();
				setFrequencyCorrection( correction );
				
				R820TGain masterGain = config.getMasterGain();
				setGain( masterGain, true );
				
				if( masterGain == R820TGain.MANUAL )
				{
					R820TMixerGain mixerGain = config.getMixerGain();
					setMixerGain( mixerGain, true );
					
					R820TLNAGain lnaGain = config.getLNAGain();
					setLNAGain( lnaGain, true );
					
					R820TVGAGain vgaGain = config.getVGAGain();
					setVGAGain( vgaGain, true );
				}
			}
			catch( UsbException e )
			{
				throw new SourceException( "R820TTunerController - usb error "
						+ "while applying tuner config", e );
			}
		}
    }

	/**
	 * Controller gui editor component for this tuner
	 */
	public JPanel getEditor( ResourceManager resourceManager )
	{
		if( mEditor == null )
		{
			mEditor = new R820TTunerEditorPanel( this, resourceManager );
		}
		
		return mEditor;
	}

	/**
	 * Not implemented.
	 */
	@Override
    public int getTunedFrequency() throws SourceException
    {
		return 0;
    }

	/**
	 * Sets the center frequency.  Setting the frequency is a two-part process
	 * of setting the multiplexer and then setting the Oscillator (PLL).
	 */
	@Override
    public void setTunedFrequency( int frequency ) throws SourceException
    {
		try
		{
			enableI2CRepeater( mUSBDevice, true );

			boolean controlI2C = false;
			
			int offsetFrequency = frequency + R820T_IF_FREQUENCY;
			
			setMux( offsetFrequency, controlI2C );
			
			setPLL( offsetFrequency, controlI2C );

			enableI2CRepeater( mUSBDevice, false );
		}
		catch( UsbException e )
		{
			throw new SourceException( "R820TTunerController - exception "
					+ "while setting frequency [" + frequency + "] - " + 
					e.getLocalizedMessage() );
		}
    }

	/**
	 * Overrides the same method from the RTL2832 tuner controller to apply 
	 * settings specific to the R820T tuner.
	 */
	public void setSamplingMode( SampleMode mode ) throws UsbException
	{
		switch( mode )
		{
			case QUADRATURE:
				/* Set intermediate frequency to R820T IF frequency */
				setIFFrequency( R820T_IF_FREQUENCY );

				/* Enable spectrum inversion */
				writeDemodRegister( mUSBDevice, 
									Page.ONE, 
									(short)0x15, 
									(short)0x01, 
									1 );
				
				/* Set default i/q path */
				writeDemodRegister( mUSBDevice, 
									Page.ZERO, 
									(short)0x06, 
									(short)0x80, 
									1 );
				break;
			default:
				break;
			
		}
	}

	/**
	 * Sets the multiplexer for the desired center frequency.
	 */
	private void setMux( int frequency, boolean controlI2C ) throws UsbException
	{
		FrequencyRange range = FrequencyRange.getRangeForFrequency( frequency );

		/* Set open drain */
		writeR820TRegister( Register.DRAIN, range.getOpenDrain(), controlI2C );

		/* RF_MUX, Polymux */
		writeR820TRegister( Register.RF_POLY_MUX, range.getRFMuxPolyMux(), controlI2C );
		
		/* TF Band */
		writeR820TRegister( Register.TF_BAND, range.getTFC(), controlI2C );
		
		/* XTAL CAP & Drive */
		writeR820TRegister( Register.PLL_XTAL_CAPACITOR_AND_DRIVE, 
				range.getXTALHighCap0P(), controlI2C );
		
		/* Register 8 - what is it? */
		writeR820TRegister( Register.UNKNOWN_REGISTER_8, (byte)0x00, controlI2C );
		
		/* Register 9 - what is it? */
		writeR820TRegister( Register.UNKNOWN_REGISTER_9, (byte)0x00, controlI2C );
	}

	/**
	 * Initializes the tuner for use.
	 */
	public void init() throws SourceException
	{
		try
		{
			/* Dummy write to test USB interface */
			writeRegister( mUSBDevice, Block.USB, 
					Address.USB_SYSCTL.getAddress(), (short)0x09, 1 );

			initBaseband( mUSBDevice );

			enableI2CRepeater( mUSBDevice, true );
			
			boolean i2CRepeaterControl = false;
			
			initTuner( i2CRepeaterControl );

			enableI2CRepeater( mUSBDevice, false );
			
			/* Initialize the super class */
			super.init();
		}
		catch( UsbException e )
		{
			e.printStackTrace();
			throw new SourceException( "E4K Tuner Controller - error during "
					+ "init()", e );
		}
	}

	/**
	 * Initializes the tuner section.
	 */
	public void initTuner( boolean controlI2C ) throws UsbException
	{
		/* Disable zero IF mode */
		writeDemodRegister( mUSBDevice, 
							Page.ONE, 
							(short)0xB1, 
							(short)0x1A, 
							1 );
		
		/* Only enable in-phase ADC input */
		writeDemodRegister( mUSBDevice, 
							Page.ZERO, 
							(short)0x08, 
							(short)0x4D, 
							1 );
		
		/* Set intermediate frequency to R820T IF frequency (3.57 MHz) */
		setIFFrequency( R820T_IF_FREQUENCY );

		/* Enable spectrum inversion */
		writeDemodRegister( mUSBDevice, 
							Page.ONE, 
							(short)0x15, 
							(short)0x01, 
							1 );
		
        initializeRegisters( controlI2C );

        setTVStandard( controlI2C );
        
        systemFrequencySelect( 0, controlI2C );
	}

	/**
	 * Partially implements the r82xx_set_tv_standard() method from librtlsdr.
	 * Sets standard to digital tv to support sdr operations only.
	 */
	private void setTVStandard( boolean controlI2C ) throws UsbException
	{
	    /* Init Flag & Xtal check Result */
	    writeR820TRegister( Register.XTAL_CHECK, (byte)0x00, controlI2C );
	    
	    /* Set version */
        writeR820TRegister( Register.VERSION, VERSION, controlI2C );
	    
        /* LT Gain Test */
        writeR820TRegister( Register.LNA_TOP, (byte)0x00, controlI2C );

        int calibrationCode = 0;
        
        for( int x = 0; x < 2; x++ )
        {
            /* Set filter cap */
            writeR820TRegister( Register.FILTER_CAPACITOR, (byte)0x6B, controlI2C );

            /* Set calibration clock on */
            writeR820TRegister( Register.CALIBRATION_CLOCK, (byte)0x04, controlI2C );

            /* XTAL capacitor 0pF for PLL */
            writeR820TRegister( Register.PLL_XTAL_CAPACITOR, (byte)0x00, controlI2C );

            setPLL( 56000 * 1000, controlI2C );
            
            /* Start trigger */
            writeR820TRegister( Register.CALIBRATION_TRIGGER, (byte)0x10, controlI2C );

            /* Stop trigger */
            writeR820TRegister( Register.CALIBRATION_TRIGGER, (byte)0x00, controlI2C );

            /* Set calibration clock off */
            writeR820TRegister( Register.CALIBRATION_CLOCK, (byte)0x00, controlI2C );

            calibrationCode = getCalibrationCode( controlI2C );
            
            if( !calibrationSuccessful( calibrationCode ) )
            {
            	Log.error( "Calibration NOT successful!!" );
            }
        }

    	if( calibrationCode == 0x0F )
    	{
    		calibrationCode = 0;
    	}
    	
    	/* Write calibration code */
    	byte filt_q = 0x10;
    	calibrationCode = calibrationCode | filt_q;
    	
    	writeR820TRegister( Register.FILTER_CALIBRATION_CODE, 
    			(byte)calibrationCode, controlI2C );

    	/* Set BW, Filter gain & HP Corner */
    	writeR820TRegister( Register.BANDWIDTH_FILTER_GAIN_HIGHPASS_FILTER_CORNER, 
    			(byte)0x6B, controlI2C );

    	/* Set Image_R */
    	writeR820TRegister( Register.IMAGE_REVERSE, (byte)0x00, controlI2C );
    	
    	/* Set filter_3db, V6MHz */
    	writeR820TRegister( Register.FILTER_GAIN, (byte)0x10, controlI2C );

    	/* Channel filter extension */
    	writeR820TRegister( Register.CHANNEL_FILTER_EXTENSION, (byte)0x60, controlI2C );

    	/* Loop through */
    	writeR820TRegister( Register.LOOP_THROUGH, (byte)0x00, controlI2C );

    	/* Loop through attenuation */
    	writeR820TRegister( Register.LOOP_THROUGH_ATTENUATION, (byte)0x00, controlI2C );
    	
    	/* Filter extension widest */
    	writeR820TRegister( Register.FILTER_EXTENSION_WIDEST, (byte)0x00, controlI2C );

    	/* RF poly filter current */
    	writeR820TRegister( Register.RF_POLY_FILTER_CURRENT, (byte)0x60, controlI2C );
	}

	/**
	 * Indicates if a calibration request was successful.
	 */
	private boolean calibrationSuccessful( int calibrationCode )
	{
		return calibrationCode != 0 && calibrationCode != 0x0F;
	}

	/**
	 * Returns the calibration code resulting from a calibration request.
	 */
	private int getCalibrationCode( boolean controlI2C ) throws UsbException
	{
		return getStatusRegister( 4, controlI2C ) & 0x0F;
	}

	/**
	 * Sets the system IF frequency
	 */
	private void systemFrequencySelect( int frequency, boolean controlI2C )
									throws UsbException
	{
		/* LNA top? */
		writeR820TRegister( Register.LNA_TOP2, (byte)0xE5, controlI2C );
		
		byte mixer_top;
		byte cp_cur;
		byte div_buf_cur;
		
		if( frequency == 506000000 || 
			frequency == 666000000 || 
			frequency == 818000000 )
		{
			mixer_top = (byte)0x14;
			cp_cur = (byte)0x28;
			div_buf_cur = (byte)0x20;
		}
		else
		{
			mixer_top = (byte)0x24;
			cp_cur = (byte)0x38;
			div_buf_cur = (byte)0x30;
		}

		writeR820TRegister( Register.MIXER_TOP, mixer_top, controlI2C );

		writeR820TRegister( Register.LNA_VTH_L, (byte)0x53, controlI2C );

		writeR820TRegister( Register.MIXER_VTH_L, (byte)0x75, controlI2C );
		
		/* Air-In only for Astrometa */
		writeR820TRegister( Register.AIR_CABLE1_INPUT_SELECTOR, (byte)0x00, controlI2C );
		
		writeR820TRegister( Register.CABLE2_INPUT_SELECTOR, (byte)0x00, controlI2C );
		
		writeR820TRegister( Register.CP_CUR, cp_cur, controlI2C );
		
		writeR820TRegister( Register.DIVIDER_BUFFER_CURRENT, div_buf_cur, controlI2C );

		writeR820TRegister( Register.FILTER_CURRENT, (byte)0x40, controlI2C );

		/* if( type != TUNER_ANALOG_TV ) ... */
		
		writeR820TRegister( Register.LNA_TOP, (byte)0x00, controlI2C );

		writeR820TRegister( Register.MIXER_TOP2, (byte)0x00, controlI2C );
		
		writeR820TRegister( Register.PRE_DETECT, (byte)0x00, controlI2C );
		
		writeR820TRegister( Register.AGC_CLOCK, (byte)0x30, controlI2C );

		writeR820TRegister( Register.LNA_TOP, (byte)0x18, controlI2C );

		writeR820TRegister( Register.MIXER_TOP2, mixer_top, controlI2C );

		/* LNA discharge current */
		writeR820TRegister( Register.LNA_DISCHARGE_CURRENT, (byte)0x14, controlI2C );
		
		/* AGC clock 1 khz, external det1 cap 1u */
		writeR820TRegister( Register.AGC_CLOCK, (byte)0x20, controlI2C );
	}
	
	/**
	 * Sets the tuner's Phase-Locked-Loop (PLL) oscillator used for frequency 
	 * (tuning) control
	 * 
	 * @param frequency - desired center frequency
	 * @param controlI2C - control the I2C repeater locally
	 * @throws UsbException - if unable to set any of the R820T registers
	 */
	private void setPLL( int frequency, boolean controlI2C ) throws UsbException
	{
	    /* Set reference divider to 0 */
        writeR820TRegister( Register.REFERENCE_DIVIDER_2, (byte)0x00, controlI2C );

        /* Set PLL autotune to 128kHz */
        writeR820TRegister( Register.PLL_AUTOTUNE, (byte)0x00, controlI2C );

        /* Set VCO current to 100 */
        writeR820TRegister( Register.VCO_CURRENT, (byte)0x80, controlI2C );

        /* Set the frequency divider - adjust for vco_fine_tune status */
        FrequencyDivider divider = FrequencyDivider.fromFrequency( frequency );

        int statusRegister4 = getStatusRegister( 4, controlI2C );

        int vco_fine_tune = ( statusRegister4 & 0x30 ) >> 4;
        
        int div_num = divider.getDividerNumber( vco_fine_tune );
        
        writeR820TRegister( Register.DIVIDER, (byte)( div_num << 5 ), controlI2C );

        /* Get the integral number for this divider and frequency */
        Integral integral = divider.getIntegral( frequency );

        writeR820TRegister( Register.PLL, integral.getRegisterValue(), controlI2C );

        /* Calculate the sigma-delta modulator fractional setting.  If it's
         * non-zero, power up the sdm and apply the fractional setting,
         * otherwise turn it off */
        int sdm = divider.getSDM( integral, frequency );

        if( sdm != 0 ) 
        {
        	writeR820TRegister( Register.SIGMA_DELTA_MODULATOR_POWER, 
        			(byte)0x00, controlI2C );

        	writeR820TRegister( Register.SIGMA_DELTA_MODULATOR_MSB, 
            		(byte)( ( sdm >> 8 ) & 0xFF ), controlI2C );

            writeR820TRegister( Register.SIGMA_DELTA_MODULATOR_LSB, 
            		(byte)( sdm & 0xFF ), controlI2C );
        }
        else
        {
        	writeR820TRegister( Register.SIGMA_DELTA_MODULATOR_POWER, 
        			(byte)0x08, controlI2C );
        }

        /* Check to see if the PLL locked with these divider, integral and sdm
         * settings */
        if( !isPLLLocked( controlI2C ) )
        {
        	/* Increase VCO current */
        	writeR820TRegister( Register.VCO_CURRENT, (byte)0x60, controlI2C );
        	
        	if( !isPLLLocked( controlI2C ) )
        	{
        		throw new UsbException( "R820T Tuner Controller - couldn't "
        				+ "achieve PLL lock on frequency [" + frequency + "]" );
        	}
        }

        /* set pll autotune to 8kHz */
        writeR820TRegister( Register.PLL_AUTOTUNE_VARIANT, (byte)0x08, controlI2C );
	}

	/**
	 * Indicates if the Phase Locked Loop (PLL) oscillator is locked following
	 * a change in the tuned center frequency.  Checks status register 2 to see 
	 * if the PLL locked indicator bit is set. 
	 */
	private boolean isPLLLocked( boolean controlI2C ) throws UsbException
	{
		int register = getStatusRegister( 2, controlI2C );
		
		return ( register & 0x40 ) == 0x40; 
	}
	
	/**
	 * Writes initial starting value of registers 0x05 through 0x1F using the 
	 * default value initialized in the shadow register array.  This method only
	 * needs to be called once, upon initialization.
	 * @throws UsbException
	 */
	private void initializeRegisters( boolean controlI2C ) throws UsbException
	{
	    for( int x = 5; x < mShadowRegister.length; x++ )
	    {
	        writeI2CRegister( mUSBDevice, 
	                          mI2CAddress,
	                          (byte)x,
	                          (byte)mShadowRegister[ x ],
	                          controlI2C );
	    }
	}

	/**
	 * Returns the contents of status registers 0 through 4 
	 */
	private int getStatusRegister( int register, boolean controlI2C ) throws UsbException
	{
		byte[] data = new byte[ 5 ];
		
		byte[] statusRegisters = read( mUSBDevice, Block.IIC, mI2CAddress, data );

		return bitReverse( statusRegisters[ register ] & 0xFF );
	}
	
	/**
	 * Assumes value is a byte value and reverses the bits in the byte.
	 */
	private static int bitReverse( int value )
	{
		return BIT_REV_LOOKUP_TABLE[ value & 0x0F ] << 4 | 
			   BIT_REV_LOOKUP_TABLE[ ( value & 0xF0 ) >> 4 ];
	}

	/**
	 * Writes the byte value to the specified register, optionally controlling
	 * the I2C repeater as needed.
	 */
	public void writeR820TRegister( Register register, 
	                                byte value, 
	                                boolean controlI2C ) throws UsbException
	{
	    if( register.isMasked() )
	    {
	        int current = mShadowRegister[ register.getRegister() ];

	        value = (byte)( ( current & ~register.getMask() ) | 
	                        ( value & register.getMask() ) );
	    }

        writeI2CRegister( mUSBDevice, mI2CAddress, 
                (byte)register.getRegister(), value, controlI2C );

        mShadowRegister[ register.getRegister() ] = value;
        
//        Log.info( "R820T writing register " + 
//        		String.format( "%02X", register.getRegister() ) + " value " + 
//        		String.format( "%02X", value ) );
	}

	/**
	 * Reads the specified register, optionally controlling the I2C repeater
	 */
	public int readR820TRegister( Register register, boolean controlI2C )
	                    throws UsbException
	{
		int value = readI2CRegister( mUSBDevice, 
	                            mI2CAddress, 
	                            (byte)register.getRegister(), 
	                            controlI2C );
		
		return value;
	}
	
	/**
	 * Sets master gain by applying gain component values to LNA, Mixer and
	 * VGA gain registers.
	 */
	public void setGain( R820TGain gain, boolean controlI2C ) throws UsbException
	{
		setLNAGain( gain.getLNAGain(), controlI2C );
		setMixerGain( gain.getMixerGain(), controlI2C );
		setVGAGain( gain.getVGAGain(), controlI2C );
	}
	
	/**
	 * Sets LNA gain
	 */
	public void setLNAGain( R820TLNAGain gain, boolean controlI2C ) throws UsbException
	{
		writeR820TRegister( Register.LNA_GAIN, gain.getSetting(), controlI2C );
	}

	/**
	 * Sets Mixer gain
	 */
	public void setMixerGain( R820TMixerGain gain, boolean controlI2C ) throws UsbException 
	{
		writeR820TRegister( Register.MIXER_GAIN, gain.getSetting(), controlI2C );
	}

	/**
	 * Sets VGA gain
	 */
	public void setVGAGain( R820TVGAGain gain, boolean controlI2C ) throws UsbException
	{
		writeR820TRegister( Register.VGA_GAIN, gain.getSetting(), controlI2C );
	}
	
	/**
	 * R820T VGA gain settings 
	 */
	public enum R820TVGAGain
	{
	    GAIN_0( "0", 0x00 ),
	    GAIN_26( "26", 0x01 ),
	    GAIN_52( "52", 0x02 ),
	    GAIN_82( "82", 0x03 ),
	    GAIN_124( "124", 0x04 ),
	    GAIN_159( "159", 0x05 ),
	    GAIN_183( "183", 0x06 ),
	    GAIN_196( "196", 0x07 ),
	    GAIN_210( "210", 0x08 ),
	    GAIN_242( "242", 0x09 ),
	    GAIN_278( "278", 0x0A ),
	    GAIN_312( "312", 0x0B ),
	    GAIN_347( "347", 0x0C ),
	    GAIN_384( "384", 0x0D ),
	    GAIN_419( "419", 0x0E ),
	    GAIN_455( "455", 0x0F );
	    
	    private String mLabel;
	    private int mSetting;
	    
	    private R820TVGAGain( String label, int setting )
	    {
	        mLabel = label;
	        mSetting = setting;
	    }
	    
	    public String toString()
	    {
	        return mLabel;
	    }
	    
	    public byte getSetting()
	    {
	        return (byte)mSetting;
	    }
	}
	
	/**
	 * R820T Low Noise Amplifier gain settings
	 */
    public enum R820TLNAGain
    {
        AUTOMATIC( "Automatic", 0x00 ),
        GAIN_0( "0", 0x10 ),
        GAIN_9( "9", 0x11 ),
        GAIN_21( "21", 0x12 ),
        GAIN_61( "61", 0x13 ),
        GAIN_99( "99", 0x14 ),
        GAIN_112( "112", 0x15 ),
        GAIN_143( "143", 0x16 ),
        GAIN_165( "165", 0x17 ),
        GAIN_191( "191", 0x18 ),
        GAIN_222( "222", 0x19 ),
        GAIN_248( "248", 0x1A ),
        GAIN_262( "262", 0x1B ),
        GAIN_281( "281", 0x1C ),
        GAIN_286( "286", 0x1D ),
        GAIN_321( "321", 0x1E ),
        GAIN_334( "334", 0x1F );
        
        private String mLabel;
        private int mSetting;
        
        private R820TLNAGain( String label, int setting )
        {
            mLabel = label;
            mSetting = setting;
        }
        
        public String toString()
        {
            return mLabel;
        }
        
        public byte getSetting()
        {
            return (byte)mSetting;
        }
    }

    /**
     * R820T mixer gain settings
     */
    public enum R820TMixerGain
    {
        AUTOMATIC( "Automatic", 0x10 ),
        GAIN_0( "0", 0x00 ),
        GAIN_5( "5", 0x01 ),
        GAIN_15( "15", 0x02 ),
        GAIN_25( "25", 0x03 ),
        GAIN_44( "44", 0x04 ),
        GAIN_53( "53", 0x05 ),
        GAIN_63( "63", 0x06 ),
        GAIN_88( "88", 0x07 ),
        GAIN_105( "105", 0x08 ),
        GAIN_115( "115", 0x09 ),
        GAIN_123( "123", 0x0A ),
        GAIN_139( "139", 0x0B ),
        GAIN_152( "152", 0x0C ),
        GAIN_158( "158", 0x0D ),
        GAIN_161( "161", 0x0E ),
        GAIN_153( "153", 0x0F );
        
        private String mLabel;
        private int mSetting;
        
        private R820TMixerGain( String label, int setting )
        {
            mLabel = label;
            mSetting = setting;
        }
        
        public String toString()
        {
            return mLabel;
        }
        
        public byte getSetting()
        {
            return (byte)mSetting;
        }
    }
    
    /**
     * R820T Master gain settings 
     */
    public enum R820TGain
    {
        AUTOMATIC( "Automatic", R820TVGAGain.GAIN_312, R820TLNAGain.AUTOMATIC, R820TMixerGain.AUTOMATIC ),
        MANUAL( "Manual", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_248, R820TMixerGain.GAIN_123 ),
        GAIN_0( "0", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_0, R820TMixerGain.GAIN_0 ),
        GAIN_9( "9", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_9, R820TMixerGain.GAIN_0 ),
        GAIN_14( "14", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_9, R820TMixerGain.GAIN_5 ),
        GAIN_26( "26", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_21, R820TMixerGain.GAIN_5 ),
        GAIN_36( "36", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_21, R820TMixerGain.GAIN_15 ),
        GAIN_76( "76", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_61, R820TMixerGain.GAIN_15 ),
        GAIN_86( "86", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_61, R820TMixerGain.GAIN_25 ),
        GAIN_124( "124", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_99, R820TMixerGain.GAIN_25 ),
        GAIN_143( "143", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_99, R820TMixerGain.GAIN_44 ),
        GAIN_156( "156", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_112, R820TMixerGain.GAIN_44 ),
        GAIN_165( "165", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_112, R820TMixerGain.GAIN_53 ),
        GAIN_196( "196", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_143, R820TMixerGain.GAIN_53 ),
        GAIN_208( "208", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_143, R820TMixerGain.GAIN_63 ),
        GAIN_228( "228", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_165, R820TMixerGain.GAIN_63 ),
        GAIN_253( "253", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_165, R820TMixerGain.GAIN_88 ),
        GAIN_279( "279", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_191, R820TMixerGain.GAIN_88 ),
        GAIN_296( "296", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_191, R820TMixerGain.GAIN_105 ),
        GAIN_327( "327", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_222, R820TMixerGain.GAIN_105 ),
        GAIN_337( "337", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_222, R820TMixerGain.GAIN_115 ),
        GAIN_363( "363", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_248, R820TMixerGain.GAIN_115 ),
        GAIN_371( "371", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_248, R820TMixerGain.GAIN_123 ),
        GAIN_385( "385", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_262, R820TMixerGain.GAIN_123 ),
        GAIN_401( "401", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_262, R820TMixerGain.GAIN_139 ),
        GAIN_420( "420", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_281, R820TMixerGain.GAIN_139 ),
        GAIN_433( "433", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_281, R820TMixerGain.GAIN_152 ),
        GAIN_438( "438", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_286, R820TMixerGain.GAIN_152 ),
        GAIN_444( "444", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_286, R820TMixerGain.GAIN_158 ),
        GAIN_479( "479", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_321, R820TMixerGain.GAIN_158 ),
        GAIN_482( "482", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_321, R820TMixerGain.GAIN_161 ),
        GAIN_495( "495", R820TVGAGain.GAIN_210, R820TLNAGain.GAIN_334, R820TMixerGain.GAIN_161 ); 
        
        private String mLabel;
        private R820TVGAGain mVGAGain;
        private R820TLNAGain mLNAGain;
        private R820TMixerGain mMixerGain;
        
        private R820TGain( String label, R820TVGAGain vga, R820TLNAGain lna, R820TMixerGain mixer )
        {
            mLabel = label;
            mVGAGain = vga;
            mLNAGain = lna;
            mMixerGain = mixer;
        }
        
        public String toString()
        {
            return mLabel;
        }
        
        public R820TVGAGain getVGAGain()
        {
            return mVGAGain;
        }

        public R820TLNAGain getLNAGain()
        {
            return mLNAGain;
        }
        
        public R820TMixerGain getMixerGain()
        {
            return mMixerGain;
        }
    }
    
    /**
     * R820T tuner registers and register mask values
     */
    public enum Register
	{
	    LNA_GAIN( 0x05, 0x1F ),
	    AIR_CABLE1_INPUT_SELECTOR( 0x05, 0x60 ),
	    LOOP_THROUGH( 0x05, 0x80 ),
	    CABLE2_INPUT_SELECTOR( 0x06, 0x08 ),
	    FILTER_GAIN( 0x06, 0x30 ),
	    PRE_DETECT( 0x06, 0x40 ),
	    MIXER_GAIN( 0x07, 0x1F ),
	    IMAGE_REVERSE( 0x07, 0x80 ),
        UNKNOWN_REGISTER_8( 0x08, 0x3F ),
        UNKNOWN_REGISTER_9( 0x09, 0x3F ),
        FILTER_CALIBRATION_CODE( 0x0A, 0x1F ),
        FILTER_CURRENT( 0x0A, 0x60 ),
        CALIBRATION_TRIGGER( 0x0B, 0x10 ),
        FILTER_CAPACITOR( 0x0B, 0x60 ),
        BANDWIDTH_FILTER_GAIN_HIGHPASS_FILTER_CORNER( 0x0B, 0xEF ),
        XTAL_CHECK( 0x0C, 0x0F ),
        VGA_GAIN( 0x0C, 0x9F ),
        LNA_VTH_L( 0x0D, 0x0 ),
        MIXER_VTH_L( 0x0E, 0x0 ),
        CALIBRATION_CLOCK( 0x0F, 0x04 ),
        FILTER_EXTENSION_WIDEST( 0x0F, 0x80 ),
        PLL_XTAL_CAPACITOR( 0x10, 0x03 ),
        UNKNOWN_REGISTER_10( 0x10, 0x04 ),
        PLL_XTAL_CAPACITOR_AND_DRIVE( 0x10, 0x0B ),
        REFERENCE_DIVIDER_2( 0x10, 0x10 ),
        CAPACITOR_SELECTOR( 0x10, 0x1B ),
        DIVIDER( 0x10, 0xE0 ),
        CP_CUR( 0x11, 0x38 ),
        SIGMA_DELTA_MODULATOR_POWER( 0x12, 0x08 ),
        VCO_CURRENT( 0x12, 0xE0 ),
        VERSION( 0x13, 0x3F ),
        PLL( 0x14, 0x0 ),
        SIGMA_DELTA_MODULATOR_LSB( 0x15, 0x0 ),
        SIGMA_DELTA_MODULATOR_MSB( 0x16, 0x0 ),
        DRAIN( 0x17, 0x08 ),
        DIVIDER_BUFFER_CURRENT( 0x17, 0x30 ),
        RF_POLY_FILTER_CURRENT( 0x19, 0x60 ),
        PLL_AUTOTUNE( 0x1A, 0x0C ),
        PLL_AUTOTUNE_VARIANT( 0x1A, 0x08 ),
        AGC_CLOCK( 0x1A, 0x30 ),
        RF_POLY_MUX( 0x1A, 0xC3 ),
        TF_BAND( 0x1B, 0x0 ),
        MIXER_TOP( 0x1C, 0xF8 ),
        MIXER_TOP2( 0X1C, 0x04 ),
        LNA_TOP( 0x1D, 0x38 ),
        LNA_TOP2( 0x1D, 0xC7 ),
        CHANNEL_FILTER_EXTENSION( 0x1E, 0x60 ),
        LNA_DISCHARGE_CURRENT( 0x1E, 0x1F ),
        LOOP_THROUGH_ATTENUATION( 0x1F, 0x80 );

	    private int mRegister;
		private int mMask;
		
		private Register( int register, int mask )
		{
			mRegister = register;
			mMask = mask;
		}
		
		public int getRegister()
		{
			return mRegister;
		}
		
		public byte getMask()
		{
		    return (byte)mMask;
		}
		
		public boolean isMasked()
		{
		    return mMask != 0;
		}
	}
	
	public enum FrequencyRange
	{
		RANGE_024(  24000000,   49999999, 0x08, 0x02, 0xDF, 0x02, 0x01 ),
		RANGE_050(  50000000,   54999999, 0x08, 0x02, 0xBE, 0x02, 0x01 ),
		RANGE_055(  55000000,   59999999, 0x08, 0x02, 0x8B, 0x02, 0x01 ),
		RANGE_060(  60000000,   64999999, 0x08, 0x02, 0x7B, 0x02, 0x01 ),
		RANGE_065(  65000000,   69999999, 0x08, 0x02, 0x69, 0x02, 0x01 ),
		RANGE_070(  70000000,   74999999, 0x08, 0x02, 0x58, 0x02, 0x01 ),
		RANGE_075(  75000000,   79999999, 0x00, 0x02, 0x44, 0x02, 0x01 ),
		RANGE_080(  80000000,   89999999, 0x00, 0x02, 0x44, 0x02, 0x01 ),
		RANGE_090(  90000000,   99999999, 0x00, 0x02, 0x34, 0x01, 0x01 ),
		RANGE_100( 100000000,  109999999, 0x00, 0x02, 0x34, 0x01, 0x01 ),
		RANGE_110( 110000000,  119999999, 0x00, 0x02, 0x24, 0x01, 0x01 ),
		RANGE_120( 120000000,  139999999, 0x00, 0x02, 0x24, 0x01, 0x01 ),
		RANGE_140( 140000000,  179999999, 0x00, 0x02, 0x14, 0x01, 0x01 ),
		RANGE_180( 180000000,  219999999, 0x00, 0x02, 0x13, 0x00, 0x00 ),
		RANGE_220( 220000000,  249999999, 0x00, 0x02, 0x13, 0x00, 0x00 ),
		RANGE_250( 250000000,  279999999, 0x00, 0x02, 0x11, 0x00, 0x00 ),
		RANGE_280( 280000000,  309999999, 0x00, 0x02, 0x00, 0x00, 0x00 ),
		RANGE_310( 310000000,  449999999, 0x00, 0x41, 0x00, 0x00, 0x00 ),
		RANGE_450( 450000000,  587999999, 0x00, 0x41, 0x00, 0x00, 0x00 ),
		RANGE_588( 588000000,  649999999, 0x00, 0x40, 0x00, 0x00, 0x00 ),
		RANGE_650( 650000000, 1766000000, 0x00, 0x40, 0x00, 0x00, 0x00 ),
		RANGE_UNK( 0, 0, 0, 0, 0, 0, 0 );

		private int mMinFrequency;
		private int mMaxFrequency;
		private int mOpenDrain;
		private int mRFMux_PolyMux;
		private int mTF_c;
		private int mXtalCap20p;
		private int mXtalCap10p;
		
		private FrequencyRange( int minFrequency,
								int maxFrequency,
								int openDrain,
								int rfMuxPloy,
								int tf_c,
								int xtalCap20p,
								int xtalCap10p )
		{
			mMinFrequency = minFrequency;
			mMaxFrequency = maxFrequency;
			mOpenDrain = openDrain;
			mRFMux_PolyMux = rfMuxPloy;
			mTF_c = tf_c;
			mXtalCap20p = xtalCap20p;
			mXtalCap10p = xtalCap10p;
		}
		
		public boolean contains( int frequency )
		{
			return mMinFrequency <= frequency && frequency <= mMaxFrequency;
		}
		
		public static FrequencyRange getRangeForFrequency( int frequency )
		{
			for( FrequencyRange range: values() )
			{
				if( range.contains( frequency ) )
				{
					return range;
				}
			}
			
			return RANGE_UNK;
		}
		
		public int getMinFrequency()
		{
			return mMinFrequency;
		}
		
		public int getMaxFrequency()
		{
			return mMaxFrequency;
		}
		
		public byte getOpenDrain()
		{
			return (byte)mOpenDrain;
		}
		
		public byte getRFMuxPolyMux()
		{
			return (byte)mRFMux_PolyMux;
		}
		
		public byte getTFC()
		{
			return (byte) mTF_c;
		}
		
		public byte getXTALCap20P()
		{
			return (byte)mXtalCap20p;
		}
		
		public byte getXTALCap10P()
		{
			return (byte)mXtalCap10p;
		}
		
		public byte getXTALLowCap0P()
		{
			return (byte)0x08;
		}

		public byte getXTALHighCap0P()
		{
			return (byte)0x00;
		}
	}
	
    /**
     * Frequency Divider Ranges
     * 
     * div_num	mix_div	min	     - max
     * 0		2		885.0000 - 1770.0000 MHz
     * 1		4		442.5000 -  885.9999 MHz
     * 2		8		221.2500 -  442.4999 MHz
     * 3		16		110.6250 -  221.2499 MHz
     * 4		32		 55.3125 -  110.6249 MHz
     * 5		64		 27.65625 -  55.31249 MHz
     * 
     * Subtract 3.57 MHz IF frequency from each of these values to yield:
     * 
     * div_num	mix_div	min	     - max
     * 0		2		881.430000 - 1766.430000 MHz
     * 1		4		438.930000 -  881.429999 MHz
     * 2		8		217.680000 -  438.929999 MHz
     * 3		16		107.055000 -  217.679999 MHz
     * 4		32		 51.742500 -  107.054999 MHz
     * 5		64		 24.086250 -   51.742499 MHz
     */
	public enum FrequencyDivider
	{
		//Subtracted IF values
//		DIVIDER_0( 0,  2, 881430000, 1664300000, 0x00, 28800000 ),
//		DIVIDER_1( 1,  4, 438930000,  881429999, 0x20, 14400000 ),
//		DIVIDER_2( 2,  8, 217680000,  438929999, 0x40,  7200000 ),
//		DIVIDER_3( 3, 16, 107055000,  217679999, 0x60,  3600000 ),
//		DIVIDER_4( 4, 32,  51742500,  107054599, 0x80,  1800000 ),
//		DIVIDER_5( 5, 64,  24086250,   51742499, 0xA0,   900000 );

		//Normal divider values
		DIVIDER_0( 0,  2, 885000000, 1770000000, 0x00, 28800000 ),
		DIVIDER_1( 1,  4, 442500000,  884999999, 0x20, 14400000 ),
		DIVIDER_2( 2,  8, 221250000,  442499999, 0x40,  7200000 ),
		DIVIDER_3( 3, 16, 110625000,  221249999, 0x60,  3600000 ),
		DIVIDER_4( 4, 32,  55312500,  110624999, 0x80,  1800000 ),
		DIVIDER_5( 5, 64,  27656250,   55312499, 0xA0,   900000 );
		
		private int mDividerNumber;
		private int mMixerDivider;
		private int mMinimumFrequency;
		private int mMaximumFrequency;
		private int mRegisterSetting;
		private int mIntegralValue;
		private static final int mVCOPowerReference = 2;
		
		private FrequencyDivider( int dividerNumber, 
								  int mixerDivider,
								  int minimumFrequency,
								  int maximumFrequency,
								  int registerSetting,
								  int integralValue )
		{
			mDividerNumber = dividerNumber;
			mMixerDivider = mixerDivider;
			mMinimumFrequency = minimumFrequency;
			mMaximumFrequency = maximumFrequency;
			mRegisterSetting = registerSetting;
			mIntegralValue = integralValue;
		}

		public int getDividerNumber( int vcoFineTune )
		{
	        if( vcoFineTune == mVCOPowerReference )
	        {
	        	return mDividerNumber;
	        }
	        else if( vcoFineTune < mVCOPowerReference )
	        {
	        	return mDividerNumber - 1;
	        }
	        else if( vcoFineTune > mVCOPowerReference )
	        {
	        	return mDividerNumber + 1;
	        }
	        
	        return mDividerNumber;
		}
		
		public int getMixerDivider()
		{
			return mMixerDivider;
		}
		
		public int getMinimumFrequency()
		{
			return mMinimumFrequency;
		}
		
		public int getMaximumFrequency()
		{
			return mMaximumFrequency;
		}
		
		public byte getDividerRegisterSetting()
		{
			return (byte)mRegisterSetting;
		}
		
		public boolean contains( int frequency )
		{
			return mMinimumFrequency <= frequency && 
					frequency <= mMaximumFrequency;
		}

		/**
		 * Returns the correct frequency divider for the specified frequency,
		 * or returns divider #5, if the frequency is outside of the specified
		 * frequency ranges.
		 * @param frequency - desired frequency 
		 * @return - FrequencyDivider to use for the specified frequency
		 */
		public static FrequencyDivider fromFrequency( int frequency )
		{
			for( FrequencyDivider divider: FrequencyDivider.values() )
			{
				if( divider.contains( frequency ) )
				{
					return divider;
				}
			}
			
			return FrequencyDivider.DIVIDER_5;
		}

		/**
		 * Returns the integral to use for this frequency
		 */
		public Integral getIntegral( int frequency )
		{
			if( contains( frequency ) )
			{
				int delta = frequency - mMinimumFrequency;
				
				int integral = (int)Math.round( (double)delta / 
											    (double)mIntegralValue );
				
				return Integral.fromValue( integral );
			}
			
			throw new IllegalArgumentException( "PLL frequency [" + frequency + 
					"] is not valid for frequency divider " + this.toString() );
		}

		/**
		 * Calculates the 16-bit value of the sigma-delta modulator setting
		 * which represents the fractional portion of the requested frequency
		 * that is left over after subtracting the divider minimum frequency
		 * and the integral frequency units.  That residual value is divided
		 * by the integral value to derive a 16-bit fractional value, returned
		 * as an integer
		 */
		public int getSDM( Integral integral, int frequency )
		{
			if( contains( frequency ) )
			{
				int delta = frequency - ( integral.getNumber() * mIntegralValue );

				double fractional = (double)delta / (double)mIntegralValue;
				
				//Left shift the double value 16 bits and truncate to an integer
				return (int)( fractional * 0x10000 ) & 0xFFFF;
			}

			return 0;
		}
	}

	/**
	 * PLL Integral values.  Each value represents one unit of the divided
	 * crystal frequency as follows:
	 * 
	 * Divider   Value
	 * 64        I *  0.9 MHz
	 * 32		 I *  1.8 MHz
	 * 16        I *  3.6 MHz
	 *  8        I *  7.2 MHz
	 *  4        I * 14.4 MHz
	 *  2        I * 28.8 MHz
	 * @author denny
	 *
	 */
	public enum Integral
	{
		I00( 0, 0x44 ),
		I01( 1, 0x84 ),
		I02( 2, 0xC4 ),
		I03( 3, 0x05 ),
		I04( 4, 0x45 ),
		I05( 5, 0x85 ),
		I06( 6, 0xC5 ),
		I07( 7, 0x06 ),
		I08( 8, 0x46 ),
		I09( 9, 0x86 ),
		I10( 10, 0xC6 ),
		I11( 11, 0x07 ),
		I12( 12, 0x47 ),
		I13( 13, 0x87 ),
		I14( 14, 0xC7 ),
		I15( 15, 0x08 ),
		I16( 16, 0x48 ),
		I17( 17, 0x88 ),
		I18( 18, 0xC8 ),
		I19( 19, 0x09 ),
		I20( 20, 0x49 ),
		I21( 21, 0x89 ),
		I22( 22, 0xC9 ),
		I23( 23, 0x0A ),
		I24( 24, 0x4A ),
		I25( 25, 0x8A ),
		I26( 26, 0xCA ),
		I27( 27, 0x0B ),
		I28( 28, 0x4B ),
		I29( 29, 0x8B ),
		I30( 30, 0xCB ),
		I31( 31, 0x0C );

		private int mNumber;
		private int mRegister;
		
		private Integral( int number, int register )
		{
			mNumber = number;
			mRegister = register;
		}
		
		public int getNumber()
		{
			return mNumber;
		}
		
		public byte getRegisterValue()
		{
			return (byte)mRegister;
		}
		
		public static Integral fromValue( int value )
		{
			for( Integral integral: Integral.values() )
			{
				if( integral.getNumber() == value )
				{
					return integral;
				}
			}
			
			throw new IllegalArgumentException( "PLL integral value [" + value + 
					"] must be in the range 0 - 31");
		}
	}
}

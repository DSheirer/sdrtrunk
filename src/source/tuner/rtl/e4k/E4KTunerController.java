/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
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
package source.tuner.rtl.e4k;

import java.util.Arrays;

import javax.swing.JPanel;
import javax.usb.UsbException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import settings.SettingsManager;
import source.SourceException;
import source.tuner.TunerConfiguration;
import source.tuner.TunerType;
import source.tuner.rtl.RTL2832TunerController;
import controller.ThreadPoolManager;

public class E4KTunerController extends RTL2832TunerController
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( E4KTunerController.class );

	public static final long MIN_FREQUENCY = 52000000;
	public static final long MAX_FREQUENCY = 2200000000l;
	public static final double USABLE_BANDWIDTH_PERCENT = 0.95;
	public static final int DC_SPIKE_AVOID_BUFFER = 15000;

	/* The local oscillator is defined by whole (integer) units of the oscillator
	 * frequency and fractional units representing 1/65536th of the oscillator
	 * frequency, meaning we can only tune the local oscillator in units of 
	 * 439.453125 hertz. */
	public static final long E4K_PLL_Y = 65536l; /* 16-bit fractional register */
	public static final byte MASTER1_RESET = (byte)0x01;
	public static final byte MASTER1_NORM_STBY = (byte)0x02;
	public static final byte MASTER1_POR_DET = (byte)0x04;
	
	public static final byte SYNTH1_PLL_LOCK = (byte)0x01;
	public static final byte SYNTH1_BAND_SHIF = (byte)0x01;

	public static final byte SYNTH7_3PHASE_EN = (byte)0x08;

	public static final byte SYNTH8_VCOCAL_UPD = (byte)0x04;
	
	public static final byte FILT3_MASK = (byte)0x20;
	public static final byte FILT3_ENABLE = (byte)0x00;
	public static final byte FILT3_DISABLE = (byte)0x20;
	
	public static final byte MIXER_FILTER_MASK = (byte)0xF0;
	public static final byte IF_CHANNEL_FILTER_MASK = (byte)0x1F;
	public static final byte IF_RC_FILTER_MASK = (byte)0x0F;
	

	public static final byte AGC1_LIN_MODE = (byte)0x10;
	public static final byte AGC1_LNA_UPDATE = (byte)0x20;
	public static final byte AGC1_LNA_G_LOW = (byte)0x40;
	public static final byte AGC1_LNA_G_HIGH = (byte)0x80;
	public static final byte AGC1_MOD_MASK = (byte)0xF;

	public static final byte GAIN1_MOD_MASK = (byte)0xF;
	public static final byte IF_GAIN_MODE_SWITCHING_MASK = (byte)0x1;

	public static final byte AGC6_LNA_CAL_REQ = (byte)0x10;
	
	public static final byte AGC7_MIXER_GAIN_MASK = (byte)0x01;
	public static final byte AGC7_MIX_GAIN_MANUAL = (byte)0x00;
	public static final byte AGC7_MIX_GAIN_AUTO = (byte)0x01;
	public static final byte AGC7_GAIN_STEP_5DB = (byte)0x20;
	
	public static final byte AGC8_SENS_LIN_AUTO = (byte)0x01;

	public static final byte AGC11_LNA_GAIN_ENH = (byte)0x01;
	public static final byte ENH_GAIN_MOD_MASK = (byte)0x07;
	public static final byte MIXER_GAIN_MASK = (byte)0x01;
	
	public static final byte DC1_CAL_REQ = (byte)0x01;
	
	public static final byte DC5_I_LUT_EN = (byte)0x01;
	public static final byte DC5_Q_LUT_EN = (byte)0x02;
	public static final byte DC5_RANGE_DETECTOR_ENABLED_MASK = (byte)0x04; //DC Offset Detector Enabled
	public static final byte DC5_RANGE_DETECTOR_ENABLED = (byte)0x04; //DC Offset Detector Enabled
	public static final byte DC5_RANGE_EN = (byte)0x08;
	public static final byte DC5_RANGE_DETECTOR_DISABLED_MASK = (byte)0x03;
	public static final byte DC5_RANGE_DETECTOR_DISABLED = (byte)0x00;
	
	public static final byte DC5_TIMEVAR_EN = (byte)0x10;
	
	public static final byte CLKOUT_DISABLE = (byte)0x96;
	
	public static final byte CHFCALIB_CMD = (byte)0x01;
	
	private E4KTunerEditorPanel mEditor;
	
	public E4KTunerController( Device device, 
							   DeviceDescriptor deviceDescriptor,
							   ThreadPoolManager threadPoolManager ) 
									   throws SourceException
	{
		super( device, deviceDescriptor, threadPoolManager, MIN_FREQUENCY, 
				MAX_FREQUENCY, DC_SPIKE_AVOID_BUFFER, USABLE_BANDWIDTH_PERCENT );
	}

	@Override
    public TunerType getTunerType()
    {
	    return TunerType.ELONICS_E4000;
    }
	
	@Override
    public void apply( TunerConfiguration config ) throws SourceException
    {
		if( config != null && config instanceof E4KTunerConfiguration )
		{
			E4KTunerConfiguration e4kConfig = (E4KTunerConfiguration)config;

			try
			{
				SampleRate sampleRate = e4kConfig.getSampleRate();
				setSampleRate( sampleRate );
				
				double correction = e4kConfig.getFrequencyCorrection();
				setFrequencyCorrection( correction );
				
				E4KGain masterGain = e4kConfig.getMasterGain();
				setGain( masterGain, true );
				
				if( masterGain == E4KGain.MANUAL )
				{
					E4KMixerGain mixerGain = e4kConfig.getMixerGain();
					setMixerGain( mixerGain, true );
					
					E4KLNAGain lnaGain = e4kConfig.getLNAGain();
					setLNAGain( lnaGain, true );
					
					E4KEnhanceGain enhanceGain = e4kConfig.getEnhanceGain();
					setEnhanceGain( enhanceGain, true );
				}
			}
			catch( UsbException e )
			{
				throw new SourceException( "E4KTunerController - usb error "
						+ "while applying tuner config", e );
			}
		}
		else
		{
			throw new IllegalArgumentException( "E4KTunerController - cannot "
					+ "apply tuner configuration of type:[" + 
					config.getClass() + "]" );
		}
	    
    }

	public void init() throws SourceException
	{
		mDeviceHandle = new DeviceHandle();
		
		int result = LibUsb.open( mDevice, mDeviceHandle );

		if( result != LibUsb.SUCCESS )
		{
			mDeviceHandle = null;
			
			throw new SourceException( "libusb couldn't open RTL2832 usb "
					+ "device [" + LibUsb.errorName( result ) + "]" );
		}

		claimInterface( mDeviceHandle );

		byte[] eeprom = null;
		
		try
		{
			/* Read the contents of the 256-byte EEPROM */
			eeprom = readEEPROM( mDeviceHandle, (short)0, 256 );
		}
		catch( Exception e )
		{
			mLog.error( "error while reading the EEPROM device descriptor", e );
		}
		
		try
		{
			mDescriptor = new Descriptor( eeprom );

			if( eeprom == null )
			{
				mLog.error( "eeprom byte array was null - constructed "
						+ "empty descriptor object" );
			}
		}
		catch( Exception e )
		{
			mLog.error( "error while constructing device descriptor using "
				+ "descriptor byte array " + 
				( eeprom == null ? "[null]" : Arrays.toString( eeprom )), e );
		}

		try
		{
			/* Dummy write to test USB interface */
			writeRegister( mDeviceHandle, Block.USB, 
					Address.USB_SYSCTL.getAddress(), (short)0x09, 1 );

			initBaseband( mDeviceHandle );

			enableI2CRepeater( mDeviceHandle, true );
			
			boolean i2CRepeaterControl = false;
			
			initTuner( i2CRepeaterControl );

			enableI2CRepeater( mDeviceHandle, false );
			
			try
			{
				setSampleRate( DEFAULT_SAMPLE_RATE );
			}
			catch( Exception e )
			{
				throw new SourceException( "RTL2832 Tuner Controller - couldn't "
					+ "set default sample rate", e );
			}

		}
		catch( UsbException e )
		{
			e.printStackTrace();
			throw new SourceException( "E4K Tuner Controller - error during "
					+ "init()", e );
		}
	}
	
	public JPanel getEditor( SettingsManager settingsManager )
	{
		if( mEditor == null )
		{
			mEditor = new E4KTunerEditorPanel( this, settingsManager );
		}
		
		return mEditor;
	}
	
	/**
	 * Sets the IF filters (mixer, channel and RC) to the correct filter setting
	 * for the selected bandwidth/sample rate
	 */
	@Override
    public void setSampleRateFilters( int bandwidth ) throws SourceException
    {
		/* Determine repeater state so we can restore it when done */
		boolean i2CRepeaterEnabled = isI2CRepeaterEnabled();

		if( !i2CRepeaterEnabled )
		{
			enableI2CRepeater( mDeviceHandle, true );
		}

		boolean controlI2CRepeater = false;
		
		MixerFilter mixer = MixerFilter.getFilter( bandwidth );
		setMixerFilter( mixer, controlI2CRepeater );

		ChannelFilter channel = ChannelFilter.getFilter( bandwidth );
		setChannelFilter( channel, controlI2CRepeater );

		RCFilter rc = RCFilter.getFilter( bandwidth ); 
		setRCFilter( rc, controlI2CRepeater );

		if( !i2CRepeaterEnabled )
		{
			enableI2CRepeater( mDeviceHandle, false );
		}
    }

	@Override
    public long getTunedFrequency() throws SourceException
    {
		try
		{
			/* Determine repeater state so we can restore it when done */
			boolean i2CRepeaterEnabled = isI2CRepeaterEnabled();

			if( !i2CRepeaterEnabled )
			{
				enableI2CRepeater( mDeviceHandle, true );
			}

			boolean controlI2CRepeater = false;
			
			byte z = (byte)readE4KRegister( Register.SYNTH3, controlI2CRepeater );
			
			int xHigh = readE4KRegister( Register.SYNTH4, controlI2CRepeater );
			int xLow = readE4KRegister( Register.SYNTH5, controlI2CRepeater );

			int x = ( Integer.rotateLeft( xHigh, 8 ) ) | xLow;

			int pllSetting = readE4KRegister( Register.SYNTH7, controlI2CRepeater );
			PLL pll = PLL.fromSetting( pllSetting );
			
			/* Return the repeater to its previous state */
			if( !i2CRepeaterEnabled )
			{
				enableI2CRepeater( mDeviceHandle, false );
			}

			return calculateActualFrequency( pll, z, x );
		}
		catch( LibUsbException e )
		{
			throw new SourceException( "E4K tuner controller - couldn't get "
					+ "tuned frequency", e );
		}
    }

	@Override
    public void setTunedFrequency( long frequency ) throws SourceException
    {
		/* Get the phase locked loop setting */ 
		PLL pll = PLL.fromFrequency( frequency );

		/* Z is an integer representing the number of scaled oscillator frequency 
		 * increments the multiplied frequency. */
		byte z = (byte)( frequency / pll.getScaledOscillator() );
		
		/* remainder is just as it describes.  It is what is left over after we
		 * carve out scaled oscillator frequency increments (z) from the desired 
		 * frequency. */
		int remainder = (int)( frequency - ( ( z & 0xFF ) * pll.getScaledOscillator() ) );

		/* X is a 16-bit representation of the remainder */
		int x = (int)( (double)remainder / (double)pll.getScaledOscillator() * E4K_PLL_Y );

		/* Calculate the exact (tunable) frequency and apply that to the tuner */
		long actualFrequency = calculateActualFrequency( pll, z, x );
		
		/** 
		 * Hack: if we're trying to set the minimum frequency for the E4K, due
		 * to rounding errors, 52 mhz becomes 51.999993, so we need to adjust
		 * x to get 52.000003 mhz ... otherwise the PLL won't lock on the freq 
		 */
		if( actualFrequency < getMinFrequency() )
		{
			x++;
			actualFrequency = calculateActualFrequency( pll, z, x );
		}
		
		/* Apply the actual frequency */
		try
		{
			enableI2CRepeater( mDeviceHandle, true );

			boolean controlI2CRepeater = false;
			
			/* Write the PLL setting */
			writeE4KRegister( Register.SYNTH7, pll.getIndex(), controlI2CRepeater );
			
			/* Write z (integral) value */
			writeE4KRegister( Register.SYNTH3, z, controlI2CRepeater );
			
			/* Write the x (fractional) value high-order byte to synth4 register */
			writeE4KRegister( Register.SYNTH4, (byte)( x & 0xFF ), controlI2CRepeater );
			
			/* Write the x (fractional) value low-order byte to synth5 register */
			writeE4KRegister( Register.SYNTH5, 
							  (byte)( ( Integer.rotateRight( x, 8 ) ) & 0xFF ),
							  controlI2CRepeater );

			/* Set the band for the new frequency */
			setBand( actualFrequency, controlI2CRepeater );

			/* Set the filter */
			setRFFilter( actualFrequency, controlI2CRepeater );
			
			/* Check for PLL lock */
			int lock = readE4KRegister( Register.SYNTH1, controlI2CRepeater );
			
			if( !( ( lock & 0x1 ) == 0x1 ) )
			{
				throw new SourceException( "E4K tuner controller - couldn't "
						+ "achieve PLL lock for frequency [" + 
						actualFrequency + "] lock value [" + lock + "]" );
			}
			
			enableI2CRepeater( mDeviceHandle, false );
		}
		catch( UsbException e )
		{
			throw new SourceException( "E4K tuner controller - error tuning "
					+ "frequency [" + frequency + "]", e );
		}
    }
	
	private long calculateActualFrequency( PLL pll, byte z, int x )
	{
		long whole = pll.getScaledOscillator() * ( z & 0xFF );

		int fractional = (int)( pll.getScaledOscillator() * 
								( (double)x / (double)E4K_PLL_Y ) );

		return whole + fractional;
	}

	public void initTuner( boolean controlI2CRepeater ) throws UsbException
	{
		if( controlI2CRepeater )
		{
			enableI2CRepeater( mDeviceHandle, true );
		}
		
		boolean i2CRepeaterControl = false;
		
		/* Perform dummy read */
		readE4KRegister( Register.DUMMY, i2CRepeaterControl );

		/* Reset everything and clear POR indicator */
		/* NOTE: register value remains 0010 even after we write 0111 to it */
		writeE4KRegister( Register.MASTER1, (byte)( MASTER1_RESET | 
													MASTER1_NORM_STBY | 
													MASTER1_POR_DET ), 
													i2CRepeaterControl );

		/* Configure clock input */
		writeE4KRegister( Register.CLK_INP, (byte)0x00, i2CRepeaterControl );
		
		/* Disable clock output */
		writeE4KRegister( Register.REF_CLK, (byte)0x00, i2CRepeaterControl );
		writeE4KRegister( Register.CLKOUT_PWDN, (byte)0x96, i2CRepeaterControl );
		
		/* Magic Init */
		magicInit( i2CRepeaterControl );

//		/* Set common mode voltage a bit higher for more margin - 850 mv */
//		writeMaskedE4KRegister( Register.DC7, (byte)0x7, 
//								(byte)0x4, i2CRepeaterControl );
//
//		/* Initialize DC offset lookup tables */
//		generateDCOffsetTables( i2CRepeaterControl );
//		
//		/* Enable time variant DC correction */
//		writeE4KRegister( Register.DCTIME1, (byte)0x01, i2CRepeaterControl );
//		writeE4KRegister( Register.DCTIME2, (byte)0x01, i2CRepeaterControl );
//		
		/* Set LNA Mode */
		writeE4KRegister( Register.AGC4, (byte)0x10, i2CRepeaterControl ); //High Threshold
		writeE4KRegister( Register.AGC5, (byte)0x04, i2CRepeaterControl ); //Low Threshold
		writeE4KRegister( Register.AGC6, (byte)0x1A, i2CRepeaterControl ); //LNA calibrate + loop rate

		//TODO: lna and mixer gain were already set to manual when we generated
		//the DC offset tables.  Check the LNA gain setting after we set the 
		//LNA mode above, to see if the lna or mixer gain settings have changed
		//and if not, remove the next two steps
		
//		/* Set LNA gain to manual using current auto LNA gain setting */
//		LNAGain lnaGain = getLNAGain( i2CRepeaterControl );
//		setLNAGain( lnaGain, i2CRepeaterControl );
//		
//		/* Set Mixer gain to manual using current auto Mixer gain setting */
//		MixerGain mixerGain = getMixerGain( i2CRepeaterControl );
//		setMixerGain( mixerGain, i2CRepeaterControl );
		
		//Temp - manual set lna mode to manual
		writeMaskedE4KRegister( Register.AGC1, 
				AGC1_MOD_MASK, 
				AGCMode.SERIAL.getValue(),
				false );
		//Temp - set mixer gain to manual
		writeMaskedE4KRegister( Register.AGC7, 
				AGC7_MIXER_GAIN_MASK, 
				AGC7_MIX_GAIN_MANUAL, 
				false );
		
		
//		/* Enable enhance gain */
//		setEnhanceGain( EnhanceGain.GAIN_5, i2CRepeaterControl );
//
//		/* Enable automatic IF gain mode switching */
//		writeMaskedE4KRegister( Register.AGC8, 
//								IF_GAIN_MODE_SWITCHING_MASK, 
//								AGC8_SENS_LIN_AUTO,
//								i2CRepeaterControl );

		
		/* Use automatic gain as a default */
		setLNAGain( E4KLNAGain.AUTOMATIC, i2CRepeaterControl );
		setMixerGain( E4KMixerGain.AUTOMATIC, i2CRepeaterControl );
		setEnhanceGain( E4KEnhanceGain.AUTOMATIC, i2CRepeaterControl );
		
		/* Set IF gain stages */
		setIFStage1Gain( IFStage1Gain.GAIN_PLUS6, i2CRepeaterControl );
		setIFStage2Gain( IFStage2Gain.GAIN_PLUS0, i2CRepeaterControl );
		setIFStage3Gain( IFStage3Gain.GAIN_PLUS0, i2CRepeaterControl );
		setIFStage4Gain( IFStage4Gain.GAIN_PLUS0, i2CRepeaterControl );
		setIFStage5Gain( IFStage5Gain.GAIN_PLUS9, i2CRepeaterControl );
		setIFStage6Gain( IFStage6Gain.GAIN_PLUS9, i2CRepeaterControl );
		
		/* Set the most narrow filter we can possible use */
		setMixerFilter( MixerFilter.BW_1M9, i2CRepeaterControl );
		setRCFilter( RCFilter.BW_1M0, i2CRepeaterControl );
		setChannelFilter( ChannelFilter.BW_2M15, i2CRepeaterControl );

		setChannelFilterEnabled( true, i2CRepeaterControl );

//		/* Disable DC detector */
//		setDCRangeDetectorEnabled( false, i2CRepeaterControl );

		/* Disable time variant DC correction */
		writeMaskedE4KRegister( Register.DC5, (byte)0x03, (byte)0, i2CRepeaterControl );
		writeMaskedE4KRegister( Register.DCTIME1, (byte)0x03, (byte)0, i2CRepeaterControl );
		writeMaskedE4KRegister( Register.DCTIME2, (byte)0x03, (byte)0, i2CRepeaterControl );
		
		if( controlI2CRepeater )
		{
			enableI2CRepeater( mDeviceHandle, false );
		}
	}
	
	@SuppressWarnings("unused")
	private void generateDCOffsetTables( boolean controlI2CRepeater ) 
							throws UsbException
	{
		boolean i2CRepeaterControl = false;
		
		if( controlI2CRepeater )
		{
			enableI2CRepeater( mDeviceHandle, true );
		}
		
		/* Capture current gain settings to reapply at the end */
		IFStage1Gain stage1 = getIFStage1Gain( i2CRepeaterControl );
		IFStage2Gain stage2 = getIFStage2Gain( i2CRepeaterControl );
		IFStage3Gain stage3 = getIFStage3Gain( i2CRepeaterControl );
		IFStage4Gain stage4 = getIFStage4Gain( i2CRepeaterControl );
		IFStage5Gain stage5 = getIFStage5Gain( i2CRepeaterControl );
		IFStage6Gain stage6 = getIFStage6Gain( i2CRepeaterControl );

		/* Set Mixer gain to manual using current auto Mixer gain setting */
		E4KMixerGain mixerGain = getMixerGain( i2CRepeaterControl );
		setMixerGain( mixerGain, i2CRepeaterControl );
		
		/* Set LNA gain to manual using current auto LNA gain setting */
		E4KLNAGain lnaGain = getLNAGain( i2CRepeaterControl );
		setLNAGain( lnaGain, i2CRepeaterControl );
		
		/* Set IF Stage 2 - 6 gains to maximum */
		setIFStage2Gain( IFStage2Gain.GAIN_PLUS9, i2CRepeaterControl );
		setIFStage3Gain( IFStage3Gain.GAIN_PLUS9, i2CRepeaterControl );
		setIFStage4Gain( IFStage4Gain.GAIN_PLUS2B, i2CRepeaterControl );
		setIFStage5Gain( IFStage5Gain.GAIN_PLUS15D, i2CRepeaterControl );
		setIFStage6Gain( IFStage6Gain.GAIN_PLUS15D, i2CRepeaterControl );
		
		/**
		 * Iterate all DC gain combinations, apply gain settings, read the 
		 * DC offset value and apply it to the lookup table entry
		 */
		for( DCGainCombination combo: DCGainCombination.values() )
		{
			/* Set mixer and stage1 gain values */
			setMixerGain( combo.getMixerGain(), i2CRepeaterControl );
			setIFStage1Gain( combo.getIFStage1Gain(), i2CRepeaterControl );

			/* Turn on the DC range detector */
			setDCRangeDetectorEnabled( true, i2CRepeaterControl );
			
			/* Calibrate the DC value */
			writeE4KRegister( Register.DC1, (byte)0x01, i2CRepeaterControl );

			/* Get the I/Q offset and range values */
			byte offsetI = (byte)( readE4KRegister( Register.DC2, 
													i2CRepeaterControl ) & 0x3F );
			byte offsetQ = (byte)( readE4KRegister( Register.DC3, 
													i2CRepeaterControl ) & 0x3F );
			byte range = (byte)readE4KRegister( Register.DC4, 
												i2CRepeaterControl );
			byte rangeI = (byte)( range & 0x3 );
			byte rangeQ = (byte)( ( range >> 4 ) & 0x3 );
			
			/* Write the offset and range values to the lookup table */
			writeE4KRegister( combo.getIRegister(), 
							  (byte)( offsetI | ( rangeI << 6 ) ), 
							  i2CRepeaterControl );

			writeE4KRegister( combo.getQRegister(), 
							  (byte)( offsetQ | ( rangeQ << 6 ) ),
							  i2CRepeaterControl );
		}
		
		if( controlI2CRepeater )
		{
			enableI2CRepeater( mDeviceHandle, false );
		}
	}
	
	public void setDCRangeDetectorEnabled( boolean enabled, 
							boolean controlI2CRepeater ) throws UsbException
	{
		if( enabled )
		{
			writeMaskedE4KRegister( Register.DC5, 
									DC5_RANGE_DETECTOR_ENABLED_MASK, 
									DC5_RANGE_DETECTOR_ENABLED,
									controlI2CRepeater );
		}
		else
		{
			writeMaskedE4KRegister( Register.DC5, 
									DC5_RANGE_DETECTOR_DISABLED_MASK, 
									DC5_RANGE_DETECTOR_DISABLED,
									controlI2CRepeater );
		}
	}

	/**
	 * Sets the LNA gain within the E4K Tuner
	 * 
	 * Note: requires I2C repeater enabled
	 * 
	 * @param gain
	 * @throws UsbException
	 */
    public void setLNAGain( E4KLNAGain gain, boolean controlI2CRepeater ) 
    								throws UsbException
	{
    	if( controlI2CRepeater )
    	{
    		enableI2CRepeater( mDeviceHandle, true );
    	}
    	
    	if( gain == E4KLNAGain.AUTOMATIC )
		{
			writeMaskedE4KRegister( Register.AGC1, 
									AGC1_MOD_MASK, 
									AGCMode.IF_SERIAL_LNA_AUTON.getValue(),
									false );
		}
		else
		{
			writeMaskedE4KRegister( Register.AGC1, 
									AGC1_MOD_MASK, 
									AGCMode.SERIAL.getValue(),
									false );
			

			writeMaskedE4KRegister( E4KLNAGain.getRegister(), 
									E4KLNAGain.getMask(), 
									gain.getValue(),
									false );
		}
    	
    	if( controlI2CRepeater )
    	{
    		enableI2CRepeater( mDeviceHandle, false );
    	}
	}
    
    /**
     * Reads LNA gain from E4K tuner
     * 
     * Note: requires I2C repeater enabled
     */
	public E4KLNAGain getLNAGain( boolean controlI2CRepeater ) throws UsbException
	{
		return E4KLNAGain.fromRegisterValue( 
				readE4KRegister( E4KLNAGain.getRegister(), controlI2CRepeater ) );
	}

	/**
	 * Sets enhanced gain for E4K repeater.
	 * 
	 * Note: requires I2C repeater enabled
	 * 
	 * @param gain
	 * @throws UsbException
	 */
	public void setEnhanceGain( E4KEnhanceGain gain, boolean controlI2CRepeater ) 
								throws UsbException
	{
		writeMaskedE4KRegister( E4KEnhanceGain.getRegister(), 
								E4KEnhanceGain.getMask(), 
								gain.getValue(),
								controlI2CRepeater );
	}

	/**
	 * Gets the enhanced gain setting in the E4K tuner.
	 * 
	 * Note: requires I2C repeater
	 * @return
	 * @throws UsbException
	 */
	public E4KEnhanceGain getEnhanceGain( boolean controlI2CRepeater ) 
								throws UsbException
	{
		return E4KEnhanceGain.fromRegisterValue( 
			readE4KRegister( E4KEnhanceGain.getRegister(), controlI2CRepeater ) );
	}
	
	public void setMixerGain( E4KMixerGain gain, boolean controlI2CRepeater ) 
								throws UsbException
	{
		if( controlI2CRepeater )
		{
			enableI2CRepeater( mDeviceHandle, true );
		}
		
		boolean localI2CRepeaterControl = false;
		
		if( gain == E4KMixerGain.AUTOMATIC )
		{
			writeMaskedE4KRegister( Register.AGC7, 
									AGC7_MIXER_GAIN_MASK, 
									AGC7_MIX_GAIN_AUTO,
									localI2CRepeaterControl );
		}
		else
		{
			writeMaskedE4KRegister( Register.AGC7, 
									AGC7_MIXER_GAIN_MASK, 
									AGC7_MIX_GAIN_MANUAL, 
									localI2CRepeaterControl );

			/* Set the desired manual gain setting */
			writeMaskedE4KRegister( E4KMixerGain.getRegister(), 
					E4KMixerGain.getMask(), 
					gain.getValue(),
					localI2CRepeaterControl );
		}
		
		if( controlI2CRepeater )
		{
			enableI2CRepeater( mDeviceHandle, false );
		}
	}
	
	public E4KMixerGain getMixerGain( boolean controlI2CRepeater ) 
								throws UsbException
	{
		byte autoOrManual = readMaskedE4KRegister( Register.AGC7, 
											  AGC7_MIXER_GAIN_MASK, 
											  controlI2CRepeater );

		if( autoOrManual == AGC7_MIX_GAIN_AUTO )
		{
			return E4KMixerGain.AUTOMATIC;
		}
		else
		{
			int register = readE4KRegister( E4KMixerGain.getRegister(), 
											controlI2CRepeater );
			
			return E4KMixerGain.fromRegisterValue( register ); 
		}
	}
	
    public void setIFStage1Gain( IFStage1Gain gain, 
    							 boolean controlI2CRepeater ) throws UsbException
	{
		writeMaskedE4KRegister( IFStage1Gain.getRegister(), 
								IFStage1Gain.getMask(), 
								gain.getValue(),
								controlI2CRepeater );
	}

	public IFStage1Gain getIFStage1Gain( boolean controlI2CRepeater ) 
								throws UsbException
	{
		return IFStage1Gain.fromRegisterValue( 
			readE4KRegister( IFStage1Gain.getRegister(), controlI2CRepeater ) );
	}
	
    public void setIFStage2Gain( IFStage2Gain gain, 
    							 boolean controlI2CRepeater ) throws UsbException
	{
		writeMaskedE4KRegister( IFStage2Gain.getRegister(), 
								IFStage2Gain.getMask(), 
								gain.getValue(),
								controlI2CRepeater );
	}

	public IFStage2Gain getIFStage2Gain( boolean controlI2CRepeater ) 
							throws UsbException
	{
		return IFStage2Gain.fromRegisterValue( 
			readE4KRegister( IFStage2Gain.getRegister(), controlI2CRepeater ) );
	}
	
    public void setIFStage3Gain( IFStage3Gain gain, 
    							 boolean controlI2CRepeater ) throws UsbException
	{
		writeMaskedE4KRegister( IFStage3Gain.getRegister(), 
								IFStage3Gain.getMask(), 
								gain.getValue(),
								controlI2CRepeater );
	}

	public IFStage3Gain getIFStage3Gain( boolean controlI2CRepeater ) 
							throws UsbException
	{
		return IFStage3Gain.fromRegisterValue( 
			readE4KRegister( IFStage3Gain.getRegister(), controlI2CRepeater ) );
	}
	
    public void setIFStage4Gain( IFStage4Gain gain, 
    							 boolean controlI2CRepeater ) throws UsbException
	{
		writeMaskedE4KRegister( IFStage4Gain.getRegister(), 
								IFStage4Gain.getMask(), 
								gain.getValue(),
								controlI2CRepeater );
	}

	public IFStage4Gain getIFStage4Gain( boolean controlI2CRepeater ) 
								throws UsbException
	{
		return IFStage4Gain.fromRegisterValue( 
			readE4KRegister( IFStage4Gain.getRegister(), controlI2CRepeater ) );
	}
	
    public void setIFStage5Gain( IFStage5Gain gain, 
    							 boolean controlI2CRepeater ) throws UsbException
	{
		writeMaskedE4KRegister( IFStage5Gain.getRegister(), 
								IFStage5Gain.getMask(), 
								gain.getValue(),
								controlI2CRepeater );
	}

	public IFStage5Gain getIFStage5Gain( boolean controlI2CRepeater ) 
								throws UsbException
	{
		return IFStage5Gain.fromRegisterValue( 
			readE4KRegister( IFStage5Gain.getRegister(), controlI2CRepeater ) );
	}
	
    public void setIFStage6Gain( IFStage6Gain gain, 
    							 boolean controlI2CRepeater ) throws UsbException
	{
		writeMaskedE4KRegister( IFStage6Gain.getRegister(), 
								IFStage6Gain.getMask(), 
								gain.getValue(),
								controlI2CRepeater );
	}

	public IFStage6Gain getIFStage6Gain( boolean controlI2CRepeater ) 
									throws UsbException
	{
		return IFStage6Gain.fromRegisterValue( 
			readE4KRegister( IFStage6Gain.getRegister(), controlI2CRepeater ) );
	}
	
    public void setMixerFilter( MixerFilter filter, 
    							boolean controlI2CRepeater ) throws LibUsbException
	{
		writeMaskedE4KRegister( MixerFilter.getRegister(), 
								MixerFilter.getMask(), 
								filter.getValue(),
								controlI2CRepeater );
	}

    public MixerFilter getMixerFilter( boolean controlI2CRepeater ) 
    								throws UsbException
	{
		int value = readE4KRegister( MixerFilter.getRegister(), 
									 controlI2CRepeater );

		return MixerFilter.fromRegisterValue( value );
	}
	
    public void setRCFilter( RCFilter filter, 
    						 boolean controlI2CRepeater ) throws LibUsbException
	{
		writeMaskedE4KRegister( RCFilter.getRegister(), 
								RCFilter.getMask(), 
								filter.getValue(),
								controlI2CRepeater );
	}

    public RCFilter getRCFilter( boolean controlI2CRepeater ) throws UsbException
	{
		int value = readE4KRegister( RCFilter.getRegister(), controlI2CRepeater );

		return RCFilter.fromRegisterValue( value );
	}
	
    public void setChannelFilter( ChannelFilter filter, 
    							  boolean controlI2CRepeater ) throws LibUsbException
	{
		writeMaskedE4KRegister( ChannelFilter.getRegister(), 
								ChannelFilter.getMask(), 
								filter.getValue(),
								controlI2CRepeater );
	}

    public ChannelFilter getChannelFilter( boolean controlI2CRepeater ) 
    							throws UsbException
	{
		int value = readE4KRegister( ChannelFilter.getRegister(), controlI2CRepeater );

		return ChannelFilter.fromRegisterValue( value );

	}
    
    public void setChannelFilterEnabled( boolean enabled, 
    									 boolean controlI2CRepeater ) throws UsbException
    {
    	if( enabled )
    	{
    		writeMaskedE4KRegister( Register.FILT3, 
    								FILT3_MASK, 
    								FILT3_ENABLE, 
    								controlI2CRepeater );
    	}
    	else
    	{
    		writeMaskedE4KRegister( Register.FILT3, 
    								FILT3_MASK, 
    								FILT3_DISABLE, 
    								controlI2CRepeater );
    	}
    }
    
    public void setBand( long frequency, 
    					 boolean controlI2CRepeater ) throws UsbException
    {
    	if( controlI2CRepeater )
    	{
    		enableI2CRepeater( mDeviceHandle, true );
    	}

    	Band band = Band.fromFrequency( frequency );

    	/* Set the bias */
    	switch( band )
    	{
			case UHF:
			case VHF2:
			case VHF3:
				writeE4KRegister( Register.BIAS, (byte)0x3, false );
				break;
			case L:
			default:
				writeE4KRegister( Register.BIAS, (byte)0x0, false );
				break;
    		
    	}
    	
    	/**
    	 * Workaround - reset register (set value to 0) before writing to it, to 
    	 * avoid a gap around 325-350 MHz
    	 */
    	writeMaskedE4KRegister( Register.SYNTH1, Band.getMask(), (byte)0x0, false );
    	writeMaskedE4KRegister( Register.SYNTH1, Band.getMask(), band.getValue(), false );

    	if( controlI2CRepeater )
    	{
    		enableI2CRepeater( mDeviceHandle, false );
    	}
    }
    
    private void setRFFilter( long frequency, 
    						  boolean controlI2CRepeater ) throws UsbException
    {
    	RFFilter filter = RFFilter.fromFrequency( frequency );

    	writeMaskedE4KRegister( Register.FILT1, 
    							RFFilter.getMask(), 
    							filter.getValue(), 
    							controlI2CRepeater );
    }
    
    /**
     * Sets the gain combination setting.
     * 
     * Note: does not respond to the MANUAL setting.  Use the manual setting
     * to configure gui components to expose the individual Mixer, LNA and 
     * Enhance gain settings, to allow the user to change those settings
     * individually.
     * 
     * @param gain - requested gain setting
     * @throws UsbException if there are any errors
     */
    public void setGain( E4KGain gain, boolean controlI2CRepeater ) throws UsbException
    {
		if( gain != E4KGain.MANUAL )
    	{
			if( controlI2CRepeater )
			{
				enableI2CRepeater( mDeviceHandle, true );
			}

			boolean i2CRepeaterControl = false;
			
    		setLNAGain( gain.getLNAGain(), i2CRepeaterControl );
			setMixerGain( gain.getMixerGain(), i2CRepeaterControl );
    		setEnhanceGain( gain.getEnhanceGain(), i2CRepeaterControl );

    		if( controlI2CRepeater )
    		{
    			enableI2CRepeater( mDeviceHandle, false );
    		}
    	}
    }
	
	/*
	 * Performs magic initialization ... and that's all we need to know, I guess
	 */
	private void magicInit( boolean controlI2CRepeater ) throws UsbException
	{
		if( controlI2CRepeater )
		{
			enableI2CRepeater( mDeviceHandle, true );
		}

		writeE4KRegister( Register.MAGIC_1, (byte)0x01, false );
		writeE4KRegister( Register.MAGIC_2, (byte)0xFE, false );
		writeE4KRegister( Register.MAGIC_3, (byte)0x00, false );
		writeE4KRegister( Register.MAGIC_4, (byte)0x50, false ); //Polarity A
		writeE4KRegister( Register.MAGIC_5, (byte)0x20, false );
		writeE4KRegister( Register.MAGIC_6, (byte)0x01, false );
		writeE4KRegister( Register.MAGIC_7, (byte)0x7F, false );
		writeE4KRegister( Register.MAGIC_8, (byte)0x07, false );

		if( controlI2CRepeater )
		{
			enableI2CRepeater( mDeviceHandle, false );
		}
	}
	
	private byte readMaskedE4KRegister( Register register, 
										byte mask, 
										boolean controlI2CRepeater )
					throws UsbException
	{
		int temp = readE4KRegister( register, controlI2CRepeater );
		return (byte)( temp & mask );
	}
	
	private void writeMaskedE4KRegister( Register register, 
										 byte mask, 
										 byte value,
										 boolean controlI2CRepeater ) throws LibUsbException
	{
		int temp = readE4KRegister( register, controlI2CRepeater );

		/* If the register is not set to the masked value, then change it */
		if( (byte)( temp & mask ) != value )
		{
			writeE4KRegister( register, 
							  (byte)( ( temp & ~mask ) | ( value & mask ) ), 
							  controlI2CRepeater );
			
			int temp2 = readE4KRegister( register, controlI2CRepeater );
		}
	}
	
	private int readE4KRegister( Register register, 
			 boolean controlI2CRepeater ) throws LibUsbException
	{
		return readI2CRegister( mDeviceHandle, Register.I2C_REGISTER.getValue(), 
						register.getValue(), controlI2CRepeater );
	}


	private void writeE4KRegister( Register register, 
								   byte value,
								   boolean controlI2CRepeater ) throws LibUsbException
	{
		writeI2CRegister( mDeviceHandle, Register.I2C_REGISTER.getValue(), 
						  register.getValue(), value, controlI2CRepeater );
	}

	public enum Band
	{
		VHF2( 0 ),
		VHF3( 2 ),
		UHF( 4 ),
		L( 6 );
		
		private int mValue;
		
		private Band( int value )
		{
			mValue = value;
		}
		
		public static byte getMask()
		{
			return (byte)0x06;
		}
		
		public byte getValue()
		{
			return (byte)mValue;
		}
		
		public String getLabel()
		{
			return toString();
		}
		
		public static Band fromFrequency( long frequency )
		{
			if( frequency < 140000000 ) //140 MHz
			{
				return VHF2;
			}
			else if( frequency < 350000000 ) //350 MHz
			{
				return VHF3;
			}
			else if( frequency < 1135000000 ) //1135 MHz
			{
				return UHF;
			}
			else
			{
				return L;
			}
		}
	}

	public enum PLL
	{
		PLL_72M4(   0x0F,   72400000, 48,  600000,  true,   "72.4 MHz" ),
		PLL_81M2(   0x0E,   81200000, 40,  720000,  true,   "81.2 MHz" ),
		PLL_108M3(  0x0D,  108300000, 32,  900000,  true,  "108.3 MHz" ),
		PLL_162M5(  0x0C,  162500000, 24, 1200000,  true,  "162.5 MHz" ),
		PLL_216M6(  0x0B,  216600000, 16, 1800000,  true,  "216.6 MHz" ),
		PLL_325M0(  0x0A,  325000000, 12, 2400000,  true,  "325.0 MHz" ),
		PLL_350M0(  0x09,  350000000,  8, 3600000,  true,  "350.0 MHz" ),
		PLL_432M0(  0x03,  432000000,  8, 3600000, false,  "432.0 MHz" ),
		PLL_667M0(  0x02,  667000000,  6, 4800000, false,  "667.0 MHz" ),
		PLL_1200M0( 0x01, 1200000000,  4, 7200000, false, "1200.0 MHz" );
		
		private int mValue;
		private long mFrequency;
		private int mMultiplier;
		private int mScaledOscillator;
		private boolean mRequires3PhaseMixing;
		private String mLabel;
		
		private PLL( int value, 
					 long frequency, 
					 int multiplier, 
					 int scaledOscillator,
					 boolean requires3Phase,
					 String label )
		{
			mValue = value;
			mFrequency = frequency;
			mMultiplier = multiplier;
			mScaledOscillator = scaledOscillator;
			mRequires3PhaseMixing = requires3Phase;
			mLabel = label;
			
		}

		public byte getIndex()
		{
			return (byte)mValue;
		}
		
		public long getFrequency()
		{
			return mFrequency;
		}
		
		public byte getMultiplier()
		{
			return (byte)mMultiplier;
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
		
		/* Returns the PLL setting with the closest frequency that is greater
		 * than the frequency argument */
		public static PLL fromFrequency( long frequency )
		{
			for( PLL pll: values() )
			{
				if( frequency < pll.getFrequency() )
				{
					return pll;
				}
			}
		
			//Default
			return PLL.PLL_72M4;
		}
		
		public static PLL fromSetting( int setting )
		{
			int value = setting & 0xF;
			
			for( PLL pll: values() )
			{
				if( value == pll.getIndex() )
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
	 * 
	 * Note: max frequency overlaps with the next minimum frequency, so valid
	 * frequencies should be less than the maximum frequency defined for the
	 * filter.
	 */
	public enum RFFilter
	{
		NO_FILTER( 0, 0, 0 ),
		
		//UHF band Filters
		M360( 0,  350000000,  370000000 ),
		M380( 1,  370000000,  392500000 ),
		M405( 2,  392500000,  417500000 ),
		M425( 3,  417500000,  437500000 ),
		M450( 4,  437500000,  462500000 ),
		M475( 5,  462500000,  490000000 ),
		M505( 6,  490000000,  522500000 ),
		M540( 7,  522500000,  557500000 ),
		M575( 8,  557500000,  595000000 ),
		M615( 9,  595000000,  642500000 ),
		M670( 10, 642500000,  695000000 ),
		M720( 11, 695000000,  740000000 ),
		M760( 12, 740000000,  800000000 ),
		M840( 13, 800000000,  865000000 ),
		M890( 14, 865000000,  930000000 ),
		M970( 15, 930000000, 1135000000 ),
		
		//L band filters
		M1300(  0, 1135000000, 1310000000 ),
		M1320(  1, 1310000000, 1340000000 ),
		M1360(  2, 1340000000, 1385000000 ),
		M1410(  3, 1385000000, 1427500000 ),
		M1445(  4, 1427500000, 1452500000 ),
		M1460(  5, 1452500000, 1475000000 ),
		M1490(  6, 1475000000, 1510000000 ),
		M1530(  7, 1510000000, 1545000000 ),
		M1560(  8, 1545000000, 1575000000 ),
		M1590(  9, 1575000000, 1615000000 ),
		M1640( 10, 1615000000, 1650000000 ),
		M1660( 11, 1650000000, 1670000000 ),
		M1680( 12, 1670000000, 1690000000 ),
		M1700( 13, 1690000000, 1710000000 ),
		M1720( 14, 1710000000, 1735000000 ),

		/* Note: max frequency is currently limited by the max integer value
		 * and we set the max frequency for this filter to 2147 MHz */
		M1750( 15, 1735000000, 2147000000 );
		
		private int mValue;
		private long mMinFrequency;
		private long mMaxFrequency;
		
		private RFFilter( int value, long minFrequency, long maxFrequency )
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
			return (byte)0xF;  //TODO: make this a constant
		}
		
		public byte getValue()
		{
			return (byte)mValue;
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
		 * Selects the appropriate filter for the frequency
		 * 
		 * @param frequency
		 * @return selected filter
		 * @throws IllegalArgumentException if the frequency is outside the 
		 * max value (2200 MHz)
		 */
		public static RFFilter fromFrequency( long frequency )
		{
			if( frequency < 350000000 )
			{
				return NO_FILTER;
			}
			
			for( RFFilter filter: values() )
			{
				if( filter.getMinimumFrequency() <= frequency &&
					frequency < filter.getMaximumFrequency() )
				{
					return filter;
				}
			}

			throw new IllegalArgumentException( "E4KTunerController - "
				+ "cannot find RF filter for frequency [" + frequency + "]" );
		}
	}
	
	public enum MixerFilter
	{
		BW_27M0( 0x00, 27000000, 4800000, 28800000, "27.0 MHz" ),
		BW_4M6(  0x80,  4600000, 4400000,  4800000,  "4.6 MHz" ),
		BW_4M2(  0x90,  4200000, 4000000,  4400000,  "4.2 MHz" ),
		BW_3M8(  0xA0,  3800000, 3600000,  4000000,  "3.8 MHz" ),
		BW_3M4(  0xB0,  3400000, 3200000,  3600000,  "3.4 MHz" ),
		BW_3M0(  0xC0,  3000000, 2850000,  3200000,  "3.0 MHz" ),
		BW_2M7(  0xD0,  2700000, 2500000,  2850000,  "2.7 MHz" ),
		BW_2M3(  0xE0,  2300000, 2100000,  2500000,  "2.3 MHz" ),
		BW_1M9(  0xF0,  1900000,       0,  2100000,  "1.9 MHz" );
	 	
		private int mValue;
		private int mBandwidth;
		private int mMinBandwidth;
		private int mMaxBandwidth;
		private String mLabel;
		
		private MixerFilter( int value, 
							 int bandwidth, 
							 int minimumBandwidth,
							 int maximumBandwidth,
							 String label )
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
			return (byte)mValue;
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
		 * @param bandwidth - current sample rate
		 * @return - correct filter to use for the bandwidth/sample rate
		 */
		public static MixerFilter getFilter( int bandwidth )
		{
			for( MixerFilter filter: values() )
			{
				if( filter.getMinimumBandwidth() <= bandwidth &&
					bandwidth < filter.getMaximumBandwidth() )
				{
					return filter;
				}
			}

			/* default */
			return MixerFilter.BW_27M0;
		}
		
		public static MixerFilter fromRegisterValue( int registerValue )
		{
			int value = registerValue & getMask();

			for( MixerFilter filter: values() )
			{
				if( value == filter.getValue() )
				{
					return filter;
				}
			}

			throw new IllegalArgumentException( "E4KTunerController - "
					+ "unrecognized mixer filter value [" + value + "]" );
		}
	}
	
	public enum RCFilter
	{
		BW_21M4( 0x00, 21400000, 21200000, 28800000, "21.4 MHz" ),
		BW_21M0( 0x01, 21000000, 19300000, 21200000, "21.0 MHz" ),
		BW_17M6( 0x02, 17600000, 16150000, 19300000, "17.6 MHz" ),
		BW_14M7( 0x03, 14700000, 13550000, 16150000, "14.7 MHz" ),
		BW_12M4( 0x04, 12400000, 11500000, 13550000, "12.4 MHz" ),
		BW_10M6( 0x05, 10600000,  9800000, 11500000, "10.6 MHz" ),
		BW_9M0(  0x06,  9000000,  8350000,  9800000,  "9.0 MHz" ),
		BW_7M7(  0x07,  7700000,  7050000,  8350000,  "7.7 MHz" ),
		BW_6M4(  0x08,  6400000,  5805000,  7050000,  "6.4 MHz" ),
		BW_5M3(  0x09,  5300000,  4850000,  5805000,  "5.3 MHz" ),
		BW_4M4(  0x0A,  4400000,  3900000,  4850000,  "4.4 MHz" ),
		BW_3M4(  0x0B,  3400000,  3000000,  3900000,  "3.4 MHz" ),
		BW_2M6(  0x0C,  2600000,  2200000,  3000000,  "2.6 MHz" ),
		BW_1M8(  0x0D,  1800000,  1500000,  2200000,  "1.8 MHz" ),
		BW_1M2(  0x0E,  1200000,  1100000,  1500000,  "1.2 MHz" ),
		BW_1M0(  0x0F,  1000000,        0,  1100000,  "1.0 MHz" );
		
		private int mValue;
		private int mBandwidth;
		private int mMinBandwidth;
		private int mMaxBandwidth;
		private String mLabel;
		
		private RCFilter( int value, 
						  int bandwidth,
						  int minimumBandwidth,
						  int maximumBandwidth,
						  String label )
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
			return (byte)mValue;
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
		 * @param bandwidth - current sample rate
		 * @return - correct filter to use for the bandwidth/sample rate
		 */
		public static RCFilter getFilter( int bandwidth )
		{
			for( RCFilter filter: values() )
			{
				if( filter.getMinimumBandwidth() <= bandwidth &&
					bandwidth < filter.getMaximumBandwidth() )
				{
					return filter;
				}
			}

			/* default */
			return RCFilter.BW_21M4;
		}
		
		public static RCFilter fromRegisterValue( int registerValue )
		{
			int value = registerValue & getMask();

			for( RCFilter filter: values() )
			{
				if( value == filter.getValue() )
				{
					return filter;
				}
			}

			throw new IllegalArgumentException( "E4KTunerController - "
					+ "unrecognized rc filter value [" + value + "]" );
		}
	}
	
	public enum ChannelFilter
	{
		BW_5M50( 0x00, 5500000, 5400000, 28800000, "5.50 MHz" ),
		BW_5M30( 0x01, 5300000, 5150000,  5400000, "5.30 MHz" ),
		BW_5M00( 0x02, 5000000, 4900000,  5150000, "5.00 MHz" ),
		BW_4M80( 0x03, 4800000, 4700000,  4900000, "4.80 MHz" ),
		BW_4M60( 0x04, 4600000, 4500000,  4700000, "4.60 MHz" ),
		BW_4M40( 0x05, 4400000, 4350000,  4500000, "4.40 MHz" ),
		BW_4M30( 0x06, 4300000, 4200000,  4350000, "4.30 MHz" ),
		BW_4M10( 0x07, 4100000, 4000000,  4200000, "4.10 MHz" ),
		BW_3M90( 0x08, 3900000, 3850000,  4000000, "3.90 MHz" ),
		BW_3M80( 0x09, 3800000, 3750000,  3850000, "3.80 MHz" ),
		BW_3M70( 0x0A, 3700000, 3650000,  3750000, "3.70 MHz" ),
		BW_3M60( 0x0B, 3600000, 3500000,  3650000, "3.60 MHz" ),
		BW_3M40( 0x0C, 3400000, 3350000,  3500000, "3.40 MHz" ),
		BW_3M30( 0x0D, 3300000, 3250000,  3350000, "3.30 MHz" ),
		BW_3M20( 0x0E, 3200000, 3150000,  3250000, "3.20 MHz" ),
		BW_3M10( 0x0F, 3100000, 3050000,  3150000, "3.10 MHz" ),
		BW_3M00( 0x10, 3000000, 2975000,  3050000, "3.00 MHz" ),
		BW_2M95( 0x11, 2950000, 2925000,  2975000, "2.95 MHz" ),
		BW_2M90( 0x12, 2900000, 2850000,  2925000, "2.90 MHz" ),
		BW_2M80( 0x13, 2800000, 2775000,  2850000, "2.80 MHz" ),
		BW_2M75( 0x14, 2750000, 2750000,  2775000, "2.75 MHz" ),
		BW_2M70( 0x15, 2700000, 2650000,  2750000, "2.70 MHz" ),
		BW_2M60( 0x16, 2600000, 2575000,  2650000, "2.60 MHz" ),
		BW_2M55( 0x17, 2550000, 2525000,  2575000, "2.55 MHz" ),
		BW_2M50( 0x18, 2500000, 2475000,  2525000, "2.50 MHz" ),
		BW_2M45( 0x19, 2450000, 2425000,  2475000, "2.45 MHz" ),
		BW_2M40( 0x1A, 2400000, 2350000,  2425000, "2.40 MHz" ),
		BW_2M30( 0x1B, 2300000, 2290000,  2350000, "2.30 MHz" ),
		BW_2M28( 0x1C, 2280000, 2260000,  2290000, "2.28 MHz" ),
		BW_2M24( 0x1D, 2240000, 2220000,  2260000, "2.24 MHz" ),
		BW_2M20( 0x1E, 2200000, 2175000,  2220000, "2.20 MHz" ),
		BW_2M15( 0x1F, 2150000,       0,  2175000, "2.15 MHz" );
		
		private int mValue;
		private int mBandwidth;
		private int mMinBandwidth;
		private int mMaxBandwidth;
		private String mLabel;
		
		private ChannelFilter( int value, 
							   int bandwidth, 
							   int minimumBandwidth,
							   int maximumBandwidth,
							   String label )
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
			return (byte)mValue;
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
		 * @param bandwidth - current sample rate
		 * @return - correct filter to use for the bandwidth/sample rate
		 */
		public static ChannelFilter getFilter( int bandwidth )
		{
			for( ChannelFilter filter: values() )
			{
				if( filter.getMinimumBandwidth() <= bandwidth &&
					bandwidth < filter.getMaximumBandwidth() )
				{
					return filter;
				}
			}

			/* default */
			return ChannelFilter.BW_5M50;
		}
		
		public static ChannelFilter fromRegisterValue( int registerValue )
		{
			int value = registerValue & getMask();

			for( ChannelFilter filter: values() )
			{
				if( value == filter.getValue() )
				{
					return filter;
				}
			}

			throw new IllegalArgumentException( "E4KTunerController - "
					+ "unrecognized channel filter value [" + value + "]" );
		}
	}
	
	public enum E4KLNAGain
	{
		AUTOMATIC(     -1,    "Auto" ),
		GAIN_MINUS_50(  0, "-5.0 db" ),
		GAIN_MINUS_25(  1, "-2.5 db" ),
		GAIN_PLUS_0(    4,  "0.0 db" ),
		GAIN_PLUS_25(   5,  "2.5 db" ),
		GAIN_PLUS_50(   6,  "5.0 db" ),
		GAIN_PLUS_75(   7,  "7.5 db" ),
		GAIN_PLUS_100(  8, "10.0 db" ),
		GAIN_PLUS_125(  9, "12.5 db" ),
		GAIN_PLUS_150( 10, "15.0 db" ),
		GAIN_PLUS_175( 11, "17.5 db" ),
		GAIN_PLUS_200( 12, "20.0 db" ),
		GAIN_PLUS_250( 13, "25.0 db" ),
		GAIN_PLUS_300( 14, "30.0 db" );
		
		private int mValue;
		private String mLabel;
		
		private E4KLNAGain( int value, String label )
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
			return (byte)mValue;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public String toString()
		{
			return mLabel;
		}
		
		public static E4KLNAGain fromRegisterValue( int registerValue )
		{
			int value = registerValue & getMask();

			for( E4KLNAGain setting: values() )
			{
				if( value == setting.getValue() )
				{
					return setting;
				}
			}

			throw new IllegalArgumentException( "E4KTunerController"
					+ " - unrecognized LNA Gain value [" + 
					value + "]" );
		}
	}

	/**
	 * Defines the default gain settings that are exposed to the user.
	 * 
	 * Manual is an override that keeps each of the settings intact and allows
	 * the user to change each of them individually
	 */
	public enum E4KGain
	{
		AUTOMATIC( "Auto", E4KMixerGain.AUTOMATIC, E4KLNAGain.AUTOMATIC,     E4KEnhanceGain.AUTOMATIC ),
		MANUAL(  "Manual", null,                null,                  null ),
		MINUS_10(  "-1.0 db", E4KMixerGain.GAIN_4,    E4KLNAGain.GAIN_MINUS_50, E4KEnhanceGain.AUTOMATIC ),
		PLUS_15(    "1.5 db", E4KMixerGain.GAIN_4,    E4KLNAGain.GAIN_MINUS_25, E4KEnhanceGain.AUTOMATIC ),
		PLUS_40(    "4.0 db", E4KMixerGain.GAIN_4,    E4KLNAGain.GAIN_PLUS_0,   E4KEnhanceGain.AUTOMATIC ),
		PLUS_65(    "6.5 db", E4KMixerGain.GAIN_4,    E4KLNAGain.GAIN_PLUS_25,  E4KEnhanceGain.AUTOMATIC ),
		PLUS_90(    "9.0 db", E4KMixerGain.GAIN_4,    E4KLNAGain.GAIN_PLUS_50,  E4KEnhanceGain.AUTOMATIC ),
		PLUS_115(  "11.5 db", E4KMixerGain.GAIN_4,    E4KLNAGain.GAIN_PLUS_75,  E4KEnhanceGain.AUTOMATIC ),
		PLUS_140(  "14.0 db", E4KMixerGain.GAIN_4,    E4KLNAGain.GAIN_PLUS_100, E4KEnhanceGain.AUTOMATIC ),
		PLUS_165(  "16.5 db", E4KMixerGain.GAIN_4,    E4KLNAGain.GAIN_PLUS_125, E4KEnhanceGain.AUTOMATIC ),
		PLUS_190(  "19.0 db", E4KMixerGain.GAIN_4,    E4KLNAGain.GAIN_PLUS_150, E4KEnhanceGain.AUTOMATIC ),
		PLUS_215(  "21.5 db", E4KMixerGain.GAIN_4,    E4KLNAGain.GAIN_PLUS_175, E4KEnhanceGain.AUTOMATIC ),
		PLUS_240(  "24.0 db", E4KMixerGain.GAIN_4,    E4KLNAGain.GAIN_PLUS_200, E4KEnhanceGain.AUTOMATIC ),
		PLUS_290(  "29.0 db", E4KMixerGain.GAIN_4,    E4KLNAGain.GAIN_PLUS_250, E4KEnhanceGain.AUTOMATIC ),
		PLUS_340(  "34.0 db", E4KMixerGain.GAIN_4,    E4KLNAGain.GAIN_PLUS_300, E4KEnhanceGain.AUTOMATIC ),
		PLUS_420(  "42.0 db", E4KMixerGain.GAIN_12,   E4KLNAGain.GAIN_PLUS_300, E4KEnhanceGain.AUTOMATIC );
		
		private String mLabel;
		private E4KMixerGain mMixerGain;
		private E4KLNAGain mLNAGain;
		private E4KEnhanceGain mEnhanceGain;
		
		private E4KGain( String label, 
					  E4KMixerGain mixerGain, 
					  E4KLNAGain lnaGain, 
					  E4KEnhanceGain enhanceGain )
		{
			mLabel = label;
			mMixerGain = mixerGain;
			mLNAGain = lnaGain;
			mEnhanceGain = enhanceGain;
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
		
		public E4KEnhanceGain getEnhanceGain()
		{
			return mEnhanceGain;
		}
	}
	
	public enum E4KEnhanceGain
	{
		AUTOMATIC( 0, "Auto" ),
		GAIN_1(    1, "10 db" ),
		GAIN_3(    3, "30 db" ),
		GAIN_5(    5, "50 db" ),
		GAIN_7(    7, "70 db" );
		
		private int mValue;
		private String mLabel;
		
		private E4KEnhanceGain( int value, String label )
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
			return (byte)mValue;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public String toString()
		{
			return mLabel;
		}

		public static E4KEnhanceGain fromRegisterValue( int registerValue )
		{
			int value = registerValue & getMask();

			for( E4KEnhanceGain setting: values() )
			{
				if( value == setting.getValue() )
				{
					return setting;
				}
			}

			throw new IllegalArgumentException( "E4KTunerController"
					+ " - unrecognized Enhance Gain value [" + 
					value + "]" );
		}
	}

	public enum DCGainCombination
	{
		LOOKUP_TABLE_0( Register.QLUT0, 
						Register.ILUT0, 
						E4KMixerGain.GAIN_4, 
						IFStage1Gain.GAIN_MINUS3 ),
		LOOKUP_TABLE_1( Register.QLUT1,
						Register.ILUT1,
						E4KMixerGain.GAIN_4,
						IFStage1Gain.GAIN_PLUS6 ),
		LOOKUP_TABLE_2( Register.QLUT2,
						Register.ILUT2,
						E4KMixerGain.GAIN_12,
						IFStage1Gain.GAIN_MINUS3 ),
	    LOOKUP_TABLE_3( Register.QLUT3,
	    				Register.ILUT3,
	    				E4KMixerGain.GAIN_12,
	    				IFStage1Gain.GAIN_PLUS6 );
		
		private Register mQRegister;
		private Register mIRegister;
		private E4KMixerGain mMixerGain;
		private IFStage1Gain mIFStage1Gain;
		
		private DCGainCombination( Register qRegister, 
							Register iRegister,
							E4KMixerGain mixer, 
							IFStage1Gain stage1 )
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
		AUTOMATIC( -1,  "Auto" ),
		GAIN_4(     0,  "4 db" ),
		GAIN_12(    1, "12 db" );
		
		private int mValue;
		private String mLabel;
		
		private E4KMixerGain( int value, String label )
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
			return (byte)mValue;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public String toString()
		{
			return mLabel;
		}
		
		public static E4KMixerGain fromRegisterValue( int registerValue )
		{
			int value = registerValue & getMask();

			for( E4KMixerGain setting: values() )
			{
				if( value == setting.getValue() )
				{
					return setting;
				}
			}

			throw new IllegalArgumentException( "E4KTunerController"
					+ " - unrecognized Mixer Gain value [" + 
					value + "]" );
		}
	}

	public enum IFStage1Gain
	{
		GAIN_MINUS3( 0x0, "-3 db" ),
		GAIN_PLUS6(  0x1,  "6 db" );

		private int mValue;
		private String mLabel;
		
		private IFStage1Gain( int value, String label )
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
			return (byte)0x1;
		}
		
		public byte getValue()
		{
			return (byte)mValue;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public String toString()
		{
			return mLabel;
		}
		
		public static IFStage1Gain fromRegisterValue( int registerValue )
		{
			int value = registerValue & getMask();

			for( IFStage1Gain setting: values() )
			{
				if( value == setting.getValue() )
				{
					return setting;
				}
			}

			throw new IllegalArgumentException( "E4KTunerController"
					+ " - unrecognized IF Gain Stage 1 value [" + 
					value + "]" );
		}
	}

	public enum IFStage2Gain
	{
		GAIN_PLUS0( 0x0, "0 db" ),
		GAIN_PLUS3( 0x2, "3 db" ),
		GAIN_PLUS6( 0x4, "6 db" ),
		GAIN_PLUS9( 0x6, "9 db" );

		private int mValue;
		private String mLabel;
		
		private IFStage2Gain( int value, String label )
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
			return (byte)0x6;
		}
		
		public byte getValue()
		{
			return (byte)mValue;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public String toString()
		{
			return mLabel;
		}

		public static IFStage2Gain fromRegisterValue( int registerValue )
		{
			int value = registerValue & getMask();

			for( IFStage2Gain setting: values() )
			{
				if( value == setting.getValue() )
				{
					return setting;
				}
			}

			throw new IllegalArgumentException( "E4KTunerController"
					+ " - unrecognized IF Gain Stage 2 value [" + 
					value + "]" );
		}
	}

	public enum IFStage3Gain
	{
		GAIN_PLUS0( 0x00, "0 db" ),
		GAIN_PLUS3( 0x08, "3 db" ),
		GAIN_PLUS6( 0x10, "6 db" ),
		GAIN_PLUS9( 0x30, "9 db" );

		private int mValue;
		private String mLabel;
		
		private IFStage3Gain( int value, String label )
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
			return (byte)0x18;
		}
		
		public byte getValue()
		{
			return (byte)mValue;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public String toString()
		{
			return mLabel;
		}

		public static IFStage3Gain fromRegisterValue( int registerValue )
		{
			int value = registerValue & getMask();

			for( IFStage3Gain setting: values() )
			{
				if( value == setting.getValue() )
				{
					return setting;
				}
			}

			throw new IllegalArgumentException( "E4KTunerController"
					+ " - unrecognized IF Gain Stage 3 value [" + 
					value + "]" );
		}
	}

	public enum IFStage4Gain
	{
		GAIN_PLUS0(  0x00, "0 db" ),
		GAIN_PLUS1(  0x20, "1 db" ),
		GAIN_PLUS2A( 0x40, "2 db" ),
		GAIN_PLUS2B( 0x60, "2 db" );

		private int mValue;
		private String mLabel;
		
		private IFStage4Gain( int value, String label )
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
			return (byte)0x60;
		}
		
		public byte getValue()
		{
			return (byte)mValue;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public String toString()
		{
			return mLabel;
		}

		public static IFStage4Gain fromRegisterValue( int registerValue )
		{
			int value = registerValue & getMask();

			for( IFStage4Gain setting: values() )
			{
				if( value == setting.getValue() )
				{
					return setting;
				}
			}

			throw new IllegalArgumentException( "E4KTunerController"
					+ " - unrecognized IF Gain Stage 4 value [" + 
					value + "]" );
		}
	}

	public enum IFStage5Gain
	{
		GAIN_PLUS3(   0x0,  "3 db" ),
		GAIN_PLUS6(   0x1,  "6 db" ),
		GAIN_PLUS9(   0x2,  "9 db" ),
		GAIN_PLUS12(  0x3, "12 db" ),
		GAIN_PLUS15A( 0x4, "15 db" ),
		GAIN_PLUS15B( 0x5, "15 db" ),
		GAIN_PLUS15C( 0x6, "15 db" ),
		GAIN_PLUS15D( 0x7, "15 db" );

		private int mValue;
		private String mLabel;
		
		private IFStage5Gain( int value, String label )
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
			return (byte)0x7;
		}
		
		public byte getValue()
		{
			return (byte)mValue;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public String toString()
		{
			return mLabel;
		}
		
		public static IFStage5Gain fromRegisterValue( int registerValue )
		{
			int value = registerValue & getMask();

			for( IFStage5Gain setting: values() )
			{
				if( value == setting.getValue() )
				{
					return setting;
				}
			}

			throw new IllegalArgumentException( "E4KTunerController"
					+ " - unrecognized IF Gain Stage 5 value [" + 
					value + "]" );
		}
		
	}

	public enum IFStage6Gain
	{
		GAIN_PLUS3(   0x00,  "3 db" ),
		GAIN_PLUS6(   0x08,  "6 db" ),
		GAIN_PLUS9(   0x10,  "9 db" ),
		GAIN_PLUS12(  0x18, "12 db" ),
		GAIN_PLUS15A( 0x20, "15 db" ),
		GAIN_PLUS15B( 0x28, "15 db" ),
		GAIN_PLUS15C( 0x30, "15 db" ),
		GAIN_PLUS15D( 0x38, "15 db" );

		private int mValue;
		private String mLabel;
		
		private IFStage6Gain( int value, String label )
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
			return (byte)0x38;
		}
		
		public byte getValue()
		{
			return (byte)mValue;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public String toString()
		{
			return mLabel;
		}

		public static IFStage6Gain fromRegisterValue( int registerValue )
		{
			int value = registerValue & getMask();

			for( IFStage6Gain setting: values() )
			{
				if( value == setting.getValue() )
				{
					return setting;
				}
			}

			throw new IllegalArgumentException( "E4KTunerController"
					+ " - unrecognized IF Gain Stage 6 value [" + 
					value + "]" );
		}
	}

	public enum IFFilter
	{
		MIX(  "Mix" ),
		CHAN( "Channel" ),
		RC(   "RC" );
		
		private String mLabel;
		
		private IFFilter( String label )
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
		SERIAL(               (byte)0x0 ),
		IF_PWM_LNA_SERIAL(    (byte)0x1 ),
		IF_PWM_LNA_AUTONL(    (byte)0x2 ),
		IF_PWM_LNA_SUPERV(    (byte)0x3 ),
		IF_SERIAL_LNA_PWM(    (byte)0x4 ),
		IF_PWM_LNA_PWM(       (byte)0x5 ),
		IF_DIG_LNA_SERIAL(    (byte)0x6 ),
		IF_DIG_LNA_AUTON(     (byte)0x7 ),
		IF_DIG_LNA_SUPERV(    (byte)0x8 ),
		IF_SERIAL_LNA_AUTON(  (byte)0x9 ),
		IF_SERIAL_LNA_SUPERV( (byte)0xA );
		
		private byte mValue;
		
		private AGCMode( byte value )
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
		DUMMY( 0x00 ),
		MASTER1( 0x00 ),
		MASTER2( 0x01 ),
		MASTER3( 0x02 ),
		MASTER4( 0x03 ),
		MASTER5( 0x04 ),
		CLK_INP( 0x05 ),
		REF_CLK( 0x06 ),
		SYNTH1( 0x07 ),
		SYNTH2( 0x08 ),
		SYNTH3( 0x09 ),
		SYNTH4( 0x0A ),
		SYNTH5( 0x0B ),
		SYNTH6( 0x0C ),
		SYNTH7( 0x0D ),
		SYNTH8( 0x0E ),
		SYNTH9( 0x0F ),
		FILT1( 0x10 ),
		FILT2( 0x11 ),
		FILT3( 0x12 ),
		GAIN1( 0x14 ),
		GAIN2( 0x15 ),
		GAIN3( 0x16 ),
		GAIN4( 0x17 ),
		AGC1( 0x1A ),
		AGC2( 0x1B ),
		AGC3( 0x1C ),
		AGC4( 0x1D ),
		AGC5( 0x1E ),
		AGC6( 0x1F ),
		AGC7( 0x20 ),
		AGC8( 0x21 ),
		AGC11( 0x24 ),
		AGC12( 0x25 ),
		DC1( 0x29 ),
		DC2( 0x2A ),
		DC3( 0x2B ),
		DC4( 0x2C ),
		DC5( 0x2D ),
		DC6( 0x2E ),
		DC7( 0x2F ),
		DC8( 0x30 ),
		QLUT0( 0x50 ),
		QLUT1( 0x51 ),
		QLUT2( 0x52 ),
		QLUT3( 0x53 ),
		ILUT0( 0x60 ),
		ILUT1( 0x61 ),
		ILUT2( 0x62 ),
		ILUT3( 0x63 ),
		DCTIME1( 0x70 ),
		DCTIME2( 0x71 ),
		DCTIME3( 0x72 ),
		DCTIME4( 0x73 ),
		PWM1( 0x74 ),
		PWM2( 0x75 ),
		PWM3( 0x76 ),
		PWM4( 0x77 ),
		BIAS( 0x78 ),
		CLKOUT_PWDN( 0x7A ),
		CHFILT_CALIB( 0x7B ),
		I2C_REG_ADDR( 0x7D ),
		MAGIC_1( 0x7E ),
		MAGIC_2( 0x7F ),
		MAGIC_3( 0x82 ),
		MAGIC_4( 0x86 ),
		MAGIC_5( 0x87 ),
		MAGIC_6( 0x88 ),
		MAGIC_7( 0x9F ),
		MAGIC_8( 0xA0 ),
		I2C_REGISTER( 0xC8 );

		private int mValue;
		
		private Register( int value )
		{
			mValue = value;
		}
		
		public byte getValue()
		{
			return (byte)mValue;
		}
	}
}

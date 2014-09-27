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
package source.tuner.fcd.proV1;

import javax.swing.JPanel;
import javax.usb.UsbClaimException;
import javax.usb.UsbException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.SourceException;
import source.tuner.TunerClass;
import source.tuner.TunerConfiguration;
import source.tuner.TunerType;
import source.tuner.fcd.FCDCommand;
import source.tuner.fcd.FCDTuner;
import source.tuner.fcd.FCDTunerController;
import source.tuner.usb.USBTunerDevice;
import controller.ResourceManager;

public class FCD1TunerController extends FCDTunerController
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( FCD1TunerController.class );

	public static final int sMINIMUM_TUNABLE_FREQUENCY = 64000000;
	public static final int sMAXIMUM_TUNABLE_FREQUENCY = 1700000000;
	public static final int sSAMPLE_RATE = 96000;
	
	private double mDCCorrectionInPhase = 0.0;
	private double mDCCorrectionQuadrature = 0.0;
	private double mPhaseCorrection = 0.0;
	private double mGainCorrection = 0.0;
	private LNAGain mLNAGain;
	private LNAEnhance mLNAEnhance;
	private MixerGain mMixerGain;
	private FCD1TunerConfiguration mTunerConfiguration;
	private FCD1TunerEditorPanel mEditor;

	public FCD1TunerController( USBTunerDevice device ) 
			throws SourceException 
	{
		super( device,
			   sMINIMUM_TUNABLE_FREQUENCY, 
			   sMAXIMUM_TUNABLE_FREQUENCY );
		
		mFrequencyController.setSampleRate( sSAMPLE_RATE );
		
		try
		{
			open();
			
			setFCDMode( Mode.APPLICATION );
			getPhaseAndGainCorrection();
			getDCIQCorrection();
			getLNAGainSetting();
			getLNAEnhanceSetting();
			getMixerGainSetting();
			
			send( FCDCommand.APP_SET_MIXER_GAIN, 1 );
		}
		catch( Exception e )
		{
			e.printStackTrace();
			
			throw new SourceException( "FCDTunerController error " +
					"during construction", e );
		}
	}
	
	public int getCurrentSampleRate()
	{
		return sSAMPLE_RATE;
	}

	@Override
    public TunerClass getTunerClass()
    {
	    return TunerClass.FUNCUBE_DONGLE_PRO;
    }

	@Override
    public TunerType getTunerType()
    {
	    return TunerType.FUNCUBE_DONGLE_PRO;
    }

	/**
	 * Get the tuner configuration
	 * 
	 * @return current config or null if one hasn't been applied yet
	 */
	public FCD1TunerConfiguration getTunerConfiguration()
	{
		return mTunerConfiguration;
	}
	
	@Override
    public void apply( TunerConfiguration tunerConfig )
            throws SourceException
    {
		FCD1TunerConfiguration config = (FCD1TunerConfiguration)tunerConfig;
		
		try
		{
			setFrequencyCorrection( config.getFrequencyCorrection() );
			setLNAGain( config.getLNAGain() );
			setLNAEnhance( config.getLNAEnhance() );
			setMixerGain( config.getMixerGain() );
			setDCCorrectionInPhase( config.getInphaseDCCorrection() );
			setDCCorrectionQuadrature( config.getQuadratureDCCorrection() );
			setGainCorrection( config.getGainCorrection() );
			setPhaseCorrection( config.getPhaseCorrection() );
			
			//If we get to here without exception, store this config
			mTunerConfiguration = config;
		}
		catch( Exception e)
		{
			//Re-cast any usb exceptions as source exceptions
			throw new SourceException( "FCD 1 Controller encountered an "
					+ "exception while applying tuner config - " + 
					e.getLocalizedMessage() );
		}
	}

	@Override
    public JPanel getEditor( FCDTuner tuner, ResourceManager resourceManager )
    {
		if( mEditor == null )
		{
			mEditor = new FCD1TunerEditorPanel( tuner, resourceManager );
		}
		
		return mEditor;
    }

	/**
	 * Gets the current LNA Gain value from the controller and stores it
	 */
	private void getLNAGainSetting() throws UsbClaimException, UsbException
	{
		int gain = (int)send( FCDCommand.APP_GET_LNA_GAIN );

		mLog.info( "FCD1: lna gain setting is:" + gain );
		
		mLNAGain = LNAGain.valueOf( gain );
	}
	
	/**
	 * @return - current lna gain setting
	 */
	public LNAGain getLNAGain()
	{
		return mLNAGain;
	}

	/**
	 * Sets lna gain for the controller
	 * @param gain
	 * @throws UsbException
	 * @throws UsbClaimException
	 */
	public void setLNAGain( LNAGain gain ) 
						throws UsbException, UsbClaimException
	{
		send( FCDCommand.APP_SET_LNA_GAIN, gain.getSetting() );
	}
	
	/**
	 * @return - current phase correction setting
	 */
	public double getPhaseCorrection()
	{
		return mPhaseCorrection;
	}

	/**
	 * Sets the phase correction
	 * @param value - new phase correction
	 * @throws IllegalArgumentException for value outside limits (0.0 <> 1.0)
	 * @throws UsbClaimException - if cannot claim the FCD HID controller
	 * @throws UsbException - if there was a usb error
	 */
	public void setPhaseCorrection( double value ) 
			throws UsbClaimException, UsbException
	{
		if( value < -1.0 || value > 1.0 )
		{
			throw new IllegalArgumentException( "Phase correction value [" + 
					value + "] outside limits ( -1.0 <> 1.0 )" );
		}
		
		mPhaseCorrection = value;
		setPhaseAndGainCorrection();
	}

	/**
	 * @return current gain correction setting
	 */
	public double getGainCorrection()
	{
		return mGainCorrection;
	}

	/**
	 * 
	 * @param value - gain correction value
	 * @throws IllegalArgumentException for value outside limits (0.0 <> 1.0)
	 * @throws UsbClaimException - if cannot claim the FCD HID controller
	 * @throws UsbException - if there was a usb error
	 */
	public void setGainCorrection( double value ) 
					throws UsbClaimException, UsbException
	{
		if( value < -1.0 || value > 1.0 )
		{
			throw new IllegalArgumentException( "Gain correction value [" + 
					value + "] outside limits ( -1.0 <> 1.0 )" );
		}
		
		mGainCorrection = value;
		setPhaseAndGainCorrection();
	}
	
	/**
	 * Sends the phase and gain correction values to FCD.
	 * 
	 * The interface expects an argument with the SIGNED phase value in the first
	 * 16 bits and the UNSIGNED gain value in the second 16 bits, both in
	 * big-endian format.  Since we're sending the argument in java little endian
	 * format, we place the phase in the high-order bits and the gain in the 
	 * low-order bits, so that the send(command) function will reorder the bytes
	 * into big endian format, and place the arguments correctly.
	 * 
	 * Gain correction values range -1.0 to 0.0 to 1.0 and applied to the dongle
	 * as values 0 to 65534
	 * 
	 * Phase correction values range -1.0 to 0.0 to 1.0 and applied to the
	 * dongle as signed values -32768 to 32767
	 */
	private void setPhaseAndGainCorrection() throws UsbClaimException,
													UsbException
	{
		//UNSIGNED short gain value - masked into a long to avoid sign-extension
		long gain = (long)( ( 1.0 + mGainCorrection ) * Short.MAX_VALUE );
		
		//Left shift gain to place value in upper 16 bits
		long longGain = Long.rotateLeft( ( 0xFFFF & gain ), 16 );

		//SIGNED short phase value
		short phase = (short)( mPhaseCorrection * Short.MAX_VALUE );

		//Merge the results
		long correction = longGain | phase;

		send( FCDCommand.APP_SET_IQ_CORRECTION, correction );
	}

	/**
	 * Retrieves the stored phase and gain correction values from the FCD.
	 * 
	 * Note: testing shows that when the dongle is unplugged, any stored phase
	 * and gain correction values are reset.  Reading the dongle after a fresh
	 * plugin shows the values reset.
	 * 
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	private void getPhaseAndGainCorrection() throws UsbClaimException,
													UsbException
	{
		long correction = send( FCDCommand.APP_GET_IQ_CORRECTION );
		
		/**
		 * Gain: mask the upper 16 phase bits and right shift to get the 
		 * unsigned short value and then divide by 32768 to get the double 
		 * value.  We place the unsigned short value in an int to avoid sign
		 * issues.
		 */
		int gain = (int)( Long.rotateRight( correction & 0xFFFF0000, 16 ) );

		mGainCorrection = ( (double)gain / (double)Short.MAX_VALUE ) - 1.0;

		/**
		 * Phase: mask the lower 16 bits and divide by 32768 to get the double
		 * value
		 */
		short phase = (short)( correction & 0x0000FFFF );
		
		mPhaseCorrection = ( (double)phase / (double)Short.MAX_VALUE );
		
		mLog.info( "FCD1: correction:" + correction + " gain:" + gain + " phase:" + phase );
	}
	
	/**
	 * @return current DC correction for the I (inphase) component
	 */
	public double getDCCorrectionInPhase()
	{
		return mDCCorrectionInPhase;
	}

	/**
	 * Sets DC correction for the I (inphase) component and sends the new value
	 * to the FCD controller
	 * @param value - new DC correction value for the inphase component
	 * @throws IllegalArgumentException for values outside limits (-1.0 <> 1.0)
	 * @throws UsbClaimException - if cannot claim the FCD HID controller
	 * @throws UsbException - if there was a usb error
	 */
	public void setDCCorrectionInPhase( double value ) 
			throws UsbClaimException, UsbException
	{
		if( value < -1.0 || value > 1.0 )
		{
			throw new IllegalArgumentException( "DC inphase correction value "
					+ "[" + value + "] outside limits ( 0.0 <> 1.0 )" );
		}
		
		mDCCorrectionInPhase = value;
		setDCIQCorrection();
	}

	/**
	 * @return current DC correction for the Q (quadrature) component
	 */
	public double getDCCorrectionQuadrature()
	{
		return mDCCorrectionQuadrature;
	}

	/**
	 * Sets the DC correction for the Q (quadrature) component and send the
	 * new value to the FCD controller
	 * @param value - new DC correction value for the quadrature component
	 * @throws IllegalArgumentException for values outside limits (-1.0 <> 1.0)
	 * @throws UsbClaimException - if cannot claim the FCD HID controller
	 * @throws UsbException - if there was a usb error
	 */
	public void setDCCorrectionQuadrature( double value ) 
			throws UsbClaimException, UsbException
	{
		if( value < -1.0 || value > 1.0 )
		{
			throw new IllegalArgumentException( "DC quadrature correction "
					+ "value [" + value + "] outside limits ( 0.0 <> 1.0 )" );
		}
		
		mDCCorrectionQuadrature = value;
		setDCIQCorrection();
	}

	/**
	 * Sends the I/Q DC offset correction values to FCD.
	 * 
	 * The interface expects an argument with the signed inphase value in the 
	 * first 16 bits and the signed quadrature value in the second 16 bits, 
	 * both in big-endian format.  
	 * 
	 * Since we're sending the argument in java little endian format, we place 
	 * the quadrature in the high-order bits and the inphase in the low-order 
	 * bits, so that the send(command) function will reorder the bytes into big 
	 * endian format, and place the arguments correctly.
	 * 
	 * Both Inphase and Quadrature correction values range -1.0 to 0.0 to 1.0 
	 * and are applied to the dongle as signed short values -32768 to 0 to 32767
	 */
	private void setDCIQCorrection() throws UsbClaimException, UsbException
	{
		//I & Q DC offset correction values are signed short values
		short inphase = (short)( mDCCorrectionInPhase * Short.MAX_VALUE );
		short quadrature = (short)( mDCCorrectionQuadrature * Short.MAX_VALUE );

		//Mask the shorts into longs to preserve the sign bit and prepare
		//for merging to a single 32-bit value
		long maskedInphase = inphase & 0x0000FFFF;
		long maskedQuadrature = quadrature & 0x0000FFFF;
		
		//Left shift quadrature to place value in upper 16 bits
		long shiftedQuadrature = Long.rotateLeft( maskedQuadrature, 16 );
		
		//Merge the results
		long correction = shiftedQuadrature | maskedInphase;
		
//		Log.info( "FCD1: setting iq correction," +
//				" inphase:" + inphase +
//				" masked inphase:" + maskedInphase + 
//				" quad:" + quadrature + 
//				" masked quad:" + maskedQuadrature + 
//				" shifted quadrature:" + shiftedQuadrature + 
//				" correction:" + correction );

		send( FCDCommand.APP_SET_DC_CORRECTION, correction );
	}
	
	private void getDCIQCorrection() throws UsbClaimException,
													UsbException
	{
		long correction = send( FCDCommand.APP_GET_DC_CORRECTION );

		/**
		 * Quadrature: mask the upper 16 bits and divide by 32768 to get the
		 * stored quadrature value.  Cast the 16-bit value to a short to get 
		 * the signed short value
		 */
		long shiftedQuadrature = correction & 0xFFFF0000;
		long quadrature = Long.rotateRight( shiftedQuadrature, 16 );

		mDCCorrectionQuadrature = ( (short)quadrature / (double)Short.MAX_VALUE );

		/**
		 * InPhase: mask the lower 16 bits to get the short 
		 * value and then divide by 32768 to get the stored inphase value.
		 * Cast the 16-bit value to a short to get the signed short value
		 */
		long inphase = correction & 0x0000FFFF;
		mDCCorrectionInPhase = ( (short)inphase / (double)Short.MAX_VALUE );
		
//		Log.info( "FCD1:" +
//				  " correction:" + correction + 
//				  " Q:" + quadrature + 
//				  " I:" + inphase + 
//				  " QCorr:" + mDCCorrectionQuadrature + 
//				  " ICorr:" + mDCCorrectionInPhase );
	}
	
	private void getLNAEnhanceSetting() throws UsbClaimException, UsbException
	{
		int enhance = (int)send( FCDCommand.APP_GET_LNA_ENHANCE );

		mLog.info( "FCD1: lna enhance setting is: " + enhance );

		mLNAEnhance = LNAEnhance.valueOf( enhance );
	}
	
	public LNAEnhance getLNAEnhance()
	{
		return mLNAEnhance;
	}
	
	public void setLNAEnhance( LNAEnhance enhance ) 
				throws UsbClaimException, UsbException
	{
		send( FCDCommand.APP_SET_LNA_ENHANCE, enhance.getSetting() );
	}
	
	private void getMixerGainSetting() throws UsbClaimException, UsbException
	{
		int gain = (int)send( FCDCommand.APP_GET_MIXER_GAIN );
		
		mLog.info( "FCD1: mixer gain setting is:" + gain );

		mMixerGain = MixerGain.valueOf( gain );
	}
	
	public MixerGain getMixerGain()
	{
		return mMixerGain;
	}
	
	public void setMixerGain( MixerGain gain ) throws UsbClaimException, UsbException
	{
		send( FCDCommand.APP_SET_MIXER_GAIN, gain.getSetting() );
	}
	
	public enum Block 
	{ 
		CELLULAR_BAND_BLOCKED( "Blocked" ),
		NO_BAND_BLOCK( "Unblocked" ),
		UNKNOWN( "Unknown" );
		
		private String mLabel;
		
		private Block( String label )
		{
		    mLabel = label;
		}
		
		public String getLabel()
		{
		    return mLabel;
		}

		public static Block getBlock( String block )
		{
			Block retVal = UNKNOWN;

			if( block.equalsIgnoreCase( "No blk" ) )
			{
				retVal = NO_BAND_BLOCK;
			}
			else if( block.equalsIgnoreCase( "Cell blk" ) )
			{
				retVal = CELLULAR_BAND_BLOCKED;
			}
			
			return retVal;
		}
	}

	/**
	 * LNA Gain values suppported by the FCD Pro 1.0
	 */
    public enum LNAGain 
    {
    	LNA_GAIN_MINUS_5_0( 0, "-5.0 dB" ),
    	LNA_GAIN_MINUS_2_5( 1, "-2.5 dB" ),
    	LNA_GAIN_PLUS_0_0( 4, "0.0 dB" ),
    	LNA_GAIN_PLUS_2_5( 5, "2.5 dB" ),
    	LNA_GAIN_PLUS_5_0( 6, "5.0 dB" ),
    	LNA_GAIN_PLUS_7_5( 7, "7.5 dB" ),
    	LNA_GAIN_PLUS_10_0( 8, "10.0 dB" ),
    	LNA_GAIN_PLUS_12_5( 9, "12.5 dB" ),
    	LNA_GAIN_PLUS_15_0( 10, "15.0 dB" ),
    	LNA_GAIN_PLUS_17_5( 11, "17.5 dB" ),
    	LNA_GAIN_PLUS_20_0( 12, "20.0 dB" ),
    	LNA_GAIN_PLUS_25_0( 13, "25.0 dB" ),
    	LNA_GAIN_PLUS_30_0( 14, "30.0 dB" );
    	
    	private int mSetting;
    	private String mLabel;
    	
    	private LNAGain( int setting, String label )
    	{
    		mSetting = setting;
    		mLabel = label;
    	}
    	
    	public static LNAGain valueOf( int setting )
    	{
    		switch( setting )
    		{
    			case 0:
    				return LNA_GAIN_MINUS_5_0;
    			case 1:
    				return LNA_GAIN_MINUS_2_5;
    			case 4:
    				return LNA_GAIN_PLUS_0_0;
    			case 5:
    				return LNA_GAIN_PLUS_2_5;
    			case 6:
    				return LNA_GAIN_PLUS_5_0;
    			case 7:
    				return LNA_GAIN_PLUS_7_5;
    			case 8:
    				return LNA_GAIN_PLUS_10_0;
    			case 9:
    				return LNA_GAIN_PLUS_12_5;
    			case 10:
    				return LNA_GAIN_PLUS_15_0;
    			case 11:
    				return LNA_GAIN_PLUS_17_5;
    			case 12:
    				return LNA_GAIN_PLUS_20_0;
    			case 13:
    				return LNA_GAIN_PLUS_25_0;
    			case 14:
    				return LNA_GAIN_PLUS_30_0;
				default:
					throw new IllegalArgumentException( "FCD 1.0 Tuner "
							+ "Controller - unrecognized LNA gain setting "
							+ "value [" + setting + "]" );
    		}
    	}
    	
    	public int getSetting()
    	{
    		return mSetting;
    	}
    	
    	public String getLabel()
    	{
    		return mLabel;
    	}
    	
    	public String toString()
    	{
    		return getLabel();
    	}
    }

	/**
	 * LNA Enhance values suppported by the FCD Pro 1.0
	 */
    public enum LNAEnhance 
    {
    	LNA_ENHANCE_OFF( 0, "Off" ),
    	LNA_ENHANCE_0( 1, "0" ),
    	LNA_ENHANCE_1( 3, "1" ),
    	LNA_ENHANCE_2( 5, "2" ),
    	LNA_ENHANCE_3( 7, "3" );
    	
    	private int mSetting;
    	private String mLabel;
    	
    	private LNAEnhance( int setting, String label )
    	{
    		mSetting = setting;
    		mLabel = label;
    	}
    	
    	public static LNAEnhance valueOf( int setting )
    	{
    		switch( setting )
    		{
    			case 0:
    				return LNA_ENHANCE_OFF;
    			case 1:
    				return LNA_ENHANCE_0;
    			case 3:
    				return LNA_ENHANCE_1;
    			case 5:
    				return LNA_ENHANCE_2;
    			case 7:
    				return LNA_ENHANCE_3;
				default:
					throw new IllegalArgumentException( "FCD 1.0 Tuner "
							+ "Controller - unrecognized LNA enhance setting "
							+ "value [" + setting + "]" );
    		}
    	}
    	
    	public int getSetting()
    	{
    		return mSetting;
    	}
    	
    	public String getLabel()
    	{
    		return mLabel;
    	}
    	
    	public String toString()
    	{
    		return getLabel();
    	}
    }

	/**
	 * Mixer Gain values suppported by the FCD Pro 1.0
	 */
    public enum MixerGain 
    {
    	MIXER_GAIN_PLUS_4_0( 0, "4.0 dB" ),
    	MIXER_GAIN_PLUS_12_0( 1, "12.0 dB" );
    	
    	private int mSetting;
    	private String mLabel;
    	
    	private MixerGain( int setting, String label )
    	{
    		mSetting = setting;
    		mLabel = label;
    	}
    	
    	public static MixerGain valueOf( int setting )
    	{
    		switch( setting )
    		{
    			case 0:
    				return MIXER_GAIN_PLUS_4_0;
    			case 1:
    				return MIXER_GAIN_PLUS_12_0;
				default:
					throw new IllegalArgumentException( "FCD 1.0 Tuner "
							+ "Controller - unrecognized mixer gain setting "
							+ "value [" + setting + "]" );
    		}
    	}
    	
    	public int getSetting()
    	{
    		return mSetting;
    	}
    	
    	public String getLabel()
    	{
    		return mLabel;
    	}
    	
    	public String toString()
    	{
    		return getLabel();
    	}
    }
}

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
	public static final int MAX_FREQUENCY = 176600000;
	public static final byte VERSION = (byte)49;
	
	private byte mI2CAddress = (byte)0x34; 

	/* Shadow register is used to keep a cached copy of all registers, so that
	 * we don't have to read a full byte from a register in order to apply a 
	 * masked value and then re-write the full byte.  With the shadow register,
	 * we can apply the masked value to the cached value, and then just write 
	 * the masked byte, skipping the need to read the byte first. */
	private int[] mShadowRegister = 
	    { 0x00, 0x00, 0x00, 0x00, 0x00, 0x83, 0x32, 0x75,
	      0xC0, 0x40, 0xD6, 0x6C, 0xF5, 0x63, 0x75, 0x68,
	      0x6C, 0x83, 0x80, 0x00, 0x0F, 0x00, 0xC0, 0x30,
	      0x48, 0xCC, 0x60, 0x00, 0x54, 0xAE, 0x4A, 0xC0 };
	
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
		//TODO: why is this being forced as an abstract method?
    }
	
	@Override
    public void apply( TunerConfiguration config ) throws SourceException
    {
		Log.error( "********* Apply tuner config not yet implemented - R820T tuner controller" );
    }

	public void init() throws SourceException
	{
	    try
	    {
	        initializeRegisters();
	    }
	    catch( UsbException e )
	    {
	        throw new SourceException( "R820TTunerController - error setting "
                + "registers to initial startup value", e );
	    }
	    
	}
	
	public JPanel getEditor( ResourceManager resourceManager )
	{
		return new R820TTunerEditorPanel( this, resourceManager );
	}
	
	@Override
    public int getTunedFrequency() throws SourceException
    {
		return 0;
    }

	@Override
    public void setTunedFrequency( int frequency ) throws SourceException
    {
    }
	
	public void initTuner( boolean controlI2CRepeater ) throws UsbException
	{
		if( controlI2CRepeater )
		{
			enableI2CRepeater( mUSBDevice, true );
		}
		
		boolean i2CRepeaterControl = false;
		
		if( controlI2CRepeater )
		{
			enableI2CRepeater( mUSBDevice, false );
		}
	}

	/**
	 * Partially implements the r82xx_set_tv_standard() method from librtlsdr.
	 * Sets standard to digital tv to support sdr operations only.
	 */
	private void setTVStandard() throws UsbException
	{
	    enableI2CRepeater( mUSBDevice, true );
	    
	    /* Init Flag & Xtal check Result */
	    writeR820TRegister( Register.XTAL_CHECK, (byte)0x00, false );
	    
	    /* Set version */
        writeR820TRegister( Register.VERSION, VERSION, false );
	    
        /* LT Gain Test */
        writeR820TRegister( Register.LNA_TOP, (byte)0x00, false );

        for( int x = 0; x < 2; x++ )
        {
            /* Set filter cap */
            writeR820TRegister( Register.FILTER_CAPACITOR, (byte)0x6B, false );

            /* Set calibration clock on */
            writeR820TRegister( Register.CALIBRATION_CLOCK, (byte)0x04, false );

            /* XTAL capacitor 0pF for PLL */
            writeR820TRegister( Register.PLL_XTAL_CAPACITOR, (byte)0x00, false );

            setPLL( 5600 * 1000, false );
        }
        
	    enableI2CRepeater( mUSBDevice, false );
	}
	
	private void setPLL( int frequency, boolean controlI2C ) throws UsbException
	{
	    /* Set reference divider to 0 */
        writeR820TRegister( Register.PLL_XTAL_CAPACITOR, (byte)0x00, false );

        /* Set PLL autotune to 128kHz */
        writeR820TRegister( Register.PLL_AUTOTUNE, (byte)0x00, false );

        /* Set VCO current to 100 */
        writeR820TRegister( Register.VCO_CURRENT, (byte)0x80, false );

        /* Calculate divider */
        int mix_div =2;
        int div_buf = 0;
        int div_num = 0;
        int vco_min = 1770000;
        int vco_max = vco_min * 2;
        
        int freq_khz = (int)( ( frequency + 500 ) / 1000 );
        
        while( mix_div <= 64 )
        {
            int value = freq_khz * mix_div;
            
            if( vco_min <= value && value < vco_max )
            {
                div_buf = mix_div;
                
                while( div_buf > 2 )
                {
                    div_buf = div_buf >> 1;
                    div_num++;
                }
                break;
                
            }
            
            mix_div = mix_div << 1;
        }
	}
	
	/**
	 * Writes initial starting value of registers 0x05 through 0x1F using the 
	 * default value initialized in the shadow register array.  This method only
	 * needs to be called once, upon initialization.
	 * @throws UsbException
	 */
	private void initializeRegisters() throws UsbException
	{
        enableI2CRepeater( mUSBDevice, true );
	    
	    for( int x = 5; x < mShadowRegister.length; x++ )
	    {
	        writeI2CRegister( mUSBDevice, 
	                          mI2CAddress,
	                          (byte)x,
	                          (byte)mShadowRegister[ x ],
	                          false );
	    }

	    enableI2CRepeater( mUSBDevice, false );
	}
	
	private void readStatusRegisters( boolean controlI2C ) throws UsbException
	{
        /* Set the I2C bus to register 0 to read 5 bytes */
	    writeI2CRegister( mUSBDevice, mI2CAddress, (byte)0, (byte)5, controlI2C );

	    byte[] data = new byte[ 5 ];
	    
	    byte[] status = read( mUSBDevice, Block.IIC, (byte)0, data );

	    for( int x = 0; x < 5; x++ )
	    {
	        mShadowRegister[ x ] = status[ x ];
	    }
	}
	
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
	}
	
	public int readR820TRegister( Register register, boolean controlI2C )
	                    throws UsbException
	{
	    return readI2CRegister( mUSBDevice, 
	                            mI2CAddress, 
	                            (byte)register.getRegister(), 
	                            controlI2C );
	}
	
	public enum VGAGain
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
	    
	    private VGAGain( String label, int setting )
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

    public enum LNAGain
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
        
        private LNAGain( String label, int setting )
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

    public enum MixerGain
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
        
        private MixerGain( String label, int setting )
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
    
    public enum Gain
    {
        AUTOMATIC( "Automatic", VGAGain.GAIN_312, LNAGain.AUTOMATIC, MixerGain.AUTOMATIC ),
        MANUAL( "Manual", VGAGain.GAIN_210, LNAGain.GAIN_112, MixerGain.GAIN_105 ),
        GAIN_0( "0", VGAGain.GAIN_210, LNAGain.GAIN_0, MixerGain.GAIN_0 ),
        GAIN_9( "9", VGAGain.GAIN_210, LNAGain.GAIN_9, MixerGain.GAIN_0 ),
        GAIN_14( "14", VGAGain.GAIN_210, LNAGain.GAIN_9, MixerGain.GAIN_5 ),
        GAIN_26( "26", VGAGain.GAIN_210, LNAGain.GAIN_21, MixerGain.GAIN_5 ),
        GAIN_36( "36", VGAGain.GAIN_210, LNAGain.GAIN_21, MixerGain.GAIN_15 ),
        GAIN_76( "76", VGAGain.GAIN_210, LNAGain.GAIN_61, MixerGain.GAIN_15 ),
        GAIN_86( "86", VGAGain.GAIN_210, LNAGain.GAIN_61, MixerGain.GAIN_25 ),
        GAIN_124( "124", VGAGain.GAIN_210, LNAGain.GAIN_99, MixerGain.GAIN_25 ),
        GAIN_143( "143", VGAGain.GAIN_210, LNAGain.GAIN_99, MixerGain.GAIN_44 ),
        GAIN_156( "156", VGAGain.GAIN_210, LNAGain.GAIN_112, MixerGain.GAIN_44 ),
        GAIN_165( "165", VGAGain.GAIN_210, LNAGain.GAIN_112, MixerGain.GAIN_53 ),
        GAIN_196( "196", VGAGain.GAIN_210, LNAGain.GAIN_143, MixerGain.GAIN_53 ),
        GAIN_208( "208", VGAGain.GAIN_210, LNAGain.GAIN_143, MixerGain.GAIN_63 ),
        GAIN_228( "228", VGAGain.GAIN_210, LNAGain.GAIN_165, MixerGain.GAIN_63 ),
        GAIN_253( "253", VGAGain.GAIN_210, LNAGain.GAIN_165, MixerGain.GAIN_88 ),
        GAIN_279( "279", VGAGain.GAIN_210, LNAGain.GAIN_191, MixerGain.GAIN_88 ),
        GAIN_296( "296", VGAGain.GAIN_210, LNAGain.GAIN_191, MixerGain.GAIN_105 ),
        GAIN_327( "327", VGAGain.GAIN_210, LNAGain.GAIN_222, MixerGain.GAIN_105 ),
        GAIN_337( "337", VGAGain.GAIN_210, LNAGain.GAIN_222, MixerGain.GAIN_115 ),
        GAIN_363( "363", VGAGain.GAIN_210, LNAGain.GAIN_248, MixerGain.GAIN_115 ),
        GAIN_371( "371", VGAGain.GAIN_210, LNAGain.GAIN_248, MixerGain.GAIN_123 ),
        GAIN_385( "385", VGAGain.GAIN_210, LNAGain.GAIN_262, MixerGain.GAIN_123 ),
        GAIN_401( "401", VGAGain.GAIN_210, LNAGain.GAIN_262, MixerGain.GAIN_139 ),
        GAIN_420( "420", VGAGain.GAIN_210, LNAGain.GAIN_281, MixerGain.GAIN_139 ),
        GAIN_433( "433", VGAGain.GAIN_210, LNAGain.GAIN_281, MixerGain.GAIN_152 ),
        GAIN_438( "438", VGAGain.GAIN_210, LNAGain.GAIN_286, MixerGain.GAIN_152 ),
        GAIN_444( "444", VGAGain.GAIN_210, LNAGain.GAIN_286, MixerGain.GAIN_158 ),
        GAIN_479( "479", VGAGain.GAIN_210, LNAGain.GAIN_321, MixerGain.GAIN_158 ),
        GAIN_482( "482", VGAGain.GAIN_210, LNAGain.GAIN_321, MixerGain.GAIN_161 ),
        GAIN_495( "495", VGAGain.GAIN_210, LNAGain.GAIN_334, MixerGain.GAIN_161 ); 
        
        private String mLabel;
        private VGAGain mVGAGain;
        private LNAGain mLNAGain;
        private MixerGain mMixerGain;
        
        private Gain( String label, VGAGain vga, LNAGain lna, MixerGain mixer )
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
        
        public VGAGain getVGAGain()
        {
            return mVGAGain;
        }

        public LNAGain getLNAGain()
        {
            return mLNAGain;
        }
        
        public MixerGain getMixerGain()
        {
            return mMixerGain;
        }
    }
    
    
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
        PW_SDM( 0x12, 0x08 ),
        VCO_CURRENT( 0x12, 0xE0 ),
        VERSION( 0x13, 0x3F ),
        UNKNOWN_REGISTER_14( 0x14, 0x0 ),
        SDM_LSB( 0x15, 0x0 ),
        SDM_MSB( 0x16, 0x0 ),
        DRAIN( 0x17, 0x08 ),
        DIVIDER_BUFFER_CURRENT( 0x17, 0x30 ),
        RF_POLY_FILTER_CURRENT( 0x19, 0x60 ),
        PLL_AUTOTUNE( 0x1A, 0x0C ),
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
		BAND_024(  24000000,   49999999, 0x08, 0x02, 0xDF, 0x02, 0x01 ),
		BAND_050(  50000000,   54999999, 0x08, 0x02, 0xBE, 0x02, 0x01 ),
		BAND_055(  55000000,   59999999, 0x08, 0x02, 0x8B, 0x02, 0x01 ),
		BAND_060(  60000000,   64999999, 0x08, 0x02, 0x7B, 0x02, 0x01 ),
		BAND_065(  65000000,   69999999, 0x08, 0x02, 0x69, 0x02, 0x01 ),
		BAND_070(  70000000,   74999999, 0x08, 0x02, 0x58, 0x02, 0x01 ),
		BAND_075(  75000000,   79999999, 0x00, 0x02, 0x44, 0x02, 0x01 ),
		BAND_080(  80000000,   89999999, 0x00, 0x02, 0x44, 0x02, 0x01 ),
		BAND_090(  90000000,   99999999, 0x00, 0x02, 0x34, 0x01, 0x01 ),
		BAND_100( 100000000,  109999999, 0x00, 0x02, 0x34, 0x01, 0x01 ),
		BAND_110( 110000000,  119999999, 0x00, 0x02, 0x24, 0x01, 0x01 ),
		BAND_120( 120000000,  139999999, 0x00, 0x02, 0x24, 0x01, 0x01 ),
		BAND_140( 140000000,  179999999, 0x00, 0x02, 0x14, 0x01, 0x01 ),
		BAND_180( 180000000,  219999999, 0x00, 0x02, 0x13, 0x00, 0x00 ),
		BAND_220( 220000000,  249999999, 0x00, 0x02, 0x13, 0x00, 0x00 ),
		BAND_250( 250000000,  279999999, 0x00, 0x02, 0x11, 0x00, 0x00 ),
		BAND_280( 280000000,  309999999, 0x00, 0x02, 0x00, 0x00, 0x00 ),
		BAND_310( 310000000,  449999999, 0x00, 0x41, 0x00, 0x00, 0x00 ),
		BAND_450( 450000000,  587999999, 0x00, 0x41, 0x00, 0x00, 0x00 ),
		BAND_588( 588000000,  649999999, 0x00, 0x40, 0x00, 0x00, 0x00 ),
		BAND_650( 650000000, 1766000000, 0x00, 0x40, 0x00, 0x00, 0x00 ),
		BAND_UNK( 0, 0, 0, 0, 0, 0, 0 );

		private int mMinFrequency;
		private int mMaxFrequency;
		private int mOpenD;
		private int mRFMuxPloy;
		private int mTF_c;
		private int mXtalCap20p;
		private int mXtalCap10p;
		
		private FrequencyRange( int minFrequency,
								int maxFrequency,
								int openD,
								int rfMuxPloy,
								int tf_c,
								int xtalCap20p,
								int xtalCap10p )
		{
			
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
			
			return BAND_UNK;
		}
		
		public int getMinFrequency()
		{
			return mMinFrequency;
		}
		
		public int getMaxFrequency()
		{
			return mMaxFrequency;
		}
		
		public byte getOpenD()
		{
			return (byte)mOpenD;
		}
		
		public byte getRFMuxPloy()
		{
			return (byte)mRFMuxPloy;
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
		
		public byte getXTALCap0P()
		{
			return (byte)0x0;
		}
	}
	
}

/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 *     
 *     Java version based on librtlsdr
 *     Copyright (C) 2012-2013 by Steve Markgraf <steve@steve-m.de>
 *     Copyright (C) 2012 by Dimitri Stolnikov <horiz0n@gmx.net>
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
package source.tuner.rtl;

import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JPanel;
import javax.usb.UsbClaimException;
import javax.usb.UsbConfiguration;
import javax.usb.UsbControlIrp;
import javax.usb.UsbDevice;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbInterface;
import javax.usb.UsbInterfacePolicy;
import javax.usb.UsbIrp;
import javax.usb.UsbNotActiveException;
import javax.usb.UsbNotOpenException;
import javax.usb.UsbPipe;
import javax.usb.event.UsbPipeDataEvent;
import javax.usb.event.UsbPipeErrorEvent;
import javax.usb.event.UsbPipeListener;
import javax.usb.util.DefaultUsbControlIrp;

import log.Log;

import org.usb4java.LibUsb;

import sample.Listener;
import source.SourceException;
import source.tuner.TunerController;
import source.tuner.TunerType;
import source.tuner.usb.USBTunerDevice;
import util.TimeStamp;
import controller.ResourceManager;

public abstract class RTL2832TunerController extends TunerController
{
	public final static int sINT_NULL_VALUE = -1;
	public final static long sLONG_NULL_VALUE = -1l;
	public final static double sDOUBLE_NULL_VALUE = -1.0D;
	
	public final static byte sUSB_INTERFACE = (byte)0x0;
	public final static byte sUSB_ENDPOINT = (byte)0x81;
	public final static boolean sUSB_FORCE_CLAIM_INTERFACE = true;
	
	public final static byte sREQUEST_TYPE_IN = 
			(byte)( LibUsb.ENDPOINT_IN | LibUsb.REQUEST_TYPE_VENDOR );
	public final static byte sREQUEST_TYPE_OUT = 
			(byte)( LibUsb.ENDPOINT_OUT | LibUsb.REQUEST_TYPE_VENDOR );
	public final static byte sREQUEST_ZERO = (byte)0;
	
	public final static int sUSB_IRP_DATA_SIZE = 65536;
	public final static int sUSB_IRP_POOL_SIZE = 10;
	
	public final static byte EEPROM_ADDRESS = (byte)0xA0;
	
	public final static byte BIT_0 = (byte)0x01;
	public final static byte BIT_1 = (byte)0x02;
	public final static byte BIT_2 = (byte)0x04;
	public final static byte BIT_3 = (byte)0x08;
	public final static byte BIT_4 = (byte)0x10;
	public final static byte BIT_5 = (byte)0x20;
	public final static byte BIT_6 = (byte)0x40;
	public final static byte BIT_7 = (byte)0x80;

	public final static byte E4K_I2C_ADDRESS = (byte)0xC8;
	public final static byte E4K_CHECK_ADDRESS = (byte)0x02;
	public final static byte E4K_CHECK_VALUE = (byte)0x40;

	public final static byte FC0012_I2C_ADDRESS	= (byte)0xC6;
	public final static byte FC0012_CHECK_ADDRESS = (byte)0x00;
	public final static byte FC0012_CHECK_VALUE = (byte)0xA1;

	public final static byte FC0013_I2C_ADDRESS = (byte)0xC6;
	public final static byte FC0013_CHECK_ADDRESS = (byte)0x00;
	public final static byte FC0013_CHECK_VALUE = (byte)0xA3;

	public final static byte FC2580_I2C_ADDRESS = (byte)0xAC;
	public final static byte FC2580_CHECK_ADDRESS = (byte)0x01;
	public final static byte FC2580_CHECK_VALUE = (byte)0x56;

	public final static byte R820T_I2C_ADDRESS = (byte)0x34;
	public final static byte R820T_CHECK_ADDRESS = (byte)0x00;
	public final static byte R820T_CHECK_VALUE = (byte)0x69;

	public final static byte R828D_I2C_ADDRESS = (byte)0x74;
	public final static byte R828D_CHECK_ADDRESS = (byte)0x00;
	public final static byte R828D_CHECK_VALUE = (byte)0x69;

	public static final int sMIN_OSCILLATOR_FREQUENCY = 28799000;
	public static final int sMAX_OSCILLATOR_FREQUENCY = 28801000;

	public final static byte[] sFIR_COEFFICIENTS = 
	{
		(byte)0xCA, (byte)0xDC, (byte)0xD7, (byte)0xD8, (byte)0xE0,
		(byte)0xF2, (byte)0x0E, (byte)0x35, (byte)0x06, (byte)0x50,
		(byte)0x9C, (byte)0x0D, (byte)0x71, (byte)0x11, (byte)0x14,
		(byte)0x71, (byte)0x74, (byte)0x19, (byte)0x41, (byte)0xA5
	};
	
	private static final DecimalFormat mDecimalFormatter = 
						new DecimalFormat( "###,###,###.#" );
	
	private USBTunerDevice mUSBTunerDevice;
	protected UsbDevice mUSBDevice;
	private UsbConfiguration mUSBConfiguration;
	protected UsbInterface mUSBInterface;
	private UsbPipe mUSBPipe;
	private Descriptor mDescriptor;

	public static final int sTWO_TO_22_POWER = 4194304;
	public static final SampleRate sDEFAULT_SAMPLE_RATE = SampleRate.RATE_1_200MHZ;
	
	private SampleRate mSampleRate = sDEFAULT_SAMPLE_RATE;
	
	private BufferProcessor mBufferProcessor = new BufferProcessor();
	
	private RTL2832SampleAdapter mSampleAdapter = new RTL2832SampleAdapter();

	private CopyOnWriteArrayList<Listener<Float[]>> mSampleListeners =
			new CopyOnWriteArrayList<Listener<Float[]>>();
	
	private LinkedTransferQueue<byte[]> mRawSampleBuffer = 
									new LinkedTransferQueue<byte[]>();
	private AtomicInteger mSampleCounter = new AtomicInteger();
	private double mSampleRateAverageSum;
	private int mSampleRateAverageCount;
	protected int mOscillatorFrequency = 28800000; //28.8 MHz
	
	/**
	 * Abstract tuner controller device.  Use the static getTunerClass() method
	 * to determine the tuner type, and construct the corresponding child
	 * tuner controller class for that tuner type.
	 */
	public RTL2832TunerController( USBTunerDevice tunerDevice, 
								   long minTunableFrequency, 
								   long maxTunableFrequency ) throws SourceException
	{
		super( minTunableFrequency, maxTunableFrequency );
		
		mUSBTunerDevice = tunerDevice;
		mUSBDevice = tunerDevice.getDevice();
		mUSBConfiguration = mUSBDevice.getActiveUsbConfiguration();
		mUSBInterface = mUSBConfiguration.getUsbInterface( sUSB_INTERFACE );

		try
		{
			if( claim( mUSBInterface ) )
			{
				/* Get the USB endpoint */
				UsbEndpoint endpoint = mUSBInterface.getUsbEndpoint( sUSB_ENDPOINT );
				mUSBPipe = endpoint.getUsbPipe();
			}
			else
			{
				throw new SourceException( "RTL2832 Tuner Controller - couldn't "
						+ "claim USB interface" );
			}
		}
		catch( UsbException e )
		{
			throw new SourceException( "RTL2832 Tuner Controller - couldn't "
					+ "claim USB interface or get endpoint or pipe", e );
		}
        catch ( UsbDisconnectedException e )
        {
			throw new SourceException( "RTL2832 Tuner Controller - usb device "
					+ "is disconnected", e );
        }
		
	}
	
	public void init() throws SourceException
	{
		try
		{
			setSampleRate( sDEFAULT_SAMPLE_RATE );

			/* Read the contents of the 256-byte EEPROM */
			byte[] eeprom = readEEPROM( mUSBDevice, (short)0, 256 );
			
			/* Create the descriptor with the contents from the EEPROM */
			mDescriptor = new Descriptor( eeprom );
		}
		catch( Exception e )
		{
			throw new SourceException( "RTL2832 Tuner Controller - couldn't "
				+ "set default sample rate or read EEPROM descriptor", e );
		}
	}

	/**
	 * Descriptor contains all identifiers and labels parsed from the EEPROM.
	 * 
	 * May return null if unable to get 256 byte eeprom descriptor from tuner
	 * or if the descriptor doesn't begin with byte values of 0x28 and 0x32 
	 * meaning it is a valid (and can be parsed) RTL2832 descriptor
	 */
	public Descriptor getDescriptor()
	{
		if( mDescriptor != null && mDescriptor.isValid() )
		{
			return mDescriptor;
		}

		return null;
	}
	
//	public void start()
//	{
//		if( !mBufferProcessor.isRunning() )
//		{
//			try
//	        {
//		        resetUSBBuffer();
//
//		        /* Open the USB pipe */
//				openPipe();
//
//				mBufferProcessor.start();
//	        }
//	        catch ( UsbException e )
//	        {
//	        	Log.error( "RTL2832TunerController - error starting sample "
//	        			+ "buffer processor - " + e.getLocalizedMessage() );
//	        }
//		}
//	}
	
	public void setSamplingMode( SampleMode mode ) throws UsbException
	{
		switch( mode )
		{
			case QUADRATURE:
				/* Set intermediate frequency to 0 Hz */
				setIFFrequency( 0 );

				/* Enable I/Q ADC Input */
				writeDemodRegister( mUSBDevice, 
									Page.ZERO, 
									(short)0x08, 
									(short)0xCD, 
									1 );
				
				/* Enable zero-IF mode */
				writeDemodRegister( mUSBDevice, 
									Page.ONE, 
									(short)0xB1, 
									(short)0x1B, 
									1 );
				
				/* Set default i/q path */
				writeDemodRegister( mUSBDevice, 
									Page.ZERO, 
									(short)0x06, 
									(short)0x80, 
									1 );
				break;
			case DIRECT:
				//TODO: add code for direct sampling
				break;
			default:
				break;
			
		}
	}
	
	public void setIFFrequency( int frequency ) throws UsbException
	{
		long ifFrequency = ( (long)sTWO_TO_22_POWER * (long)frequency ) / 
						   (long)mOscillatorFrequency * -1;

		/* Write byte 2 (high) */
		writeDemodRegister( mUSBDevice, 
							Page.ONE, 
							(short)0x19, 
							(short)( Long.rotateRight( ifFrequency, 16 ) & 0x3F ), 
							1 );
		
		/* Write byte 1 (middle) */
		writeDemodRegister( mUSBDevice, 
							Page.ONE, 
							(short)0x1A, 
							(short)( Long.rotateRight( ifFrequency, 8 ) & 0xFF ), 
							1 );
		
		/* Write byte 0 (low) */
		writeDemodRegister( mUSBDevice, 
							Page.ONE, 
							(short)0x1B, 
							(short)( ifFrequency & 0xFF ), 
							1 );
	}

	public abstract void initTuner( boolean controlI2CRepeater ) 
									throws UsbException;

	/**
	 * Provides a unique identifier to use in distinctly identifying this
	 * tuner from among other tuners of the same type, so that we can fetch a
	 * tuner configuration from the settings manager for this specific tuner.
	 * 
	 * @return serial number of the device
	 */
	public String getUniqueID()
	{
		if( mDescriptor != null && mDescriptor.hasSerial() )
		{
			return mDescriptor.getSerial();
		}
		else
		{
			int serial = 
					( 0xFF & mUSBDevice.getUsbDeviceDescriptor().iSerialNumber() );

			return "SER#" + serial;
		}
	}
	
	public abstract JPanel getEditor( ResourceManager resourceManager );
	
	public abstract void setSampleRateFilters( int sampleRate ) throws UsbException;
	
	public abstract TunerType getTunerType();
	
	public static TunerType identifyTunerType( USBTunerDevice tunerDevice )
	{
		TunerType tunerClass = TunerType.UNKNOWN;
		
		try
		{
			UsbDevice device = tunerDevice.getDevice();
			UsbConfiguration config = device.getActiveUsbConfiguration();
			UsbInterface iface = config.getUsbInterface( sUSB_INTERFACE );
			
			if( claim( iface ) )
			{
				/* Perform a dummy write to see if the device needs reset */
				writeRegister( device, Block.USB, Address.USB_SYSCTL.getAddress(), 0x09, 1 );

				/* Initialize the baseband */
				initBaseband( device );

				enableI2CRepeater( device, true );
				
				boolean controlI2CRepeater = false;

				/* Test for each tuner type until we find the correct one */
				if( isE4000Tuner( device, controlI2CRepeater ) )
				{
					tunerClass = TunerType.ELONICS_E4000;
				}
				else if( isFC0013Tuner( device, controlI2CRepeater ) )
				{
					tunerClass =  TunerType.FITIPOWER_FC0013;
				}
				else if( isR820TTuner( device, controlI2CRepeater ) )
				{
					tunerClass =  TunerType.RAFAELMICRO_R820T;
				}
				else if( isR828DTuner( device, controlI2CRepeater ) )
				{
					tunerClass =  TunerType.RAFAELMICRO_R828D;
				}
				else if( isFC2580Tuner( device, controlI2CRepeater ) )
				{
					tunerClass =  TunerType.FCI_FC2580;
				}
				else if( isFC0012Tuner( device, controlI2CRepeater ) )
				{
					tunerClass =  TunerType.FITIPOWER_FC0012;
				}
				
				enableI2CRepeater( device, false );

				iface.release();
			}
		}
		catch( Exception e )
		{
			Log.error( "RTL2832TunerController - error while determining "
					+ "tuner type - " + e.getLocalizedMessage() );
		}
		
		return tunerClass;
	}

	public USBTunerDevice getUSBTunerDevice()
	{
		return mUSBTunerDevice;
	}
	
//	protected void openPipe() throws UsbException, UsbClaimException
//	{
//		/* Get the USB endpoint */
//		UsbEndpoint endpoint = mUSBInterface.getUsbEndpoint( sUSB_ENDPOINT );
//		
//		/* Get the pipe from the endpoint */
//		if( endpoint != null )
//		{
//			mUSBPipe = endpoint.getUsbPipe();
//			
//			if( mUSBPipe != null )
//			{
//				if( !mUSBPipe.isOpen() )
//				{
//					try
//					{
//						resetUSBBuffer();
//
//						mUSBPipe.open();
//					}
//					catch( Exception e )
//					{
//						Log.error( "RTL2832 Tuner Controller - exception while " +
//								"opening the endpoint pipe." + 
//								e.getLocalizedMessage() );
//					}
//				}
//			}
//			else
//			{
//				Log.error( "RTL2832 Tuner Controller - returned endpoint "
//						+ "OUT pipe is null" );
//			}
//		}
//	}
//	
	/**
	 * Claims the USB interface.  If another application currently has
	 * the interface claimed, the sFCD_FORCE_CLAIM_HID_INTERFACE setting
	 * will dictate if the interface is forcibly claimed from the other 
	 * application
	 */
	public static boolean claim( UsbInterface iface ) throws UsbException, 
														   UsbClaimException
	{
		if( !iface.isClaimed() )
		{
			iface.claim( new UsbInterfacePolicy() 
			{
				@Override
                public boolean forceClaim( UsbInterface arg0 )
                {
                    return sUSB_FORCE_CLAIM_INTERFACE;
                }
			} );
			
			return iface.isClaimed();
		}
		else
		{
			Log.error( "RTL2832 Tuner Controller - attempt to claim USB "
					+ "interface failed - in use by another application" );
		}
		
		return false;
	}
	
	/**
	 * Releases the claimed USB interface
	 */
    private void closePipe() throws UsbException
	{
    	mBufferProcessor.stop();
    	
		mUSBPipe.close();
	}

    /**
     * Releases the USB interface
     */
    public void release()
    {
		try
        {
			if( mBufferProcessor.isRunning() )
			{
				mBufferProcessor.stop();
			}
			
			if( mUSBPipe.isOpen() )
			{
				mUSBPipe.close();
			}
			
	        mUSBInterface.release();
        }
        catch ( Exception e )
        {
			Log.error( "RTL2832 Tuner Controller - attempt to release USB "
					+ "interface failed - " + e.getLocalizedMessage() );
        }
    }
	
	public void resetUSBBuffer() throws UsbException
	{
		writeRegister( mUSBDevice, Block.USB, Address.USB_EPA_CTL.getAddress(), 0x1002,  2 );
		writeRegister( mUSBDevice, Block.USB, Address.USB_EPA_CTL.getAddress(), 0x0000,  2 );
	}
	
	public static void initBaseband( UsbDevice device )
		throws IllegalArgumentException, UsbDisconnectedException, UsbException
	{
		/* Initialize USB */
		writeRegister( device, Block.USB, Address.USB_SYSCTL.getAddress(), 0x09, 1 );
		writeRegister( device, Block.USB, Address.USB_EPA_MAXPKT.getAddress(), 0x0002, 2 );
		writeRegister( device, Block.USB, Address.USB_EPA_CTL.getAddress(), 0x1002, 2 );

		/* Power on demod */
		writeRegister( device, Block.SYS, Address.DEMOD_CTL_1.getAddress(), 0x22, 1 );
		writeRegister( device, Block.SYS, Address.DEMOD_CTL.getAddress(), 0xE8, 1 );

		/* Reset demod */
		writeDemodRegister( device, Page.ONE, (short)0x01, 0x14, 1 ); //Bit 3 = soft reset
		writeDemodRegister( device, Page.ONE, (short)0x01, 0x10, 1 );
		
		/* Disable spectrum inversion and adjacent channel rejection */
		writeDemodRegister( device, Page.ONE, (short)0x15, 0x00, 1 );
		writeDemodRegister( device, Page.ONE, (short)0x16, 0x0000, 2 );

		/* Clear DDC shift and IF frequency registers */
		writeDemodRegister( device, Page.ONE, (short)0x16, 0x00, 1 );
		writeDemodRegister( device, Page.ONE, (short)0x17, 0x00, 1 );
		writeDemodRegister( device, Page.ONE, (short)0x18, 0x00, 1 );
		writeDemodRegister( device, Page.ONE, (short)0x19, 0x00, 1 );
		writeDemodRegister( device, Page.ONE, (short)0x1A, 0x00, 1 );
		writeDemodRegister( device, Page.ONE, (short)0x1B, 0x00, 1 );
		
		/* Set FIR coefficients */
		for( int x = 0; x < sFIR_COEFFICIENTS.length; x++ )
		{
			writeDemodRegister( device, 
								Page.ONE, 
								(short)( 0x1C + x ), 
								sFIR_COEFFICIENTS[ x ], 
								1 );
		}
		
		/* Enable SDR mode, disable DAGC (bit 5) */
		writeDemodRegister( device, Page.ZERO, (short)0x19, 0x05, 1 );
		
		/* Init FSM state-holding register */
		writeDemodRegister( device, Page.ONE, (short)0x93, 0xF0, 1 );
		writeDemodRegister( device, Page.ONE, (short)0x94, 0x0F, 1 );
		
		/* Disable AGC (en_dagc, bit 0) (seems to have no effect) */
		writeDemodRegister( device, Page.ONE, (short)0x11, 0x00, 1 );

		/* Disable RF and IF AGC loop */
		writeDemodRegister( device, Page.ONE, (short)0x04, 0x00, 1 );
		
		/* Disable PID filter */
		writeDemodRegister( device, Page.ZERO, (short)0x61, 0x60, 1 );

		/* opt_adc_iq = 0, default ADC_I/ADC_Q datapath */
		writeDemodRegister( device, Page.ZERO, (short)0x06, 0x80, 1 );
		
		/* Enable Zero-if mode (en_bbin bit), 
		 *        DC cancellation (en_dc_est),
		 *        IQ estimation/compensation (en_iq_comp, en_iq_est) */
		writeDemodRegister( device, Page.ONE, (short)0xB1, 0x1B, 1 );

		/* Disable 4.096 MHz clock output on pin TP_CK0 */
		writeDemodRegister( device, Page.ZERO, (short)0x0D, 0x83, 1 );
	}
	
	protected void deinitBaseband( UsbDevice device )
		throws IllegalArgumentException, UsbDisconnectedException, UsbException
	{
//		setI2CRepeater( device, true );

		writeRegister( device, Block.SYS, Address.DEMOD_CTL.getAddress(), 0x20, 1);

//		setI2CRepeater( device, false );
	}

	/**
	 * Sets the General Purpose Input/Output (GPIO) register bit
	 * 
	 * @param device - USB tuner device
	 * @param bitMask - bit mask with one for targeted register bits and zero 
	 *		for the non-targeted register bits
	 * @param enabled - true to set the bit and false to clear the bit
	 * @throws UsbDisconnectedException - if the tuner device is disconnected
	 * @throws UsbException - if there is a USB error while communicating with 
	 *		the device
	 */
	protected static void setGPIOBit( UsbDevice device, byte bitMask, boolean enabled )
			throws UsbDisconnectedException, UsbException
	{
		//Get current register value
		int value = readRegister( device, Block.SYS, Address.GPO.getAddress(), 1 );

		//Update the masked bits
		if( enabled )
		{
			value |= bitMask;
		}
		else
		{
			value &= ~bitMask;
		}

		//Write the change back to the device
		writeRegister( device, Block.SYS, Address.GPO.getAddress(), value, 1 );
	}

	/**
	 * Enables GPIO Output
	 * @param device - usb tuner device
	 * @param bitMask - mask containing one bit value in targeted bit field(s)
	 * @throws UsbDisconnectedException 
	 * @throws UsbException
	 */
	protected static void setGPIOOutput( UsbDevice device, byte bitMask )
			throws UsbDisconnectedException, UsbException
	{
		//Get current register value
		int value = readRegister( device, Block.SYS, Address.GPD.getAddress(), 1 );

		//Mask the value and rewrite it
		writeRegister( device, Block.SYS, Address.GPO.getAddress(), 
							value & ~bitMask, 1 );
		
		//Get current register value
		value = readRegister( device, Block.SYS, Address.GPOE.getAddress(), 1 );

		//Mask the value and rewrite it
		writeRegister( device, Block.SYS, Address.GPOE.getAddress(), 
							value | bitMask, 1 );
	}
	
	protected static void enableI2CRepeater( UsbDevice device, boolean enabled )
		throws IllegalArgumentException, UsbDisconnectedException, UsbException
	{
		Page page = Page.ONE;
		short address = 1;
		int value;
		
		if( enabled )
		{
			value = 0x18; //ON
		}
		else
		{
			value = 0x10; //OFF
		}

		writeDemodRegister( device, page, address, value, 1 );
	}
	
	protected boolean isI2CRepeaterEnabled() throws UsbException
	{
		int register = readDemodRegister( mUSBDevice, Page.ONE, (short)0x1, 1 );
		
		return register == 0x18;
	}
	
	protected static int readI2CRegister( UsbDevice device, 
										  byte i2CAddress, 
										  byte i2CRegister,
										  boolean controlI2CRepeater )
		throws IllegalArgumentException, UsbDisconnectedException, UsbException
	{
		short address = (short)( i2CAddress & 0xFF );

		byte[] writeData = new byte[ 1 ];
		writeData[ 0 ] = i2CRegister;
		
		byte[] readData = new byte[ 1 ];

		if( controlI2CRepeater )
		{
			enableI2CRepeater( device, true );

			write( device, Block.IIC, address, writeData );
			read( device, Block.IIC, address, readData );

			enableI2CRepeater( device, false );
		}
		else
		{
			write( device, Block.IIC, address, writeData );
			read( device, Block.IIC, address, readData );
		}

		return (int)( readData[ 0 ] & 0xFF );
	}
	
	protected void writeI2CRegister( UsbDevice device, 
									 byte i2CAddress,
									 byte i2CRegister, 
									 byte value,
									 boolean controlI2CRepeater ) throws UsbException
	{
		
		short address = (short)( i2CAddress & 0xFF );

		byte[] data = new byte[ 2 ];
		
		data[ 0 ] = i2CRegister;
		data[ 1 ] = value;

		if( controlI2CRepeater )
		{
			enableI2CRepeater( device, true );
			write( mUSBDevice, Block.IIC, address, data );
			enableI2CRepeater( device, false );
		}
		else
		{
			write( mUSBDevice, Block.IIC, address, data );
		}
	}

	protected static void writeDemodRegister( UsbDevice device, Page page, 
									short address, int value, int length )
		throws IllegalArgumentException, UsbDisconnectedException, UsbException
	{
		byte[] data;
		
		if( length == 1 )
		{
			data = new byte[ 1 ];
			data[0] = (byte)( value & 0xFF );
		}
		else if( length == 2 )
		{
			data = new byte[ 2 ];
			data[1] = (byte)( value & 0xFF );          //LSB
			data[0] = (byte)( ( value >> 8 ) & 0xFF ); //MSB
		}
		else
		{
			throw new IllegalArgumentException( "Cannot write value greater "
					+ "than 16 bits to the register - length [" + length + "]" );
		}

		short index = (short)( 0x10 | page.getPage() );
		
		short newAddress = (short)( address << 8 | 0x20 );

		write( device, newAddress, index, data );
		
		readDemodRegister( device, Page.TEN, (short)1, length );
	}

	protected static int readDemodRegister( UsbDevice device, 
											Page page, 
											short address, 
											int length )
		throws IllegalArgumentException, UsbDisconnectedException, UsbException
	{
		short index = page.getPage();
		short newAddress = (short)( ( address << 8 ) | 0x20 );
		byte[] empty = new byte[ 2 ];
		
		byte[] data = read( device, newAddress, index, empty );

		if( length == 2 )
		{
			return (int)( ( data[ 1 ] << 8 | data[ 0 ] ) & 0xFFFF );
		}
		else
		{
			return (int)( data[ 0 ] & 0xFF );
		}
	}
	
	protected static void writeRegister( UsbDevice device, Block block, short address, 
									int value, int length )
		throws IllegalArgumentException, UsbDisconnectedException, UsbException
	{
		byte[] data;
		
		if( length == 1 )
		{
			data = new byte[ 1 ];
			data[0] = (byte)( value & 0xFF );
		}
		else if( length == 2 )
		{
			data = new byte[ 2 ];
			data[1] = (byte)( value & 0xFF );          //LSB
			data[0] = (byte)( ( value >> 8 ) & 0xFF ); //MSB
		}
		else
		{
			throw new IllegalArgumentException( "Cannot write value greater "
				+ "than 16 bits to the register - length [" + length + "]" );
		}

		write( device, block, address, data );
	}

	protected static int readRegister( UsbDevice device, Block block, short address, int length )
		throws IllegalArgumentException, UsbDisconnectedException, UsbException
	{
		byte[] empty = new byte[ 2 ]; 		

		byte[] data = read( device, block, address, empty );
		
		if( length == 2 )
		{
			return (int)( ( data[ 1 ] << 8 | data[ 0 ] ) & 0xFFFF );
		}
		else
		{
			return (int)( data[ 0 ] & 0xFF );
		}
	}

	/**
	 * Write array convenience method.  Allow you to specify the Block that you
	 * are writing to.
	 * 
	 * @param device
	 * @param block
	 * @param address
	 * @param data
	 * @throws IllegalArgumentException
	 * @throws UsbDisconnectedException
	 * @throws UsbException
	 */
	protected static void write( UsbDevice device, Block block, short address, byte[] data )
		throws IllegalArgumentException, UsbDisconnectedException, UsbException
	{
			write( device, address, block.getWriteIndex(), data );
	}
	
	/**
	 * Write byte array
	 * @param device - tuner
	 * @param value - 
	 * @param index - start
	 * @param data - to write
	 * @throws IllegalArgumentException
	 * @throws UsbDisconnectedException
	 * @throws UsbException
	 */
	protected static void write( UsbDevice device, short value, short index, byte[] data )
		throws IllegalArgumentException, UsbDisconnectedException, UsbException
	{
		UsbControlIrp irp = 
				device.createUsbControlIrp( sREQUEST_TYPE_OUT, 
												sREQUEST_ZERO, 
												value,
												index );

		irp.setData( data );
		
		device.syncSubmit( irp );
	}

	/**
	 * Reads byte array from index at the address.
	 * 
	 * @return big-endian byte array (needs to be swapped to be usable)
	 * 
	 */
	protected static byte[] read( UsbDevice device, Block block, short address, byte[] data )
			throws IllegalArgumentException, UsbDisconnectedException, UsbException
	{
		byte[] retVal;
		
		retVal = read( device, address, block.getReadIndex(), data );
		
		return retVal;
	}

	/**
	 * Reads the byte array from the index at the address with the length of the
	 * read determined by the size of the data array
	 * 
	 * @return - big-endian byte array (needs to be swapped to be usable)
	 */
	protected static byte[] read( UsbDevice device, short address, short index, byte[] data )
		throws IllegalArgumentException, UsbDisconnectedException, UsbException
	{
		DefaultUsbControlIrp irp = 
				new DefaultUsbControlIrp( sREQUEST_TYPE_IN, 
										  (byte)0, 
										  address,
										  index );

		irp.setData( data );
		
		device.syncSubmit( irp );
		
		return irp.getData();
	}
	
	protected static boolean isE4000Tuner( UsbDevice device, 
			boolean controlI2CRepeater ) throws UsbClaimException
	{
		try
		{
			int value = readI2CRegister( device, E4K_I2C_ADDRESS, 
					E4K_CHECK_ADDRESS, controlI2CRepeater );
			
			return ( value == E4K_CHECK_VALUE );
		}
		catch( UsbException e )
		{
			//Do nothing ... it's not an E4000
		}
		
		return false;
	}
	
	protected static boolean isFC0012Tuner( UsbDevice device, 
			boolean controlI2CRepeater ) throws UsbClaimException
	{
		try
		{
			/* Initialize the GPIOs */
			setGPIOOutput( device, BIT_5 );

			/* Reset tuner before probing */
			setGPIOBit( device, BIT_5, true );
			setGPIOBit( device, BIT_5, false );
			
			
			int value = readI2CRegister( device, 
										 FC0012_I2C_ADDRESS, 
										 FC0012_CHECK_ADDRESS,
										 controlI2CRepeater );
			
			return ( value == FC0012_CHECK_VALUE );
		}
		catch( UsbException e )
		{
			//Do nothing, it's not an FC0012
		}
		
		return false;
	}
	
	protected static boolean isFC0013Tuner( UsbDevice device, 
											boolean controlI2CRepeater )
									throws UsbClaimException
	{
		try
		{
			int value = readI2CRegister( device, FC0013_I2C_ADDRESS, 
					FC0013_CHECK_ADDRESS, controlI2CRepeater );

			return ( value == FC0013_CHECK_VALUE );
		}
		catch( UsbException e )
		{
			//Do nothing ... it's probably not an FC0013
		}
		
		return false;
	}
	
	/**
	 * Determines if the device's tuner is an FC2580 Tuner.  
	 * 
	 * Note: call initBaseband(device) prior to calling this method.
	 * 
	 * @param device - usb tuner device
	 * @return true if the tuner is an FC2580
	 * @throws UsbException
	 * @throws UsbClaimException
	 */
	protected static boolean isFC2580Tuner( UsbDevice device, 
			boolean controlI2CRepeater ) throws UsbClaimException
	{
		try
		{
			/* Initialize the GPIOs */
			setGPIOOutput( device, BIT_5 );

			/* Reset tuner before probing */
			setGPIOBit( device, BIT_5, true );
			setGPIOBit( device, BIT_5, false );
			

			/* Read the check value */
			int value = readI2CRegister( device, FC2580_I2C_ADDRESS, 
								FC2580_CHECK_ADDRESS, controlI2CRepeater );

			/* Compare masked return value to the known check value */
			return ( ( value & 0x7F ) == FC2580_CHECK_VALUE );
		}
		catch( UsbException e )
		{
			//Do nothing, it's not an FC2580
		}
		
		return false;
	}
	
	protected static boolean isR820TTuner( UsbDevice device, 
			boolean controlI2CRepeater ) throws UsbClaimException
	{
		try
		{
			int value = readI2CRegister( device, R820T_I2C_ADDRESS, 
					R820T_CHECK_ADDRESS, controlI2CRepeater );

			return ( value == R820T_CHECK_VALUE );
		}
		catch( UsbException e )
		{
			//Do nothing, it's not an R820T
		}
		
		return false;
	}
	
	protected static boolean isR828DTuner( UsbDevice device, 
			boolean controlI2CRepeater ) throws UsbClaimException
	{
		try
		{
			int value = readI2CRegister( device, R828D_I2C_ADDRESS, 
					R828D_CHECK_ADDRESS, controlI2CRepeater );

			return ( value == R828D_CHECK_VALUE );
		}
		catch( UsbException e )
		{
			//Do nothing, it's not an R828D
		}
		
		return false;
	}
	
	public int getCurrentSampleRate() throws SourceException 
	{
		return mSampleRate.getRate();
	}
	
    public int getSampleRate() throws SourceException
	{
        try
        {
    		int high= readDemodRegister( mUSBDevice, Page.ONE, (short)0x9F, 2 );
			int low = readDemodRegister( mUSBDevice, Page.ONE, (short)0xA1, 2 );

			int ratio = Integer.rotateLeft( high, 16 ) | low;
			
			int rate = (int)( mOscillatorFrequency * sTWO_TO_22_POWER / ratio );
			
			SampleRate sampleRate = SampleRate.getClosest( rate );

			/* If we're not currently set to this rate, set it as the current rate */
			if( sampleRate.getRate() != rate )
			{
				setSampleRate( sampleRate );
				
				return sampleRate.getRate();
			}
        }
        catch ( Exception e )
        {
        	throw new SourceException( "RTL2832 Tuner Controller - cannot get "
        			+ "current sample rate", e );
        }

        return sDEFAULT_SAMPLE_RATE.getRate();
	}
	
	public void setSampleRate( SampleRate sampleRate ) 
							throws SourceException, UsbException
	{
		/* Write high-order 16-bits of sample rate ratio to demod register */
		writeDemodRegister( mUSBDevice, Page.ONE, (short)0x9F, 
				sampleRate.getRatioHighBits(), 2 );
		
		/* Write low-order 16-bits of sample rate ratio to demod register */
		writeDemodRegister( mUSBDevice, Page.ONE, (short)0xA1, 
				sampleRate.getRatioLowBits(), 2 );
		
		/* Set sample rate correction to 0 */
		setSampleRateFrequencyCorrection( 0 );

		/* Reset the demod for the changes to take effect */
		writeDemodRegister( mUSBDevice, Page.ONE, (short)0x01, 0x14, 1 );
		writeDemodRegister( mUSBDevice, Page.ONE, (short)0x01, 0x10, 1 );

		/* Apply any tuner specific sample rate filter settings */
		setSampleRateFilters( sampleRate.getRate() );

		mSampleRate = sampleRate;

		mFrequencyController.setBandwidth( sampleRate.getRate() );
	}
	
	public void setSampleRateFrequencyCorrection( int ppm ) 
							throws SourceException, UsbException
	{
		int offset = -ppm * sTWO_TO_22_POWER / 1000000;
		
		writeDemodRegister( mUSBDevice, 
							Page.ONE, 
							(short)0x3F, 
							( offset & 0xFF ), 
							1 );
		writeDemodRegister( mUSBDevice, 
							Page.ONE, 
							(short)0x3E, 
							( Integer.rotateRight( offset, 8 ) & 0xFF ), 
							1 );
		/* Test to retune controller to apply frequency correction */
		mFrequencyController.setFrequency( mFrequencyController.getFrequency() );
	}
	
	public int getSampleRateFrequencyCorrection() throws UsbException
	{
		int high = readDemodRegister( mUSBDevice, Page.ONE, (short)0x3E, 1 );
		int low = readDemodRegister( mUSBDevice, Page.ONE, (short)0x3F, 1 );
		
		return ( Integer.rotateLeft( high, 8 ) | low );
	}

	/**
	 * Returns contents of the 256-byte EEPROM.  The contents are as follows:
	 * 
	 * 256-byte EEPROM (in hex):
	 * 00/01 - 2832 Signature
	 * 03/02 - 0BDA Vendor ID
	 * 05/04 - 2832 Product ID
	 * 06 - A5 (has serial id?)
	 * 07 - 16 (bit field - bit 0 = remote wakeup, bit 1 = IR enabled
	 * 08 - 02 or 12
	 * 10/09 0310 ETX(0x03) plus label length (includes length and ETX bytes)
	 * 12/11 First UTF-16 character
	 * 14/13 Second UTF-16 character ...
	 * 
	 * Label 1: vendor
	 * Label 2: product
	 * Label 3: serial
	 * Label 4,5 ... (user defined)
	 */
	public byte[] readEEPROM( UsbDevice device, short offset, int length ) 
		throws IllegalArgumentException, UsbDisconnectedException, UsbException
	{
		if( offset + length > 256 )
		{
			throw new IllegalArgumentException( "RTL2832 Tuner Controller - "
					+ "cannot read more than 256 bytes from EEPROM" );
		}

		byte[] data = new byte[ length ];
		byte[] onebyte = new byte[ 1 ];
		
		/* Tell the RTL-2832 to address the EEPROM */
		writeRegister( device, Block.IIC, EEPROM_ADDRESS, (byte)offset, 1 );

		for( int x = 0; x < length; x++ )
		{
			byte[] temp = read( device, Block.IIC, EEPROM_ADDRESS, onebyte );
			data[ x ] = temp[ 0 ];
		}
		
		return data;
	}

	/**
	 * Writes a single byte to the 256-byte EEPROM using the specified offset.
	 * 
	 * Note: introduce a 5 millisecond delay between each successive write to
	 * the EEPROM or subsequent writes may fail.
	 */
	public void writeEEPROMByte( UsbDevice device, byte offset, byte value ) 
		throws IllegalArgumentException, UsbDisconnectedException, UsbException
	{
		if( offset < 0 || offset > 255 )
		{
			throw new IllegalArgumentException( "RTL2832 Tuner Controller - "
					+ "EEPROM offset must be within range of 0 - 255" );
		}
		
		int offsetAndValue = Integer.rotateLeft( ( 0xFF & offset ), 8 ) | 
								( 0xFF & value );
		
		writeRegister( device, Block.IIC, EEPROM_ADDRESS, offsetAndValue, 2 );
	}
	
	public enum Address
	{
		USB_SYSCTL( 0x2000 ),
		USB_CTRL( 0x2010 ),
		USB_STAT( 0x2014 ),
		USB_EPA_CFG( 0x2144 ),
		USB_EPA_CTL( 0x2148 ),
		USB_EPA_MAXPKT( 0x2158 ),
		USB_EPA_MAXPKT_2( 0x215A ),
		USB_EPA_FIFO_CFG( 0x2160 ),
		DEMOD_CTL( 0x3000 ),
		GPO( 0x3001 ),
		GPI( 0x3002 ),
		GPOE( 0x3003 ),
		GPD( 0x3004 ),
		SYSINTE( 0x3005 ),
		SYSINTS( 0x3006 ),
		GP_CFG0( 0x3007 ),
		GP_CFG1( 0x3008 ),
		SYSINTE_1( 0x3009 ),
		SYSINTS_1( 0x300A ),
		DEMOD_CTL_1( 0x300B ),
		IR_SUSPEND( 0x300C );
		
		private int mAddress;
		
		private Address( int address )
		{
			mAddress = address;
		}
		
		public short getAddress()
		{
			return (short)mAddress;
		}
	}

	public enum Page
	{
		ZERO( 0x0 ),
		ONE( 0x1 ),
		TEN( 0xA );
		
		private int mPage;
		
		private Page( int page )
		{
			mPage = page;
		}
		
		public byte getPage()
		{
			return (byte)( mPage & 0xFF );
		}
	}

	public enum SampleMode
	{
		QUADRATURE, DIRECT;
	}

	public enum Block
	{
		DEMOD( 0 ),
		USB( 1 ),
		SYS( 2 ),
		TUN( 3 ),
		ROM( 4 ),
		IR( 5 ),
		IIC( 6 ); //I2C controller
		
		private int mValue;
		
		private Block( int value )
		{
			mValue = value;
		}
		
		public int getValue()
		{
			return mValue;
		}

		/**
		 * Returns the value left shifted 8 bits
		 */
		public short getReadIndex()
		{
			return (short)Integer.rotateLeft( mValue, 8 );
		}
		
		public short getWriteIndex()
		{
			return (short)( getReadIndex() | 0x10 );
		}
	}

	/**
	 * Sample rates supported by the RTL-2832.
	 * 
	 * Formula to calculate the ratio value:
	 * 
	 * ratio = ( ( crystal_frequency * 2^22 ) / sample_rate ) & ~3
	 * 
	 * Default crystal_frequency is 28,800,000
	 * 
	 * This produces a 32-bit value that has to be set in 2 x 16-bit registers.
	 * Place the high 16-bit value in ratioMSB and the low 16-bit value in 
	 * ratioLSB.  Use integer for these values to avoid sign-extension issues.
	 * 
	 * Mask the value with 0xFFFF when setting the register.
	 */
	public enum SampleRate
	{
		/* Note: sample rates below 1.0MHz are subject to aliasing */
		RATE_0_240MHZ( 0x0DFC, 0x0000,  240000, "0.240 MHz" ),
		RATE_0_288MHZ( 0x08FC, 0x0000,  288000, "0.288 MHz" ),
		RATE_0_912MHZ( 0x07E6, 0x0000,  912000, "0.912 MHz" ),
		RATE_0_960MHZ( 0x077A, 0x0000,  960000, "0.960 MHz" ),
		RATE_1_200MHZ( 0x05F4, 0x0000, 1200000, "1.200 MHz" ),
		RATE_1_440MHZ( 0x0500, 0x0000, 1440000, "1.440 MHz" ),
		RATE_1_680MHZ( 0x044A, 0x0000, 1680000, "1.680 MHz" ),
		RATE_1_824MHZ( 0x03F3, 0x0000, 1824000, "1.824 MHz" ),
		RATE_2_016MHZ( 0x0387, 0x0000, 2016000, "2.016 MHz" );

		/* Note: usb4java library buffer processing scheme doesn't seem capable 
		 * of supporting these rates */
		//RATE_2_400MHZ( 0x0300, 0x0000, 2400000, "2.400 MHz" ),
		//RATE_2_880MHZ( 0x0280, 0x0000, 2880000, "2.880 MHz" ),
		//RATE_3_072MHZ( 0x0258, 0x0000, 3072000, "3.072 MHz" ),
		//RATE_3_216MHZ( 0x023D, 0x0000, 3216000, "3.216 MHz" );
		
		private int mRatioHigh;
		private int mRatioLow;
		private int mRate;
		private String mLabel;
		
		private SampleRate( int ratioHigh, int ratioLow, int rate, String label )
		{
			mRatioHigh = ratioHigh;
			mRatioLow = ratioLow;
			mRate = rate;
			mLabel = label;
		}

		public int getRatioHighBits()
		{
			return mRatioHigh;
		}
		
		public int getRatioLowBits()
		{
			return mRatioLow;
		}
		
		public int getRate()
		{
			return mRate;
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
		 * Returns the sample rate that is equal to the argument or the next
		 * higher sample rate
		 * @param sampleRate
		 * @return
		 */
		public static SampleRate getClosest( int sampleRate )
		{
			for( SampleRate rate: values () )
			{
				if( rate.getRate() >= sampleRate )
				{
					return rate;
				}
			}
			
			return sDEFAULT_SAMPLE_RATE;
		}
	}

	/**
	 * Adds a sample listener.  If the buffer processing thread is
	 * not currently running, starts it running in a new thread.
	 */
    public void addListener( Listener<Float[]> listener )
    {
		mSampleListeners.add( listener );

		mBufferProcessor.start();
    }

	/**
	 * Removes the sample listener.  If this is the last registered listener,
	 * shuts down the buffer processing thread.
	 */
    public void removeListener( Listener<Float[]> listener )
    {
		mSampleListeners.remove( listener );
		
		if( mSampleListeners.isEmpty() )
		{
			mBufferProcessor.stop();
		}
    }

	/**
	 * Dispatches float sample buffers to all registered listeners
	 */
    public void broadcast( Float[] samples )
    {
		Iterator<Listener<Float[]>> it = mSampleListeners.iterator();
		
		while( it.hasNext() )
		{
			Listener<Float[]> next = it.next();
			
			/* if this is the last (or only) listener, send him the original 
			 * buffer, otherwise send him a copy of the buffer */
			if( it.hasNext() )
			{
				next.receive( Arrays.copyOf( samples, samples.length ) );
			}
			else
			{
				next.receive( samples );
			}
		}
    }

	/**
	 * Buffer processing thread.  Fetches samples from the RTL2832 Tuner and 
	 * dispatches them to all registered listeners
	 */
	public class BufferProcessor implements UsbPipeListener
	{
		private ScheduledExecutorService mExecutor = 
							Executors.newScheduledThreadPool( 2 );
		private ScheduledFuture<?> mSampleDispatcherTask;
		private ScheduledFuture<?> mSampleRateCounterTask;
		
		private AtomicBoolean mRunning = new AtomicBoolean();
		private AtomicBoolean mResetting = new AtomicBoolean();
		private Boolean mTransitioning = false;

        public void start()
        {
			synchronized( mTransitioning )
			{
				if( mRunning.compareAndSet( false, true ) )
				{
					Log.debug( "RTL2832TunerController [" + getUniqueID() + "] - starting sample fetch thread" );

					ArrayList<UsbIrp> irps = new ArrayList<UsbIrp>();
					
					for( int x = 0; x < sUSB_IRP_POOL_SIZE; x++ )
					{
						UsbIrp irp = mUSBPipe.createUsbIrp();
						irp.setData( new byte[ sUSB_IRP_DATA_SIZE ] );
						irps.add( irp );
					}

					try
		            {
						if( mUSBPipe.isActive() )
						{
							if( !mUSBPipe.isOpen() )
							{
								resetUSBBuffer();
								
								mUSBPipe.open();
								
								//TODO: move this to the constructor
								mUSBPipe.addUsbPipeListener( this );
							}
							
							if( mUSBPipe.isOpen() )
							{
					            mSampleDispatcherTask = mExecutor
					            		.scheduleAtFixedRate( new BufferDispatcher(), 
					            							  0, 20, TimeUnit.MILLISECONDS );
					            
					            mSampleRateCounterTask = mExecutor
					            		.scheduleAtFixedRate( new SampleRateMonitor(), 
					            							  60, 60, TimeUnit.SECONDS );

					            mUSBPipe.asyncSubmit( irps );
							}
						}
		            }
		            catch ( Exception e )
		            {
			            e.printStackTrace();
		            }
				}
	        }
		}

		/**
		 * Stops the sample fetching thread
		 */
		public void stop()
		{
			synchronized( mTransitioning )
			{
				if( mRunning.compareAndSet( true, false ) )
				{
					Log.debug( "RTL2832TunerController - stopping sample fetch thread" );

					mUSBPipe.abortAllSubmissions();
					
					if( mSampleDispatcherTask != null )
					{
						mSampleDispatcherTask.cancel( true );
						mSampleRateCounterTask.cancel( true );
						mRawSampleBuffer.clear();
						mSampleCounter.set( 0 );
					}
				}
			}
		}

		/**
		 * Indicates if this thread is running
		 */
		public boolean isRunning()
		{
			return mRunning.get();
		}

		/**
		 * This method is invoked by the usb pipe when a response is returned
		 * from the data request.  If there is byte sample data in the response
		 * then we will buffer it and allow the buffer processor to convert it
		 * and dispatch it to listeners.  We automatically insert another 
		 * irp to get more data.
		 * 
		 * This method runs on the usb4java library processing thread, so we
		 * don't want to weigh it down.
		 */
		@Override
        public synchronized void dataEventOccurred( UsbPipeDataEvent event )
        {
			UsbIrp irp = event.getUsbIrp();
			
			byte[] data = irp.getData();
			
			if( data.length > 0 )
			{
				mRawSampleBuffer.add( data );
				mSampleCounter.addAndGet( data.length );
			}

			if( mRunning.get() )
			{
				try
	            {
	                mUSBPipe.asyncSubmit( new byte[ sUSB_IRP_DATA_SIZE ] );
	            }
	            catch ( UsbNotActiveException | UsbNotOpenException
	                    | IllegalArgumentException | UsbDisconnectedException
	                    | UsbException e )
	            {
	            	Log.error( "RTL2832TunerController - error while "
	        			+ "processing samples - " + e.getLocalizedMessage() );
	            }
			}
        }

		@Override
        public void errorEventOccurred( UsbPipeErrorEvent e )
        {
			/* When an error occurs, we'll get errors on all of the 
			 * queued IRPS in succession.  We use the mResetting to control
			 * the reset process and ignore all subsequent errors */
			if( mResetting.compareAndSet( false, true ) )
			{
		        Log.error( "RTL2832TunerController - error during data transfer - "
		        		+ "resetting USB pipe to RTL-2832 - " +
		        		e.getUsbException().getLocalizedMessage() );
				
	        	stop();
	        	
	        	if( !mSampleListeners.isEmpty() )
	        	{
		        	start();
	        	}

		        mResetting.set( false );
			}
        }
	}

	/**
	 * Fetches byte[] chunks from the raw sample buffer.  Converts each byte
	 * array and broadcasts the array to all registered listeners
	 */
	public class BufferDispatcher implements Runnable
	{
		@Override
        public void run()
        {
			ArrayList<byte[]> buffers = new ArrayList<byte[]>();
			
			mRawSampleBuffer.drainTo( buffers );
			
			for( byte[] buffer: buffers )
			{
				Float[] samples = mSampleAdapter.convert( buffer );
				broadcast( samples );
			}
        }
	}

	/**
	 * Averages the sample rate over a 10-second period.  The count is for the
	 * number of bytes received from the tuner.  There are two bytes for each
	 * sample.  So, we divide by 20 to get the average sample rate.
	 */
	public class SampleRateMonitor implements Runnable
	{
		@Override
        public void run()
        {
			int count = mSampleCounter.getAndSet( 0 );
			
			double rate = (double)count / 120.0d;

			mSampleRateAverageSum += rate;
			mSampleRateAverageCount++;
			
			double average = mSampleRateAverageSum / (double)mSampleRateAverageCount;
			
			StringBuilder sb = new StringBuilder();
			sb.append( TimeStamp.getTimeStamp( " " ) );
			sb.append( " RTL-2832 [" );
			if( mDescriptor != null )
			{
				sb.append( mDescriptor.getSerial() );
			}
			else
			{
				sb.append( "DESCRIPTOR IS NULL" );
			}
			sb.append( "] Sample Rate - current: " );
			sb.append( mDecimalFormatter.format( rate ) );
			sb.append( "\taverage: " );
			sb.append( mDecimalFormatter.format( average ) );
			sb.append( " count:" );
			sb.append( mSampleRateAverageCount );
			
			Log.info( sb.toString() );
        }
	}

	/**
	 * RTL2832 EEPROM byte array descriptor parsing class
	 */
	public class Descriptor
	{
		private byte[] mData;
		private ArrayList<String> mLabels = new ArrayList<String>();
		
		public Descriptor( byte[] data )
		{
			mData = data;
			
			getLabels();
		}
		
		public boolean isValid()
		{
			return mData[ 0 ] == (byte)0x28 &&
				   mData[ 1 ] == (byte)0x32;
		}
		
		public String getVendorID()
		{
			int id = Integer.rotateLeft( ( 0xFF & mData[ 3] ), 8 ) |
										 ( 0xFF & mData[ 2 ] );

			return String.format("%04X", id );
		}
		
		public String getVendorLabel()
		{
			return mLabels.get( 0 );
		}
		
		public String getProductID()
		{
			int id = Integer.rotateLeft( ( 0xFF & mData[ 5] ), 8 ) |
										 ( 0xFF & mData[ 4 ] );

			return String.format("%04X", id );
		}
		
		public String getProductLabel()
		{
			return mLabels.get( 1 );
		}
		
		public boolean hasSerial()
		{
			return mData[ 6 ] == (byte)0xA5;
		}

		public String getSerial()
		{
			return mLabels.get( 2 );
		}
		
		public boolean remoteWakeupEnabled()
		{
			byte mask = (byte)0x01;
			
			return ( mData[ 7 ] & mask ) == mask; 
		}
		
		public boolean irEnabled()
		{
			byte mask = (byte)0x02;
			
			return ( mData[ 7 ] & mask ) == mask; 
		}

		private void getLabels()
		{
			mLabels.clear();
			
			int start = 0x09;
			
			while( start < 256 )
			{
				start = getLabel( start );
			}
		}
		
		private int getLabel( int start )
		{
			/* Validate length and check second byte for ETX (0x03) */
			if( start > 254 || mData[ start + 1 ] != (byte)0x03 )
			{
				return 256;
			}

			/* Get label length, including the length and ETX bytes */
			int length = 0xFF & mData[ start ];

			if( start + length > 255 )
			{
				return 256;
			}

			/* Get the label bytes */
			byte[] data = Arrays.copyOfRange( mData, start + 2, start + length );
			
			/* Translate the bytes as UTF-16 Little Endian and store the label */
			String label = new String( data, Charset.forName( "UTF-16LE" ) );
			
			mLabels.add( label );

			return start + length;
		}
		
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append( "RTL-2832 EEPROM Descriptor\n" );

			sb.append( "Vendor: " );
			sb.append( getVendorID() );
			sb.append( " [" );
			sb.append( getVendorLabel() );
			sb.append( "]\n" );

			sb.append( "Product: " );
			sb.append( getProductID() );
			sb.append( " [" );
			sb.append( getProductLabel() );
			sb.append( "]\n" );

			sb.append( "Serial: " );
			if( hasSerial() )
			{
				sb.append( "yes [" );
				sb.append( getSerial() );
				sb.append( "]\n" );
			}
			else
			{
				sb.append( "no\n" );
			}

			sb.append( "Remote Wakeup Enabled: " );
			sb.append( ( remoteWakeupEnabled() ? "yes" : "no" ) );
			sb.append( "\n" );

			sb.append( "IR Enabled: " );
			sb.append( ( irEnabled() ? "yes" : "no" ) );
			sb.append( "\n" );

			if( mLabels.size() > 3 )
			{
				sb.append( "Additional Labels: " );
				
				for( int x = 3; x < mLabels.size(); x++ )
				{
					sb.append( " [" );
					sb.append( mLabels.get( x ) );
					sb.append( "\n" );
				}
			}
			
			return sb.toString();
		}
	}
}

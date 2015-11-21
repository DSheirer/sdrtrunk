package source.tuner.airspy;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.usb.UsbException;

import org.apache.commons.io.EndianUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Device;
import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;
import org.usb4java.Transfer;
import org.usb4java.TransferCallback;

import sample.Broadcaster;
import sample.Listener;
import sample.complex.ComplexBuffer;
import source.SourceException;
import source.tuner.Tuner;
import source.tuner.TunerChannel;
import source.tuner.TunerChannelSource;
import source.tuner.TunerConfiguration;
import source.tuner.TunerController;
import controller.ThreadPoolManager;
import controller.ThreadPoolManager.ThreadType;
import dsp.filter.dc.DCRemovalFilter_RB;
import dsp.filter.hilbert.HilbertTransform;

/**
 * SDR Trunk 
 * Copyright (C) 2015 Dennis Sheirer
 *    
 * Ported from libairspy at:
 * https://github.com/airspy/host/tree/master/libairspy
 * -----------------------------------------------------------------------------
 * Copyright (c) 2013, Michael Ossmann <mike@ossmann.com>
 * Copyright (c) 2012, Jared Boone <jared@sharebrained.com>
 * Copyright (c) 2014, Youssef Touil <youssef@airspy.com>
 * Copyright (c) 2014, Benjamin Vernoux <bvernoux@airspy.com>
 * Copyright (c) 2015, Ian Gilmour <ian@sdrsharp.com>
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of AirSpy nor the names of its contributors may be used to 
 * endorse or promote products derived from this software without specific prior 
 * written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

public class AirspyTunerController extends TunerController
{
	public static final int LNA_GAIN_MIN = 0;
	public static final int LNA_GAIN_MAX = 14;
	public static final int LNA_GAIN_DEFAULT = 1;
	public static final int MIXER_GAIN_MIN = 0;
	public static final int MIXER_GAIN_MAX = 15;
	public static final int MIXER_GAIN_DEFAULT = 5;
	public static final int IF_GAIN_MIN = 0;
	public static final int IF_GAIN_MAX = 15;
	public static final int IF_GAIN_DEFAULT = 5;
	public static final long FREQUENCY_MIN = 24000000l;
	public static final long FREQUENCY_MAX = 1800000000l;
	public static final long FREQUENCY_DEFAULT = 101100000;
	public static final AirspySampleRate DEFAULT_SAMPLE_RATE =
			new AirspySampleRate( 0, 10000000, "10.00 MHz" );

	public static final long USB_TIMEOUT_MS = 2000l; //milliseconds
	public static final byte USB_ENDPOINT = (byte)0x81;
	public static final byte USB_INTERFACE = (byte)0x0;
	public static final int TRANSFER_BUFFER_POOL_SIZE = 16;
	
	public static final byte USB_REQUEST_IN = 
								(byte)( LibUsb.ENDPOINT_IN | 
										LibUsb.REQUEST_TYPE_VENDOR | 
										LibUsb.RECIPIENT_DEVICE );
	
	public static final byte USB_REQUEST_OUT = 
								(byte)( LibUsb.ENDPOINT_OUT | 
										LibUsb.REQUEST_TYPE_VENDOR | 
										LibUsb.RECIPIENT_DEVICE );

	public static final DecimalFormat MHZ_FORMATTER = new DecimalFormat( "#.00 MHz" );
	
	private final static Logger mLog = 
			LoggerFactory.getLogger( AirspyTunerController.class );
	
	private Device mDevice;
	private DeviceHandle mDeviceHandle;

	private ThreadPoolManager mThreadPoolManager;
	private LinkedTransferQueue<byte[]> mFilledBuffers = 
			new LinkedTransferQueue<byte[]>();
    private Broadcaster<ComplexBuffer> mComplexBufferBroadcaster = new Broadcaster<>();
	private int mBufferSize = 262144;
	private BufferProcessor mBufferProcessor = new BufferProcessor();
	private AirspySampleAdapter mSampleAdapter = new AirspySampleAdapter();
	private DCRemovalFilter_RB mDCFilter = new DCRemovalFilter_RB( 0.01f );
	private HilbertTransform mHilbertTransform = new HilbertTransform();
	
	private AirspyDeviceInformation mDeviceInfo;
	private List<AirspySampleRate> mSampleRates = new ArrayList<>();
	private int mSampleRate = 0;
	
	public AirspyTunerController( Device device, ThreadPoolManager threadPoolManager ) 
										  throws SourceException 
	{
		super( FREQUENCY_MIN, FREQUENCY_MAX );
		
		mDevice = device;
		mThreadPoolManager = threadPoolManager;
	}
	
	public void init() throws SourceException
	{
		mDeviceHandle = new DeviceHandle();

		int result = LibUsb.open( mDevice, mDeviceHandle );
		
		if( result != 0 )
		{
			if( result == LibUsb.ERROR_ACCESS )
			{
				mLog.error( "Unable to access Airspy - insufficient permissions."
					+ "  If you are running a Linux OS, have you installed the "
					+ "airspy rules file in \\etc\\udev\\rules.d ??" );
			}
			
			throw new SourceException( "Couldn't open airspy device - " +
				LibUsb.strError( result ) );
		}

		try
		{
			claimInterface();
		}
		catch( Exception e )
		{
			throw new SourceException( "Airspy Tuner Controller - error while "
					+ "setting USB configuration, claiming USB interface or "
					+ "reset the kernel mode driver", e );
		}
		
		try
		{
			setSamplePacking( false );
		}
		catch( LibUsbException | UsbException | UnsupportedOperationException e )
		{
			mLog.info( "Sample packing is not supported by airspy firmware" );
		}
		
		try
		{
			setReceiverMode( true );
		}
		catch( Exception e )
		{
			mLog.error( "Couldn't enable airspy receiver mode", e );
		}

		setFrequency( FREQUENCY_DEFAULT );
		
		try
		{
			determineAvailableSampleRates();
		} 
		catch ( LibUsbException | UsbException e )
		{
			mLog.error( "Error identifying available samples rates", e );
		}
		
		try
		{
			setSampleRate( DEFAULT_SAMPLE_RATE );
		} 
		catch ( IllegalArgumentException | LibUsbException | UsbException e )
		{
			mLog.error( "Setting sample rate is not supported by firmware", e );
		}
	}
	
	/**
	 * Claims the USB interface.  If another application currently has
	 * the interface claimed, the USB_FORCE_CLAIM_INTERFACE setting
	 * will dictate if the interface is forcibly claimed from the other 
	 * application
	 */
	private void claimInterface() throws SourceException
	{
		if( mDeviceHandle != null )
		{
			int result = LibUsb.kernelDriverActive( mDeviceHandle, USB_INTERFACE );
					
			if( result == 1 )
			{
				
				result = LibUsb.detachKernelDriver( mDeviceHandle, USB_INTERFACE );

				if( result != LibUsb.SUCCESS )
				{
					mLog.error( "failed attempt to detach kernel driver [" + 
							LibUsb.errorName( result ) + "]" );
					
					throw new SourceException( "couldn't detach kernel driver "
							+ "from device" );
				}
			}

			result = LibUsb.setConfiguration( mDeviceHandle, 1 );
			
			if( result != LibUsb.SUCCESS )
			{
				throw new SourceException( "couldn't set USB configuration 1 [" + 
					LibUsb.errorName( result ) + "]" );
			}

			result = LibUsb.claimInterface( mDeviceHandle, USB_INTERFACE );
			
			if( result != LibUsb.SUCCESS )
			{
				throw new SourceException( "couldn't claim usb interface [" + 
					LibUsb.errorName( result ) + "]" );
			}
		}
		else
		{
			throw new SourceException( "couldn't claim usb interface - no "
					+ "device handle" );
		}
	}
	
	@Override
	public void apply( TunerConfiguration config ) throws SourceException
	{
		if( config instanceof AirspyTunerConfiguration )
		{
			AirspyTunerConfiguration airspy = (AirspyTunerConfiguration)config;

			int sampleRate = airspy.getSampleRate();

			AirspySampleRate rate = getSampleRate( sampleRate );

			if( rate == null )
			{
				rate = DEFAULT_SAMPLE_RATE;
			}
			
			try
			{
				setSampleRate( rate );
			}
			catch( UsbException e )
			{
				throw new SourceException( "Couldn't set sample rate [" + 
						rate.toString() + "]", e );
			}

			try
			{
				setIFGain( airspy.getIFGain() );
				setMixerGain( airspy.getMixerGain() );
				setLNAGain( airspy.getLNAGain() );
				
				setMixerAGC( airspy.isMixerAGC() );
				setLNAAGC( airspy.isLNAAGC() );
			}
			catch( Exception e )
			{
				throw new SourceException( "Couldn't apply gain settings from "
						+ "airspy config", e );
			}
			
		}
		else
		{
			throw new IllegalArgumentException( "Invalid tuner config:" + 
						config.getClass() );
		}
	}

	@Override
	public long getTunedFrequency() throws SourceException
	{
		return mFrequencyController.getTunedFrequency();
	}

	@Override
	public void setTunedFrequency( long frequency ) throws SourceException
	{
		if( FREQUENCY_MIN <= frequency && frequency <= FREQUENCY_MAX )
		{
			ByteBuffer buffer = ByteBuffer.allocateDirect( 4 );
			
			buffer.order( ByteOrder.LITTLE_ENDIAN );

			buffer.putInt( (int)frequency );

			buffer.rewind();

			try
			{
				write( Command.SET_FREQUENCY, 0, 0, buffer );
			}
			catch( UsbException e )
			{
				mLog.error( "error setting frequency [" + frequency + "]", e );
				
				throw new SourceException( "error setting frequency [" + 
						frequency + "]", e );
			}
		}
		else
		{
			throw new SourceException( "Frequency [" + frequency + "] outside "
				+ "of tunable range " + FREQUENCY_MIN + "-" + FREQUENCY_MAX );
		}
	}

	@Override
	public int getCurrentSampleRate() throws SourceException
	{
		return mSampleRate;
	}

	/**
	 * Sets the sample rate to the rate specified by the index value in the 
	 * available sample rates map
	 * 
	 * @param index to a sample rate in the available samples rates map.
	 * 
	 * @throws IllegalArgumentException if index is not a valid rate index
	 * @throws LibUsbException if there was a read error or if this operation
	 * is not supported by the current firmware
	 * 
	 * @throws UsbException if there was a USB error
	 */
	public void setSampleRate( AirspySampleRate rate ) throws
				LibUsbException, UsbException
	{
		if( rate.getRate() != mSampleRate )
		{
			int result = readByte( Command.SET_SAMPLE_RATE, 0, rate.getIndex(), true );

			if( result != 1 )
			{
				throw new UsbException( "Error setting sample rate [" + 
						rate + "] rate - return value [" + result + "]" );
			}
			else
			{
				mSampleRate = rate.getRate();
				mFrequencyController.setSampleRate( mSampleRate );
			}
		}
	}

	/**
	 * Returns a list of sample rates supported by the firmware version 
	 */
	public List<AirspySampleRate> getSampleRates()
	{
		return mSampleRates;
	}
	
	/**
	 * Airspy sample rate object that matches the current sample rate setting.
	 */
	public AirspySampleRate getSampleRate()
	{
		return getSampleRate( mSampleRate );
	}

	/**
	 * Airspy sample rate object that matches the specified rate in hertz, or
	 * null if there are no available sample rates for the tuner that match the
	 * argument value.
	 */
	public AirspySampleRate getSampleRate( int rate )
	{
		for( AirspySampleRate sampleRate: mSampleRates )
		{
			if( sampleRate.getRate() == rate )
			{
				return sampleRate;
			}
		}
		
		//We should never get to here ...
		return null;
	}
	
	/**
	 * Enables/Disables sample packing to allow two 12-bit samples to be packed
	 * into 3 bytes (enabled) or 4 bytes (disabled).
	 * 
	 * @param enabled
	 * 
	 * @throws UsbException if sample packing is not supported by the current
	 * device firmware or if there were usb communication issues
	 */
	public void setSamplePacking( boolean enabled ) 
						throws LibUsbException, UsbException
	{
		int result = readByte( Command.SET_PACKING, 0, ( enabled ? 1 : 0 ), true );
		
		if( result != 1 )
		{
			throw new UsbException( "Couldnt set sample packing enabled: " + enabled  );
		}

		/* If we didn't throw an exception above, then update the sample adapter
		 * to process samples accordingly */
		mSampleAdapter.setSamplePacking( enabled );
	}
	
	/**
	 * Enables/disables the mixer automatic gain setting
	 * 
	 * @param enabled
	 * 
	 * @throws LibUsbException on unsuccessful read operation
	 * @throws UsbException on USB error
	 */
	public void setMixerAGC( boolean enabled )
						throws LibUsbException, UsbException
	{
		int result = readByte( Command.SET_MIXER_AGC, 0, ( enabled ? 1 : 0 ), true );
		
		if( result != LibUsb.SUCCESS )
		{
			throw new UsbException( "Couldnt set mixer AGC enabled: " + enabled  );
		}
	}

	/**
	 * Enables/disables the low noise amplifier automatic gain setting
	 * 
	 * @param enabled
	 * 
	 * @throws LibUsbException on unsuccessful read operation
	 * @throws UsbException on USB error
	 */
	public void setLNAAGC( boolean enabled ) throws LibUsbException, UsbException
	{
		int result = readByte( Command.SET_LNA_AGC, 0, ( enabled ? 1 : 0 ), true );
		
		if( result != LibUsb.SUCCESS )
		{
			throw new UsbException( "Couldnt set LNA AGC enabled: " + enabled  );
		}
	}

	/**
	 * Sets LNA gain
	 * 
	 * @param gain - value within range of LNA_GAIN_MIN to LNA_GAIN_MAX
	 * 
	 * @throws LibUsbException on error in java USB wrapper
	 * @throws UsbException on error in USB transfer
	 * @throws IllegalArgumentException if gain value is invalid
	 */
	public void setLNAGain( int gain ) 
			throws LibUsbException, UsbException, IllegalArgumentException
	{
		if( LNA_GAIN_MIN <= gain && gain <= LNA_GAIN_MAX )
		{
			int result = readByte( Command.SET_LNA_GAIN, 0, gain, true );
			
			if( result != LibUsb.SUCCESS )
			{
				throw new UsbException( "Couldnt set LNA gain to: " + gain  );
			}
		}
		else
		{
			throw new IllegalArgumentException( "LNA gain value [" + gain + 
				"] is outside value range: " + LNA_GAIN_MIN + "-" + LNA_GAIN_MAX );
		}
	}

	/**
	 * Sets Mixer gain
	 * 
	 * @param gain - value within range of MIXER_GAIN_MIN to MIXER_GAIN_MAX
	 * 
	 * @throws LibUsbException on error in java USB wrapper
	 * @throws UsbException on error in USB transfer
	 * @throws IllegalArgumentException if gain value is invalid
	 */
	public void setMixerGain( int gain ) 
			throws LibUsbException, UsbException, IllegalArgumentException
	{
		if( MIXER_GAIN_MIN <= gain && gain <= MIXER_GAIN_MAX )
		{
			int result = readByte( Command.SET_MIXER_GAIN, 0, gain, true );
			
			if( result != LibUsb.SUCCESS )
			{
				throw new UsbException( "Couldnt set mixer gain to: " + gain  );
			}
		}
		else
		{
			throw new IllegalArgumentException( "Mixer gain value [" + gain + 
				"] is outside value range: " + MIXER_GAIN_MIN + "-" + MIXER_GAIN_MAX );
		}
	}

	/**
	 * Sets IF (VGA) gain
	 * 
	 * @param gain - value within range of VGA_GAIN_MIN to VGA_GAIN_MAX
	 * 
	 * @throws LibUsbException on error in java USB wrapper
	 * @throws UsbException on error in USB transfer
	 * @throws IllegalArgumentException if gain value is invalid
	 */
	public void setIFGain( int gain ) 
			throws LibUsbException, UsbException, IllegalArgumentException
	{
		if( IF_GAIN_MIN <= gain && gain <= IF_GAIN_MAX )
		{
			int result = readByte( Command.SET_VGA_GAIN, 0, gain, true );
			
			if( result != LibUsb.SUCCESS )
			{
				throw new UsbException( "Couldnt set VGA gain to: " + gain  );
			}
		}
		else
		{
			throw new IllegalArgumentException( "VGA gain value [" + gain + 
				"] is outside value range: " + IF_GAIN_MIN + "-" + IF_GAIN_MAX );
		}
	}
	
	public void setReceiverMode( boolean enabled ) throws LibUsbException, UsbException
	{
		//Empty buffer to throw away
		ByteBuffer buffer = ByteBuffer.allocateDirect( 0 );
		
		write( Command.RECEIVER_MODE, ( enabled ? 1 : 0 ), 0, buffer );
	}
	
	/**
	 * Queries the device for available sample rates.  Will always provide at
	 * least the default 10 MHz sample rate.
	 */
	private void determineAvailableSampleRates() 
						throws LibUsbException, UsbException
	{
		mSampleRates.clear();
		
		mSampleRates.add( DEFAULT_SAMPLE_RATE );
		
		//Get a count of available sample rates.  If we get an exception, then
		//we're using an older firmware revision and only the default 10 MHz
		//rate is supported
		try
		{
			byte[] rawCount = readArray( Command.GET_SAMPLE_RATES, 0, 0, 4 );

			
			if( rawCount != null )
			{
				int count = EndianUtils.readSwappedInteger( rawCount, 0 );
				
				mLog.debug( "There are [" + count + "] sample rates available" );

				byte[] rawRates = readArray( Command.GET_SAMPLE_RATES, 0, 
						count, ( count * 4 ) );

				for( int x = 0; x < count; x++ )
				{
					int rate = EndianUtils.readSwappedInteger( rawRates, ( x * 4 ) );
					
					if( rate != DEFAULT_SAMPLE_RATE.getRate() )
					{
						mSampleRates.add( new AirspySampleRate( x, rate, 
								formatSampleRate( rate ) ) );
					}
				}
			}
		}
		catch( LibUsbException e )
		{
			//Press on, nothing else to do here ..
		}
	}

	/**
	 * Formats the rate in hertz for display as megahertz
	 */
	private static String formatSampleRate( int rate )
	{
		return MHZ_FORMATTER.format( (double)rate / 1E6d );
	}

	/**
	 * Device information
	 */
	public AirspyDeviceInformation getDeviceInfo()
	{
		//Lazy initialization
		if( mDeviceInfo == null )
		{
			readDeviceInfo();
		}
		
		return mDeviceInfo;
	}

	/**
	 * Reads version information from the device and populates the info object
	 */
	private void readDeviceInfo()
	{
		if( mDeviceInfo == null )
		{
			mDeviceInfo = new AirspyDeviceInformation();
		}

		/* Board ID */
		try
		{
			int boardID = readByte( Command.BOARD_ID_READ, 0, 0, true );
			
			mDeviceInfo.setBoardID( boardID );
		} 
		catch ( LibUsbException | UsbException e )
		{
			mLog.error( "Error reading airspy board ID", e );
		}

		/* Version String */
		try
		{
			//NOTE: libairspy is internally reading 127 bytes, however airspy_info 
			//script is telling it to read 255 bytes ... things that make you go hmmmm
			byte[] version = readArray( Command.VERSION_STRING_READ, 0, 0, 127 );

			mDeviceInfo.setVersion( version );
		} 
		catch ( LibUsbException | UsbException e )
		{
			mLog.error( "Error reading airspy version string", e );
		}
		
		/* Part ID and Serial Number */
		try
		{
			//Read 6 x 32-bit integers = 24 bytes
			byte[] serial = readArray( 
					Command.BOARD_PART_ID_SERIAL_NUMBER_READ, 0, 0, 24 );
			
			mDeviceInfo.setPartAndSerialNumber( serial );
		} 
		catch ( LibUsbException | UsbException e )
		{
			mLog.error( "Error reading airspy version string", e );
		}
	}
	
	/**
	 * Reads a single byte value from the device.
	 * 
	 * @param command - airspy command
	 * 
	 * @param value - value field for usb setup packet
	 * @param index - index field for usb setup packet
	 * 
	 * @return - byte value as an integer
	 * 
	 * @throws LibUsbException if the operation is unsuccesful
	 * @throws UsbException on any usb errors
	 */
	private int readByte( Command command, int value, int index, boolean signed ) 
							throws LibUsbException, UsbException
	{
		if( mDeviceHandle != null )
		{
			ByteBuffer buffer = ByteBuffer.allocateDirect( 1 );
			
			int transferred = LibUsb.controlTransfer( mDeviceHandle, 
													  USB_REQUEST_IN, 
													  command.getValue(), 
													  (short)value,
													  (short)index,
													  buffer, 
													  USB_TIMEOUT_MS );

			if( transferred < 0 )
			{
				throw new LibUsbException( "read error", transferred );
			}

			byte result = buffer.get( 0 );
			
			if( signed )
			{
				return ( result & 0xFF );
			}
			else
			{
				return result;
			}
		}
		else
		{
			throw new LibUsbException( "device handle is null", 
							LibUsb.ERROR_NO_DEVICE );
		}
	}

	/**
	 * Reads a multi-byte value from the device 
	 * 
	 * @param command - airspy command
	 * @param value - usb packet value
	 * @param index - usb packet index
	 * @param length - number of bytes to read
	 * @return - bytes read from the device 
	 * 
	 * @throws LibUsbException if quantity of bytes read doesn't equal the
	 * 			requested number of bytes
	 * @throws UsbException on error communicating with the device
	 */
	private byte[] readArray( Command command, int value, int index, int length ) 
			throws LibUsbException, UsbException
	{
		if( mDeviceHandle != null )
		{
			ByteBuffer buffer = ByteBuffer.allocateDirect( length );
			
			int transferred = LibUsb.controlTransfer( mDeviceHandle, 
													  USB_REQUEST_IN, 
													  command.getValue(), 
													  (short)value,
													  (short)index,
													  buffer, 
													  USB_TIMEOUT_MS );
			
			if( transferred < 0 )
			{
				throw new LibUsbException( "read error", transferred );
			}

			byte[] results = new byte[ transferred ];

			buffer.get( results );
			
			return results;
		}
		else
		{
			throw new LibUsbException( "device handle is null", 
							LibUsb.ERROR_NO_DEVICE );
		}
	}
	
	/**
	 * Writes the buffer contents to the device
	 * 
	 * @param command - airspy command
	 * @param value - usb packet value
	 * @param index - usb packet index
	 * @param buffer - data to write to the device
	 * @throws UsbException on error
	 */
	public void write( Command command, int value, int index, ByteBuffer buffer ) 
									throws UsbException
	{
		if( mDeviceHandle != null )
		{
			int transferred = LibUsb.controlTransfer( mDeviceHandle, 
													  USB_REQUEST_OUT,
													  command.getValue(),
													  (short)value, 
													  (short)index, 
													  buffer, 
													  USB_TIMEOUT_MS );
		
			if( transferred < 0 )
			{
				throw new LibUsbException( "error writing byte buffer", 
								transferred );
			}
			else if( transferred != buffer.capacity() )
			{
				throw new LibUsbException( "transferred bytes [" + 
						transferred + "] is not what was expected [" + 
						buffer.capacity() + "]", transferred );
			}
		}
		else
		{
			throw new LibUsbException( "device handle is null", 
							LibUsb.ERROR_NO_DEVICE );
		}
	}
	
	/**
	 * Indicates if the tuner can accomodate this new channel frequency and
	 * bandwidth, along with all of the existing tuned channels currently in 
	 * place.
	 */
	public boolean canTuneChannel( TunerChannel channel )
	{
		//If this is the first lock, then we're good
		if( mTunedChannels.isEmpty() )
		{
			return true;
		}
		else
		{
			//Sort the existing locks and get the min/max locked frequencies
			Collections.sort( mTunedChannels );

			long minLockedFrequency = mTunedChannels.get( 0 ).getMinFrequency();
			long maxLockedFrequency = mTunedChannels
					.get( mTunedChannels.size() - 1 ).getMaxFrequency();
			
			if( channel.getMinFrequency() >= minLockedFrequency &&
				channel.getMaxFrequency() <= maxLockedFrequency )
			{
				return true;
			}

			if( channel.getMinFrequency() < minLockedFrequency )
			{
				return ( maxLockedFrequency - channel.getMinFrequency() ) < getBandwidth() - 1E5;
			}
			
			if( channel.getMaxFrequency() > maxLockedFrequency )
			{
				return ( channel.getMaxFrequency() - minLockedFrequency ) <= getBandwidth() - 1E5;
			}
		}
		
		return false;
	}
	
	public TunerChannelSource getChannel( ThreadPoolManager threadPoolManager,
		Tuner tuner, TunerChannel tunerChannel )
				throws RejectedExecutionException, SourceException
	{
		TunerChannelSource source = null;
		
		if( canTuneChannel( tunerChannel ) )
		{
			mTunedChannels.add( tunerChannel );
			
			Collections.sort( mTunedChannels );
			
			long min = mTunedChannels.get( 0 ).getMinFrequency();
			long max = mTunedChannels.get( mTunedChannels.size() - 1 ).getMaxFrequency();
			
			if( !( getMinTunedFrequency() <= min && 
				   max <= getMaxTunedFrequency() ) )
			{
				long freq = min + ( mFrequencyController.getBandwidth() / 2 ) + 50000;
				
				mFrequencyController.setFrequency( freq );
			}
			
			source = new TunerChannelSource( threadPoolManager, 
					tuner, tunerChannel );
		}

		return source;
	}
	
	public void releaseChannel( TunerChannelSource tunerChannelSource )
	{
		if( tunerChannelSource != null )
		{
			mTunedChannels.remove( tunerChannelSource.getTunerChannel() );
		}
		else
		{
			mLog.error( "Tuner Controller - couldn't find the tuned channel "
					+ "to release it" );
		}
	}
	
	
	/**
	 * Airspy Board Identifier
	 */
	public enum BoardID
	{
		AIRSPY( 0, "Airspy" ),
		UNKNOWN( -1, "Unknown" );

		private int mValue;
		private String mLabel;
		
		private BoardID( int value, String label )
		{
			mValue = value;
			mLabel = label;
		}
		
		public int getValue()
		{
			return mValue;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public static BoardID fromValue( int value )
		{
			if( value == 0 )
			{
				return AIRSPY;
			}
			
			return UNKNOWN;
		}
	}
	
	/**
	 * Airspy Commands
	 */
	public enum Command
	{
		INVALID( 0 ),
		RECEIVER_MODE( 1 ),
		SI5351C_WRITE( 2 ),
		SI5351C_READ( 3 ),
		R820T_WRITE( 4 ),
		R820T_READ( 5 ),
		SPIFLASH_ERASE( 6 ),
		SPIFLASH_WRITE( 7 ),
		SPIFLASH_READ( 8 ),
		BOARD_ID_READ( 9 ),
		VERSION_STRING_READ( 10 ),
		BOARD_PART_ID_SERIAL_NUMBER_READ( 11 ),
		SET_SAMPLE_RATE( 12 ),
		SET_FREQUENCY( 13 ),
		SET_LNA_GAIN( 14 ),
		SET_MIXER_GAIN( 15 ),
		SET_VGA_GAIN( 16 ),
		SET_LNA_AGC( 17 ),
		SET_MIXER_AGC( 18 ),
		MS_VENDOR_COMMAND( 19 ),
		SET_RF_BIAS_COMMAND( 20 ),
		GPIO_WRITE( 21 ),
		GPIO_READ( 22 ),
		GPIO_DIR__WRITE( 23 ),
		GPIO_DIR_READ( 24 ),
		GET_SAMPLE_RATES( 25 ),
		SET_PACKING( 26 );
		
		private int mValue;
		
		private Command( int value )
		{
			mValue = value;
		}
		
		public byte getValue()
		{
			return (byte)mValue;
		}
		
		public static Command fromValue( int value )
		{
			if( 0 <= value && value <= 25 )
			{
				return Command.values()[ value ];
			}
			
			return INVALID;
		}
	}
	
	public enum ReceiverMode
	{
		OFF( 0 ),
		ON( 1 );
		
		private int mValue;
		
		private ReceiverMode( int value )
		{
			mValue = value;
		}
		
		public int getValue()
		{
			return mValue;
		}
	}

	/**
	 * General Purpose Input/Output Ports (accessible on the airspy board)
	 */
	public enum GPIOPort
	{
		PORT_0( 0 ),
		PORT_1( 1 ),
		PORT_2( 2 ),
		PORT_3( 3 ),
		PORT_4( 4 ),
		PORT_5( 5 ),
		PORT_6( 6 ),
		PORT_7( 7 );
		
		private int mValue;
		
		private GPIOPort( int value )
		{
			mValue = value;
		}
		
		public int getValue()
		{
			return mValue;
		}
	}
	
	/**
	 * General Purpose Input/Output Pins (accessible on the airspy board)
	 */
	public enum GPIOPin
	{
		PIN_0( 0 ),
		PIN_1( 1 ),
		PIN_2( 2 ),
		PIN_3( 3 ),
		PIN_4( 4 ),
		PIN_5( 5 ),
		PIN_6( 6 ),
		PIN_7( 7 ),
		PIN_8( 8 ),
		PIN_9( 9 ),
		PIN_10( 10 ),
		PIN_11( 11 ),
		PIN_12( 12 ),
		PIN_13( 13 ),
		PIN_14( 14 ),
		PIN_15( 15 ),
		PIN_16( 16 ),
		PIN_17( 17 ),
		PIN_18( 18 ),
		PIN_19( 19 ),
		PIN_20( 20 ),
		PIN_21( 21 ),
		PIN_22( 22 ),
		PIN_23( 23 ),
		PIN_24( 24 ),
		PIN_25( 25 ),
		PIN_26( 26 ),
		PIN_27( 27 ),
		PIN_28( 28 ),
		PIN_29( 29 ),
		PIN_30( 30 ),
		PIN_31( 31 );
		
		private int mValue;
		
		private GPIOPin( int value )
		{
			mValue = value;
		}
		
		public int getValue()
		{
			return mValue;
		}
	}
	
	/**
	 * Adds a sample listener.  If the buffer processing thread is
	 * not currently running, starts it running in a new thread.
	 */
    public void addListener( Listener<ComplexBuffer> listener )
    {
    	mComplexBufferBroadcaster.addListener( listener );

		if( mBufferProcessor == null || !mBufferProcessor.isRunning() )
		{
			mBufferProcessor = new BufferProcessor();

			Thread thread = new Thread( mBufferProcessor );
			thread.setDaemon( true );
			thread.setName( "Airspy Buffer Processor" );

			thread.start();
		}
    }

	/**
	 * Removes the sample listener.  If this is the last registered listener,
	 * shuts down the buffer processing thread.
	 */
    public void removeListener( Listener<ComplexBuffer> listener )
    {
    	mComplexBufferBroadcaster.removeListener( listener );
    	
		if( !mComplexBufferBroadcaster.hasListeners() )
		{
			mBufferProcessor.stop();
		}
    }
	
	/**
	 * Buffer processing thread.  Fetches samples from the Airspy Tuner and 
	 * dispatches them to all registered listeners
	 */
	public class BufferProcessor implements Runnable, TransferCallback
	{
		private ScheduledFuture<?> mSampleDispatcherTask;
        private CopyOnWriteArrayList<Transfer> mTransfers;
		private AtomicBoolean mRunning = new AtomicBoolean();

		@Override
        public void run()
        {
			if( mRunning.compareAndSet( false, true ) )
			{
	            prepareTransfers();
	            
				for( Transfer transfer: mTransfers )
				{
					int result = LibUsb.submitTransfer( transfer );
					
					if( result != LibUsb.SUCCESS )
					{
						mLog.error( "error submitting transfer [" + 
								LibUsb.errorName( result ) + "]" );
						break;
					}
				}

				mSampleDispatcherTask = mThreadPoolManager.scheduleFixedRate( 
					ThreadType.SOURCE_SAMPLE_PROCESSING, new BufferDispatcher(),
					20, TimeUnit.MILLISECONDS );

            	while( mRunning.get() )
				{
					ByteBuffer completed = ByteBuffer.allocateDirect( 4 );
					
					int result = LibUsb.handleEventsTimeoutCompleted( 
							null, USB_TIMEOUT_MS, completed.asIntBuffer() );
					
					if( result != LibUsb.SUCCESS )
					{
						mLog.error( "error handling events for libusb" );
					}
				}
			}
        }

		/**
		 * Stops the sample fetching thread
		 */
		public void stop()
		{
			if( mRunning.compareAndSet( true, false ) )
			{
				cancel();
				
				if( mSampleDispatcherTask != null )
				{
					mSampleDispatcherTask.cancel( true );
					mFilledBuffers.clear();
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
		
		@Override
	    public void processTransfer( Transfer transfer )
	    {
			if( transfer.status() == LibUsb.TRANSFER_COMPLETED )
			{
				ByteBuffer buffer = transfer.buffer();
				
				byte[] data = new byte[ transfer.actualLength() ];
				
				buffer.get( data );

				buffer.rewind();

				mFilledBuffers.add( data );
				
				if( !isRunning() )
				{
					LibUsb.cancelTransfer( transfer );
				}
			}
			
			switch( transfer.status() )
			{
				case LibUsb.TRANSFER_COMPLETED:
					/* resubmit the transfer */
					int result = LibUsb.submitTransfer( transfer );
					
					if( result != LibUsb.SUCCESS )
					{
						mLog.error( "couldn't resubmit buffer transfer to tuner" );
						LibUsb.freeTransfer( transfer );
						mTransfers.remove( transfer );
					}
					break;
				case LibUsb.TRANSFER_CANCELLED:
					/* free the transfer and remove it */
					LibUsb.freeTransfer( transfer );
					mTransfers.remove( transfer );
					break;
				default:
					/* unexpected error */
					mLog.error( "transfer error" );
			}
	    }
		
		private void cancel()
		{
			for( Transfer transfer: mTransfers )
			{
				LibUsb.cancelTransfer( transfer );
			}

			ByteBuffer completed = ByteBuffer.allocateDirect( 4 );

			int result = LibUsb.handleEventsTimeoutCompleted( 
					null, USB_TIMEOUT_MS, completed.asIntBuffer() );
			
			if( result != LibUsb.SUCCESS )
			{
				mLog.error( "error handling usb events during cancel [" + 
						LibUsb.errorName( result ) + "]" );
			}
		}
		
	    /**
	     * Prepares (allocates) a set of transfer buffers for use in 
	     * transferring data from the tuner via the bulk interface
	     */
	    private void prepareTransfers() throws LibUsbException
	    {
	    	mTransfers = new CopyOnWriteArrayList<Transfer>();

	    	for( int x = 0; x < TRANSFER_BUFFER_POOL_SIZE; x++ )
	    	{
	    		Transfer transfer = LibUsb.allocTransfer();

	    		if( transfer == null )
	    		{
	    			throw new LibUsbException( "couldn't allocate transfer", 
	    						LibUsb.ERROR_NO_MEM );
	    		}
	    		
	    		final ByteBuffer buffer = 
	    				ByteBuffer.allocateDirect( mBufferSize );
	    		
	    		LibUsb.fillBulkTransfer( transfer, 
	    								 mDeviceHandle, 
	    								 USB_ENDPOINT, 
	    								 buffer, 
	    								 BufferProcessor.this, 
	    								 "Buffer #" + x,
	    								 USB_TIMEOUT_MS );
	    		
	    		mTransfers.add( transfer );
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
			try
			{
				ArrayList<byte[]> buffers = new ArrayList<>();
				
				mFilledBuffers.drainTo( buffers );

				for( byte[] buffer: buffers )
				{
					float[] realSamples = mSampleAdapter.convert( buffer );

					realSamples = mDCFilter.filter( realSamples );

					float[] quadratureSamples = mHilbertTransform.filter( realSamples );
					
					mComplexBufferBroadcaster.broadcast( 
							new ComplexBuffer( quadratureSamples ) );
				}
			}
			catch( Exception e )
			{
				mLog.error( "error during Airspy buffer dispatching", e );
			}
        }
	}
}

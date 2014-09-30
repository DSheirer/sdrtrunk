package source.tuner.hackrf;

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

import javax.usb.UsbClaimException;
import javax.usb.UsbConfiguration;
import javax.usb.UsbControlIrp;
import javax.usb.UsbDevice;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbEndpoint;
import javax.usb.UsbEndpointDescriptor;
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

import org.apache.commons.io.EndianUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.LibUsb;

import sample.Listener;
import source.SourceException;
import source.tuner.TunerConfiguration;
import source.tuner.TunerController;
import source.tuner.rtl.ByteSampleAdapter;
import source.tuner.usb.USBTunerDevice;

public class HackRFTunerController extends TunerController
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( HackRFTunerController.class );

	public static final byte USB_ENDPOINT = (byte)0x81;
	public static final byte USB_INTERFACE = (byte)0x0;
	public static final boolean USB_FORCE_CLAIM_INTERFACE = true;
	
	public static final byte REQUEST_TYPE_IN = 
								(byte)( LibUsb.ENDPOINT_IN | 
										LibUsb.REQUEST_TYPE_VENDOR | 
										LibUsb.RECIPIENT_DEVICE );
	
	public static final byte REQUEST_TYPE_OUT = 
								(byte)( LibUsb.ENDPOINT_OUT | 
										LibUsb.REQUEST_TYPE_VENDOR | 
										LibUsb.RECIPIENT_DEVICE );

	public static final long MIN_FREQUENCY = 10000000l;
	public static final long MAX_FREQUENCY = 6000000000l;
	public static final long DEFAULT_FREQUENCY = 101100000;

	public final static int USB_IRP_POOL_SIZE = 10;
	
	private LinkedTransferQueue<UsbIrp> mReceivedBuffers = 
								new LinkedTransferQueue<UsbIrp>();
	
	private CopyOnWriteArrayList<Listener<Float[]>> mSampleListeners =
							new CopyOnWriteArrayList<Listener<Float[]>>();
	
	private ByteSampleAdapter mSampleAdapter = new ByteSampleAdapter();
	private BufferProcessor mBufferProcessor = new BufferProcessor();
	
	private UsbDevice mUSBDevice;
	private UsbInterface mUSBInterface;
	private UsbPipe mUSBPipe;
	
	private HackRFSampleRate mSampleRate = HackRFSampleRate.RATE4_464MHZ;
	public int mBufferSize = mSampleRate.getBufferSize();
	
	public HackRFTunerController( USBTunerDevice tunerDevice ) 
						throws SourceException
	{
	    super( MIN_FREQUENCY, MAX_FREQUENCY );
	    
	    mUSBDevice = tunerDevice.getDevice();
	}
	
	public void init() throws SourceException
	{
		mLog.info( "HackRF Tuner Controller init() starting" );

		UsbConfiguration config = mUSBDevice.getActiveUsbConfiguration();
		
		mUSBInterface = config.getUsbInterface( USB_INTERFACE );

		try
		{
			if( claim( mUSBInterface ) )
			{
				/* Get the USB endpoint */
				UsbEndpoint endpoint = mUSBInterface.getUsbEndpoint( USB_ENDPOINT );
				
				UsbEndpointDescriptor d = endpoint.getUsbEndpointDescriptor();
				System.out.println( "Endpoint max packet size:" + d.wMaxPacketSize() );
				
				mUSBPipe = endpoint.getUsbPipe();
				
				setMode( Mode.RECEIVE );
				
				setFrequency( DEFAULT_FREQUENCY );
			}
			else
			{
				throw new SourceException( "HackRF Tuner Controller - couldn't "
						+ "claim USB interface" );
			}
		}
		catch( UsbException e )
		{
			throw new SourceException( "HackRF Tuner Controller - couldn't "
					+ "claim USB interface or get endpoint or pipe", e );
		}
        catch ( UsbDisconnectedException e )
        {
			throw new SourceException( "HackRF Tuner Controller - usb device "
					+ "is disconnected", e );
        }
		
		mLog.info( "HackRF Tuner Controller init() complete" );
	}

	/**
	 * Claims the USB interface.  If another application currently has
	 * the interface claimed, the USB_FORCE_CLAIM_INTERFACE setting
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
                    return USB_FORCE_CLAIM_INTERFACE;
                }
			} );
			
			return iface.isClaimed();
		}
		else
		{
			mLog.error( "attempt to claim USB interface failed - in use by "
					+ "another application" );
		}
		
		return false;
	}

	/**
	 * HackRF board identifier/type
	 */
	public BoardID getBoardID() throws UsbException
	{
		int id = readByte( Request.BOARD_ID_READ, (byte)0, (byte)0, false );
		
		return BoardID.lookup( id );
	}
	
	/**
	 * HackRF firmware version string
	 */
	public String getFirmwareVersion() throws UsbException
	{
		byte[] data = readArray( Request.VERSION_STRING_READ, 0, 0, 255 ); 
		
		StringBuilder sb = new StringBuilder();
		
		for( byte b: data )
		{
			if( b != (byte)0 )
			{
				sb.append( (char)b );
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * HackRF part id number and serial number
	 */
	public Serial getSerial() throws UsbException
	{
		byte[] data = readArray( Request.BOARD_PARTID_SERIALNO_READ, 0, 0, 24 ); 

		return new Serial( data ); 
	}

	/**
	 * Sets the HackRF transceiver mode
	 */
	public void setMode( Mode mode ) throws UsbException
	{
		write( Request.SET_TRANSCEIVER_MODE, mode.getNumber(), 0 );
	}

	/**
	 * Sets the HackRF baseband filter
	 */
	public void setBasebandFilter( BasebandFilter filter ) throws UsbException
	{
		write( Request.BASEBAND_FILTER_BANDWIDTH_SET, 
			   filter.getLowValue(), 
			   filter.getHighValue() );
	}

	/**
	 * Enables (true) or disables (false) the amplifier
	 */
	public void setAmplifierEnabled( boolean enabled ) throws UsbException
	{
		write( Request.AMP_ENABLE, ( enabled ? 1 : 0 ), 0 );
	}

	/**
	 * Sets the IF LNA Gain
	 */
	public void setLNAGain( HackRFLNAGain gain ) throws UsbException
	{
		int result = readByte( Request.SET_LNA_GAIN, 0, gain.getValue(), true );
		
		if( result != 1 )
		{
			throw new UsbException( "couldn't set lna gain to " + gain );
		}
	}
	
	/**
	 * Sets the Baseband VGA Gain
	 */
	public void setVGAGain( HackRFVGAGain gain ) throws UsbException
	{
		int result = readByte( Request.SET_VGA_GAIN, 0, gain.getValue(), true );
		
		if( result != 1 )
		{
			throw new UsbException( "couldn't set vga gain to " + gain );
		}
	}

	/**
	 * Not implemented
	 */
    public long getTunedFrequency() throws SourceException
    {
		return 0;
    }

	@Override
    public void setTunedFrequency( long frequency ) throws SourceException
    {
		int mhz = (int)( frequency / 1E6 );
		int hz = (int)( frequency - ( mhz * 1E6 ) );
		
		byte[] data = new byte[ 8 ];
		
		EndianUtils.writeSwappedInteger( data, 0, mhz );
		EndianUtils.writeSwappedInteger( data, 4, hz );

		try
		{
			write( Request.SET_FREQUENCY, 0, 0, data );
		}
		catch( UsbException e )
		{
			mLog.error( "error setting frequency [" + frequency + "]", e );
			
			throw new SourceException( "error setting frequency [" + 
					frequency + "]", e );
		}
	    
    }

	@Override
    public int getCurrentSampleRate() throws SourceException
    {
	    return mSampleRate.getRate();
    }

	@Override
    public void apply( TunerConfiguration config ) throws SourceException
    {
		if( config instanceof HackRFTunerConfiguration )
		{
			HackRFTunerConfiguration hackConfig = 
					(HackRFTunerConfiguration)config;
			
			try
            {
	            setSampleRate( hackConfig.getSampleRate() );
	            setFrequencyCorrection( hackConfig.getFrequencyCorrection() );
	            setLNAGain( hackConfig.getLNAGain() );
	            setVGAGain( hackConfig.getVGAGain() );
	            setFrequency( getFrequency() );
            }
            catch ( UsbException e )
            {
            	throw new SourceException( "Error while applying tuner "
            			+ "configuration", e );
            }
		}
		else
		{
			throw new IllegalArgumentException( "Invalid tuner configuration "
					+ "type [" + config.getClass() + "]" );
		}
    }
	
	public byte[] readArray( Request request, 
							 int value, 
							 int index, 
							 int length ) throws UsbException
	{
		UsbControlIrp irp = mUSBDevice.createUsbControlIrp( REQUEST_TYPE_IN, 
				request.getRequestNumber(), (short)value, (short)index );

		byte[] data = new byte[ length ];
		
		irp.setData( data );
		
		mUSBDevice.syncSubmit( irp );
		
		return data;
	}
	
	public int read( Request request, int value, int index, int length ) 
							throws UsbException
	{
		if( !( length == 1 || length == 2 || length == 4 ) )
		{
			throw new IllegalArgumentException( "invalid length [" + length + 
					"] must be: byte=1, short=2, int=3 to read a primitive" );
		}
		
		byte[] data = readArray( request, value, index, length );
		
		switch( data.length )
		{
			case 1:
				return data[ 0 ];
			case 2:
				return EndianUtils.readSwappedShort( data, 0 );
			case 4:
				return EndianUtils.readSwappedInteger( data, 0 );
			default:
				throw new UsbException( "read() primitive returned an "
					+ "unrecognized byte array " + Arrays.toString( data ) );
		}
	}
	
	public int readByte( Request request, int value, int index, boolean signed ) 
									throws UsbException
	{
		byte[] data = readArray( request, value, index, 1 );

		if( signed )
		{
			return (int)( data[ 0 ] );
		}
		else
		{
			return (int)( data[ 0 ] & 0xFF );
		}
	}
	
	public void write( Request request, 
					   int value, 
					   int index, 
					   byte[] data ) throws UsbException
	{
		UsbControlIrp irp = mUSBDevice.createUsbControlIrp( REQUEST_TYPE_OUT, 
					request.getRequestNumber(), (short)value, (short)index );

		irp.setData( data );
		
		mUSBDevice.syncSubmit( irp );
	}

	/**
	 * Sends a request that doesn't have a data payload
	 */
	public void write( Request request, 
					   int value, 
					   int index ) throws UsbException
	{
		write( request, value, index, new byte[ 0 ] );
	}

	/**
	 * Sample Rate
	 * 
	 *  Note: this is direct translation of the libhackrf code, because I still
	 *  can't figure out how it works ... one day, maybe.
	 */
	public void setSampleRate( HackRFSampleRate rate ) throws UsbException
	{
//		int MAX_N = 32;
//		
//		double freq = (double)rate.getRate() / 1E6d;
//		
//		double freq_frac = 1.0d + freq - (int)freq;
//		
//		long u64 = Double.doubleToLongBits( freq );
//		
//		int e = (int)( ( u64 >> 52 ) - 1023 );
//		
//		long m = (long)( 1l << 52 ) - 1l;
//		
//		u64 = Double.doubleToLongBits( freq_frac );
//		
//		u64 &= m;
//		
//		m &= ~( ( 1 << ( e + 4 ) ) - 1 );
//
//		long a = 0;
//
//		int x;
//		
//		for( x = 1; x < MAX_N; x++ )
//		{
//			a += u64;
//
//			if( ( ( a & m ) == 0 ) || ( ( ~a & m ) == 0 ) )
//			{
//				break;
//			}
//		}
//
//		if( x == MAX_N )
//		{
//			x = 1;
//		}
//
//		int freq_hz = (int)( freq * (double)x * 0.5d );
//		
//		setSampleRateManual( freq_hz, x );

		setSampleRateManual( rate.getRate(), 1 );
		
		mFrequencyController.setSampleRate( rate.getRate() );
		
		mBufferSize = rate.getBufferSize();
		
		setBasebandFilter( rate.getFilter() );
		
		mSampleRate = rate;
	}
	
	public void setSampleRateManual( int frequency, int divider ) 
							throws UsbException
	{
		byte[] data = new byte[ 8 ];
		
		EndianUtils.writeSwappedInteger( data, 0, frequency );
		EndianUtils.writeSwappedInteger( data, 4, divider );

		write( Request.SET_SAMPLE_RATE, 0, 0, data );
	}
	
	public int getSampleRate()
	{
		return mSampleRate.getRate();
	}
	
	public enum Request
	{
		SET_TRANSCEIVER_MODE( 1 ),
		MAX2837_TRANSCEIVER_WRITE( 2 ),
		MAX2837_TRANSCEIVER_READ( 3 ),
		SI5351C_CLOCK_GENERATOR_WRITE( 4 ),
		SI5351C_CLOCK_GENERATOR_READ( 5 ),
		SET_SAMPLE_RATE( 6 ),
		BASEBAND_FILTER_BANDWIDTH_SET( 7 ),
		RFFC5071_MIXER_WRITE( 8 ),
		RFFC5071_MIXER_READ( 9 ),
		SPIFLASH_ERASE( 10 ),
		SPIFLASH_WRITE( 11 ),
		SPIFLASH_READ( 12 ),
		BOARD_ID_READ( 14 ),
		VERSION_STRING_READ( 15 ),
		SET_FREQUENCY( 16 ),
		AMP_ENABLE( 17 ),
		BOARD_PARTID_SERIALNO_READ( 18 ),
		SET_LNA_GAIN( 19 ),
		SET_VGA_GAIN( 20 ),
		SET_TXVGA_GAIN( 21 ),
		ANTENNA_ENABLE( 23 ),
		SET_FREQUENCY_EXPLICIT( 24 );
		
		private byte mRequestNumber;
		
		private Request( int number )
		{
			mRequestNumber = (byte)number;
		}
		
		public byte getRequestNumber()
		{
			return mRequestNumber;
		}
	}

	public enum HackRFSampleRate
	{
		RATE2_016MHZ(   2016000,  65536,  "2.016 MHz", BasebandFilter.F2_50 ),
		RATE3_024MHZ(   3024000,  98304,  "3.024 MHz", BasebandFilter.F3_50 ),
		RATE4_464MHZ(   4464000, 131072,  "4.464 MHz", BasebandFilter.F5_00 ),
		RATE5_472MHZ(   5472000, 196608,  "5.472 MHz", BasebandFilter.F6_00 ),
		RATE7_488MHZ(   7488000, 262144,  "7.488 MHz", BasebandFilter.F8_00 ),
		RATE10_032MHZ(  9984000, 131072, " 9.984 MHz", BasebandFilter.F10_00 ),
		RATE12_000MHZ( 12000000, 262144, "12.000 MHz", BasebandFilter.F12_00 ),
		RATE13_488MHZ( 13488000, 262144, "13.488 MHz", BasebandFilter.F14_00 ),
		RATE14_976MHZ( 14976000, 262144, "14.976 MHz", BasebandFilter.F15_00 ),
		RATE19_968MHZ( 19968000, 262144, "19.968 MHz", BasebandFilter.F20_00 );
		
		private int mRate;
		private int mBufferSize;
		private String mLabel;
		private BasebandFilter mFilter;
		
		private HackRFSampleRate( int rate, 
								  int bufferSize, 
								  String label, 
								  BasebandFilter filter )
		{
			mRate = rate;
			mBufferSize = bufferSize;
			mLabel = label;
			mFilter = filter;
		}
		
		public int getRate()
		{
			return mRate;
		}
		
		public int getBufferSize()
		{
			return mBufferSize;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public String toString()
		{
			return mLabel;
		}
		
		public BasebandFilter getFilter()
		{
			return mFilter;
		}
	}
	
	public enum BasebandFilter
	{
		F1_75(   1750000,  "1.75 MHz" ),
		F2_50(   2500000,  "2.50 MHz" ),
		F3_50(   3500000,  "3.50 MHz" ),
		F5_00(   5000000,  "5.00 MHz" ),
		F5_50(   5500000,  "5.50 MHz" ),
		F6_00(   6000000,  "6.00 MHz" ),
		F7_00(   7000000,  "7.00 MHz" ),
		F8_00(   8000000,  "8.00 MHz" ),
		F9_00(   9000000,  "9.00 MHz" ),
		F10_00( 10000000, "10.00 MHz" ),
		F12_00( 12000000, "12.00 MHz" ),
		F14_00( 14000000, "14.00 MHz" ),
		F15_00( 15000000, "15.00 MHz" ),
		F20_00( 20000000, "20.00 MHz" ),
		F24_00( 24000000, "24.00 MHz" ),
		F28_00( 28000000, "28.00 MHz" );
		
		private int mBandwidth;
		private String mLabel;
		
		private BasebandFilter( int bandwidth, String label )
		{
			mBandwidth = bandwidth;
			mLabel = label;
		}
		
		public int getBandwidth()
		{
			return mBandwidth;
		}
		
		public int getHighValue()
		{
			return mBandwidth >> 16;
		}
		
		public int getLowValue()
		{
			return mBandwidth & 0xFFFF;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
	}
	
	public enum BoardID
	{
		JELLYBEAN(  0x00, "HackRF Jelly Bean" ),
		JAWBREAKER( 0x01, "HackRF Jaw Breaker" ),
		HACKRF_ONE( 0x02, "HackRF One" ),
		INVALID(    0xFF, "HackRF Unknown Board" );
		
		private byte mIDNumber;
		private String mLabel;
		
		private BoardID( int number, String label )
		{
			mIDNumber = (byte)number;
			mLabel = label;
		}
		
		public String toString()
		{
			return mLabel;
		}
		
		public String getLabel()
		{
			return mLabel;
		}
		
		public byte getNumber()
		{
			return mIDNumber;
		}
		
		public static BoardID lookup( int value )
		{
			switch( value )
			{
				case 0:
					return JELLYBEAN;
				case 1:
					return JAWBREAKER;
				case 2:
					return HACKRF_ONE;
				default:
					return INVALID;
			}
		}
	}
	
	public enum Mode
	{
		OFF( 0, "Off" ),
		RECEIVE( 1, "Receive" ),
		TRANSMIT( 2, "Transmit" ),
		SS( 3, "SS" );
		
		private byte mNumber;
		private String mLabel;
		
		private Mode( int number, String label )
		{
			mNumber = (byte)number;
			mLabel = label;
		}
		
		public byte getNumber()
		{
			return mNumber;
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
	
	public enum HackRFLNAGain
	{
		GAIN_0( 0 ),
		GAIN_8( 8 ),
		GAIN_16( 16 ),
		GAIN_24( 24 ),
		GAIN_32( 32 ),
		GAIN_40( 40 );
		
		private int mValue;
		
		private HackRFLNAGain( int value )
		{
			mValue = value;
		}
		
		public int getValue()
		{
			return mValue;
		}
		
		public String toString()
		{
			return String.valueOf( mValue ) + " dB";
		}
	}

	/**
	 * Receive (baseband) VGA Gain values
	 */
	public enum HackRFVGAGain
	{
		GAIN_0( 0 ),
		GAIN_2( 2 ),
		GAIN_4( 4 ),
		GAIN_6( 6 ),
		GAIN_8( 8 ),
		GAIN_10( 10 ),
		GAIN_12( 12 ),
		GAIN_14( 14 ),
		GAIN_16( 16 ),
		GAIN_18( 18 ),
		GAIN_20( 20 ),
		GAIN_22( 22 ),
		GAIN_23( 24 ),
		GAIN_26( 26 ),
		GAIN_28( 28 ),
		GAIN_30( 30 ),
		GAIN_32( 32 ),
		GAIN_34( 34 ),
		GAIN_36( 36 ),
		GAIN_38( 38 ),
		GAIN_40( 40 ),
		GAIN_42( 42 ),
		GAIN_44( 44 ),
		GAIN_46( 46 ),
		GAIN_48( 48 ),
		GAIN_50( 50 ),
		GAIN_52( 52 ),
		GAIN_54( 54 ),
		GAIN_56( 56 ),
		GAIN_58( 58 ),
		GAIN_60( 60 ),
		GAIN_62( 62 );
		
		private int mValue;
		
		private HackRFVGAGain( int value )
		{
			mValue = value;
		}
		
		public int getValue()
		{
			return mValue;
		}
		
		public String toString()
		{
			return String.valueOf( mValue ) + " dB";
		}
	}
	
	/**
	 * HackRF part id and serial number parsing class
	 */
	public class Serial
	{
		private byte[] mData;
		
		public Serial( byte[] data )
		{
			mData = data;
		}
		
		public String getPartID()
		{
			int part0 = EndianUtils.readSwappedInteger( mData, 0 );
			int part1 = EndianUtils.readSwappedInteger( mData, 4 );

			StringBuilder sb = new StringBuilder();
			
			sb.append( String.format( "%08X", part0 ) );
			sb.append( "-" );
			sb.append( String.format( "%08X", part1 ) );
		
			return sb.toString();
		}
		
		public String getSerialNumber()
		{
			int serial0 = EndianUtils.readSwappedInteger( mData, 8 );
			int serial1 = EndianUtils.readSwappedInteger( mData, 12 );
			int serial2 = EndianUtils.readSwappedInteger( mData, 16 );
			int serial3 = EndianUtils.readSwappedInteger( mData, 20 );

			StringBuilder sb = new StringBuilder();
			
			sb.append( String.format( "%08X", serial0 ) );
			sb.append( "-" );
			sb.append( String.format( "%08X", serial1 ) );
			sb.append( "-" );
			sb.append( String.format( "%08X", serial2 ) );
			sb.append( "-" );
			sb.append( String.format( "%08X", serial3 ) );
		
			return sb.toString();
		}
	}
	
	/**
	 * Adds a sample listener.  If the buffer processing thread is
	 * not currently running, starts it running in a new thread.
	 */
    public void addListener( Listener<Float[]> listener )
    {
		mSampleListeners.add( listener );

		System.out.println( "HackRF - listener added - count:" + mSampleListeners.size() );
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
	 * Dispatches the sample array to each registered listener. If there is more
	 * than one listener, they receive a copy of the samples.  If there is only
	 * one listener, or the last listener, they receive the original sample array.
	 * 
	 * This is to facilitate garbage collection of the array when the listener
	 * is done processing the samples.
	 */
    public void broadcast( Float[] samples )
    {
    	Iterator<Listener<Float[]>> it = mSampleListeners.iterator();

    	while( it.hasNext() )
    	{
        	Listener<Float[]> next = it.next();
			
			/* if this is the last (or only) listener, send the original 
			 * buffer, otherwise send a copy of the buffer */
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
//		private ScheduledFuture<?> mSampleRateCounterTask;
		
		private AtomicBoolean mRunning = new AtomicBoolean();
		private AtomicBoolean mResetting = new AtomicBoolean();
		private Boolean mTransitioning = false;

        public void start()
        {
			synchronized( mTransitioning )
			{
				if( mRunning.compareAndSet( false, true ) )
				{
					mLog.debug( "HackRF - starting sample fetch thread" );

					ArrayList<UsbIrp> irps = new ArrayList<UsbIrp>();
					
					for( int x = 0; x < USB_IRP_POOL_SIZE; x++ )
					{
						UsbIrp irp = mUSBPipe.createUsbIrp();
						irp.setData( new byte[ mBufferSize ] );
						irps.add( irp );
					}

					try
		            {
						if( mUSBPipe.isActive() )
						{
							if( !mUSBPipe.isOpen() )
							{
								mUSBPipe.open();
								
								mUSBPipe.addUsbPipeListener( this );
							}
							
							if( mUSBPipe.isOpen() )
							{
					            mSampleDispatcherTask = mExecutor
				            		.scheduleAtFixedRate( new BufferDispatcher(), 
	            							  0, 20, TimeUnit.MILLISECONDS );
					            
//					            mSampleRateMonitor = 
//				            		new SampleRateMonitor( mSampleRate.getRate() );
					            
//					            mSampleRateCounterTask = mExecutor
//					            		.scheduleAtFixedRate( mSampleRateMonitor, 
//	            							  10, 10, TimeUnit.SECONDS );

					            mUSBPipe.asyncSubmit( irps );
							}
						}
		            }
		            catch ( Exception e )
		            {
		            	mLog.error( "Error in buffer processor thread", e );
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
					mLog.debug( "HackRF - stopping sample fetch thread" );

					mUSBPipe.abortAllSubmissions();
					
					if( mSampleDispatcherTask != null )
					{
						mSampleDispatcherTask.cancel( true );
//						mSampleRateCounterTask.cancel( true );
//						mRawSampleBuffer.clear();
//						mSampleCounter.set( 0 );
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
		 * Receives filled data buffers and places them in the queue for the 
		 * BufferDispatcher to process
		 */
		@Override
        public synchronized void dataEventOccurred( UsbPipeDataEvent event )
        {
			mReceivedBuffers.add( event.getUsbIrp() );
        }

		@Override
        public void errorEventOccurred( UsbPipeErrorEvent e )
        {
			/* When an error occurs, we'll get errors on all of the 
			 * queued IRPS in succession.  We use the mResetting to control
			 * the reset process and ignore all subsequent errors */
			if( mResetting.compareAndSet( false, true ) )
			{
				mLog.error( "error during data transfer resetting USB pipe to "
						+ "HackRF", e );
				
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
	 * Fetches received UsbIrp buffers from the received queue, dispatches them
	 * and resubmits the processed buffer for more samples
	 */
	public class BufferDispatcher implements Runnable
	{
		@Override
        public void run()
        {
			ArrayList<UsbIrp> buffers = new ArrayList<UsbIrp>();
			
			mReceivedBuffers.drainTo( buffers );
			
			for( UsbIrp buffer: buffers )
			{
				Float[] samples = mSampleAdapter.convert( buffer.getData() );
				
				broadcast( samples );
				
				if( mBufferProcessor != null && mBufferProcessor.isRunning() )
				{
					try
		            {
						/* We adjust the buffer size according to sample rate.
						 * If this byte array matches the current buffer size
						 * then we reuse the byte array, otherwise create a 
						 * new one. */
						if( buffer.getData().length == mBufferSize )
						{
			                mUSBPipe.asyncSubmit( buffer.getData() );
						}
						else
						{
			                mUSBPipe.asyncSubmit( new byte[ mBufferSize ] );
						}
		            }
		            catch ( UsbNotActiveException | UsbNotOpenException
		                    | IllegalArgumentException | UsbDisconnectedException
		                    | UsbException e )
		            {
		            	mLog.error( "error submitting a byte array to the usb "
		            			+ "pipe for samples", e );
		            }
				}
			}
        }
	}
}

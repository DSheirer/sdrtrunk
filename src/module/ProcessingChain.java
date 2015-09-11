/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
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
package module;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import message.IMessageListener;
import message.IMessageProvider;
import message.Message;
import module.decode.Decoder;
import module.decode.event.CallEvent;
import module.decode.event.ICallEventListener;
import module.decode.event.ICallEventProvider;
import module.decode.state.DecoderStateEvent;
import module.decode.state.DecoderStateEvent.Event;
import module.decode.state.IDecoderStateEventListener;
import module.decode.state.IDecoderStateEventProvider;
import module.decode.state.State;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Broadcaster;
import sample.Listener;
import sample.complex.ComplexBuffer;
import sample.complex.IComplexBufferListener;
import sample.real.IRealBufferListener;
import sample.real.IRealBufferProvider;
import sample.real.RealBuffer;
import source.ComplexSource;
import source.RealSource;
import source.Source;
import source.SourceException;
import source.tuner.TunerChannelSource;
import source.tuner.frequency.FrequencyCorrectionControl;
import source.tuner.frequency.IFrequencyCorrectionController;
import audio.AudioPacket;
import audio.IAudioPacketListener;
import audio.IAudioPacketProvider;
import audio.metadata.IMetadataListener;
import audio.metadata.IMetadataProvider;
import audio.metadata.Metadata;
import audio.squelch.ISquelchStateListener;
import audio.squelch.ISquelchStateProvider;
import audio.squelch.SquelchState;
import controller.ThreadPoolManager;
import controller.ThreadPoolManager.ThreadType;
import controller.channel.ChannelEvent;
import controller.channel.IChannelEventListener;
import controller.channel.IChannelEventProvider;

/**
 * Processing chain provides a framework for connecting a complex or real sample
 * source to a set of one primary decoder and zero or more auxiliary decoders.  
 * All decoded messages and call events produced by the decoders and the decoder
 * call states are aggregated by the various broadcasters.  You can register
 * listeners to receive aggregated messages, call events, and audio packets.
 * 
 * Normal setup sequence:
 * 
 * 1) Add one or more modules
 * 2) Register listeners to receive messages, call events, audio, etc.
 * 3) Add a valid source
 * 4) Invoke the start() method to start processing.
 * 5) Invoke the stop() method to stop processing.
 * 
 * Optional: if you want to reuse the processing chain with a new sample source,
 * invoke the following method sequence:  stop(), setSource(), start()
 */
public class ProcessingChain implements IChannelEventListener
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( ProcessingChain.class );

	private BufferReceiver<ComplexBuffer> mComplexReceiver = new BufferReceiver<>( 5 );
	private BufferReceiver<RealBuffer> mRealReceiver = new BufferReceiver<>( 5 );
	
	private Broadcaster<AudioPacket> mAudioPacketBroadcaster = new Broadcaster<>();
	private Broadcaster<Metadata> mMetadataBroadcaster = new Broadcaster<>();
	private Broadcaster<CallEvent> mCallEventBroadcaster = new Broadcaster<>();
	private Broadcaster<ChannelEvent> mChannelEventBroadcaster = new Broadcaster<>();
	private Broadcaster<ComplexBuffer> mComplexBufferBroadcaster = new Broadcaster<>();
	private Broadcaster<DecoderStateEvent> mDecoderStateEventBroadcaster = new Broadcaster<>();
	private Broadcaster<Message> mMessageBroadcaster = new Broadcaster<>();
	private Broadcaster<RealBuffer> mRealBufferBroadcaster = new Broadcaster<>();
	private Broadcaster<SquelchState> mSquelchStateBroadcaster = new Broadcaster<>();
	
	private ThreadPoolManager mThreadPoolManager;
	private ScheduledFuture<?> mBufferProcessingTask;
	private AtomicBoolean mRunning = new AtomicBoolean();
	
	private Source mSource;
	private List<Module> mModules = new ArrayList<>();
	private IFrequencyCorrectionController mFrequencyCorrectionController;
	
	public ProcessingChain()
	{
		this( new ThreadPoolManager() );
	}

	public ProcessingChain( ThreadPoolManager threadManager )
	{
		mThreadPoolManager = threadManager;
	}
	
	public void dispose()
	{
		stop();
		
		for( Module module: mModules )
		{
			module.dispose();
		}
		
		mModules.clear();
		
		mComplexReceiver.dispose();
		mComplexReceiver = null;
		
		mRealReceiver.dispose();
		mRealReceiver = null;
		
		mThreadPoolManager = null;
		mBufferProcessingTask = null;
		mFrequencyCorrectionController = null;
		
		mAudioPacketBroadcaster.dispose();
		mCallEventBroadcaster.dispose();
		mChannelEventBroadcaster.dispose();
		mComplexBufferBroadcaster.dispose();
		mMessageBroadcaster.dispose();
		mRealBufferBroadcaster.dispose();
		mSquelchStateBroadcaster.dispose();
	}

	/**
	 * Broadcasts the metadata to any registered listeners
	 */
	public void broadcast( Metadata metadata )
	{
		if( mMetadataBroadcaster != null )
		{
			mMetadataBroadcaster.broadcast( metadata );
		}
	}
	
	/**
	 * Indicates if this processing chain is currently receiving samples from
	 * a source and sending those samples to the decoders.
	 */
	public boolean processing()
	{
		return mRunning.get();
	}

	/**
	 * Indicates if this chain currently has a valid sample source.
	 */
	public boolean hasSource()
	{
		return mSource != null;
	}

	/**
	 * Indicates if one of the modules provides frequency error control.
	 */
	public boolean hasFrequencyCorrectionControl()
	{
		return mFrequencyCorrectionController != null &&
			   mFrequencyCorrectionController.hasFrequencyCorrectionControl();
	}

	/**
	 * Frequency correction controller.  Can be null.
	 */
	public FrequencyCorrectionControl getFrequencyCorrectionControl()
	{
		if( mFrequencyCorrectionController != null )
		{
			return mFrequencyCorrectionController.getFrequencyCorrectionControl();
		}
		
		return null;
	}

	/**
	 * Applies a sample source to this processing chain.  Processing won't 
	 * start until the start() method is invoked.
	 * 
	 * @param source - real or complex sample source
	 * 
	 * @throws IllegalStateException if the processing chain is currently 
	 * processing with another source.  Invoke stop() before applying a new
	 * source.
	 */
	public void setSource( Source source ) throws IllegalStateException
	{
		if( processing() )
		{
			throw new IllegalStateException( "Processing chain is currently "
				+ "processing.  Invoke stop() on the processing chain before "
				+ "applying a new sample source" );
		}
		
		mSource = source;

		/* Establish two-way communication between the tuner channel source and
		 * the decoder for fine tuning frequency error correction feedback from 
		 * the decoder.  The tuner channel source will also notify the frequency 
		 * controller during frequency changes to allow the controller to reset 
		 * frequency error tracking */
		if( mSource != null &&  
			mSource instanceof TunerChannelSource &&
			hasFrequencyCorrectionControl() )
		{
			TunerChannelSource channel = (TunerChannelSource)mSource;

			FrequencyCorrectionControl control = getFrequencyCorrectionControl();
			
			channel.setFrequencyChangeListener( control );
			control.setFrequencyChangeListener( channel );
		}
	}

	/**
	 * List of current modules for this processing chain
	 */
	public List<Module> getModules()
	{
		return mModules;
	}

	/**
	 * List of decoders for this processing chain
	 */
	public List<Decoder> getDecoders()
	{
		List<Decoder> decoders = new ArrayList<>();
		
		for( Module module: mModules )
		{
			if( module instanceof Decoder )
			{
				decoders.add( (Decoder)module );
			}
		}
		
		return decoders;
	}
	
	/**
	 * Adds a module to the processing chain.  Each module is tested for the
	 * interfaces that it supports and is registered or receives a listener
	 * to consume or produce the supported interface data type.  All elements
	 * that are produced by any component are automatically routed to all other
	 * components that support the corresponding listener interface.
	 * 
	 * At least one module should consume complex samples and either produce
	 * decoded messages and/or audio, or produce decoded real sample buffers
	 * for all other modules to consume.
	 * 
	 * @param module - processing module, demodulator or decoder
	 */
	public void addModule( Module module )
	{
		mModules.add( module );

		/* >>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Listeners <<<<<<<<<<<<<<<<<<<<<<<<<<< */
		if( module instanceof IAudioPacketListener )
		{
			mAudioPacketBroadcaster.addListener( 
					((IAudioPacketListener)module).getAudioPacketListener() );
		}
		
		if( module instanceof ICallEventListener )
		{
			mCallEventBroadcaster.addListener( 
					((ICallEventListener)module).getCallEventListener() );
		}
		
		if( module instanceof IChannelEventListener )
		{
			mChannelEventBroadcaster.addListener( 
					((IChannelEventListener)module).getChannelEventListener() );
		}
		
		if( module instanceof IComplexBufferListener )
		{
			mComplexBufferBroadcaster.addListener( 
				((IComplexBufferListener)module).getComplexBufferListener() );
		}
		
		if( module instanceof IDecoderStateEventListener )
		{
			mDecoderStateEventBroadcaster.addListener( 
				((IDecoderStateEventListener)module).getDecoderStateListener() );
		}

		if( module instanceof IMessageListener )
		{
			mMessageBroadcaster.addListener( 
					((IMessageListener)module).getMessageListener() );
		}
		
		if( module instanceof IMetadataListener )
		{
			mMetadataBroadcaster.addListener( 
				((IMetadataListener)module).getMetadataListener() );
		}
		
		if( module instanceof IRealBufferListener )
		{
			mRealBufferBroadcaster.addListener( 
				((IRealBufferListener)module).getRealBufferListener() );
		}

		if( module instanceof ISquelchStateListener )
		{
			mSquelchStateBroadcaster.addListener( 
				((ISquelchStateListener)module).getSquelchStateListener() );
		}
		
		/* >>>>>>>>>>>>>>>>>>> Providers <<<<<<<<<<<<<<<<<<<<<<<<< */
		if( module instanceof IAudioPacketProvider )
		{
			((IAudioPacketProvider)module).setAudioPacketListener( 
					mAudioPacketBroadcaster );
		}
		
		if( module instanceof ICallEventProvider )
		{
			((ICallEventProvider)module).setCallEventListener( mCallEventBroadcaster );
		}

		if( module instanceof IChannelEventProvider )
		{
			((IChannelEventProvider)module).setChannelEventListener( 
					mChannelEventBroadcaster );
		}
		
		if( module instanceof IDecoderStateEventProvider )
		{
			((IDecoderStateEventProvider)module).setDecoderStateListener( 
					mDecoderStateEventBroadcaster );
		}
		
		if( module instanceof IFrequencyCorrectionController )
		{
			mFrequencyCorrectionController = (IFrequencyCorrectionController)module;
		}

		if( module instanceof IMessageProvider )
		{
			((IMessageProvider)module).addMessageListener( mMessageBroadcaster );
		}
		
		if( module instanceof IMetadataProvider )
		{
			((IMetadataProvider)module).setMetadataListener( 
					mMetadataBroadcaster );
		}

		if( module instanceof IRealBufferProvider )
		{
			((IRealBufferProvider)module).setRealBufferListener( mRealBufferBroadcaster );
		}
		
		if( module instanceof ISquelchStateProvider )
		{
			((ISquelchStateProvider)module).setSquelchStateListener( 
					mSquelchStateBroadcaster );
		}
	}
	
	/**
	 * Adds the list of modules to this processing chain
	 */
	public void addModules( List<Module> modules )
	{
		for( Module module: modules )
		{
			addModule( module );
		}
	}

	
	/**
	 * Starts processing if the chain has a valid source.  Invocations on an
	 * already started chain have no effect. 
	 */
	public void start()
	{
		if( mRunning.compareAndSet( false, true ) )
		{
			if( mSource != null )
			{
				/* Reset each of the modules */
				for( Module module: mModules )
				{
					module.reset();
				}
				
				/* Start each of the modules */
				for( Module module: mModules )
				{
					module.start();
				}

				/* Register with the source to receive sample data.  Setup a 
				 * timer task to process the buffer queues 50 times a second 
				 * (every 20 ms) */
				mBufferProcessingTask = mThreadPoolManager.scheduleFixedRate( 
					ThreadType.DECODER, new BufferProcessor(), 20, TimeUnit.MILLISECONDS );

				switch( mSource.getSampleType() )
				{
					case COMPLEX:
						((ComplexSource)mSource).setListener( mComplexBufferBroadcaster );
						break;
					case REAL:
						((RealSource)mSource).setListener( mRealBufferBroadcaster );
						break;
					default:
						throw new IllegalArgumentException( "Unrecognized source "
							+ "sample type - cannot start processing chain" );
				}
				
				/* If this is a tuner source, broadcast the frequency to all 
				 * of the decoder state's */
				if( mSource instanceof TunerChannelSource )
				{
					try
					{
						long frequency = ((TunerChannelSource)mSource).getFrequency();
						
						mDecoderStateEventBroadcaster.broadcast( 
							new DecoderStateEvent( this, Event.SOURCE_FREQUENCY, 
									State.IDLE, frequency ) );
					}
					catch( SourceException e )
					{
						mLog.error( "Error getting frequency from tuner channel source", e );
					}
				}
			}
		}
	}

	/**
	 * Stops processing if the chain is currently processing.  Invocations on 
	 * an already stopped chain have no effect.
	 */
	public void stop()
	{
		if( mRunning.compareAndSet( true, false ) )
		{
			if( mSource != null )
			{
				switch( mSource.getSampleType() )
				{
					case COMPLEX:
						((ComplexSource)mSource).removeListener( mComplexBufferBroadcaster );
						break;
					case REAL:
						((RealSource)mSource).removeListener( mRealBufferBroadcaster );
						break;
					default:
						throw new IllegalArgumentException( "Unrecognized source "
							+ "sample type - cannot start processing chain" );
				}

				/* Release the source */
				mSource.dispose();
				mSource = null;
			}
			
			if( mBufferProcessingTask != null )
			{
				mThreadPoolManager.cancel( mBufferProcessingTask );
				mBufferProcessingTask = null;
			}
			
			/* Stop each of the modules */
			for( Module module: mModules )
			{
				module.stop();
			}
		}
	}
	
	/**
	 * Adds the listener to receive audio packets from all modules.
	 */
	public void addAudioPacketListener( Listener<AudioPacket> listener )
	{
		mAudioPacketBroadcaster.addListener( listener );
	}
	
	public void removeAudioPacketListener( Listener<AudioPacket> listener )
	{
		mAudioPacketBroadcaster.removeListener( listener );
	}
	
	/**
	 * Adds the listener to receive call events from all modules.
	 */
	public void addCallEventListener( Listener<CallEvent> listener )
	{
		mCallEventBroadcaster.addListener( listener );
	}
	
	public void removeCallEventListener( Listener<CallEvent> listener )
	{
		mCallEventBroadcaster.removeListener( listener );
	}
	
	/**
	 * Adds the listener to receive call events from all modules.
	 */
	public void addChannelEventListener( Listener<ChannelEvent> listener )
	{
		mChannelEventBroadcaster.addListener( listener );
	}
	
	public void removeChannelEventListener( Listener<ChannelEvent> listener )
	{
		mChannelEventBroadcaster.removeListener( listener );
	}

	/**
	 * Adds the listener to receive decoder state events from decoder modules
	 */
	public void addDecoderStateEventListener( Listener<DecoderStateEvent> listener )
	{
		mDecoderStateEventBroadcaster.addListener( listener );
	}
	
	public void removeDecoderStateEventListener( Listener<DecoderStateEvent> listener)
	{
		mDecoderStateEventBroadcaster.removeListener( listener );
	}
	
	public Listener<DecoderStateEvent> getDecoderStateEventListener()
	{
		return mDecoderStateEventBroadcaster;
	}
	
	/**
	 * Adds the listener to receive decoded messages from all decoders.
	 */
	public void addMessageListener( Listener<Message> listener )
	{
		mMessageBroadcaster.addListener( listener );
	}
	
	/**
	 * Adds the list of listeners to receive decoded messages from all decoders.
	 */
	public void addMessageListeners( List<Listener<Message>> listeners )
	{
		for( Listener<Message> listener: listeners )
		{
			mMessageBroadcaster.addListener( listener );
		}
	}
	
	public void removeMessageListener( Listener<Message> listener )
	{
		mMessageBroadcaster.removeListener( listener );
	}
	
	/**
	 * Adds the listener to receive call events from all modules.
	 */
	public void addSquelchStateListener( Listener<SquelchState> listener )
	{
		mSquelchStateBroadcaster.addListener( listener );
	}
	
	public void removeSquelchStateListener( Listener<SquelchState> listener )
	{
		mSquelchStateBroadcaster.removeListener( listener );
	}
	
	public void addRealBufferListener( Listener<RealBuffer> listener )
	{
		mRealBufferBroadcaster.addListener( listener );
	}
	
	public void removeRealBufferListener( Listener<RealBuffer> listener )
	{
		mRealBufferBroadcaster.removeListener( listener );
	}
	
	
    /**
     * Processes complex and real sample buffers and broadcasts buffers to any
     * registered listeners.
     */
	private class BufferProcessor implements Runnable
	{
		private AtomicBoolean mProcessing = new AtomicBoolean();
		
		@Override
        public void run()
        {
			/* Allow only one instance of this processor to run to ensure 
			 * correct serial processing of incoming buffers.  Overlapping 
			 * invocations of this processor by the timer service will end 
			 * immediately without doing anything for cases where a single 
			 * iteration runs longer than the scheduled processing interval */
			if( mProcessing.compareAndSet( false, true ) )
			{
				try
				{
					for( ComplexBuffer buffer: mComplexReceiver.get() )
					{
						mComplexBufferBroadcaster.broadcast( buffer );
					}
				}
				catch( Exception e )
				{
					mLog.error( "Error processing complex sample buffers", e );
				}
				
				try
				{
					for( RealBuffer buffer: mRealReceiver.get() )
					{
						mRealBufferBroadcaster.broadcast( buffer );
					}
				}
				catch( Exception e )
				{
					mLog.error( "Error processing real sample buffers", e );
				}
				
				mProcessing.set( false );
			}
        }
	}
	
	/**
	 * Buffer receiver - non-blocking receiver to allow calling thread to return 
	 * without blocking on any internal processing.  This ensures that we don't
	 * impair any receiver sample processing.
	 */
	private class BufferReceiver<T> implements Listener<T>
	{
		private LinkedTransferQueue<T> mQueue = new LinkedTransferQueue<>();
		private int mMaxFetchSize;
		
		public BufferReceiver( int maxFetchSize )
		{
			mMaxFetchSize = maxFetchSize;
		}
		
		@Override
        public void receive( T t )
        {
			mQueue.add( t );
        }
		
		public List<T> get()
		{
			List<T> buffers = new ArrayList<>();

			mQueue.drainTo( buffers, mMaxFetchSize );

			return buffers;
		}
		
		public void dispose()
		{
			mQueue.clear();
			mQueue = null;
		}
	}

	@Override
	public Listener<ChannelEvent> getChannelEventListener()
	{
		return mChannelEventBroadcaster;
	}
}

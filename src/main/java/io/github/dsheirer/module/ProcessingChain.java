/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.IAudioPacketListener;
import io.github.dsheirer.audio.IAudioPacketProvider;
import io.github.dsheirer.audio.squelch.ISquelchStateListener;
import io.github.dsheirer.audio.squelch.ISquelchStateProvider;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.channel.state.ChannelState;
import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.IDecoderStateEventListener;
import io.github.dsheirer.channel.state.IDecoderStateEventProvider;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.IChannelEventListener;
import io.github.dsheirer.controller.channel.IChannelEventProvider;
import io.github.dsheirer.identifier.IdentifierUpdateListener;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.IdentifierUpdateProvider;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageListener;
import io.github.dsheirer.message.IMessageProvider;
import io.github.dsheirer.module.decode.event.DecodeEventModel;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventListener;
import io.github.dsheirer.module.decode.event.IDecodeEventProvider;
import io.github.dsheirer.module.decode.event.MessageActivityModel;
import io.github.dsheirer.module.log.EventLogger;
import io.github.dsheirer.record.wave.ComplexBufferWaveRecorder;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IReusableBufferListener;
import io.github.dsheirer.sample.buffer.IReusableBufferProvider;
import io.github.dsheirer.sample.buffer.IReusableByteBufferListener;
import io.github.dsheirer.sample.buffer.IReusableByteBufferProvider;
import io.github.dsheirer.sample.buffer.IReusableComplexBufferListener;
import io.github.dsheirer.sample.buffer.ReusableAudioPacket;
import io.github.dsheirer.sample.buffer.ReusableBufferBroadcaster;
import io.github.dsheirer.sample.buffer.ReusableByteBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableFloatBuffer;
import io.github.dsheirer.source.ComplexSource;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.ISourceEventProvider;
import io.github.dsheirer.source.RealSource;
import io.github.dsheirer.source.Source;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.heartbeat.Heartbeat;
import io.github.dsheirer.source.heartbeat.IHeartbeatListener;
import io.github.dsheirer.source.heartbeat.IHeartbeatProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
public class ProcessingChain implements Listener<ChannelEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(ProcessingChain.class);

    private ReusableBufferBroadcaster<ReusableAudioPacket> mAudioPacketBroadcaster = new ReusableBufferBroadcaster<>();
    private ReusableBufferBroadcaster<ReusableFloatBuffer> mDemodulatedAudioBufferBroadcaster = new ReusableBufferBroadcaster();
    private ReusableBufferBroadcaster<ReusableComplexBuffer> mBasebandComplexBufferBroadcaster = new ReusableBufferBroadcaster();
    private ReusableBufferBroadcaster<ReusableByteBuffer> mDemodulatedBitstreamBufferBroadcaster = new ReusableBufferBroadcaster();
    private Broadcaster<IDecodeEvent> mDecodeEventBroadcaster = new Broadcaster<>();
    private Broadcaster<ChannelEvent> mChannelEventBroadcaster = new Broadcaster<>();
    private Broadcaster<DecoderStateEvent> mDecoderStateEventBroadcaster = new Broadcaster<>();
    private Broadcaster<Heartbeat> mHeartbeatBroadcaster = new Broadcaster<>();
    private Broadcaster<IdentifierUpdateNotification> mIdentifierUpdateNotificationBroadcaster = new Broadcaster<>();
    private Broadcaster<SourceEvent> mSourceEventBroadcaster = new Broadcaster<>();
    private Broadcaster<IMessage> mMessageBroadcaster = new Broadcaster<>();
    private Broadcaster<SquelchState> mSquelchStateBroadcaster = new Broadcaster<>();

    private AtomicBoolean mRunning = new AtomicBoolean();

    protected Source mSource;
    private List<Module> mModules = new ArrayList<>();
    private DecodeEventModel mDecodeEventModel;
    private ChannelState mChannelState;
    private MessageActivityModel mMessageActivityModel;

    /**
     * Creates a processing chain for managing a set of modules
     *
     * @param channel
     */
    public ProcessingChain(Channel channel, AliasModel aliasModel)
    {
        mChannelState = new ChannelState(channel, aliasModel);
        addModule(mChannelState);

        mDecodeEventModel = new DecodeEventModel();
        addDecodeEventListener(mDecodeEventModel);
    }

    public DecodeEventModel getDecodeEventModel()
    {
        return mDecodeEventModel;
    }

    public ChannelState getChannelState()
    {
        return mChannelState;
    }

    public MessageActivityModel getMessageActivityModel()
    {
        return mMessageActivityModel;
    }

    public void setMessageActivityModel(MessageActivityModel model)
    {
        mMessageActivityModel = model;

        addMessageListener(mMessageActivityModel);
    }

    public void dispose()
    {
        stop();

        for(Module module : mModules)
        {
            module.dispose();
        }

        mModules.clear();

        mAudioPacketBroadcaster.dispose();
        mDecodeEventBroadcaster.dispose();
        mChannelEventBroadcaster.dispose();
        mBasebandComplexBufferBroadcaster.dispose();
        mDemodulatedBitstreamBufferBroadcaster.dispose();
        mMessageBroadcaster.dispose();
        mSquelchStateBroadcaster.dispose();
    }

    /**
     * Indicates if this processing chain is currently receiving samples from
     * a source and sending those samples to the decoders.
     */
    public boolean isProcessing()
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
     * Applies a sample source to this processing chain.  Processing won't
     * start until the start() method is invoked.
     *
     * @param source - real or complex sample source
     * @throws IllegalStateException if the processing chain is currently
     *                               processing with another source.  Invoke stop() before applying a new
     *                               source.
     */
    public void setSource(Source source) throws IllegalStateException
    {
        if(isProcessing())
        {
            throw new IllegalStateException("Processing chain is currently processing.  Invoke stop() on the " +
                "processing chain before applying a new sample source");
        }

        mSource = source;

        addModule(mSource);
    }

    /**
     * List of current modules for this processing chain
     */
    public List<Module> getModules()
    {
        return mModules;
    }

    /**
     * List of decoder states for this processing chain
     */
    public List<DecoderState> getDecoderStates()
    {
        List<DecoderState> decoderStates = new ArrayList<>();

        for(Module module : mModules)
        {
            if(module instanceof DecoderState)
            {
                decoderStates.add((DecoderState)module);
            }
        }

        return decoderStates;
    }

    /**
     * Adds the list of modules to this processing chain
     */
    public void addModules(List<Module> modules)
    {
        for(Module module : modules)
        {
            addModule(module);
        }
    }

    /**
     * Adds a module to the processing chain.  Each module is tested for the
     * interfaces that it supports and is registered or receives a listener
     * to consume or produce the supported interface data type.  All elements
     * and events that are produced by any module are automatically routed to
     * all other components that support the corresponding listener interface.
     *
     * At least one module should consume complex samples and either produce
     * decoded messages and/or audio, or produce decoded real sample buffers
     * for all other modules to consume.
     *
     * @param module - processing module, demodulator, decoder, source, state
     * machine, etc.
     */
    public void addModule(Module module)
    {
        mModules.add(module);

        registerListeners(module);
        registerProviders(module);
    }

    /**
     * Removes the module from the processing chain and deregisters the module as a listener and
     * as a provider
     */
    public void removeModule(Module module)
    {
        unregisterListeners(module);
        unregisterProviders(module);

        mModules.remove(module);
    }

    /**
     * Registers the module as a listener to each of the broadcasters that
     * provide the data interface(s) supported by the module.
     */
    private void registerListeners(Module module)
    {
        if(module instanceof IdentifierUpdateListener)
        {
            mIdentifierUpdateNotificationBroadcaster.addListener(((IdentifierUpdateListener)module).getIdentifierUpdateListener());
        }

        if(module instanceof IAudioPacketListener)
        {
            mAudioPacketBroadcaster.addListener(((IAudioPacketListener)module).getAudioPacketListener());
        }

        if(module instanceof IDecodeEventListener)
        {
            mDecodeEventBroadcaster.addListener(((IDecodeEventListener)module).getDecodeEventListener());
        }

        if(module instanceof IChannelEventListener)
        {
            mChannelEventBroadcaster.addListener(((IChannelEventListener)module).getChannelEventListener());
        }

        if(module instanceof IDecoderStateEventListener)
        {
            mDecoderStateEventBroadcaster.addListener(((IDecoderStateEventListener)module).getDecoderStateListener());
        }

        if(module instanceof IHeartbeatListener)
        {
            mHeartbeatBroadcaster.addListener(((IHeartbeatListener)module).getHeartbeatListener());
        }

        if(module instanceof IMessageListener)
        {
            mMessageBroadcaster.addListener(((IMessageListener)module).getMessageListener());
        }

        if(module instanceof IReusableBufferListener)
        {
            mDemodulatedAudioBufferBroadcaster.addListener(((IReusableBufferListener)module).getReusableBufferListener());
        }

        if(module instanceof IReusableByteBufferListener)
        {
            mDemodulatedBitstreamBufferBroadcaster.addListener(((IReusableByteBufferListener)module).getReusableByteBufferListener());
        }

        if(module instanceof IReusableComplexBufferListener)
        {
            mBasebandComplexBufferBroadcaster.addListener(((IReusableComplexBufferListener)module).getReusableComplexBufferListener());
        }

        if(module instanceof ISourceEventListener)
        {
            Listener<SourceEvent> listener = ((ISourceEventListener)module).getSourceEventListener();

            if(listener != null)
            {
                mSourceEventBroadcaster.addListener(listener);
            }
        }

        if(module instanceof ISquelchStateListener)
        {
            mSquelchStateBroadcaster.addListener(((ISquelchStateListener)module).getSquelchStateListener());
        }
    }

    /**
     * Registers the module as a listener to each of the broadcasters that
     * provide the data interface(s) supported by the module.
     */
    private void unregisterListeners(Module module)
    {
        if(module instanceof IdentifierUpdateListener)
        {
            mIdentifierUpdateNotificationBroadcaster.removeListener(((IdentifierUpdateListener)module).getIdentifierUpdateListener());
        }

        if(module instanceof IAudioPacketListener)
        {
            mAudioPacketBroadcaster.removeListener(((IAudioPacketListener)module).getAudioPacketListener());
        }

        if(module instanceof IDecodeEventListener)
        {
            mDecodeEventBroadcaster.removeListener(((IDecodeEventListener)module).getDecodeEventListener());
        }

        if(module instanceof IChannelEventListener)
        {
            mChannelEventBroadcaster.removeListener(((IChannelEventListener)module).getChannelEventListener());
        }

        if(module instanceof IDecoderStateEventListener)
        {
            mDecoderStateEventBroadcaster.removeListener(((IDecoderStateEventListener)module).getDecoderStateListener());
        }

        if(module instanceof IHeartbeatListener)
        {
            mHeartbeatBroadcaster.removeListener(((IHeartbeatListener)module).getHeartbeatListener());
        }

        if(module instanceof IReusableBufferListener)
        {
            mDemodulatedAudioBufferBroadcaster.removeListener(((IReusableBufferListener)module).getReusableBufferListener());
        }

        if(module instanceof IReusableByteBufferListener)
        {
            mDemodulatedBitstreamBufferBroadcaster.removeListener(((IReusableByteBufferListener)module).getReusableByteBufferListener());
        }

        if(module instanceof IReusableComplexBufferListener)
        {
            mBasebandComplexBufferBroadcaster.removeListener(((IReusableComplexBufferListener)module).getReusableComplexBufferListener());
        }

        if(module instanceof ISourceEventListener)
        {
            mSourceEventBroadcaster.removeListener(((ISourceEventListener)module).getSourceEventListener());
        }

        if(module instanceof IMessageListener)
        {
            mMessageBroadcaster.removeListener(((IMessageListener)module).getMessageListener());
        }

        if(module instanceof ISquelchStateListener)
        {
            mSquelchStateBroadcaster.removeListener(((ISquelchStateListener)module).getSquelchStateListener());
        }
    }

    /**
     * Registers the broadcaster(s) as listeners to the module for each
     * provider interface that is supported by the module.
     */
    private void registerProviders(Module module)
    {
        if(module instanceof IdentifierUpdateProvider)
        {
            ((IdentifierUpdateProvider)module).setIdentifierUpdateListener(mIdentifierUpdateNotificationBroadcaster);
        }

        if(module instanceof IAudioPacketProvider)
        {
            ((IAudioPacketProvider)module).setAudioPacketListener(mAudioPacketBroadcaster);
        }

        if(module instanceof IDecodeEventProvider)
        {
            ((IDecodeEventProvider)module).addDecodeEventListener(mDecodeEventBroadcaster);
        }

        if(module instanceof IChannelEventProvider)
        {
            ((IChannelEventProvider)module).setChannelEventListener(mChannelEventBroadcaster);
        }

        if(module instanceof IDecoderStateEventProvider)
        {
            ((IDecoderStateEventProvider)module).setDecoderStateListener(mDecoderStateEventBroadcaster);
        }

        if(module instanceof IHeartbeatProvider)
        {
            ((IHeartbeatProvider)module).getHeartbeatManager().addHeartbeatListener(mHeartbeatBroadcaster);
        }

        if(module instanceof IMessageProvider)
        {
            ((IMessageProvider)module).setMessageListener(mMessageBroadcaster);
        }

        if(module instanceof IMessageProvider)
        {
            ((IMessageProvider)module).setMessageListener(mMessageBroadcaster);
        }

        if(module instanceof IReusableByteBufferProvider)
        {
            ((IReusableByteBufferProvider)module).setBufferListener(mDemodulatedBitstreamBufferBroadcaster);
        }

        if(module instanceof IReusableBufferProvider)
        {
            ((IReusableBufferProvider)module).setBufferListener(mDemodulatedAudioBufferBroadcaster);
        }

        if(module instanceof ISourceEventProvider)
        {
            ((ISourceEventProvider)module).setSourceEventListener(mSourceEventBroadcaster);
        }

        if(module instanceof ISquelchStateProvider)
        {
            ((ISquelchStateProvider)module).setSquelchStateListener(mSquelchStateBroadcaster);
        }
    }

    /**
     * Unregisters the broadcaster(s) as listeners to the module for each
     * provider interface that is supported by the module.
     */
    private void unregisterProviders(Module module)
    {
        if(module instanceof IdentifierUpdateProvider)
        {
            ((IdentifierUpdateProvider)module).removeIdentifierUpdateListener();
        }

        if(module instanceof IAudioPacketProvider)
        {
            ((IAudioPacketProvider)module).setAudioPacketListener(null);
        }

        if(module instanceof IReusableByteBufferProvider)
        {
            ((IReusableByteBufferProvider)module).removeBufferListener(mDemodulatedBitstreamBufferBroadcaster);
        }

        if(module instanceof IDecodeEventProvider)
        {
            ((IDecodeEventProvider)module).removeDecodeEventListener(mDecodeEventBroadcaster);
        }

        if(module instanceof IChannelEventProvider)
        {
            ((IChannelEventProvider)module).removeChannelEventListener();
        }

        if(module instanceof IDecoderStateEventProvider)
        {
            ((IDecoderStateEventProvider)module).setDecoderStateListener(null);
        }

        if(module instanceof IHeartbeatProvider)
        {
            ((IHeartbeatProvider)module).getHeartbeatManager().removeHeartbeatListener(mHeartbeatBroadcaster);
        }

        if(module instanceof IMessageProvider)
        {
            ((IMessageProvider)module).setMessageListener(null);
        }

        if(module instanceof IMessageProvider)
        {
            ((IMessageProvider)module).setMessageListener(null);
        }

        if(module instanceof IReusableBufferProvider)
        {
            ((IReusableBufferProvider)module).setBufferListener(null);
        }

        if(module instanceof ISourceEventProvider)
        {
            ((ISourceEventProvider)module).setSourceEventListener(null);
        }

        if(module instanceof ISquelchStateProvider)
        {
            ((ISquelchStateProvider)module).setSquelchStateListener(null);
        }
    }

    /**
     * Starts processing if the chain has a valid source.  Invocations on an
     * already started chain have no effect.
     */
    public void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            if(mSource != null)
            {
                //Broadcast the source sample rate so that each of the modules can self-configure
                mSourceEventBroadcaster.broadcast(SourceEvent.sampleRateChange(mSource.getSampleRate(), "Processing Chain Startup"));

                //Broadcast the source center frequency / sample rate so that each of the modules can self-configure
                mSourceEventBroadcaster.broadcast(SourceEvent.frequencyChange(mSource.getFrequency(), "Processing Chain Startup"));

                //Setup the channel state to monitor source overflow conditions
                mSource.setOverflowListener(mChannelState);

                /* Register with the source to receive sample data.  Setup a
                 * timer task to process the buffer queues 50 times a second
                 * (every 20 ms) */
                switch(mSource.getSampleType())
                {
                    case COMPLEX:
                        ((ComplexSource)mSource).setListener(mBasebandComplexBufferBroadcaster);
                        break;
                    case REAL:
                        ((RealSource)mSource).setListener(mDemodulatedAudioBufferBroadcaster);
                        break;
                    default:
                        throw new IllegalArgumentException("Unrecognized source "
                            + "sample type - cannot start processing chain");
                }

                /* Start each of the modules */
                for(Module module : mModules)
                {
                    try
                    {
                        module.start();
                    }
                    catch(Exception e)
                    {
                        mLog.error("Error starting module", e);
                    }
                }
            }
            else
            {
                mLog.error("Source is null on start()");
            }
        }
    }

    /**
     * Stops processing if the chain is currently processing.  Invocations on an already stopped chain have no effect.
     */
    public void stop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            if(mSource != null)
            {
                removeModule(mSource);

                mSource.stop();

                mSource.setOverflowListener(null);

                switch(mSource.getSampleType())
                {
                    case COMPLEX:
                        ((ComplexSource)mSource).removeListener(mBasebandComplexBufferBroadcaster);
                        break;
                    case REAL:
                        ((RealSource)mSource).removeListener(mDemodulatedAudioBufferBroadcaster);
                        break;
                    default:
                        throw new IllegalArgumentException("Unrecognized source sample type - cannot start processing " +
                            "chain");
                }

                mSource = null;
            }

            /* Stop each of the remaining modules */
            for(Module module : mModules)
            {
                module.stop();
            }

            //Reset each of the modules
            for(Module module : mModules)
            {
                module.reset();
            }
        }
    }

    /**
     * Removes any logging modules that are currently registered with this processing chain
     */
    public void removeEventLoggingModules()
    {
        List<Module> eventLoggingModules = new ArrayList<>();

        for(Module module : mModules)
        {
            if(module instanceof EventLogger)
            {
                eventLoggingModules.add(module);
            }
        }

        for(Module eventLoggingModule : eventLoggingModules)
        {
            removeModule(eventLoggingModule);
        }
    }

    /**
     * Removes any recording modules that are currently registered with this processing chain
     */
    public void removeRecordingModules()
    {
        List<Module> recordingModules = new ArrayList<>();

        for(Module module : mModules)
        {
            if(module instanceof ComplexBufferWaveRecorder)
            {
                recordingModules.add(module);
            }
        }

        for(Module recordingModule : recordingModules)
        {
            removeModule(recordingModule);
        }
    }

    /**
     * Adds the listener to receive audio packets from all modules.
     */
    public void addAudioPacketListener(Listener<ReusableAudioPacket> listener)
    {
        mAudioPacketBroadcaster.addListener(listener);
    }

    public void removeAudioPacketListener(Listener<ReusableAudioPacket> listener)
    {
        mAudioPacketBroadcaster.removeListener(listener);
    }

    /**
     * Adds the listener to receive decode events from all modules.
     */
    public void addDecodeEventListener(Listener<IDecodeEvent> listener)
    {
        mDecodeEventBroadcaster.addListener(listener);
    }

    public void removeDecodeEventListener(Listener<IDecodeEvent> listener)
    {
        mDecodeEventBroadcaster.removeListener(listener);
    }

    /**
     * Adds the listener to receive call events from all modules.
     */
    public void addChannelEventListener(Listener<ChannelEvent> listener)
    {
        mChannelEventBroadcaster.addListener(listener);
    }

    public void removeChannelEventListener(Listener<ChannelEvent> listener)
    {
        mChannelEventBroadcaster.removeListener(listener);
    }

    /**
     * Adds the listener to receive decoder state events from decoder modules
     */
    public void addDecoderStateEventListener(Listener<DecoderStateEvent> listener)
    {
        mDecoderStateEventBroadcaster.addListener(listener);
    }

    public void removeDecoderStateEventListener(Listener<DecoderStateEvent> listener)
    {
        mDecoderStateEventBroadcaster.removeListener(listener);
    }

    public Listener<DecoderStateEvent> getDecoderStateEventListener()
    {
        return mDecoderStateEventBroadcaster;
    }

    /**
     * Adds the listener to receive decoded messages from all decoders.
     */
    public void addMessageListener(Listener<IMessage> listener)
    {
        mMessageBroadcaster.addListener(listener);
    }

    /**
     * Adds the list of listeners to receive decoded messages from all decoders.
     */
    public void addMessageListeners(List<Listener<IMessage>> listeners)
    {
        for(Listener<IMessage> listener : listeners)
        {
            mMessageBroadcaster.addListener(listener);
        }
    }

    /**
     * Removes the listener from receiving decoded messages from all decoders.
     */
    public void removeMessageListener(Listener<IMessage> listener)
    {
        mMessageBroadcaster.removeListener(listener);
    }

    /**
     * Adds the listener to receive frequency change events from the processing chain
     *
     * @param listener to receive events
     */
    public void addFrequencyChangeListener(Listener<SourceEvent> listener)
    {
        mSourceEventBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving frequency change events
     *
     * @param listener to remove
     */
    public void removeFrequencyChangeListener(Listener<SourceEvent> listener)
    {
        mSourceEventBroadcaster.removeListener(listener);
    }

    /**
     * Adds the listener to receive call events from all modules.
     */
    public void addSquelchStateListener(Listener<SquelchState> listener)
    {
        mSquelchStateBroadcaster.addListener(listener);
    }

    public void removeSquelchStateListener(Listener<SquelchState> listener)
    {
        mSquelchStateBroadcaster.removeListener(listener);
    }

    /**
     * Adds listener to receive demodulated audio buffers from an modules that produce demodulated audio.
     */
    public void addDemodulatedAudioListener(Listener<ReusableFloatBuffer> listener)
    {
        mDemodulatedAudioBufferBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving demodulated audio buffers.
     */
    public void removeDemodulatedAudioListener(Listener<ReusableFloatBuffer> listener)
    {
        mDemodulatedAudioBufferBroadcaster.removeListener(listener);
    }

    /**
     * Primary method for the channel processing manager to broadcast channel events, namely traffic channel processing
     * stop events so that traffic channel manager(s) can maintain a correct state of traffic channels.
     */
    @Override
    public void receive(ChannelEvent channelEvent)
    {
        mChannelEventBroadcaster.broadcast(channelEvent);
    }

    /**
     * Broadcasts an identifier update notification to all modules
     * @param updateNotification to broadcast
     */
    public void receive(IdentifierUpdateNotification updateNotification)
    {
        mIdentifierUpdateNotificationBroadcaster.broadcast(updateNotification);
    }
}

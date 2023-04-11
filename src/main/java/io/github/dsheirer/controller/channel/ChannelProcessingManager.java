/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.controller.channel;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.channel.metadata.ChannelAndMetadata;
import io.github.dsheirer.channel.metadata.ChannelMetadata;
import io.github.dsheirer.channel.metadata.ChannelMetadataModel;
import io.github.dsheirer.controller.channel.event.ChannelStartProcessingRequest;
import io.github.dsheirer.controller.channel.event.ChannelStopProcessingRequest;
import io.github.dsheirer.controller.channel.event.PreloadDataContent;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.decoder.DecoderLogicalChannelNameIdentifier;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.log.EventLogManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.RecorderFactory;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.Source;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel processing manager handles all starting and stopping of channel decoding.  A processing chain is created
 * for each channel that is enabled.  The processing chain contains all of the components needed to decode a specific
 * channel and protocol along with all logging and baseband or bitstream recording.  Audio recording is handled outside
 * of this class by the RecorderManager.
 */
public class ChannelProcessingManager implements Listener<ChannelEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelProcessingManager.class);
    private static final String TUNER_UNAVAILABLE_DESCRIPTION = "TUNER UNAVAILABLE";
    private Map<Channel,ProcessingChain> mProcessingChains = new ConcurrentHashMap<>();
    private Lock mLock = new ReentrantLock();

    private ChannelSourceEventErrorListener mSourceErrorListener = new ChannelSourceEventErrorListener();
    private List<Listener<AudioSegment>> mAudioSegmentListeners = new CopyOnWriteArrayList<>();
    private List<Listener<IDecodeEvent>> mDecodeEventListeners = new CopyOnWriteArrayList<>();
    private Broadcaster<ChannelEvent> mChannelEventBroadcaster = new Broadcaster();

    private ChannelMapModel mChannelMapModel;
    private ChannelMetadataModel mChannelMetadataModel;
    private EventLogManager mEventLogManager;
    private TunerManager mTunerManager;
    private AliasModel mAliasModel;
    private UserPreferences mUserPreferences;
    private List<Long> mLoggedFrequencies = new ArrayList<>();
    private List<ScheduledFuture<?>> mDelayedChannelStartTasks = new ArrayList<>();

    /**
     * Constructs the channel processing manager
     *
     * @param channelMapModel containing channel maps defined by the user
     * @param eventLogManager for adding event loggers to channels
     * @param tunerManager for obtaining a tuner channel source for the channel
     * @param aliasModel for aliasing of identifiers produced by the channel
     * @param userPreferences for user defined behavior and settings
     */
    public ChannelProcessingManager(ChannelMapModel channelMapModel, EventLogManager eventLogManager,
                                    TunerManager tunerManager, AliasModel aliasModel, UserPreferences userPreferences)
    {
        mChannelMapModel = channelMapModel;
        mEventLogManager = eventLogManager;
        mTunerManager = tunerManager;
        mAliasModel = aliasModel;
        mUserPreferences = userPreferences;
        mChannelMetadataModel = new ChannelMetadataModel();
    }

    /**
     * Channel metadata model containing metadata for each channel or channel time-slice that is currently processing.
     */
    public ChannelMetadataModel getChannelMetadataModel()
    {
        return mChannelMetadataModel;
    }

    /**
     * Indicates if a processing chain is constructed for the channel and that
     * the processing chain is currently processing.
     */
    private boolean isProcessing(Channel channel)
    {
        boolean isProcessing = false;

        mLock.lock();

        try
        {
            isProcessing = mProcessingChains.containsKey(channel) && mProcessingChains.get(channel).isProcessing();
        }
        finally
        {
            mLock.unlock();
        }

        return isProcessing;
    }

    /**
     * Indicates if any channels are currently processing.
     * @return true if channels are processing.
     */
    public boolean isProcessing()
    {
        return !mProcessingChains.isEmpty();
    }

    /**
     * Returns the current processing chain associated with the channel, or
     * null if a processing chain is not currently setup for the channel
     */
    public ProcessingChain getProcessingChain(Channel channel)
    {
        return mProcessingChains.get(channel);
    }

    /**
     * Returns the channel associated with the processing chain
     *
     * @param processingChain
     * @return channel associated with the processing chain or null
     */
    public Channel getChannel(ProcessingChain processingChain)
    {
        Channel channel = null;

        if(processingChain != null)
        {
            mLock.lock();

            try
            {
                for(Map.Entry<Channel,ProcessingChain> entry : mProcessingChains.entrySet())
                {
                    if(entry.getValue() == processingChain)
                    {
                        channel = entry.getKey();
                        break;
                    }
                }
            }
            finally
            {
                mLock.unlock();
            }
        }

        return channel;
    }

    /**
     * Retrieves the channel associated with the processing chain that is consuming from the tuner channel source
     * @param tunerChannelSource to find the channel
     * @return channel
     */
    private Channel getChannel(TunerChannelSource tunerChannelSource)
    {
        Channel channel = null;

        mLock.lock();

        try
        {
            for(Map.Entry<Channel,ProcessingChain> entry : mProcessingChains.entrySet())
            {
                if(entry.getValue().hasSource(tunerChannelSource))
                {
                    channel = entry.getKey();
                    break;
                }
            }
        }
        finally
        {
            mLock.unlock();
        }

        return channel;
    }

    /**
     * Primary method for receiving requests to start and stop a channel
     *
     * @param event that requests either enable/start or disable/stop a channel.
     */
    @Override
    public void receive(ChannelEvent event)
    {
        Channel channel = event.getChannel();

        switch(event.getEvent())
        {
            case REQUEST_ENABLE:
                if(!isProcessing(channel))
                {
                    try
                    {
                        startProcessing(new ChannelStartProcessingRequest(event.getChannel()));
                    }
                    catch(ChannelException ce)
                    {
                        if(channel.getSourceConfiguration() instanceof SourceConfigTuner)
                        {
                            long frequency = ((SourceConfigTuner)channel.getSourceConfiguration()).getFrequency();

                            if(!mLoggedFrequencies.contains(frequency))
                            {
                                mLoggedFrequencies.add(frequency);
                                mLog.error("Error starting requested channel [" + channel.getName() + ":" + frequency +
                                    "] - " + ce.getMessage());
                            }
                        }
                        else if(channel.getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency)
                        {
                            List<Long> frequencies = ((SourceConfigTunerMultipleFrequency)channel
                                .getSourceConfiguration()).getFrequencies();

                            if(frequencies.size() > 0 && !mLoggedFrequencies.contains(frequencies.get(0)))
                            {
                                mLoggedFrequencies.add(frequencies.get(0));
                                mLog.error("Error starting requested channel [" + channel.getName() + ":" + frequencies +
                                    "] - " + ce.getMessage());
                            }
                        }
                        else
                        {
                            mLog.error("Error starting requested channel [" + channel.getName() + "] - " + ce.getMessage());
                        }
                    }
                }
                break;
            case REQUEST_DISABLE:
            case NOTIFICATION_DELETE:
                if(channel.isProcessing())
                {
                    try
                    {
                        stopProcessing(channel);
                    }
                    catch(ChannelException ce)
                    {
                        mLog.error("Error stopping channel [" + channel.getName() + "] - " + ce.getMessage());
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * Starts the specified channel.
     * @param channel to start
     * @throws ChannelException if the channel can't be started
     */
    public void start(Channel channel) throws ChannelException
    {
        startProcessing(new ChannelStartProcessingRequest(channel));
    }

    /**
     * Request to start processing a channel received over the Guava event bus.
     *
     * Note: since this is received over the event bus, we handle any channel exceptions inside this method.
     */
    @Subscribe
    public void startChannelRequest(ChannelStartProcessingRequest request)
    {
        if(!isProcessing(request.getChannel()))
        {
            try
            {
                startProcessing(request);
            }
            catch(ChannelException ce)
            {
                if(request.isPersistentAttempt())
                {
                    ScheduledFuture<?> future = ThreadPool.SCHEDULED
                        .schedule(new DelayedChannelStartTask(request), 500, TimeUnit.MILLISECONDS);
                    mDelayedChannelStartTasks.add(future);
                }
            }
        }
    }

    /**
     * Request to stop a channel that is currently processing
     * @param request with the tuner channel source feeding the channel to be stopped.
     */
    @Subscribe
    public void stopChannelRequest(ChannelStopProcessingRequest request)
    {
        Channel channel = getChannel(request.getTunerChannelSource());

        if(channel != null)
        {
            try
            {
                stop(channel);
            }
            catch(ChannelException ce)
            {
                mLog.error("Error stopping channel [" + channel + "]", ce);
            }
        }
    }

    /**
     * Stops the specified channel
     * @param channel to stop
     */
    public void stop(Channel channel) throws ChannelException
    {
        stopProcessing(channel);
    }

    /**
     * Starts a channel processing
     * @param request containing channel and other details
     * @throws ChannelException if a source is not available for the channel
     */
    private void startProcessing(ChannelStartProcessingRequest request) throws ChannelException
    {
        Channel channel = request.getChannel();

        if(isProcessing(channel))
        {
            return;
        }

        //Ensure that we can get a source before we construct a new processing chain
        Source source = null;

        try
        {
            source = mTunerManager.getSource(channel.getSourceConfiguration(),
                channel.getDecodeConfiguration().getChannelSpecification());
        }
        catch(SourceException se)
        {
            mLog.debug("Error obtaining source for channel [" + channel.getName() + "]", se);
        }

        if(source == null)
        {
            //This has to be done on the FX event thread when the playlist editor is constructed
            Platform.runLater(() -> channel.setProcessing(false));

            mChannelEventBroadcaster.broadcast(new ChannelEvent(channel,
                ChannelEvent.Event.NOTIFICATION_PROCESSING_START_REJECTED, TUNER_UNAVAILABLE_DESCRIPTION));

            throw new ChannelException("No Tuner Available");
        }

        ProcessingChain processingChain = new ProcessingChain(channel, mAliasModel);

        //Certain decoders aggregate the decode events in the parent channel that also includes any events produced
        //by the traffic channels.  Establish listener registration depending on if this channel is a traffic channel
        //and the request contains the parent event history, or if this is a parent channel and the request contains
        //the traffic channel event history.
        if(request.hasParentDecodeEventHistory())
        {
            processingChain.getDecodeEventHistory().addListener(request.getParentDecodeEventHistory());
        }
        else if(request.hasChildDecodeEventHistory())
        {
            request.getChildDecodeEventHistory().addListener(processingChain.getDecodeEventHistory());
        }

        //Register to receive event bus requests/notifications
        processingChain.getEventBus().register(ChannelProcessingManager.this);

        mChannelEventBroadcaster.addListener(processingChain);

        /* Register global listeners */
        for(Listener<AudioSegment> listener : mAudioSegmentListeners)
        {
            processingChain.addAudioSegmentListener(listener);
        }

        for(Listener<IDecodeEvent> listener : mDecodeEventListeners)
        {
            processingChain.addDecodeEventListener(listener);
        }

        //Add a listener to detect source error state that indicates the channel should be shutdown.
        //Note: processing chain will only add this once.
        processingChain.addSourceEventListener(mSourceErrorListener);

        //Register this manager to receive channel events from traffic channel manager modules within
        //the processing chain
        processingChain.addChannelEventListener(this);

        //Register channel to receive frequency correction events to show in the spectral display (hack!)
        processingChain.addFrequencyChangeListener(channel);

        /* Processing Modules */
        List<Module> modules = DecoderFactory.getModules(mChannelMapModel, channel, mAliasModel, mUserPreferences,
            request.getTrafficChannelManager());
        processingChain.addModules(modules);

        //Post preload data from the request to the event bus.  Modules that can handle preload data will annotate
        //their processor method with @Subscribe to receive each specific preload data content class.
        for(PreloadDataContent preloadDataContent: request.getPreloadDataContents())
        {
            processingChain.getEventBus().post(preloadDataContent);
        }

        //Setup event logging
        List<Module> loggers = mEventLogManager.getLoggers(channel);

        if(!loggers.isEmpty())
        {
            processingChain.addModules(loggers);
        }

        //Add recorders
        processingChain.addModules(RecorderFactory.getRecorders(mUserPreferences, channel));

        //Set the samples source
        processingChain.setSource(source);

        //Inject the channel identifier for traffic channels and preload user identifiers
        if(channel.isTrafficChannel())
        {
            if(request.hasChannelDescriptor() && request.hasIdentifierCollection())
            {
                for(int timeslot = 0; timeslot < request.getChannelDescriptor().getTimeslotCount(); timeslot++)
                {
                    DecoderLogicalChannelNameIdentifier identifier =
                        DecoderLogicalChannelNameIdentifier.create(request.getChannelDescriptor().toString(),
                            request.getChannelDescriptor().getProtocol());
                    IdentifierUpdateNotification notification = new IdentifierUpdateNotification(identifier,
                        IdentifierUpdateNotification.Operation.ADD, timeslot);
                    processingChain.getChannelState().updateChannelStateIdentifiers(notification);

                    //Inject scramble parameters
                    for(Identifier scrambleParameters: request.getIdentifierCollection()
                        .getIdentifiers(Form.SCRAMBLE_PARAMETERS))
                    {
                        //Broadcast scramble parameters to both timeslots
                        IdentifierUpdateNotification scrambleNotification = new IdentifierUpdateNotification(scrambleParameters,
                            IdentifierUpdateNotification.Operation.ADD, timeslot);
                        processingChain.getChannelState().updateChannelStateIdentifiers(scrambleNotification);
                    }
                }
            }

            for(Identifier userIdentifier : request.getIdentifierCollection().getIdentifiers(IdentifierClass.USER))
            {
                if(request.getChannelDescriptor().getTimeslotCount() > 1)
                {
                    //Only broadcast an identifier update for the timeslot specified in the originating collection
                    IdentifierUpdateNotification notification = new IdentifierUpdateNotification(userIdentifier,
                        IdentifierUpdateNotification.Operation.ADD, request.getIdentifierCollection().getTimeslot());
                    processingChain.getChannelState().updateChannelStateIdentifiers(notification);
                }
                else
                {
                    //Only broadcast an identifier update for the timeslot specified in the originating collection
                    IdentifierUpdateNotification notification = new IdentifierUpdateNotification(userIdentifier,
                        IdentifierUpdateNotification.Operation.ADD, 0);
                    processingChain.getChannelState().updateChannelStateIdentifiers(notification);
                }
            }
        }

        if(addProcessingChain(channel, processingChain))
        {
            processingChain.start();

            if(GraphicsEnvironment.isHeadless())
            {
                channel.setProcessing(true);
            }
            else
            {
                //This has to be done on the FX event thread when the playlist editor is constructed
                Platform.runLater(() -> channel.setProcessing(true));
            }

            mChannelEventBroadcaster.broadcast(new ChannelEvent(channel, ChannelEvent.Event.NOTIFICATION_PROCESSING_START));
        }
        else
        {
            mLog.warn("Channel [" + channel.getName() + "] processing chain not added because it already exists");
            processingChain.removeEventLoggingModules();
            processingChain.removeRecordingModules();
            processingChain.removeFrequencyChangeListener(channel);
            channel.resetFrequencyCorrection();
            mChannelEventBroadcaster.broadcast(new ChannelEvent(channel, ChannelEvent.Event.NOTIFICATION_PROCESSING_STOP));
            mChannelEventBroadcaster.removeListener(processingChain);
            processingChain.getEventBus().unregister(ChannelProcessingManager.this);
            processingChain.dispose();
        }
    }

    /**
     * Thread-safe add processing chain and add channel metadata to channel metadata model.
     * @param channel for the processing chain
     * @param processingChain to add
     * @return true if processing chain was added or false if it was not added due to there already
     * being a processing chain registered for that channel.
     */
    private boolean addProcessingChain(Channel channel, ProcessingChain processingChain)
    {
        boolean added = false;

        mLock.lock();

        try
        {
            if(!mProcessingChains.containsKey(channel))
            {
                added = true;
                mProcessingChains.put(channel, processingChain);
                getChannelMetadataModel().add(new ChannelAndMetadata(channel, processingChain.getChannelState().getChannelMetadata()));
            }
        }
        finally
        {
            mLock.unlock();
        }

        return added;
    }

    /**
     * Thread-safe remove processing chain.
     * @param channel for identifying the processing chain
     * @return the removed processing chain or null
     */
    private ProcessingChain removeProcessingChain(Channel channel)
    {
        ProcessingChain removed = null;

        mLock.lock();

        try
        {
            removed = mProcessingChains.remove(channel);

            if(removed != null)
            {
                for(ChannelMetadata channelMetadata: removed.getChannelState().getChannelMetadata())
                {
                    getChannelMetadataModel().remove(channelMetadata);
                }
            }
        }
        finally
        {
            mLock.unlock();
        }

        return removed;
    }

    /**
     * Stops the channel/processing chain.
     *
     * @param channel to stop
     */
    private void stopProcessing(Channel channel) throws ChannelException
    {
        ProcessingChain processingChain = removeProcessingChain(channel);

        if(processingChain != null)
        {
            if(GraphicsEnvironment.isHeadless())
            {
                channel.setProcessing(false);
            }
            else
            {
                //This has to be done on the FX event thread when the playlist editor is constructed
                Platform.runLater(() -> channel.setProcessing(false));
            }

            try
            {
                processingChain.stop();
                processingChain.removeEventLoggingModules();
                processingChain.removeRecordingModules();

                //Deregister channel from receive frequency correction events to show in the spectral display (hack!)
                processingChain.removeFrequencyChangeListener(channel);
                channel.resetFrequencyCorrection();

                mChannelEventBroadcaster.broadcast(new ChannelEvent(channel, ChannelEvent.Event.NOTIFICATION_PROCESSING_STOP));
                mChannelEventBroadcaster.removeListener(processingChain);

                //Unregister for event bus requests and notifications
                processingChain.getEventBus().unregister(ChannelProcessingManager.this);
                processingChain.dispose();
            }
            catch(Exception e)
            {
                mLog.error("Error during shutdown of processing chain for channel [" + channel.getName() + "}", e);
            }
        }
    }

    /**
     * Stops all currently processing channels to prepare for shutdown.
     */
    public void shutdown()
    {
        List<ScheduledFuture<?>> delayedTasks = new ArrayList<>(mDelayedChannelStartTasks);

        for(ScheduledFuture<?> delayedTask: delayedTasks)
        {
            delayedTask.cancel(true);
            mDelayedChannelStartTasks.remove(delayedTask);
        }

        List<Channel> channelsToStop = new ArrayList<>(mProcessingChains.keySet());

        for(Channel channel : channelsToStop)
        {
            try
            {
                stopProcessing(channel);
            }
            catch(ChannelException ce)
            {
                mLog.error("Error stopping channel [" + channel.getName() + "] - " + ce.getMessage());
            }
        }
    }

    /**
     * Process a request to convert a currently processing standard channel type to a traffic channel type.
     * @param request from the currently processing channel's processing chain event bus
     */
    @Subscribe
    public void convertToTrafficChannel(ChannelConversionRequest request)
    {
        //Update the channel to processing chain map.
        ProcessingChain processingChain = mProcessingChains.remove(request.getCurrentChannel());

        if(processingChain != null)
        {
            //Remove the traffic channel manager from this processing chain.  Reuse or reinsertion of the traffic
            //channel manager to another processing chain is handled separately.
            processingChain.removeTrafficChannelManager();

            //Update processing flag for each configuration.
            Platform.runLater(() -> {
                request.getCurrentChannel().setProcessing(false);
                request.getTrafficChannel().setProcessing(true);
            });

            mProcessingChains.put(request.getTrafficChannel(), processingChain);
            mChannelMetadataModel.updateChannelMetadataToChannelMap(processingChain.getChannelState().getChannelMetadata(),
                request.getTrafficChannel());

            //Post a change notification so that processing chain modules can reconfigure
            processingChain.channelConfigurationChanged(new ChannelConfigurationChangeNotification(request.getTrafficChannel()));
        }
        else
        {
            mLog.warn("Request to convert to traffic channel ignored - no processing chain was found");
        }
    }

    /**
     * Adds a message listener that will be added to all channels to receive
     * any messages.
     */
    public void addAudioSegmentListener(Listener<AudioSegment> listener)
    {
        mAudioSegmentListeners.add(listener);
    }

    /**
     * Removes a message listener.
     */
    public void removeAudioSegmentListener(Listener<AudioSegment> listener)
    {
        mAudioSegmentListeners.remove(listener);
    }

    /**
     * Adds a message listener that will be added to all channels to receive
     * any messages.
     */
    public void addDecodeEventListener(Listener<IDecodeEvent> listener)
    {
        mDecodeEventListeners.add(listener);
    }

    /**
     * Removes a message listener.
     */
    public void removeDecodeEventListener(Listener<IDecodeEvent> listener)
    {
        mDecodeEventListeners.remove(listener);
    }

    /**
     * Adds a listener to receive channel events from this manager
     */
    public void addChannelEventListener(Listener<ChannelEvent> listener)
    {
        mChannelEventBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving channel events from this manager
     */
    public void removeChannelEventListener(Listener<ChannelEvent> listener)
    {
        mChannelEventBroadcaster.removeListener(listener);
    }

    /**
     * Task to scheduling attempt to start a channel after previous attempts failed for lack of tuner channel
     */
    public class DelayedChannelStartTask implements Runnable
    {
        private ChannelStartProcessingRequest mRequest;

        public DelayedChannelStartTask(ChannelStartProcessingRequest request)
        {
            mRequest = request;
        }

        @Override
        public void run()
        {
            try
            {
                mDelayedChannelStartTasks.remove(this);
                startChannelRequest(mRequest);
            }
            catch(Throwable t)
            {
                mLog.error("Error executing persistent channel start task");
            }
        }
    }

    /**
     * Monitors all channels for an error in the source event that would require the
     * channel's processing chain to be stopped
     */
    private class ChannelSourceEventErrorListener implements Listener<SourceEvent>
    {
        @Override public void receive(SourceEvent sourceEvent)
        {
            if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_ERROR_STATE && sourceEvent.getSource() != null)
            {
                Channel toShutdown = null;

                mLock.lock();

                try
                {
                    for(Map.Entry<Channel,ProcessingChain> entry: mProcessingChains.entrySet())
                    {
                        if(entry.getValue().hasSource(sourceEvent.getSource()))
                        {
                            toShutdown = entry.getKey();
                            break;
                        }
                    }
                }
                finally
                {
                    mLock.unlock();
                }

                if(toShutdown != null)
                {
                    if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_ERROR_STATE)
                    {
                        mLog.warn("Channel source error detected - stopping channel [" + toShutdown.getName() + "]");
                    }
                    else
                    {
                        mLog.warn("Source event error - stopping channel [" + toShutdown.getName() + "]");
                    }

                    try
                    {
                        stopProcessing(toShutdown);
                    }
                    catch(ChannelException ce)
                    {
                        mLog.error("Error stopping channel [" + (toShutdown != null ? toShutdown.getName() : "unknown") +
                                "] with source error - " + ce.getMessage());
                    }
                }
            }
        }
    }
}

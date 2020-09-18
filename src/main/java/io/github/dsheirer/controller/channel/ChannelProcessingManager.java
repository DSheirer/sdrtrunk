/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.controller.channel;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.channel.metadata.ChannelMetadata;
import io.github.dsheirer.channel.metadata.ChannelMetadataModel;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.decoder.DecoderLogicalChannelNameIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.MessageActivityModel;
import io.github.dsheirer.module.log.EventLogManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.RecorderFactory;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.Source;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.SourceManager;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.gui.SDRTrunk;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private Map<Channel,ProcessingChain> mProcessingChains = new HashMap<>();

    private List<Listener<AudioSegment>> mAudioSegmentListeners = new CopyOnWriteArrayList<>();
    private List<Listener<IDecodeEvent>> mDecodeEventListeners = new CopyOnWriteArrayList<>();
    private Broadcaster<ChannelEvent> mChannelEventBroadcaster = new Broadcaster();

    private ChannelMapModel mChannelMapModel;
    private ChannelMetadataModel mChannelMetadataModel;
    private EventLogManager mEventLogManager;
    private SourceManager mSourceManager;
    private AliasModel mAliasModel;
    private UserPreferences mUserPreferences;
    private List<Long> mUnTunableFrequencies = new ArrayList<>();

    /**
     * Constructs the channel processing manager
     *
     * @param channelMapModel containing channel maps defined by the user
     * @param eventLogManager for adding event loggers to channels
     * @param sourceManager for obtaining a tuner channel source for the channel
     * @param aliasModel for aliasing of identifiers produced by the channel
     * @param userPreferences for user defined behavior and settings
     */
    public ChannelProcessingManager(ChannelMapModel channelMapModel, EventLogManager eventLogManager,
                                    SourceManager sourceManager, AliasModel aliasModel, UserPreferences userPreferences)
    {
        mChannelMapModel = channelMapModel;
        mEventLogManager = eventLogManager;
        mSourceManager = sourceManager;
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
        return mProcessingChains.containsKey(channel) && mProcessingChains.get(channel).isProcessing();
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
        if(processingChain != null)
        {
            for(Map.Entry<Channel,ProcessingChain> entry : mProcessingChains.entrySet())
            {
                if(entry.getValue() == processingChain)
                {
                    return entry.getKey();
                }
            }
        }

        return null;
    }

    /**
     * Primary method for receiving requests to start and stop a channel
     *
     * @param event that requests either enable/start or disable/stop a channel.
     */
    @Override
    public synchronized void receive(ChannelEvent event)
    {
        Channel channel = event.getChannel();

        switch(event.getEvent())
        {
            case REQUEST_ENABLE:
                if(!isProcessing(channel))
                {
                    try
                    {
                        startProcessing(event);
                    }
                    catch(ChannelException ce)
                    {
                        if(channel.getSourceConfiguration() instanceof SourceConfigTuner)
                        {
                            long frequency = ((SourceConfigTuner)channel.getSourceConfiguration()).getFrequency();

                            if(!mUnTunableFrequencies.contains(frequency))
                            {
                                mUnTunableFrequencies.add(frequency);
                                mLog.error("Error starting requested channel [" + channel.getName() + ":" + frequency +
                                    "] - " + ce.getMessage());
                            }
                        }
                        else if(channel.getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency)
                        {
                            List<Long> frequencies = ((SourceConfigTunerMultipleFrequency)channel
                                .getSourceConfiguration()).getFrequencies();

                            if(frequencies.size() > 0 && !mUnTunableFrequencies.contains(frequencies.get(0)))
                            {
                                mUnTunableFrequencies.add(frequencies.get(0));
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
                if(channel.isProcessing())
                {
                    try
                    {
                        switch(channel.getChannelType())
                        {
                            case STANDARD:
                                stopProcessing(channel, true);
                                break;
                            case TRAFFIC:
                                //Don't remove traffic channel processing chains
                                //until explicitly deleted, so that we can reuse them
                                stopProcessing(channel, false);
                                break;
                            default:
                                break;
                        }
                    }
                    catch(ChannelException ce)
                    {
                        mLog.error("Error stopping channel [" + channel.getName() + "] - " + ce.getMessage());
                    }
                }
                break;
            case NOTIFICATION_DELETE:
                if(channel.isProcessing())
                {
                    try
                    {
                        stopProcessing(channel, true);
                    }
                    catch(ChannelException ce)
                    {
                        mLog.error("Error stopping deleted channel [" + channel.getName() + "] - " + ce.getMessage());
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
        startProcessing(new ChannelEvent(channel, ChannelEvent.Event.REQUEST_ENABLE));
    }

    /**
     * Stops the specified channel
     * @param channel to stop
     */
    public void stop(Channel channel) throws ChannelException
    {
        stopProcessing(channel, !channel.isTrafficChannel());
    }

    /**
     * Starts a channel/processing chain
     *
     * @param event that requested the channel start
     */
    private void startProcessing(ChannelEvent event) throws ChannelException
    {
        Channel channel = event.getChannel();

        ProcessingChain processingChain = mProcessingChains.get(channel);

        //If we're already processing, ignore the request
        if(processingChain != null && processingChain.isProcessing())
        {
            throw new ChannelException("Channel is already playing");
        }

        //Ensure that we can get a source before we construct a new processing chain
        Source source = null;

        try
        {
            source = mSourceManager.getSource(channel.getSourceConfiguration(),
                channel.getDecodeConfiguration().getChannelSpecification());
        }
        catch(SourceException se)
        {
            mLog.debug("Error obtaining source for channel [" + channel.getName() + "]", se);
        }

        if(source == null)
        {
            //This has to be done on the FX event thread when the playlist editor is constructed
            if (!SDRTrunk.mHeadlessMode) {
                Platform.runLater(() -> channel.setProcessing(false));
            } else {
                channel.setProcessing(false);
            }

            mChannelEventBroadcaster.broadcast(new ChannelEvent(channel,
                ChannelEvent.Event.NOTIFICATION_PROCESSING_START_REJECTED, TUNER_UNAVAILABLE_DESCRIPTION));

            throw new ChannelException("No Tuner Available");
        }

        if(processingChain == null)
        {
            processingChain = new ProcessingChain(channel, mAliasModel);
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

            //Add a listener to detect source error state that indicates the channel should be shutdown
            processingChain.addSourceEventListener(sourceEvent ->
            {
                if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_ERROR_STATE && sourceEvent.getSource() != null)
                {
                    Channel toShutdown = null;

                    for(Map.Entry<Channel,ProcessingChain> entry: mProcessingChains.entrySet())
                    {
                        if(entry.getValue().hasSource(sourceEvent.getSource()))
                        {
                            toShutdown = entry.getKey();
                            break;
                        }
                    }

                    if(toShutdown != null)
                    {
                        mLog.warn("Channel source error detected - stopping channel [" + toShutdown.getName() + "]");

                        try
                        {
                            stopProcessing(toShutdown, true);
                        }
                        catch(ChannelException ce)
                        {
                            mLog.error("Error stopping channel [" + channel.getName() + "] with source error - " +
                                ce.getMessage());
                        }
                    }
                }
            });

            //Register this manager to receive channel events from traffic channel manager modules within
            //the processing chain
            processingChain.addChannelEventListener(this);

            //Register channel to receive frequency correction events to show in the spectral display (hack!)
            processingChain.addFrequencyChangeListener(channel);

            /* Processing Modules */
            List<Module> modules = DecoderFactory.getModules(mChannelMapModel, channel, mAliasModel, mUserPreferences);
            processingChain.addModules(modules);

            /* Setup message activity model with filtering */
            FilterSet<IMessage> messageFilter = DecoderFactory.getMessageFilters(modules);
            MessageActivityModel messageModel = new MessageActivityModel(messageFilter);
            processingChain.setMessageActivityModel(messageModel);

        }

        /* Setup event logging */
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
        if(channel.isTrafficChannel() && event instanceof ChannelGrantEvent)
        {
            ChannelGrantEvent channelGrantEvent = (ChannelGrantEvent)event;
            IChannelDescriptor channelDescriptor = channelGrantEvent.getChannelDescriptor();

            IdentifierCollection identifierCollection = channelGrantEvent.getIdentifierCollection();

            if(channelDescriptor != null)
            {
                for(int timeslot = 0; timeslot < channelDescriptor.getTimeslotCount(); timeslot++)
                {
                    DecoderLogicalChannelNameIdentifier identifier =
                        DecoderLogicalChannelNameIdentifier.create(channelDescriptor.toString(), channelDescriptor.getProtocol());
                    IdentifierUpdateNotification notification = new IdentifierUpdateNotification(identifier,
                        IdentifierUpdateNotification.Operation.ADD, timeslot);
                    processingChain.getChannelState().updateChannelStateIdentifiers(notification);

                    //Inject scramble parameters
                    for(Identifier scrambleParameters: identifierCollection.getIdentifiers(Form.SCRAMBLE_PARAMETERS))
                    {
                        //Broadcast scramble parameters to both timeslots
                        IdentifierUpdateNotification scrambleNotification = new IdentifierUpdateNotification(scrambleParameters,
                            IdentifierUpdateNotification.Operation.ADD, timeslot);
                        processingChain.getChannelState().updateChannelStateIdentifiers(scrambleNotification);
                    }
                }
            }

            for(Identifier userIdentifier : identifierCollection.getIdentifiers(IdentifierClass.USER))
            {
                if(channelDescriptor.getTimeslotCount() > 1)
                {
                    //Only broadcast an identifier update for the timeslot specified in the originating collection
                    IdentifierUpdateNotification notification = new IdentifierUpdateNotification(userIdentifier,
                        IdentifierUpdateNotification.Operation.ADD, channelGrantEvent.getIdentifierCollection().getTimeslot());
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

        processingChain.start();
        //This has to be done on the FX event thread when the playlist editor is constructed
        if (!SDRTrunk.mHeadlessMode) {
            Platform.runLater(() -> channel.setProcessing(true));
        } else {
            channel.setProcessing(true);
        }

        getChannelMetadataModel().add(processingChain.getChannelState().getChannelMetadata(), channel);

        mProcessingChains.put(channel, processingChain);

        mChannelEventBroadcaster.broadcast(new ChannelEvent(channel, ChannelEvent.Event.NOTIFICATION_PROCESSING_START));
    }

    /**
     * Stops the channel/processing chain.
     *
     * @param channel to stop
     * @param remove set to true to remove the associated processing chain.
     */
    private void stopProcessing(Channel channel, boolean remove) throws ChannelException
    {
        //This has to be done on the FX event thread when the playlist editor is constructed
        if (!SDRTrunk.mHeadlessMode) {
            Platform.runLater(() -> channel.setProcessing(false));
        }

        if(mProcessingChains.containsKey(channel))
        {
            ProcessingChain processingChain = mProcessingChains.get(channel);

            for(ChannelMetadata channelMetadata: processingChain.getChannelState().getChannelMetadata())
            {
                getChannelMetadataModel().remove(channelMetadata);
            }

            processingChain.stop();

            processingChain.removeEventLoggingModules();

            processingChain.removeRecordingModules();

            //Deregister channel from receive frequency correction events to show in the spectral display (hack!)
            processingChain.removeFrequencyChangeListener(channel);
            channel.resetFrequencyCorrection();

            mChannelEventBroadcaster.broadcast(new ChannelEvent(channel, ChannelEvent.Event.NOTIFICATION_PROCESSING_STOP));

            if(remove)
            {
                mChannelEventBroadcaster.removeListener(processingChain);
                mProcessingChains.remove(channel);
                processingChain.dispose();
            }
        }
        else
        {
            throw new ChannelException("Channel is not currently playing");
        }
    }

    /**
     * Stops all currently processing channels to prepare for shutdown.
     */
    public void shutdown()
    {
        List<Channel> channelsToStop = new ArrayList<>(mProcessingChains.keySet());

        for(Channel channel : channelsToStop)
        {
            try
            {
                stopProcessing(channel, true);
            }
            catch(ChannelException ce)
            {
                mLog.error("Error stopping channel [" + channel.getName() + "] - " + ce.getMessage());
            }
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
}

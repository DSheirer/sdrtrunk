/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.controller.channel;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.channel.metadata.ChannelMetadataModel;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.identifier.IdentifierUpdateNotification;
import io.github.dsheirer.identifier.configuration.ChannelDescriptorConfigurationIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.event.MessageActivityModel;
import io.github.dsheirer.module.log.EventLogManager;
import io.github.dsheirer.record.RecorderManager;
import io.github.dsheirer.record.RecorderType;
import io.github.dsheirer.record.binary.BinaryRecorder;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableAudioPacket;
import io.github.dsheirer.source.Source;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.SourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChannelProcessingManager implements Listener<ChannelEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelProcessingManager.class);
    private static final String TUNER_UNAVAILABLE_DESCRIPTION = "TUNER UNAVAILABLE";
    private Map<Channel, ProcessingChain> mProcessingChains = new HashMap<>();

    private List<Listener<ReusableAudioPacket>> mAudioPacketListeners = new CopyOnWriteArrayList<>();
    private List<Listener<IMessage>> mMessageListeners = new CopyOnWriteArrayList<>();
    private Broadcaster<ChannelEvent> mChannelEventBroadcaster = new Broadcaster();

    private ChannelMapModel mChannelMapModel;
    private ChannelMetadataModel mChannelMetadataModel;
    private EventLogManager mEventLogManager;
    private RecorderManager mRecorderManager;
    private SourceManager mSourceManager;
    private AliasModel mAliasModel;

    public ChannelProcessingManager(ChannelMapModel channelMapModel, EventLogManager eventLogManager,
                                    RecorderManager recorderManager, SourceManager sourceManager, AliasModel aliasModel)
    {
        mChannelMapModel = channelMapModel;
        mEventLogManager = eventLogManager;
        mRecorderManager = recorderManager;
        mSourceManager = sourceManager;
        mAliasModel = aliasModel;
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
            for(Map.Entry<Channel, ProcessingChain> entry : mProcessingChains.entrySet())
            {
                if(entry.getValue() == processingChain)
                {
                    return entry.getKey();
                }
            }
        }

        return null;
    }

    @Override
    public synchronized void receive(ChannelEvent event)
    {
        Channel channel = event.getChannel();

        switch(event.getEvent())
        {
            case REQUEST_ENABLE:
                if(!isProcessing(channel))
                {
                    startProcessing(event);
                }
                break;
            case REQUEST_DISABLE:
                if(channel.isProcessing())
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
            case NOTIFICATION_DELETE:
                if(channel.isProcessing())
                {
                    stopProcessing(channel, true);
                }
                break;
            case NOTIFICATION_CONFIGURATION_CHANGE:
                if(isProcessing(channel))
                {
                    stopProcessing(channel, true);
                    startProcessing(event);
                }
                break;
            default:
                break;
        }
    }

    private void startProcessing(ChannelEvent event)
    {
        Channel channel = event.getChannel();

        ProcessingChain processingChain = mProcessingChains.get(channel);

        //If we're already processing, ignore the request
        if(processingChain != null && processingChain.isProcessing())
        {
            return;
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
            channel.setProcessing(false);

            mChannelEventBroadcaster.broadcast(new ChannelEvent(channel, ChannelEvent.Event.NOTIFICATION_START_PROCESSING_REJECTED,
                TUNER_UNAVAILABLE_DESCRIPTION));

            return;
        }

        if(processingChain == null)
        {
            processingChain = new ProcessingChain(channel, mAliasModel);
            mChannelEventBroadcaster.addListener(processingChain);

            /* Register global listeners */
            for(Listener<ReusableAudioPacket> listener : mAudioPacketListeners)
            {
                processingChain.addAudioPacketListener(listener);
            }

            for(Listener<IMessage> listener : mMessageListeners)
            {
                processingChain.addMessageListener(listener);
            }

            //Register this manager to receive channel events from traffic channel manager modules within
            //the processing chain
            processingChain.addChannelEventListener(this);

            //Register channel to receive frequency correction events to show in the spectral display (hack!)
            processingChain.addFrequencyChangeListener(channel);

            /* Processing Modules */
            List<Module> modules = DecoderFactory.getModules(mChannelMapModel, channel, mAliasModel);
            processingChain.addModules(modules);

            /* Setup message activity model with filtering */
            FilterSet<IMessage> messageFilter = DecoderFactory.getMessageFilters(modules);
            MessageActivityModel messageModel = new MessageActivityModel(messageFilter);
            processingChain.setMessageActivityModel(messageModel);

        }

        /* Setup event logging */
        List<Module> loggers = mEventLogManager.getLoggers(channel.getEventLogConfiguration(), channel.getName());

        if(!loggers.isEmpty())
        {
            processingChain.addModules(loggers);
        }

        /* Setup recorders */
        List<RecorderType> recorders = channel.getRecordConfiguration().getRecorders();

        if(!recorders.isEmpty())
        {
            /* Add baseband recorder */
            if((recorders.contains(RecorderType.BASEBAND) && channel.getChannelType() == Channel.ChannelType.STANDARD))
            {
                processingChain.addModule(mRecorderManager.getBasebandRecorder(channel.toString()));
            }

            /* Add traffic channel baseband recorder */
            if(recorders.contains(RecorderType.TRAFFIC_BASEBAND) && channel.getChannelType() == Channel.ChannelType.TRAFFIC)
            {
                processingChain.addModule(mRecorderManager.getBasebandRecorder(channel.toString()));
            }

            /* Add decoded bit stream recorder if the decoder supports bitstream output */
            if(DecoderFactory.getBitstreamDecoders().contains(channel.getDecodeConfiguration().getDecoderType()))
            {
                if((recorders.contains(RecorderType.DEMODULATED_BIT_STREAM) &&
                        channel.getChannelType() == Channel.ChannelType.STANDARD))
                {
                    processingChain.addModule(new BinaryRecorder(mRecorderManager.getRecordingBasePath(),
                            channel.toString(), channel.getDecodeConfiguration().getDecoderType().getBitRate()));
                }

                /* Add traffic channel decoded bit stream recorder */
                if(recorders.contains(RecorderType.TRAFFIC_DEMODULATED_BIT_STREAM) &&
                        channel.getChannelType() == Channel.ChannelType.TRAFFIC)
                {
                    processingChain.addModule(new BinaryRecorder(mRecorderManager.getRecordingBasePath(),
                            channel.toString(), channel.getDecodeConfiguration().getDecoderType().getBitRate()));
                }
            }
        }

        processingChain.setSource(source);

        //Inject the channel identifier for traffic channels
        if(channel.isTrafficChannel() && event instanceof ChannelGrantEvent)
        {
            IChannelDescriptor channelDescriptor = ((ChannelGrantEvent)event).getChannelDescriptor();

            if(channelDescriptor != null)
            {
                ChannelDescriptorConfigurationIdentifier identifier = new ChannelDescriptorConfigurationIdentifier(channelDescriptor);
                IdentifierUpdateNotification notification = new IdentifierUpdateNotification(identifier,
                    IdentifierUpdateNotification.Operation.ADD);
                processingChain.getChannelState().updateChannelStateIdentifiers(notification);
            }
        }

        processingChain.start();

        getChannelMetadataModel().add(processingChain.getChannelState().getChannelMetadata(), channel);

        channel.setProcessing(true);

        mProcessingChains.put(channel, processingChain);

        mChannelEventBroadcaster.broadcast(new ChannelEvent(channel, ChannelEvent.Event.NOTIFICATION_PROCESSING_START));
    }

    private void stopProcessing(Channel channel, boolean remove)
    {
        channel.setProcessing(false);

        if(mProcessingChains.containsKey(channel))
        {
            ProcessingChain processingChain = mProcessingChains.get(channel);

            getChannelMetadataModel().remove(processingChain.getChannelState().getChannelMetadata());

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
    }

    /**
     * Stops all currently processing channels to prepare for shutdown.
     */
    public void shutdown()
    {
        mLog.debug("Stopping Channels ...");

        List<Channel> channelsToStop = new ArrayList<>(mProcessingChains.keySet());

        for(Channel channel: channelsToStop)
        {
            mLog.debug("Stopping channel: " + channel.toString());
            stopProcessing(channel, true);
        }
    }

    /**
     * Adds a message listener that will be added to all channels to receive
     * any messages.
     */
    public void addAudioPacketListener(Listener<ReusableAudioPacket> listener)
    {
        mAudioPacketListeners.add(listener);
    }

    /**
     * Removes a message listener.
     */
    public void removeAudioPacketListener(Listener<ReusableAudioPacket> listener)
    {
        mAudioPacketListeners.remove(listener);
    }

    /**
     * Adds a message listener that will be added to all channels to receive
     * any messages.
     */
    public void addMessageListener(Listener<IMessage> listener)
    {
        mMessageListeners.add(listener);
    }

    /**
     * Removes a message listener.
     */
    public void removeMessageListener(Listener<IMessage> listener)
    {
        mMessageListeners.remove(listener);
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

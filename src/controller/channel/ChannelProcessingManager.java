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
package controller.channel;

import alias.AliasModel;
import audio.AudioPacket;
import channel.metadata.Attribute;
import channel.metadata.AttributeChangeRequest;
import channel.metadata.ChannelMetadataModel;
import controller.channel.Channel.ChannelType;
import controller.channel.ChannelEvent.Event;
import controller.channel.map.ChannelMapModel;
import filter.FilterSet;
import message.Message;
import module.Module;
import module.ProcessingChain;
import module.decode.DecoderFactory;
import module.decode.DecoderType;
import module.decode.event.MessageActivityModel;
import module.log.EventLogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import record.RecorderManager;
import record.RecorderType;
import sample.Listener;
import source.Source;
import source.SourceException;
import source.SourceManager;
import source.SourceType;
import source.config.SourceConfigTuner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChannelProcessingManager implements ChannelEventListener
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelProcessingManager.class);

    private Map<Integer,ProcessingChain> mProcessingChains = new HashMap<>();

    private List<Listener<AudioPacket>> mAudioPacketListeners = new CopyOnWriteArrayList<>();
    private List<Listener<Message>> mMessageListeners = new CopyOnWriteArrayList<>();

    private ChannelModel mChannelModel;
    private ChannelMapModel mChannelMapModel;
    private ChannelMetadataModel mChannelMetadataModel = new ChannelMetadataModel();
    private AliasModel mAliasModel;
    private EventLogManager mEventLogManager;
    private RecorderManager mRecorderManager;
    private SourceManager mSourceManager;

    public ChannelProcessingManager(ChannelModel channelModel,
                                    ChannelMapModel channelMapModel,
                                    AliasModel aliasModel,
                                    EventLogManager eventLogManager,
                                    RecorderManager recorderManager,
                                    SourceManager sourceManager)
    {
        mChannelModel = channelModel;
        mChannelMapModel = channelMapModel;
        mAliasModel = aliasModel;
        mEventLogManager = eventLogManager;
        mRecorderManager = recorderManager;
        mSourceManager = sourceManager;
    }

    /**
     * Channel metadata model containing metadata for each channel or channel time-slice that is currently processing.
     */
    public ChannelMetadataModel getChannelMetadataModel()
    {
        return mChannelMetadataModel;
    }

    /**
     * Channel model
     */
    public ChannelModel getChannelModel()
    {
        return mChannelModel;
    }

    /**
     * Indicates if a processing chain is constructed for the channel and that
     * the processing chain is currently processing.
     */
    private boolean isProcessing(Channel channel)
    {
        return mProcessingChains.containsKey(channel.getChannelID()) &&
            mProcessingChains.get(channel.getChannelID()).isProcessing();
    }

    /**
     * Returns the current processing chain associated with the channel, or
     * null if a processing chain is not currently setup for the channel
     */
    public ProcessingChain getProcessingChain(Channel channel)
    {
        return mProcessingChains.get(channel.getChannelID());
    }

    /**
     * Returns the channel associated with the processing chain
     * @param processingChain
     * @return channel associated with the processing chain or null
     */
    public Channel getChannel(ProcessingChain processingChain)
    {
        return mChannelModel.getChannelFromChannelID(getChannelID(processingChain));
    }

    /**
     * Returns the channel ID associated with the processing chain
     */
    private Integer getChannelID(ProcessingChain processingChain)
    {
        if(processingChain != null)
        {
            for(Map.Entry<Integer,ProcessingChain> entry: mProcessingChains.entrySet())
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
    public synchronized void channelChanged(ChannelEvent event)
    {
        Channel channel = event.getChannel();

        switch(event.getEvent())
        {
            case REQUEST_ENABLE:
                if(!mProcessingChains.containsKey(channel.getChannelID()) ||
                    !mProcessingChains.get(channel.getChannelID()).isProcessing())
                {
                    startProcessing(event);
                }
                break;
            case REQUEST_DISABLE:
                if(channel.getEnabled())
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
                if(channel.getEnabled())
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

        ProcessingChain processingChain = mProcessingChains.get(channel.getChannelID());

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
                channel.getDecodeConfiguration().getDecoderType().getChannelBandwidth());
        }
        catch(SourceException se)
        {
            mLog.debug("Error obtaining source for channel [" + channel.getName() + "]", se);
        }

        if(source == null)
        {
            channel.setEnabled(false);

            mChannelModel.broadcast(new ChannelEvent(channel, Event.NOTIFICATION_ENABLE_REJECTED));

            return;
        }

        if(processingChain == null)
        {
            processingChain = new ProcessingChain(channel.getChannelType());

			/* Register global listeners */
            for(Listener<AudioPacket> listener : mAudioPacketListeners)
            {
                processingChain.addAudioPacketListener(listener);
            }

            for(Listener<Message> listener : mMessageListeners)
            {
                processingChain.addMessageListener(listener);
            }

            //Register channel to receive frequency correction events to show in the spectral display (hack!)
            processingChain.addFrequencyChangeListener(channel);

			/* Processing Modules */
            List<Module> modules = DecoderFactory.getModules(mChannelModel, mChannelMapModel, this,
                mAliasModel, channel, processingChain.getChannelState().getMutableMetadata());
            processingChain.addModules(modules);

			/* Setup message activity model with filtering */
            FilterSet<Message> messageFilter = DecoderFactory.getMessageFilters(modules);
            MessageActivityModel messageModel = new MessageActivityModel(messageFilter);
            processingChain.setMessageActivityModel(messageModel);

        }

        //Set the recordable flag to true if the user has requested recording.  The metadata class can still
        //override recordability if any of the aliased values has 'Do Not Record' alias identifier.
        boolean recordable = channel.getRecordConfiguration() != null &&
            channel.getRecordConfiguration().getRecorders().contains(RecorderType.AUDIO);

        processingChain.getChannelState().getMutableMetadata().setRecordable(recordable);

        //Inject channel metadata that will be inserted into audio packets for recorder manager and streaming
        processingChain.getChannelState().getMutableMetadata().receive(
            new AttributeChangeRequest<String>(Attribute.CHANNEL_CONFIGURATION_SYSTEM, channel.getSystem()));
        processingChain.getChannelState().getMutableMetadata().receive(
            new AttributeChangeRequest<String>(Attribute.CHANNEL_CONFIGURATION_SITE, channel.getSite()));
        processingChain.getChannelState().getMutableMetadata().receive(
            new AttributeChangeRequest<String>(Attribute.CHANNEL_CONFIGURATION_NAME, channel.getName()));

        if(channel.getSourceConfiguration().getSourceType() == SourceType.TUNER)
        {
            long frequency = ((SourceConfigTuner)channel.getSourceConfiguration()).getFrequency();

            processingChain.getChannelState().getMutableMetadata().receive(
                new AttributeChangeRequest<Long>(Attribute.CHANNEL_FREQUENCY, frequency));
        }

        processingChain.getChannelState().getMutableMetadata().receive(
            new AttributeChangeRequest<DecoderType>(Attribute.PRIMARY_DECODER_TYPE,
                channel.getDecodeConfiguration().getDecoderType()));

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
            if((recorders.contains(RecorderType.BASEBAND) && channel.getChannelType() == ChannelType.STANDARD))
            {
                processingChain.addModule(mRecorderManager.getBasebandRecorder(channel.toString()));
            }

				/* Add traffic channel baseband recorder */
            if(recorders.contains(RecorderType.TRAFFIC_BASEBAND) && channel.getChannelType() == ChannelType.TRAFFIC)
            {
                processingChain.addModule(mRecorderManager.getBasebandRecorder(channel.toString()));
            }
        }

        processingChain.setSource(source);

        if(event instanceof TrafficChannelEvent)
        {
            TrafficChannelEvent trafficChannelEvent = (TrafficChannelEvent) event;

            processingChain.getChannelState().configureAsTrafficChannel(
                trafficChannelEvent.getTrafficChannelManager(),
                trafficChannelEvent.getCallEvent());
        }

        processingChain.start();

        getChannelMetadataModel().add(processingChain.getChannelState().getMutableMetadata(), channel);

        channel.setEnabled(true);

        mProcessingChains.put(channel.getChannelID(), processingChain);

        mChannelModel.broadcast(new ChannelEvent(channel, Event.NOTIFICATION_PROCESSING_START));
    }

    private void stopProcessing(Channel channel, boolean remove)
    {
        channel.setEnabled(false);

        if(mProcessingChains.containsKey(channel.getChannelID()))
        {
            ProcessingChain processingChain = mProcessingChains.get(channel.getChannelID());

            getChannelMetadataModel().remove(processingChain.getChannelState().getMutableMetadata());

            processingChain.stop();

            processingChain.removeEventLoggingModules();

            processingChain.removeRecordingModules();

            //Deregister channel from receive frequency correction events to show in the spectral display (hack!)
            processingChain.removeFrequencyChangeListener(channel);
            channel.resetFrequencyCorrection();

            mChannelModel.broadcast(new ChannelEvent(channel, Event.NOTIFICATION_PROCESSING_STOP));

            if(remove)
            {
                mProcessingChains.remove(channel.getChannelID());

                processingChain.dispose();
            }
        }
    }

    /**
     * Adds a message listener that will be added to all channels to receive
     * any messages.
     */
    public void addAudioPacketListener(Listener<AudioPacket> listener)
    {
        mAudioPacketListeners.add(listener);
    }

    /**
     * Removes a message listener.
     */
    public void removeAudioPacketListener(Listener<AudioPacket> listener)
    {
        mAudioPacketListeners.remove(listener);
    }

    /**
     * Adds a message listener that will be added to all channels to receive
     * any messages.
     */
    public void addMessageListener(Listener<Message> listener)
    {
        mMessageListeners.add(listener);
    }

    /**
     * Removes a message listener.
     */
    public void removeMessageListener(Listener<Message> listener)
    {
        mMessageListeners.remove(listener);
    }
}

/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.dsheirer.controller.config.Configuration;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.log.EventLogType;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import io.github.dsheirer.record.RecorderType;
import io.github.dsheirer.record.config.RecordConfiguration;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.config.SourceConfigFactory;
import io.github.dsheirer.source.config.SourceConfigRecording;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JacksonXmlRootElement(localName = "channel")
public class Channel extends Configuration implements Listener<SourceEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(Channel.class);

    // Standard channels are persisted and traffic channels are temporary
    public enum ChannelType
    {
        STANDARD, TRAFFIC
    }

    // Static unique channel identifier tracking
    private static int UNIQUE_ID = 0;

    private DecodeConfiguration mDecodeConfiguration = DecoderFactory.getDefaultDecodeConfiguration();
    private AuxDecodeConfiguration mAuxDecodeConfiguration = new AuxDecodeConfiguration();
    private SourceConfiguration mSourceConfiguration = SourceConfigFactory.getDefaultSourceConfiguration();
    private EventLogConfiguration mEventLogConfiguration = new EventLogConfiguration();
    private RecordConfiguration mRecordConfiguration = new RecordConfiguration();

    private StringProperty mAliasListName = new SimpleStringProperty();
    private StringProperty mSystem = new SimpleStringProperty();
    private StringProperty mSite = new SimpleStringProperty();
    private StringProperty mName = new SimpleStringProperty();
    private ObservableList<Long> mFrequencyList;

    private BooleanProperty mProcessing = new SimpleBooleanProperty();
    private BooleanProperty mAutoStart = new SimpleBooleanProperty();
    private IntegerProperty mAutoStartOrder = new SimpleIntegerProperty();
    private boolean mSelected;
    private List<TunerChannel> mTunerChannels = null;

    private ChannelType mChannelType = ChannelType.STANDARD;

    private int mChannelID;
    private int mChannelFrequencyCorrection = 0;

    /**
     * Channel represents a complete set of configurations needed to setup and
     * manage a processing chain and/or manage as a set of business objects that
     * can be displayed in graphical components.
     *
     * @param channelName
     * @param channelType
     */
    public Channel(String channelName, ChannelType channelType)
    {
        this(channelName);
        mChannelType = channelType;
    }

    /**
     * Constructs a new standard channel with the specified channel name
     */
    public Channel(String channelName)
    {
        this();
        mName.set(channelName);
    }

    /**
     * Constructs a new standard channel with a default name of "Channel"
     */
    public Channel()
    {
        mChannelID = UNIQUE_ID++;
    }

    /**
     * Creates a (new) deep copy of this channel
     */
    public Channel copyOf()
    {
        Channel channel = new Channel(mName.get());
        channel.setSystem(mSystem.get());
        channel.setSite(mSite.get());
        channel.setAliasListName(mAliasListName.get());
        channel.setAutoStart(mAutoStart.get());
        channel.setAutoStartOrder(mAutoStartOrder.get());

        AuxDecodeConfiguration auxCopy = new AuxDecodeConfiguration();

        if(getAuxDecodeConfiguration() != null)
        {
            for(DecoderType auxType : getAuxDecodeConfiguration().getAuxDecoders())
            {
                auxCopy.addAuxDecoder(auxType);
            }
        }

        channel.setAuxDecodeConfiguration(auxCopy);

        channel.setDecodeConfiguration(DecoderFactory.copy(mDecodeConfiguration));

        EventLogConfiguration logCopy = new EventLogConfiguration();

        if(mEventLogConfiguration != null)
        {
            for(EventLogType logType : mEventLogConfiguration.getLoggers())
            {
                logCopy.addLogger(logType);
            }
        }

        channel.setEventLogConfiguration(logCopy);

        RecordConfiguration recordCopy = new RecordConfiguration();

        if(mRecordConfiguration != null)
        {
            for(RecorderType recordType : mRecordConfiguration.getRecorders())
            {
                recordCopy.addRecorder(recordType);
            }
        }

        channel.setRecordConfiguration(recordCopy);

        channel.setSourceConfiguration(SourceConfigFactory.copy(mSourceConfiguration));

        return channel;
    }

    /**
     * Creates a short title containing the system, site and channel name where each value is constrained to
     * ten characters each.
     * @return short title
     */
    @JsonIgnore
    public String getShortTitle()
    {
        StringBuilder sb = new StringBuilder();

        if(getSystem() != null)
        {
            if(getSystem().length() > 10)
            {
                sb.append(getSystem().substring(0, 10)).append("..");
            }
            else
            {
                sb.append(getSystem());
            }
        }
        else
        {
            sb.append("No System");
        }

        sb.append("/");

        if(getSite() != null)
        {
            if(getSite().length() > 10)
            {
                sb.append(getSite().substring(0, 10)).append("..");
            }
            else
            {
                sb.append(getSite());
            }
        }
        else
        {
            sb.append("No Site");
        }
        sb.append("/");

        if(getName() != null)
        {
            if(getName().length() > 10)
            {
                sb.append(getName().substring(0, 10)).append("..");
            }
            else
            {
                sb.append(getName());
            }
        }
        else
        {
            sb.append("No Channel");
        }

        return sb.toString();
    }

    /**
     * Updates the frequencies property as the source configuration changes or is initialized.
     */
    private void updateFrequencies()
    {
        getFrequencyList().clear();

        if(mSourceConfiguration instanceof SourceConfigTuner)
        {
            getFrequencyList().add(((SourceConfigTuner)mSourceConfiguration).getFrequency());
        }
        else if(mSourceConfiguration instanceof SourceConfigTunerMultipleFrequency)
        {
            getFrequencyList().addAll(((SourceConfigTunerMultipleFrequency)mSourceConfiguration).getFrequencies());
        }
    }

    /**
     * Alias list name property
     */
    public StringProperty aliasListNameProperty()
    {
        return mAliasListName;
    }

    /**
     * System property
     */
    public StringProperty systemProperty()
    {
        return mSystem;
    }

    /**
     * Site property
     */
    public StringProperty siteProperty()
    {
        return mSite;
    }

    /**
     * Channel name property
     */
    public StringProperty nameProperty()
    {
        return mName;
    }

    /**
     * Processing property.  Indicates if this channel configuration is currently processing.
     */
    public BooleanProperty processingProperty()
    {
        return mProcessing;
    }

    /**
     * Auto-Start property.  Indicates if this channel is setup for auto-start
     */
    public BooleanProperty autoStartProperty()
    {
        return mAutoStart;
    }

    /**
     * Auto-start order property.  Indicates the order for starting channels that are flagged for auto-start.
     */
    public IntegerProperty autoStartOrderProperty()
    {
        return mAutoStartOrder;
    }

    /**
     * Unique identifier for this channel.  Value is transient (not persisted).
     */
    @JsonIgnore
    public int getChannelID()
    {
        return mChannelID;
    }

    /**
     * List of frequencies extracted from the source configuration.  This method only exists to support JavaFX
     * monitoring of frequency list changes.  Use the source configuration to manage frequencies.
     */
    @JsonIgnore
    @Transient
    public ObservableList<Long> getFrequencyList()
    {
        if(mFrequencyList == null)
        {
            mFrequencyList = FXCollections.observableArrayList();
            updateFrequencies();
        }

        return mFrequencyList;
    }

    /**
     * Indicates if this channel has been selected for prioritized audio output
     * and display of processing chain products
     */
    @JsonIgnore
    public boolean isSelected()
    {
        return mSelected;
    }

    /**
     * Sets selection status of the channel.
     *
     * NOTE: this method is package private so that only the ChannelSelectionManager
     * can toggle the selection state.  Use the channel model to broadcast a
     * request select/deselect channel event.
     *
     * @param selected
     */
    void setSelected(boolean selected)
    {
        mSelected = selected;
    }

    @JsonIgnore
    public ChannelType getChannelType()
    {
        return mChannelType;
    }

    @JsonIgnore
    public boolean isTrafficChannel()
    {
        return mChannelType == ChannelType.TRAFFIC;
    }

    @JsonIgnore
    public boolean isStandardChannel()
    {
        return mChannelType == ChannelType.STANDARD;
    }

    /**
     * Returns the owning system for this channel.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "system")
    public String getSystem()
    {
        return mSystem.get();
    }

    public void setSystem(String system)
    {
        mSystem.set(system);
    }

    public boolean hasSystem()
    {
        return mSystem != null && mSystem.get() != null;
    }

    /**
     * Returns the owning site for this channel.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "site")
    public String getSite()
    {
        return mSite.get();
    }

    public void setSite(String site)
    {
        mSite.set(site);
    }

    public boolean hasSite()
    {
        return mSite != null && mSite.get() != null;
    }

    /**
     * Returns the name of this channel.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "name")
    public String getName()
    {
        return mName.get();
    }

    /**
     * Sets the name of this channel.
     */
    public void setName(String name)
    {
        mName.set(name);
    }

    /**
     * Default display string for this channel: SYSTEM_SITE_NAME
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(hasSystem() ? getSystem() : "SYSTEM");
        sb.append("_");
        sb.append(hasSite() ? getSite() : "SITE");
        sb.append("_");
        sb.append(getName());
        sb.append("_");
        sb.append(getChannelID());

        return sb.toString();
    }

    /**
     * Indicates if this channel is currently started and processing.
     */
    @JsonIgnore
    public boolean isProcessing()
    {
        return mProcessing.get();
    }

    /**
     * Indicates if this channel is currently started and processing.
     *
     * Note: this method is package private and is intended to be managed by a channel processing manager instance
     *
     * @see ChannelProcessingManager
     */
    void setProcessing(boolean processing)
    {
        mProcessing.set(processing);
    }

    /**
     * Indicates if this channel has auto-start enabled.
     *
     * Note: the Json (de)serialization tag for auto-start is 'enabled'.  We've re-purposed the now
     * transient enabled property for the auto-start persisted value to maintain backward compatibility.
     *
     * @return true if this channel should be auto-started on application startup
     */
    @JacksonXmlProperty(isAttribute = true, localName = "enabled")
    public boolean getAutoStart()
    {
        return mAutoStart.get();
    }

    /**
     * Indicates if this channel is set for auto-start
     */
    @JsonIgnore
    public boolean isAutoStart()
    {
        return mAutoStart.get();
    }

    /**
     * Sets the auto-start status for this channel.
     *
     * @param autoStart
     */
    public void setAutoStart(boolean autoStart)
    {
        mAutoStart.set(autoStart);
    }

    /**
     * Auto-start order.  Specifies an optional ordering for channels so that when the application starts up,
     * the channels can be started in a user-specified order.
     *
     * @return auto-start order or null
     */
    @JacksonXmlProperty(isAttribute = true, localName = "order")
    public Integer getAutoStartOrder()
    {
        return mAutoStartOrder.get();
    }

    /**
     * Sets the auto-start order for this channel
     *
     * @param order
     */
    public void setAutoStartOrder(Integer order)
    {
        if(order != null)
        {
            mAutoStartOrder.set(order);
        }
        else
        {
            mAutoStartOrder.setValue(null);
        }
    }

    /**
     * Indicates if this channel has an auto-start order specified (ie non-null).
     *
     * @return true if this channel has an auto-start order
     */
    @JsonIgnore
    public boolean hasAutoStartOrder()
    {
        return mAutoStartOrder != null && mAutoStartOrder.getValue() != null;
    }

    /**
     * Returns the alias list that is used by this channel for looking up alias
     * values for the various identifiers produced by the decoder(s).
     */
    @JacksonXmlProperty(isAttribute = false, localName = "alias_list_name")
    public String getAliasListName()
    {
        return mAliasListName.get();
    }

    /**
     * Sets the alias list to be used for looking up the alias values for the
     * various identifiers produced by the decoder
     */
    public void setAliasListName(String name)
    {
        mAliasListName.set(name);
    }

    /**
     * Gets the primary decoder configuration used by this channel
     */
    @JacksonXmlProperty(isAttribute = false, localName = "decode_configuration")
    public DecodeConfiguration getDecodeConfiguration()
    {
        return mDecodeConfiguration;
    }

    /**
     * Sets the primary decoder configuration used by this channel
     */
    public void setDecodeConfiguration(DecodeConfiguration config)
    {
        if(config != null)
        {
            mDecodeConfiguration = config;
        }
    }

    /**
     * Gets the aux decoder configuration used by this channel.  Aux decoders
     * operate on demodulated audio
     */
    @JacksonXmlProperty(isAttribute = false, localName = "aux_decode_configuration")
    public AuxDecodeConfiguration getAuxDecodeConfiguration()
    {
        return mAuxDecodeConfiguration;
    }

    /**
     * Sets the decoder configuration used by this channel
     */
    public void setAuxDecodeConfiguration(AuxDecodeConfiguration config)
    {
        if(config != null)
        {
            mAuxDecodeConfiguration = config;
        }
    }

    /**
     * Returns the source configuration for this channel.  A channel source
     * identifies where the processing chain will get source sample data from.
     */
    @JacksonXmlProperty(isAttribute = false, localName = "source_configuration")
    public SourceConfiguration getSourceConfiguration()
    {
        return mSourceConfiguration;
    }

    /**
     * Sets the source configuration for this channel.
     */
    public void setSourceConfiguration(SourceConfiguration config)
    {
        if(config != null)
        {
            mSourceConfiguration = config;
        }

        updateFrequencies();

        //Clear the tune channels object so that it can be recreated if the source configuration changes
        mTunerChannels = null;
    }

    /**
     * Returns the event logger configuration for this channel.
     */
    @JacksonXmlProperty(isAttribute = false, localName = "event_log_configuration")
    public EventLogConfiguration getEventLogConfiguration()
    {
        return mEventLogConfiguration;
    }

    /**
     * Sets the event logger configuration for this channel.
     */
    public void setEventLogConfiguration(EventLogConfiguration config)
    {
        if(config != null)
        {
            mEventLogConfiguration = config;
        }
    }

    /**
     * Returns the recorder configuration for this channel.
     */
    @JacksonXmlProperty(isAttribute = false, localName = "record_configuration")
    public RecordConfiguration getRecordConfiguration()
    {
        return mRecordConfiguration;
    }

    /**
     * Sets the recorder configuration for this channel.
     */
    public void setRecordConfiguration(RecordConfiguration config)
    {
        if(config != null)
        {
            mRecordConfiguration = config;
        }
    }

    /**
     * Convenience method to construct a tuner channel object representing a tuner or recording source frequency and
     * bandwidth that can be used by application components for graphically representing this channel.
     *
     * If the source configuration is not a tuner or recording, this method returns an empty list.
     */
    @JsonIgnore
    public List<TunerChannel> getTunerChannels()
    {
        if(mTunerChannels == null && mSourceConfiguration != null)
        {
            mTunerChannels = new ArrayList<>();

            switch(mSourceConfiguration.getSourceType())
            {
                case TUNER:
                    if(mSourceConfiguration instanceof SourceConfigTuner tunerConfig)
                    {
                        mTunerChannels.add(new TunerChannel(tunerConfig.getFrequency(),
                                mDecodeConfiguration.getChannelSpecification().getBandwidth()));
                    }
                    break;
                case TUNER_MULTIPLE_FREQUENCIES:
                    if(mSourceConfiguration instanceof SourceConfigTunerMultipleFrequency multiConfig)
                    {
                        for(long frequency: multiConfig.getFrequencies())
                        {
                            mTunerChannels.add(new TunerChannel(frequency,
                                    mDecodeConfiguration.getChannelSpecification().getBandwidth()));
                        }
                    }
                    break;
                case RECORDING:
                    if(mSourceConfiguration instanceof  SourceConfigRecording recordingConfig)
                    {
                        mTunerChannels.add(new TunerChannel(recordingConfig.getFrequency(),
                                mDecodeConfiguration.getChannelSpecification().getBandwidth()));
                    }
                    break;
                default:
                    mLog.warn("Unrecognized channel source type: " + mSourceConfiguration.getSourceType());
            }
        }

        return mTunerChannels;
    }

    /**
     * Convenience method to indicate if any part of this channel is contained
     * within the minimum and maximum frequency values.
     */
    public boolean isWithin(long minimum, long maximum)
    {
        for(TunerChannel tunerChannel: getTunerChannels())
        {
            if(tunerChannel.overlaps(minimum, maximum))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Convenience method to make the current channel frequency correction value
     * available for use outside of a constructed processing chain while maintaining
     * linkage to the source channel.  This hack allows the correction value to
     * be used to visually show in the spectral display.
     */
    @Override
    public void receive(SourceEvent event)
    {
        if(event.getEvent() == SourceEvent.Event.NOTIFICATION_CHANNEL_FREQUENCY_CORRECTION_CHANGE)
        {
            mChannelFrequencyCorrection = event.getValue().intValue();
        }
    }

    /**
     * Current channel frequency correction value (when this channel is processing)
     */
    @JsonIgnore
    public int getChannelFrequencyCorrection()
    {
        return mChannelFrequencyCorrection;
    }

    /**
     * Resets frequency correction value to 0
     */
    public void resetFrequencyCorrection()
    {
        mChannelFrequencyCorrection = 0;
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
        {
            return true;
        }
        if(o == null || getClass() != o.getClass())
        {
            return false;
        }
        Channel channel = (Channel)o;
        return getChannelID() == channel.getChannelID();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getChannelID());
    }

    /**
     * Creates an observable property extractor for use with observable lists to detect changes internal to this object.
     */
    public static Callback<Channel,Observable[]> extractor()
    {
        return (Channel c) -> new Observable[] {c.processingProperty(), c.nameProperty(), c.aliasListNameProperty(),
            c.autoStartOrderProperty(), c.autoStartProperty(), c.siteProperty(), c.systemProperty(),
            c.getFrequencyList()};
    }
}

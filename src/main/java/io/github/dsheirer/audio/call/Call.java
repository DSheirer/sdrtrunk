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

package io.github.dsheirer.audio.call;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import javafx.application.Platform;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Call database entity.
 */
@Entity
@Table(name="calls", schema="sdrtrunk")
public class Call
{
    public static final String COLUMN_CALL_TYPE = "callType";
    public static final String COLUMN_CHANNEL = "channel";
    public static final String COLUMN_DUPLICATE = "duplicate";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_EVENT_TIME = "eventTime";
    public static final String COLUMN_FILE = "file";
    public static final String COLUMN_FREQUENCY = "frequency";
    public static final String COLUMN_FROM_ALIAS = "fromAlias";
    public static final String COLUMN_FROM_ID = "fromId";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_MONITOR = "monitor";
    public static final String COLUMN_PROTOCOL = "protocol";
    public static final String COLUMN_RECORD = "record";
    public static final String COLUMN_STREAM = "stream";
    public static final String COLUMN_TO_ALIAS = "toAlias";
    public static final String COLUMN_TO_ID = "toId";
    public static final String COLUMN_SITE = "site";
    public static final String COLUMN_SYSTEM = "system";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator")
    @SequenceGenerator(name = "id_generator", allocationSize = 1)
    @Column(name=COLUMN_ID)
    private long id;
    @Column(name=COLUMN_CALL_TYPE)
    private String callType;
    @Column(name=COLUMN_CHANNEL)
    private String channel;
    @Column(name=COLUMN_DUPLICATE)
    private boolean duplicate;
    @Column(name=COLUMN_DURATION)
    private double duration;
    @Column(name=COLUMN_EVENT_TIME)
    private long eventTime;
    @Column(name=COLUMN_FILE)
    private String file;
    @Column(name=COLUMN_FREQUENCY)
    private double frequency;
    @Column(name=COLUMN_FROM_ALIAS)
    private String fromAlias;
    @Column(name=COLUMN_FROM_ID)
    private String fromId;
    @Column(name=COLUMN_MONITOR)
    private int monitor;
    @Column(name=COLUMN_PROTOCOL)
    private String protocol;
    @Column(name=COLUMN_RECORD)
    private boolean record;
    @Column(name=COLUMN_SITE)
    private String site;
    @Column(name=COLUMN_STREAM)
    private boolean stream;
    @Column(name=COLUMN_SYSTEM)
    private String system;
    @Column(name=COLUMN_TO_ALIAS)
    private String toAlias;
    @Column(name=COLUMN_TO_ID)
    private String toId;

    @Transient
    private boolean mComplete = true;
    @Transient
    private LongProperty mLastUpdated = new SimpleLongProperty();
    @Transient
    private StringProperty mPlaybackChannel = new SimpleStringProperty();

    /**
     * Constructs an instance
     */
    public Call()
    {
    }

    /**
     * Record ID, auto-assigned by the database.
     * @return id
     */
    public long getId()
    {
        return id;
    }

    /**
     * Timestamp for the event
     * @return timestamp in milliseconds since epoch
     */
    public long getEventTime()
    {
        return eventTime;
    }

    /**
     * Sets the timestamp
     * @param eventTime in ms.
     */
    public void setEventTime(long eventTime)
    {
        this.eventTime = eventTime;
    }

    /**
     * From identifier
     * @return from ID
     */
    public String getFromId()
    {
        return fromId;
    }

    /**
     * Sets the from identifier
     * @param fromId ID
     */
    public void setFromId(String fromId)
    {
        this.fromId = fromId;
        updateTimestamp();
    }

    /**
     * From alias
     * @return from alias
     */
    public String getFromAlias()
    {
        return fromAlias;
    }

    /**
     * Sets the from alias value.
     * @param fromAlias value
     */
    public void setFromAlias(String fromAlias)
    {
        this.fromAlias = fromAlias;
        updateTimestamp();
    }

    /**
     * To identifier
     * @return to
     */
    public String getToId()
    {
        return toId;
    }

    /**
     * Sets the to identifier
     * @param toId value
     */
    public void setToId(String toId)
    {
        this.toId = toId;
        updateTimestamp();
    }

    /**
     * To alias value
     * @return alias value
     */
    public String getToAlias()
    {
        return toAlias;
    }

    /**
     * Sets the to alias value.
     * @param toAlias value
     */
    public void setToAlias(String toAlias)
    {
        this.toAlias = toAlias;
        updateTimestamp();
    }

    /**
     * System name
     * @return name
     */
    public String getSystem()
    {
        return system;
    }

    /**
     * Sets the system name
     * @param system name
     */
    public void setSystem(String system)
    {
        this.system = system;
    }

    /**
     * Site name
     * @return site
     */
    public String getSite()
    {
        return site;
    }

    /**
     * Sets the site name
     * @param site name
     */
    public void setSite(String site)
    {
        this.site = site;
    }

    /**
     * Channel name
     * @return channel
     */
    public String getChannel()
    {
        return channel;
    }

    /**
     * Sets the channel name
     * @param channel name
     */
    public void setChannel(String channel)
    {
        this.channel = channel;
    }

    /**
     * Frequency for the channel
     * @return frequency.
     */
    public double getFrequency()
    {
        return frequency;
    }

    /**
     * Sets the frequency value for the channel.
     * @param frequency value.
     */
    public void setFrequency(double frequency)
    {
        this.frequency = frequency;
    }

    /**
     * File path for the call
     * @return file path
     */
    public String getFile()
    {
        return file;
    }

    /**
     * Sets the file path for the call.
     * @param file path
     */
    public void setFile(String file)
    {
        this.file = file;
    }

    /**
     * Duration of the call in seconds
     * @return duration seconds
     */
    public double getDuration()
    {
        return duration;
    }

    /**
     * Sets the call duration.
     * @param duration value in seconds
     */
    public void setDuration(double duration)
    {
        this.duration = duration;
        updateTimestamp();
    }

    /**
     * Radio protocol
     * @return protocol
     */
    public String getProtocol()
    {
        return protocol;
    }

    /**
     * Sets the radio protocol
     * @param protocol for the call
     */
    public void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }

    /**
     * Indicates if this call is complete and will receive no further updates.
     * @return true if complete.
     */
    public boolean isComplete()
    {
        return mComplete;
    }

    /**
     * Sets the complete status for this call.
     * @param complete true if complete
     */
    public void setComplete(boolean complete)
    {
        mComplete = complete;
        updateTimestamp();
    }

    /**
     * Call type
     * @return call type
     */
    public String getCallType()
    {
        return callType;
    }

    /**
     * Sets the call type
     * @param callType of call
     */
    public void setCallType(String callType)
    {
        this.callType = callType;
    }

    /**
     * Indicates if this call was flagged as a duplicate
     * @return true if duplicate
     */
    public boolean isDuplicate()
    {
        return duplicate;
    }

    /**
     * Sets the duplicate flag for this call.
     * @param duplicate flag
     */
    public void setDuplicate(boolean duplicate)
    {
        this.duplicate = duplicate;
    }

    /**
     * Monitor/listen priority
     * @return priority where a -1 value is do not monitor.
     */
    public int getMonitor()
    {
        return monitor;
    }

    /**
     * Sets the monitor/listen priority
     * @param monitor priority
     */
    public void setMonitor(int monitor)
    {
        this.monitor = monitor;
    }

    /**
     * Indicates if the call is recordable/retainable.
     * @return is recordable
     */
    public boolean isRecord()
    {
        return record;
    }

    /**
     * Sets the record/retain flag.
     * @param record flag
     */
    public void setRecord(boolean record)
    {
        this.record = record;
    }

    /**
     * Indicates if the call is streamable.
     * @return streamable
     */
    public boolean isStream()
    {
        return stream;
    }

    /**
     * Sets the stream flag
     * @param stream flag
     */
    public void setStream(boolean stream)
    {
        this.stream = stream;
    }

    /**
     * Updates the last updated timestamp.
     */
    private void updateTimestamp()
    {
        if(Platform.isFxApplicationThread())
        {
            lastUpdatedProperty().set(System.currentTimeMillis());
        }
        else
        {
            Platform.runLater(() -> lastUpdatedProperty().set(System.currentTimeMillis()));
        }
    }

    /**
     * Last updated timestamp.  This value is updated each time any attribute in the call record is updated so that
     * consumer processes can monitor this property to be notified of updates.
     * @return timestamp of last update in milliseconds.
     */
    public LongProperty lastUpdatedProperty()
    {
        return mLastUpdated;
    }

    /**
     * Current playback channel for this call.
     * @return current playback channel or null if this call is not being played back.
     */
    public StringProperty playbackChannelProperty()
    {
        return mPlaybackChannel;
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof Call c && getId() == c.getId();
    }
}


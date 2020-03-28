/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
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
package io.github.dsheirer.alias.id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.esn.Esn;
import io.github.dsheirer.alias.id.legacy.fleetsync.FleetsyncID;
import io.github.dsheirer.alias.id.legacy.mdc.MDC1200ID;
import io.github.dsheirer.alias.id.legacy.mobileID.Min;
import io.github.dsheirer.alias.id.legacy.mpt1327.MPT1327ID;
import io.github.dsheirer.alias.id.legacy.nonrecordable.NonRecordable;
import io.github.dsheirer.alias.id.legacy.siteID.SiteID;
import io.github.dsheirer.alias.id.legacy.talkgroup.LegacyTalkgroupID;
import io.github.dsheirer.alias.id.legacy.uniqueID.UniqueID;
import io.github.dsheirer.alias.id.lojack.LoJackFunctionAndID;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.alias.id.radio.RadioRange;
import io.github.dsheirer.alias.id.record.Record;
import io.github.dsheirer.alias.id.status.StatusID;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.alias.id.talkgroup.TalkgroupRange;
import io.github.dsheirer.controller.channel.Channel;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Callback;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BroadcastChannel.class, name = "broadcastChannel"),
    @JsonSubTypes.Type(value = Esn.class, name = "esn"),
    @JsonSubTypes.Type(value = FleetsyncID.class, name = "fleetsyncID"),
    @JsonSubTypes.Type(value = LoJackFunctionAndID.class, name = "loJackFunctionAndID"),
    @JsonSubTypes.Type(value = MDC1200ID.class, name = "mdc1200ID"),
    @JsonSubTypes.Type(value = Min.class, name = "min"),
    @JsonSubTypes.Type(value = MPT1327ID.class, name = "mpt1327ID"),
    @JsonSubTypes.Type(value = NonRecordable.class, name = "nonRecordable"),
    @JsonSubTypes.Type(value = Radio.class, name = "radio"),
    @JsonSubTypes.Type(value = RadioRange.class, name = "radioRange"),
    @JsonSubTypes.Type(value = Talkgroup.class, name = "talkgroup"),
    @JsonSubTypes.Type(value = TalkgroupRange.class, name = "talkgroupRange"),
    @JsonSubTypes.Type(value = Priority.class, name = "priority"),
    @JsonSubTypes.Type(value = Record.class, name = "record"),
    @JsonSubTypes.Type(value = SiteID.class, name = "siteID"),
    @JsonSubTypes.Type(value = StatusID.class, name = "statusID"),
    @JsonSubTypes.Type(value = LegacyTalkgroupID.class, name = "talkgroupID"),
    @JsonSubTypes.Type(value = UniqueID.class, name = "uniqueID")
})
@JacksonXmlRootElement(localName = "id")
public abstract class AliasID
{
    private SimpleStringProperty mValueProperty = new SimpleStringProperty();

    public AliasID()
    {
    }

    /**
     * String property representation of this alias ID
     */
    @JsonIgnore
    public SimpleStringProperty valueProperty()
    {
        return mValueProperty;
    }

    /**
     * Updates the value property for this alias ID.  Note: this method is intended to be invoked by all subclasses
     * each time that any of the subclass member variable values change.
     */
    protected void updateValueProperty()
    {
        valueProperty().set(toString());

        //Hack: harmless, but the list extractor does not consistently update unless we do this.
        valueProperty().get();
    }

    @JsonIgnore
    public abstract AliasIDType getType();

    public abstract boolean matches(AliasID id);

    @JsonIgnore
    public abstract boolean isValid();

    /**
     * Indicates if the identifier is an audio identifier (monitor, record, stream)
     */
    @JsonIgnore
    public abstract boolean isAudioIdentifier();

    /**
     * Creates an observable property extractor for use with observable lists to detect changes internal to this object.
     */
    public static Callback<AliasID,Observable[]> extractor()
    {
        return (AliasID aid) -> new Observable[] {aid.valueProperty()};
    }
}

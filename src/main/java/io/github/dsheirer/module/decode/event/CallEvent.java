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
package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.channel.traffic.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.DecoderType;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

public abstract class CallEvent implements Comparable<CallEvent>
{
    protected long mEventStart = System.currentTimeMillis();
    protected long mEventEnd;

    private SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyyMMdd");
    private SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("HHmmss");

    protected DecoderType mDecoderType;
    protected CallEventType mCallEventType;
    protected AliasList mAliasList;
    protected String mFromID;
    protected String mToID;
    protected String mDetails;

    //New call event format member variables
    protected IChannelDescriptor mChannelDescriptor;
    protected Collection<Identifier> mIdentifiers;

    private boolean mValid = true;

    /**
     * Constructs a call event
     * @param decoder type
     * @param callEventType description
     * @param channelDescriptor that identifies the channel and frequency value(s)
     * @param identifiers for to, from, comments, etc.
     */
    public CallEvent(DecoderType decoder, CallEventType callEventType, IChannelDescriptor channelDescriptor, Collection<Identifier> identifiers)
    {
        mDecoderType = decoder;
        mCallEventType = callEventType;
        mChannelDescriptor = channelDescriptor;
        mIdentifiers = identifiers;
    }

    /**
     * Legacy call event constructor
     */
    @Deprecated //Remove once all decoder states have been updated to new constructor
    public CallEvent(DecoderType decoder, CallEventType callEventType, AliasList aliasList, String fromID,
                     String toID, String details)
    {
        mDecoderType = decoder;
        mCallEventType = callEventType;
        mAliasList = aliasList;
        mFromID = fromID;
        mToID = toID;
        mDetails = details;
    }

    /**
     * Identifier collection for this call event
     */
    public Collection<Identifier> getIdentifiers()
    {
        return mIdentifiers;
    }

    /**
     * Indicates if this event is valid.  Use this method to mark a previously
     * dispatched event as invalid and resend it so that any downstream
     * processing can remove the invalid event.
     */
    public void setValid(boolean valid)
    {
        mValid = valid;
    }

    public boolean isValid()
    {
        return mValid;
    }

    public void setEnd(long end)
    {
        mEventEnd = end;
    }

    public void end()
    {
        setEnd(System.currentTimeMillis());
    }

    public DecoderType getDecoderType()
    {
        return mDecoderType;
    }

    public CallEventType getCallEventType()
    {
        return mCallEventType;
    }

    public void setCallEventType(CallEventType type)
    {
        mCallEventType = type;
    }

    public AliasList getAliasList()
    {
        return mAliasList;
    }

    public void setAliasList(AliasList aliasList)
    {
        mAliasList = aliasList;
    }

    public boolean hasAliasList()
    {
        return mAliasList != null;
    }

    public long getEventStartTime()
    {
        return mEventStart;
    }

    public long getEventEndTime()
    {
        return mEventEnd;
    }

    public String getFromID()
    {
        return mFromID;
    }

    public boolean hasFromID()
    {
        return mFromID != null;
    }

    public void setFromID(String id)
    {
        mFromID = id;
    }

    public String getToID()
    {
        return mToID;
    }

    public boolean hasToID()
    {
        return mToID != null;
    }

    public void setToID(String id)
    {
        mToID = id;
    }

    public String getDetails()
    {
        return mDetails;
    }

    public void setDetails(String details)
    {
        mDetails = details;
    }

    public abstract Alias getFromIDAlias();

    public abstract Alias getToIDAlias();

    public abstract String getChannel();

    public abstract long getFrequency();

    public static String getCSVHeader()
    {
        return "START_DATE,START_TIME,END_DATE,END_TIME,DECODER,EVENT,FROM,FROM_ALIAS,TO,TO_ALIAS,CHANNEL_NUMBER," +
            "FREQUENCY,DETAILS";
    }

    public String toCSV()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("'");

        Date eventStart = new Date(getEventStartTime());
        sb.append(DATE_FORMATTER.format(eventStart));
        sb.append("','");
        sb.append(TIME_FORMATTER.format(eventStart));
        sb.append("','");
        if(mEventEnd != 0)
        {
            Date eventEnd = new Date(getEventEndTime());
            sb.append(DATE_FORMATTER.format(eventEnd));
            sb.append("','");
            sb.append(TIME_FORMATTER.format(eventEnd));
            sb.append("','");
        }
        else
        {
            sb.append("','',");
        }
        sb.append("','");
        sb.append(getDecoderType().toString());
        sb.append("','");
        sb.append(getCallEventType().toString());
        sb.append("',");
        if(getFromID() != null)
        {
            sb.append("'");
            sb.append(getFromID());
            sb.append("'");
        }
        sb.append(",");

        Alias fromAlias = getFromIDAlias();

        if(fromAlias != null)
        {
            sb.append("'");
            sb.append(fromAlias.getName());
            sb.append("'");
        }
        sb.append(",");

        if(getToID() != null)
        {
            sb.append("'");
            sb.append(getToID());
            sb.append("'");
        }
        sb.append(",");

        Alias toAlias = getToIDAlias();

        if(toAlias != null)
        {
            sb.append("'");
            sb.append(toAlias.getName());
            sb.append("'");
        }
        sb.append(",'");
        sb.append(getChannel());
        sb.append("','");
        sb.append(getFrequency());
        sb.append("',");
        if(getDetails() != null)
        {
            sb.append("'");
            sb.append(getDetails());
            sb.append("'");
        }

        return sb.toString();
    }

    public enum CallEventType
    {
        ANNOUNCEMENT("Announcement"),
        AUTOMATIC_REGISTRATION_SERVICE("Motorola ARS"),
        CALL("Call"),
        CALL_ALERT("Call Alert"),
        CALL_DETECT("Call Detect"),
        CALL_DO_NOT_MONITOR("Call-Do Not Monitor"),
        CALL_END("Call End"),
        CALL_UNIQUE_ID("UID Call"),
        CALL_NO_TUNER("Call - No Tuner"),
        CALL_TIMEOUT("Call Timeout"),
        COMMAND("Command"),
        DATA_CALL("Data Call"),
        DATA_PACKET("Data Packet"),
        DEREGISTER("Deregister"),
        EMERGENCY("EMERGENCY"),
        ENCRYPTED_CALL("Encrypted Call"),
        FUNCTION("Function"),
        GPS("GPS"),
        GROUP_CALL("Group Call"),
        ID_ANI("ANI"),
        ID_UNIQUE("Unique ID"),
        IP_PACKET("IP Packet"),
        NOTIFICATION("Notification"),
        PAGE("Page"),
        PATCH_GROUP_ADD("Patch Group Add"),
        PATCH_GROUP_CALL("Patch Group Call"),
        PATCH_GROUP_DELETE("Patch Group Delete"),
        QUERY("Query"),
        REGISTER("Register"),
        REGISTER_ESN("ESN"),
        RESPONSE("Response"),
        RESPONSE_PACKET("Response Packet"),
        SDM("Short Data Message"),
        SNDCP_PACKET("SNDCP Packet"),
        STATION_ID("Station ID"),
        STATUS("Status"),
        TCP_PACKET("TCP/IP Packet"),
        TELEPHONE_INTERCONNECT("Telephone Interconnect"),
        UDP_PACKET("UDP/IP Packet"),
        UNIT_TO_UNIT_CALL("Unit To Unit Call"),
        UNKNOWN("Unknown"),
        INVALID("Invalid");

        private String mLabel;

        private CallEventType(String label)
        {
            mLabel = label;
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

    @Override
    public int compareTo(CallEvent other)
    {
        if(other != null && other == this)
        {
            return 0;
        }

        return Long.compare(mEventStart, other.getEventStartTime());
    }
}

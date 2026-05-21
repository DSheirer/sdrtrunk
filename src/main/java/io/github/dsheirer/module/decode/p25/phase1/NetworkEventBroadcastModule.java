/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.IDecodeEventListener;
import io.github.dsheirer.sample.Listener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module that receives decode events and broadcasts them as JSON to the NetworkStreamManager event stream.
 */
public class NetworkEventBroadcastModule extends Module implements IDecodeEventListener, Listener<IDecodeEvent>
{
    private static final Logger mLog = LoggerFactory.getLogger(NetworkEventBroadcastModule.class);
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    

    private final String mSystemName;
    private final NetworkStreamManager mStreamManager;

    /**
     * Constructs an instance.
     * @param systemName for inclusion in JSON output
     * @param streamManager to broadcast to
     */
    public NetworkEventBroadcastModule(String systemName, NetworkStreamManager streamManager)
    {
        mSystemName = systemName;
        mStreamManager = streamManager;
    }

    @Override
    public void start()
    {
        // no-op
    }

    @Override
    public void stop()
    {
        // no-op
    }

    @Override
    public void reset()
    {
        // no-op
    }

    @Override
    public Listener<IDecodeEvent> getDecodeEventListener()
    {
        return this;
    }

    @Override
    public void receive(IDecodeEvent event)
    {
        if(event != null)
        {
            mStreamManager.broadcastEvent(toJson(event));
        }
    }

    private String toJson(IDecodeEvent event)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        appendField(sb, "pipe", "events", true);
        appendField(sb, "system", mSystemName, false);
        appendField(sb, "timestamp", LocalDateTime.now().format(TIMESTAMP_FMT), false);

        sb.append(",\"durationMs\":");
        sb.append(event.getDuration());

        appendField(sb, "protocol", safeString(event.getProtocol()), false);
        appendField(sb, "event", safeString(event.getEventType()), false);

        // FROM identifier
        Identifier fromId = event.getIdentifierCollection().getFromIdentifier();
        appendField(sb, "from", fromId != null ? fromId.toString() : "", false);

        // TO identifier
        Identifier toId = event.getIdentifierCollection().getToIdentifier();
        appendField(sb, "to", toId != null ? toId.toString() : "", false);

        // Channel descriptor
        IChannelDescriptor descriptor = event.getChannelDescriptor();
        appendField(sb, "channel", descriptor != null ? descriptor.toString() : "", false);

        // Frequency
        String freqStr = "";
        if(descriptor != null && descriptor.getDownlinkFrequency() > 0)
        {
            freqStr = String.format("%.6f", descriptor.getDownlinkFrequency() / 1_000_000.0);
        }
        else
        {
            Identifier freqId = event.getIdentifierCollection()
                    .getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL_FREQUENCY, Role.ANY);
            if(freqId instanceof FrequencyConfigurationIdentifier fci)
            {
                freqStr = String.format("%.6f", fci.getValue() / 1_000_000.0);
            }
        }
        appendField(sb, "frequency", freqStr, false);

        // Details
        String details = event.getDetails();
        appendField(sb, "details", details != null ? details : "", false);

        // Event ID
        sb.append(",\"eventId\":").append(event.hashCode());

        sb.append("}");
        return sb.toString();
    }

    private void appendField(StringBuilder sb, String key, String value, boolean first)
    {
        if(!first)
        {
            sb.append(",");
        }
        sb.append("\"").append(escape(key)).append("\":\"").append(escape(value)).append("\"");
    }

    private String safeString(Object obj)
    {
        return obj != null ? obj.toString() : "";
    }

    private String escape(String s)
    {
        if(s == null)
        {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

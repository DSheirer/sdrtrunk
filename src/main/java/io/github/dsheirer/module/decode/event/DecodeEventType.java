/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.event;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * Enumeration of event types for decoded events.
 */
public enum DecodeEventType
{
    AFFILIATE("Affiliate"),
    ANNOUNCEMENT("Announcement"),
    ACKNOWLEDGE("Acknowledge"),
    AUTOMATIC_REGISTRATION_SERVICE("Motorola ARS"),
    CALL("Call"),
    CALL_ENCRYPTED("Encrypted Call"),
    CALL_GROUP("Group Call"),
    CALL_GROUP_ENCRYPTED("Encrypted Group Call"),
    CALL_PATCH_GROUP("Patch Call"),
    CALL_PATCH_GROUP_ENCRYPTED("Encrypted Patch Call"),
    CALL_ALERT("Call Alert"),
    CALL_DETECT("Call Detect"),
    CALL_IN_PROGRESS("Call In Progress"),
    CALL_DO_NOT_MONITOR("Call-Do Not Monitor"),
    CALL_END("Call End"),
    CALL_INTERCONNECT("Telephone Call"),
    CALL_INTERCONNECT_ENCRYPTED("Encrypted Telephone Call"),
    CALL_UNIQUE_ID("UID Call"),
    CALL_UNIT_TO_UNIT("Unit To Unit Call"),
    CALL_UNIT_TO_UNIT_ENCRYPTED("Encrypted Unit To Unit Call"),
    CALL_NO_TUNER("Call - No Tuner"),
    CALL_TIMEOUT("Call Timeout"),
    CELLOCATOR("Cellocator"),
    COMMAND("Command"),
    DATA_CALL("Data Call"),
    DATA_CALL_ENCRYPTED("Encrypted Data Call"),
    DATA_PACKET("Data Packet"),
    DEREGISTER("Deregister"),
    DYNAMIC_REGROUP("Dynamic Regroup"),
    EMERGENCY("EMERGENCY"),
    FUNCTION("Function"),
    GPS("GPS"),
    ICMP_PACKET("ICMP Packet"),
    ID_ANI("ANI"),
    ID_UNIQUE("Unique ID"),
    IP_PACKET("IP Packet"),
    LRRP("Motorola LRRP"),
    NOTIFICATION("Notification"),
    PAGE("Page"),
    QUERY("Query"),
    RADIO_CHECK("Radio Check"),
    RADIO_REGISTRATION_SERVICE("Hytera RRS"),
    REGISTER("Register"),
    REGISTER_ESN("ESN"),
    REQUEST("Request"),
    RESPONSE("Response"),
    RESPONSE_PACKET("Response Packet"),
    SDM("Short Data Message"),
    SMS("SMS"),
    STATION_ID("Station ID"),
    STATUS("Status"),
    TEXT_MESSAGE("Text Message"),
    UDP_PACKET("UDP/IP Packet"),
    UNKNOWN_PACKET("Unknown Packet"),
    XCMP("Motorola XCMP"),
    UNKNOWN("Unknown");

    private final String mLabel;

    /**
     * Encrypted voice call event types for filtering
     */
    public static final EnumSet<DecodeEventType> VOICE_CALLS_ENCRYPTED = EnumSet.of(DecodeEventType.CALL_ENCRYPTED,
        DecodeEventType.CALL_GROUP_ENCRYPTED, DecodeEventType.CALL_PATCH_GROUP_ENCRYPTED,
        DecodeEventType.CALL_INTERCONNECT_ENCRYPTED, DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED);

    /**
     * Voice call event types for filtering
     */
    public static final EnumSet<DecodeEventType> VOICE_CALLS = EnumSet.of(DecodeEventType.CALL, DecodeEventType.CALL_GROUP,
            DecodeEventType.CALL_PATCH_GROUP, DecodeEventType.CALL_ALERT, DecodeEventType.CALL_DETECT,
            DecodeEventType.CALL_DO_NOT_MONITOR, DecodeEventType.CALL_END, DecodeEventType.CALL_INTERCONNECT,
            DecodeEventType.CALL_UNIQUE_ID, DecodeEventType.CALL_UNIT_TO_UNIT, DecodeEventType.CALL_NO_TUNER,
            DecodeEventType.CALL_TIMEOUT);

    /**
     * Command event types for filtering
     */
    public static final EnumSet<DecodeEventType> COMMANDS = EnumSet.of(DecodeEventType.ANNOUNCEMENT,
            DecodeEventType.STATION_ID, DecodeEventType.ACKNOWLEDGE, DecodeEventType.PAGE, DecodeEventType.QUERY,
            DecodeEventType.RADIO_CHECK, DecodeEventType.STATUS, DecodeEventType.COMMAND, DecodeEventType.EMERGENCY,
            DecodeEventType.NOTIFICATION, DecodeEventType.FUNCTION, DecodeEventType.DYNAMIC_REGROUP);

    /**
     * Data call event types for filtering
     */
    public static final EnumSet<DecodeEventType> DATA_CALLS = EnumSet.of(DecodeEventType.DATA_CALL,
            DecodeEventType.DATA_CALL_ENCRYPTED, DecodeEventType.DATA_PACKET, DecodeEventType.GPS,
            DecodeEventType.IP_PACKET, DecodeEventType.UDP_PACKET, DecodeEventType.SDM, DecodeEventType.ID_ANI,
            DecodeEventType.ID_UNIQUE);

    /**
     * Registration event types for filtering
     */
    public static final EnumSet<DecodeEventType> REGISTRATION = EnumSet.of(DecodeEventType.AFFILIATE,
            DecodeEventType.AUTOMATIC_REGISTRATION_SERVICE, DecodeEventType.REGISTER, DecodeEventType.REGISTER_ESN,
            DecodeEventType.DEREGISTER, DecodeEventType.REQUEST, DecodeEventType.RESPONSE, DecodeEventType.RESPONSE_PACKET);

    /**
     * All other event types of this enumeration that are not included in the groupings above.
     */
    public static final EnumSet<DecodeEventType> OTHERS = EnumSet.copyOf(Arrays.stream(DecodeEventType.values())
            .filter(decodeEventType -> !decodeEventType.isGrouped()).toList());

    /**
     * Voice call event types.
     */
    public static final EnumSet<DecodeEventType> VOICE_CALL_EVENTS = EnumSet.of(CALL, CALL_ENCRYPTED, CALL_GROUP,
            CALL_GROUP_ENCRYPTED, CALL_PATCH_GROUP, CALL_PATCH_GROUP_ENCRYPTED, CALL_INTERCONNECT,
            CALL_INTERCONNECT_ENCRYPTED, CALL_UNIT_TO_UNIT, CALL_UNIT_TO_UNIT_ENCRYPTED);

    /**
     * Constructor
     * @param label for the element
     */
    DecodeEventType(String label)
    {
        mLabel = label;
    }

    /**
     * Indicates if the enumeration element is contained in one of the enumset groupings above.
     * @return true if the element is grouped.
     */
    public boolean isGrouped()
    {
        return VOICE_CALLS.contains(this) || VOICE_CALLS_ENCRYPTED.contains(this) || COMMANDS.contains(this) ||
                DATA_CALLS.contains(this) || REGISTRATION.contains(this);
    }

    /**
     * Indicates if this is a voice call event.
     * @return true if this is a voice call event type.
     */
    public boolean isVoiceCallEvent()
    {
        return VOICE_CALL_EVENTS.contains(this);
    }

    /**
     * Label or pretty value for the element
     * @return label
     */
    public String getLabel()
    {
        return mLabel;
    }

    /**
     * Uses label as the default string value.
     */
    public String toString()
    {
        return mLabel;
    }
}

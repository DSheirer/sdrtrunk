/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.event;

public enum DecodeEventType
{
    AFFILIATE("Affiliate"),
    ANNOUNCEMENT("Announcement"),
    AUTOMATIC_REGISTRATION_SERVICE("Motorola ARS"),
    CALL("Call"),
    CALL_ENCRYPTED("Encrypted Call"),
    CALL_GROUP("Group Call"),
    CALL_GROUP_ENCRYPTED("Encrypted Group Call"),
    CALL_PATCH_GROUP("Patch Call"),
    CALL_PATCH_GROUP_ENCRYPTED("Encrypted Patch Call"),
    CALL_ALERT("Call Alert"),
    CALL_DETECT("Call Detect"),
    CALL_DO_NOT_MONITOR("Call-Do Not Monitor"),
    CALL_END("Call End"),
    CALL_INTERCONNECT("Telephone Call"),
    CALL_INTERCONNECT_ENCRYPTED("Encrypted Telephone Call"),
    CALL_UNIQUE_ID("UID Call"),
    CALL_UNIT_TO_UNIT("Unit To Unit Call"),
    CALL_UNIT_TO_UNIT_ENCRYPTED("Encrypted Unit To Unit Call"),
    CALL_NO_TUNER("Call - No Tuner"),
    CALL_TIMEOUT("Call Timeout"),
    COMMAND("Command"),
    DATA_CALL("Data Call"),
    DATA_CALL_ENCRYPTED("Encrypted Data Call"),
    DATA_PACKET("Data Packet"),
    DEREGISTER("Deregister"),
    EMERGENCY("EMERGENCY"),
    FUNCTION("Function"),
    GPS("GPS"),
    ID_ANI("ANI"),
    ID_UNIQUE("Unique ID"),
    IP_PACKET("IP Packet"),
    NOTIFICATION("Notification"),
    PAGE("Page"),
    QUERY("Query"),
    RADIO_CHECK("Radio Check"),
    REGISTER("Register"),
    REGISTER_ESN("ESN"),
    REQUEST("Request"),
    RESPONSE("Response"),
    RESPONSE_PACKET("Response Packet"),
    SDM("Short Data Message"),
    STATION_ID("Station ID"),
    STATUS("Status"),
    UDP_PACKET("UDP/IP Packet"),
    UNKNOWN("Unknown");

    private String mLabel;

    DecodeEventType(String label)
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

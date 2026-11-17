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

package io.github.dsheirer.module.decode.moto.message;

/**
 * Motorola Type II OSW message types.
 * Reference: OP25 tk_smartnet.py, TrunkRecorder smartnet_parser.cc
 */
public enum MotorolaTypeIIMessageType
{
    // One-OSW messages
    IDLE,                       // cmd 0x2F8, system idle
    GROUP_UPDATE,               // 1-OSW: isChan && isGroup — ongoing voice on channel
    CC_BROADCAST,               // 1-OSW: (addr & 0xFF00) == 0x1F00 — control channel broadcast
    GROUP_BUSY,                 // cmd 0x300, group busy queued
    EMERGENCY_BUSY,             // cmd 0x303, emergency busy queued
    SEND_AFFILIATION,           // cmd 0x32A, request affiliation
    SYSTEM_ID,                  // cmd 0x32B, system ID broadcast
    ROAMING,                    // cmd 0x32C, radio roaming
    AMSS,                       // cmd 0x360-0x39F, site ID = cmd - 0x360 + 1
    BSI_DIAGNOSTIC,             // cmd 0x3A0, BSI diagnostic
    NETWORK_STATUS,             // cmd 0x3BF, network status
    SYSTEM_STATUS,              // cmd 0x3C0, system status
    INTERCONNECT_REJECT,        // cmd 0x324, interconnect reject

    // Two-OSW messages (OSW2 cmd determines type)
    ANALOG_GROUP_GRANT,         // 0x308 + isChan && isGroup
    ANALOG_PRIVATE_CALL,        // 0x308 + isChan && !isGroup
    DIGITAL_GROUP_GRANT,        // 0x321 + isChan && isGroup
    DIGITAL_PRIVATE_CALL,       // 0x321 + isChan && !isGroup
    SYSTEM_ID_CC,               // 0x308 + 0x30B — system ID + adjacent/alt CC
    AFFILIATION,                // 0x308 + 0x310
    DEAFFILIATION,              // 0x308 + 0x30B (specific addr patterns)
    PATCH,                      // 0x308 + 0x340 — patch/multiselect
    DATE_TIME,                  // 0x308 + 0x322
    EMERGENCY_PTT,              // 0x308 + 0x32E
    RADIO_CHECK,                // 0x308 + 0x30B (specific addr)
    STATUS_ACK,                 // 0x308 + 0x30D
    CALL_ALERT,                 // 0x308 + 0x319
    CALL_ALERT_ACK,             // 0x308 + 0x31A
    OMNILINK_TRESPASS,          // 0x308 + 0x31B
    DYNAMIC_REGROUP,            // 0x308 + 0x30A
    GROUP_BUSY_QUEUED,          // 0x308 + 0x300
    PRIVATE_CALL_BUSY,          // 0x308 + 0x302
    EMERGENCY_BUSY_QUEUED,      // 0x308 + 0x303
    PRIVATE_CALL_RING,          // 0x308 + 0x315/0x317/0x318
    MESSAGE,                    // 0x308 + 0x311

    // Three-OSW messages
    ADJACENT_SITE,              // 0x308 + 0x320 + 0x30B (addr mask 0x6000)
    SYSTEM_INFO,                // 0x320 + ... + 0x30B
    CONTROL_CHANNEL,            // 0x308 + 0x30B (0x28xx) + channel (0x1Fxx addr)

    // Special
    UNKNOWN,                    // unrecognized message
    QUEUE_RESET;                // queue reset marker (not a real message)
}

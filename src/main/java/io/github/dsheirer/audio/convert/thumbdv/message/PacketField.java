/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.audio.convert.thumbdv.message;

/**
 * AMBE-3000R Control Packet Field enumeration
 */
public enum PacketField
{
    PKT_ENCODER_CMODE((byte)0x05),
    PKT_DECODER_CMODE((byte)0x06),
    PKT_RATE_TABLE((byte)0x09),
    PKT_RATE_PARAMETER((byte)0x0A),
    PKT_INIT((byte)0x0B),
    PKT_LOW_POWER((byte)0x10),
    PKT_CHANNEL_FORMAT((byte)0x15),
    PKT_SPEECH_FORMAT((byte)0x16),
    PKT_CODEC_START((byte)0x2A),
    PKT_CODEC_STOP((byte)0x2B),
    PKT_PRODUCT_ID((byte)0x30),
    PKT_VERSION_STRING((byte)0x31),
    PKT_COMPAND((byte)0x32),
    PKT_RESET((byte)0x33),
    PKT_RESET_TO_SOFTWARE_CONFIG((byte)0x34),
    PKT_HALT((byte)0x35),
    PKT_GET_CONFIG((byte)0x36),
    PKT_READ_CONFIG((byte)0x37),
    PKT_CODEC_CFG((byte)0x38),
    PKT_READY((byte)0x39),
    PKT_PARITY_MODE((byte)0x3F),
    PKT_CHANNEL_0((byte)0x40),
    PKT_WRITE_I2C((byte)0x44),
    PKT_CLEAR_CODEC_RESET((byte)0x46),
    PKT_SET_CODEC_RESET((byte)0x47),
    PKT_DISCARD_CODEC_SAMPLES((byte)0x48),
    PKT_DELAY_NUMBER_MICRO_SECONDS((byte)0x49),
    PKT_DELAY_NUMBER_NANO_SECONDS((byte)0x4A),
    PKT_GAIN((byte)0x4B),
    PKT_RTS_THRESHOLD((byte)0x4E),

    //Speech packet values
    SPEECH_DATA((byte)0x00),
    CHANNEL_DATA_HARD_SYMBOL((byte)0x01),
    CMODE((byte)0x02),
    SAMPLE_COUNT((byte)0x03),
    TONE((byte)0x08),
    CHANNEL_DATA_SOFT_SYMBOL((byte)0x17),
    VOCODER((byte)0x40),

    //Packet Types
    PACKET_TYPE_CONTROL((byte)0x00),
    PACKET_TYPE_ENCODE_SPEECH((byte)0x01),
    PACKET_TYPE_DECODE_SPEECH((byte)0x02),

    UNKNOWN((byte)0x00);

    private byte mCode;

    PacketField(byte code)
    {
        mCode = code;
    }

    /**
     * Packet type byte code value
     */
    public byte getCode()
    {
        return mCode;
    }

    /**
     * Lookup a packet type from the code byte value
     */
    public static PacketField fromValue(byte value)
    {
        switch(value)
        {
            case (byte)0x05:
                return PKT_ENCODER_CMODE;
            case (byte)0x06:
                return PKT_DECODER_CMODE;
            case (byte)0x09:
                return PKT_RATE_TABLE;
            case (byte)0x0A:
                return PKT_RATE_PARAMETER;
            case (byte)0x0B:
                return PKT_INIT;
            case (byte)0x10:
                return PKT_LOW_POWER;
            case (byte)0x15:
                return PKT_CHANNEL_FORMAT;
            case (byte)0x16:
                return PKT_SPEECH_FORMAT;
            case (byte)0x2A:
                return PKT_CODEC_START;
            case (byte)0x2B:
                return PKT_CODEC_STOP;
            case (byte)0x30:
                return PKT_PRODUCT_ID;
            case (byte)0x31:
                return PKT_VERSION_STRING;
            case (byte)0x32:
                return PKT_COMPAND;
            case (byte)0x33:
                return PKT_RESET;
            case (byte)0x34:
                return PKT_RESET_TO_SOFTWARE_CONFIG;
            case (byte)0x35:
                return PKT_HALT;
            case (byte)0x36:
                return PKT_GET_CONFIG;
            case (byte)0x37:
                return PKT_READ_CONFIG;
            case (byte)0x38:
                return PKT_CODEC_CFG;
            case (byte)0x39:
                return PKT_READY;
            case (byte)0x3F:
                return PKT_PARITY_MODE;
            case (byte)0x40:
                return PKT_CHANNEL_0;
            case (byte)0x44:
                return PKT_WRITE_I2C;
            case (byte)0x46:
                return PKT_CLEAR_CODEC_RESET;
            case (byte)0x47:
                return PKT_SET_CODEC_RESET;
            case (byte)0x48:
                return PKT_DISCARD_CODEC_SAMPLES;
            case (byte)0x49:
                return PKT_DELAY_NUMBER_MICRO_SECONDS;
            case (byte)0x4A:
                return PKT_DELAY_NUMBER_NANO_SECONDS;
            case (byte)0x4B:
                return PKT_GAIN;
            case (byte)0x4E:
                return PKT_RTS_THRESHOLD;
            default:
                return UNKNOWN;
        }
    }
}

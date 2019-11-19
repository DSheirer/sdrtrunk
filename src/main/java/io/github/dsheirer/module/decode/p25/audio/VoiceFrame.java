/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2019 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/

package io.github.dsheirer.module.decode.p25.audio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;

/**
 * Voice frame using hexadecimal representation of the transmitted voice and ecc bits
 */
@JsonRootName(value = "frame")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"encryption_algorithm", "encryption_key_id", "encryption_mi", "time", "hex"})
public class VoiceFrame
{
    private long mTimestamp;
    private String mFrame;
    private Integer mAlgorithm;
    private Integer mKeyId;
    private String mMessageIndicator;

    public VoiceFrame()
    {
        //no-arg constructor for faster jackson deserialization
    }

    /**
     * Constructs an unencrypted voice frame
     *
     * @param timestamp the frame was transmitted
     * @param frame with voice and ecc bits
     */
    public VoiceFrame(long timestamp, String frame)
    {
        mTimestamp = timestamp;
        mFrame = frame;
    }

    /**
     * Constructs an encrypted voice frame
     *
     * @param timestamp the message was transmitted
     * @param frame of hexadecimal values
     * @param algorithm used to encrypt the voice frame
     * @param keyid used to encrypt the voice frame
     * @param messageIndicator for the key generator fill
     */
    public VoiceFrame(long timestamp, String frame, int algorithm, int keyid, String messageIndicator)
    {
        this(timestamp, frame);
        mAlgorithm = algorithm;
        mKeyId = keyid;
        mMessageIndicator = messageIndicator;
    }

    /**
     * Timestamp the frame was transmitted
     *
     * @return timestamp in milliseconds since epoch (1970)
     */
    @JsonProperty("time")
    public long getTimestamp()
    {
        return mTimestamp;
    }

    public void setTimestamp(long timestamp)
    {
        mTimestamp = timestamp;
    }

    /**
     * Transmitted voice frame and ecc bits in hexadecimal
     */
    @JsonProperty("hex")
    public String getFrame()
    {
        return mFrame;
    }

    public void setFrame(String frame)
    {
        mFrame = frame;
    }

    /**
     * Audio frame bytes
     * @return frame bytes or null
     */
    @JsonIgnore
    public byte[] getFrameBytes()
    {
        if(mFrame != null)
        {
            byte[] bytes = new byte[mFrame.length() / 2];
            for(int x = 0; x < mFrame.length(); x += 2)
            {
                String hex = mFrame.substring(x, x + 2);
                bytes[x / 2] = (byte) (0xFF & Integer.parseInt(hex, 16));
            }

            return bytes;
        }

        return null;
    }

    /**
     * Algorithm identifier numeric value
     *
     * @return algorithm id
     */
    @JsonProperty("encryption_algorithm")
    public Integer getAlgorithm()
    {
        return mAlgorithm;
    }

    public void setAlgorithm(int algorithm)
    {
        mAlgorithm = algorithm;
    }

    /**
     * Key identifier
     *
     * @return key id
     */
    @JsonProperty("encryption_key_id")
    public Integer getKeyId()
    {
        return mKeyId;
    }

    public void setKeyId(int keyId)
    {
        mKeyId = keyId;
    }

    /**
     * Key generator fill values
     *
     * @return message indicator
     */
    @JsonProperty("encryption_mi")
    public String getMessageIndicator()
    {
        return mMessageIndicator;
    }

    public void setMessageIndicator(String messageIndicator)
    {
        mMessageIndicator = messageIndicator;
    }
}

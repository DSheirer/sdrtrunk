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

package io.github.dsheirer.module.decode.p25.audio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.github.dsheirer.identifier.encryption.EncryptionKey;

import java.util.ArrayList;
import java.util.List;

/**
 * MBE Call Sequence containing one or more voice frames with optional encryption parameters and optional from and to
 * radio identifiers.
 */
@JsonRootName("mbe_call")
@JsonPropertyOrder({"protocol", "call_type", "from", "to", "encrypted", "frames"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MBECallSequence
{
    private String mProtocol;
    private String mFromIdentifier;
    private String mToIdentifier;
    private String mCallType;
    private boolean mEncrypted;
    private List<VoiceFrame> mVoiceFrames = new ArrayList<>();
    private IEncryptionSyncParameters mTemporaryEncryptionSyncParameters;

    /**
     * Constructs a call sequence
     */
    public MBECallSequence(String protocol)
    {
        mProtocol = protocol;
    }

    /**
     * Protocol/format for the voice frames
     */
    @JsonProperty("protocol")
    public String getProtocol()
    {
        return mProtocol;
    }

    /**
     * Indicates if this sequences contains any audio frames
     */
    @JsonIgnore
    public boolean hasAudio()
    {
        return mVoiceFrames.size() > 4;
    }

    /**
     * Indicates if this call sequence is encrypted
     */
    @JsonProperty("encrypted")
    public boolean isEncrypted()
    {
        return mEncrypted;
    }

    /**
     * Sets the encrypted statsu of this call sequence
     *
     * @param encrypted status
     */
    public void setEncrypted(boolean encrypted)
    {
        mEncrypted = encrypted;
    }

    /**
     * Sets encryption sync parameters to use with the next sequence of voice frames.  This is usually obtained from the
     * HDU (phase1) or the PTT (phase2)
     *
     * @param encryptionSyncParameters to apply to the next set of voice frames
     */
    public void setEncryptionSyncParameters(IEncryptionSyncParameters encryptionSyncParameters)
    {
        mTemporaryEncryptionSyncParameters = encryptionSyncParameters;
    }

    /**
     * Sets the from radio identifier
     *
     * @param from id
     */
    public void setFromIdentifier(String from)
    {
        if(from != null && !from.isEmpty() && !from.contentEquals("0"))
        {
            mFromIdentifier = from;
        }
    }

    /**
     * Radio identifier that originated the call
     *
     * @return from id
     */
    @JsonProperty("from")
    public String getFromIdentifier()
    {
        return mFromIdentifier;
    }

    /**
     * Sets the to radio identifier
     *
     * @param to id
     */
    public void setToIdentifer(String to)
    {
        if(to != null && !to.isEmpty() && !to.contentEquals("0"))
        {
            mToIdentifier = to;
        }
    }

    /**
     * Radio identifier that received the call
     *
     * @return to id
     */
    @JsonProperty("to")
    public String getToIdentifier()
    {
        return mToIdentifier;
    }

    /**
     * Call type
     *
     * @param type from GROUP, INDIVIDUAL, or TELEPHONE INTERCONNECT
     */
    public void setCallType(String type)
    {
        mCallType = type;
    }

    /**
     * Sets the call type
     *
     * @return call type
     */
    @JsonProperty("call_type")
    public String getCallType()
    {
        return mCallType;
    }

    /**
     * Ordered list of voice frames for the call sequence
     *
     * @return list of unencrypted or encrypted audio frames
     */
    @JsonProperty("frames")
    public List<VoiceFrame> getVoiceFrames()
    {
        return mVoiceFrames;
    }

    /**
     * Adds an unencrypted audio frame to this call sequence
     *
     * @param timestamp of the audio frame
     * @param frame of hexadecimal values representing the transmitted audio frame and ecc bits
     */
    public void addVoiceFrame(long timestamp, String frame)
    {
        if(mTemporaryEncryptionSyncParameters != null)
        {
            addEncryptedVoiceFrame(timestamp, frame, mTemporaryEncryptionSyncParameters);
            mTemporaryEncryptionSyncParameters = null;
        }
        else
        {
            mVoiceFrames.add(new VoiceFrame(timestamp, frame));
        }
    }

    /**
     * Adds an encrypted audio frame to this call sequence
     *
     * @param timestamp of the audio frame
     * @param frame of hexadecimal values representing the transmitted audio frame and ecc bits
     * @param parameters identifying the encryption used for the frame
     */
    public void addEncryptedVoiceFrame(long timestamp, String frame, IEncryptionSyncParameters parameters)
    {
        EncryptionKey encryption = parameters.getEncryptionKey().getValue();
        addEncryptedVoiceFrame(timestamp, frame, encryption.getAlgorithm(), encryption.getKey(), parameters.getMessageIndicator());
        setEncrypted(true);
    }

    /**
     * Adds an encrypted audio frame to this call sequence
     *
     * @param timestamp of the audio frame
     * @param frame of hexadecimal values representing the transmitted audio frame and ecc bits
     * @param algorithm identifier for encryption
     * @param keyid identifying which encryption key was used by the radio which may have multiple keys
     * @param messageIndicator to seed the key generator
     */
    public void addEncryptedVoiceFrame(long timestamp, String frame, int algorithm, int keyid, String messageIndicator)
    {
        mVoiceFrames.add(new VoiceFrame(timestamp, frame, algorithm, keyid, messageIndicator));
    }

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

        /**
         * Transmitted voice frame and ecc bits in hexadecimal
         */
        @JsonProperty("hex")
        public String getFrame()
        {
            return mFrame;
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
    }
}

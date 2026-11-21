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

package io.github.dsheirer.module.decode.edacs.message;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.protocol.Protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jmbe.frame.CorrectedProVoiceFrame;

/**
 * EDACS ProVoice message carrying 4 IMBE 7100 voice frames (one per
 * 20 ms of audio) decoded from a single outbound ProVoice frame.
 */
public class EDACSProVoiceMessage implements IMessage
{
    private static final int DEFAULT_VOICE_MARKER = 0x2175;
    private static final int VOICE_MARKER_MAXIMUM_BIT_ERRORS = 3;

    /**
     * Value of the 64-bit encryption initialization vector (IV) field on clear voice.  The field changes on every
     * frame of an encrypted call, where it is the cipher's IV (per the DSD-FME maintainer).
     */
    public static final long CLEAR_IV = 0x6969696969696969L;

    /**
     * Minimum number of IV bits that differ from {@link #CLEAR_IV} for a frame to be treated as encrypted.  Measured
     * on recordings of Extended Addressing systems: clear frames differed by 0-15 bits (weak signal included) and
     * encrypted frames by 22-39 bits, where a random value averages 32.
     */
    public static final int ENCRYPTED_IV_MINIMUM_BIT_DIFFERENCE = 18;
    private final int mVoiceMarker;

    private final byte[][] mImbeFrames = new byte[4][];
    private final int[][][] mImbeGrids;
    private List<CorrectedProVoiceFrame> mCorrectedImbeFrames;
    private final boolean[] mFrameBits;
    private final int mLid;
    private final int mBfMarker;
    private final long mIv;
    private final String mSyncPattern;
    private final long mTimestamp;
    private final List<Identifier> mIdentifiers;

    public EDACSProVoiceMessage(byte[] imbeFrame1, byte[] imbeFrame2, byte[] imbeFrame3, byte[] imbeFrame4,
                                int[][][] imbeGrids, boolean[] frameBits, int lid, int bfMarker,
                                String syncPattern, long timestamp)
    {
        this(imbeFrame1, imbeFrame2, imbeFrame3, imbeFrame4, imbeGrids, frameBits, lid, bfMarker, syncPattern,
                timestamp, Collections.emptyList(), DEFAULT_VOICE_MARKER);
    }

    public EDACSProVoiceMessage(byte[] imbeFrame1, byte[] imbeFrame2, byte[] imbeFrame3, byte[] imbeFrame4,
                                int[][][] imbeGrids, boolean[] frameBits, int lid, int bfMarker,
                                String syncPattern, long timestamp, List<Identifier> identifiers, int voiceMarker)
    {
        this(imbeFrame1, imbeFrame2, imbeFrame3, imbeFrame4, imbeGrids, frameBits, lid, bfMarker, syncPattern,
                timestamp, identifiers, voiceMarker, CLEAR_IV);
    }

    /**
     * Constructs an instance
     * @param voiceMarker that voice frames carry on this system, or a negative value to treat every frame as voice.
     */
    public EDACSProVoiceMessage(byte[] imbeFrame1, byte[] imbeFrame2, byte[] imbeFrame3, byte[] imbeFrame4,
                                int[][][] imbeGrids, boolean[] frameBits, int lid, int bfMarker,
                                String syncPattern, long timestamp, List<Identifier> identifiers, int voiceMarker,
                                long iv)
    {
        mVoiceMarker = voiceMarker;
        mIv = iv;
        mImbeFrames[0] = imbeFrame1;
        mImbeFrames[1] = imbeFrame2;
        mImbeFrames[2] = imbeFrame3;
        mImbeFrames[3] = imbeFrame4;
        mImbeGrids = imbeGrids;
        mFrameBits = frameBits;
        mLid = lid;
        mBfMarker = bfMarker;
        mSyncPattern = syncPattern;
        mTimestamp = timestamp;
        mIdentifiers = identifiers != null ? Collections.unmodifiableList(new ArrayList<>(identifiers)) :
                Collections.emptyList();
    }

    public byte[][] getImbeFrames()
    {
        return mImbeFrames;
    }

    /**
     * ProVoice IMBE7100 grids packed row-major, MSB first, for the JMBE PROVOICE codec.
     */
    /**
     * Error corrected versions of the 4 IMBE 7100 voice frames from getPackedImbe7100Frames(), in the same order.
     * The frames are error corrected and derandomized once and cached.
     */
    public synchronized List<CorrectedProVoiceFrame> getCorrectedImbe7100Frames()
    {
        if(mCorrectedImbeFrames == null)
        {
            List<CorrectedProVoiceFrame> correctedFrames = new ArrayList<>();

            for(byte[] frame : getPackedImbe7100Frames())
            {
                correctedFrames.add(CorrectedProVoiceFrame.correct(frame));
            }

            mCorrectedImbeFrames = correctedFrames;
        }

        return mCorrectedImbeFrames;
    }

    public byte[][] getPackedImbe7100Frames()
    {
        byte[][] frames = new byte[4][];

        for(int x = 0; x < frames.length; x++)
        {
            frames[x] = packGrid(mImbeGrids[x]);
        }

        return frames;
    }

    private byte[] packGrid(int[][] grid)
    {
        byte[] bytes = new byte[21];
        int bit = 0;

        for(int row = 0; row < grid.length; row++)
        {
            for(int column = 0; column < grid[row].length; column++)
            {
                if(grid[row][column] != 0)
                {
                    bytes[bit / 8] |= (byte)(0x80 >> (bit % 8));
                }

                bit++;
            }
        }

        return bytes;
    }

    /**
     * The 16 bits that DSD-FME labels LID.  On Extended Addressing systems these are the middle 16 bits of the
     * encryption IV field ({@link #getIv()}), not a radio ID: 0x6969 on clear voice and random on encrypted voice.
     */
    public int getLid()
    {
        return mLid;
    }

    /**
     * 64-bit encryption initialization vector (IV) field: the 64 bits that follow the 32 bits after the sync word.
     * Reads {@link #CLEAR_IV} on clear voice.
     */
    public long getIv()
    {
        return mIv;
    }

    /**
     * Indicates if this is a voice frame whose IV field is far enough from the clear value to be an encrypted frame.
     * A single frame can be corrupted, so callers decide a call is encrypted from consecutive encrypted frames.
     * Signalling frames (no voice marker) are never encrypted: their header bits carry other content even on clear
     * calls.
     */
    public boolean isEncrypted()
    {
        return isVoice() && Long.bitCount(mIv ^ CLEAR_IV) >= ENCRYPTED_IV_MINIMUM_BIT_DIFFERENCE;
    }

    /**
     * Four IMBE 7100 7x24 grids before packing into JMBE's 18-byte IMBE input format.
     */
    public int[][][] getImbeGrids()
    {
        return mImbeGrids;
    }

    public boolean[] getFrameBits()
    {
        return mFrameBits;
    }

    public int getBfMarker()
    {
        return mBfMarker;
    }

    public String getSyncPattern()
    {
        return mSyncPattern;
    }

    @Override
    public long getTimestamp()
    {
        return mTimestamp;
    }

    /**
     * Indicates if this frame carries voice.  A voice frame has a fixed 16-bit marker between the first halves of
     * IMBE frames 1,2 and 3,4 (DSD-FME provoice.c: "usually 2175").  The signalling burst that starts an over and
     * the dotting that ends it also follow a sync word, but do not carry the marker, and sound as a burst of noise
     * if they are passed to the vocoder.  This check is not in DSD-FME.  The marker is set per system in the EDACS
     * decode configuration; every frame is voice when no marker is configured.
     */
    public boolean isVoice()
    {
        return mVoiceMarker < 0 || Integer.bitCount(mBfMarker ^ mVoiceMarker) <= VOICE_MARKER_MAXIMUM_BIT_ERRORS;
    }

    /**
     * Shown in the Messages tab and the decoded messages log.  The MARKER value is what to enter as the ProVoice
     * voice marker for a system: it is the value that repeats through the middle of each over.
     */
    @Override
    public String toString()
    {
        return String.format("PROVOICE %s%s MARKER:%04X IV:%016X %s", isVoice() ? "VOICE" : "SIGNALLING",
            isEncrypted() ? " ENCRYPTED" : "", mBfMarker, mIv, mSyncPattern);
    }

    @Override
    public boolean isValid()
    {
        if(!isVoice())
        {
            return false;
        }

        for(byte[] frame : mImbeFrames)
        {
            if(frame == null || frame.length != 18)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.EDACS;
    }

    @Override
    public int getTimeslot()
    {
        return 0;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return mIdentifiers;
    }
}

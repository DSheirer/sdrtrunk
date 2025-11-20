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

package io.github.dsheirer.module.decode.nxdn.layer3.call;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.FragmentedIntField;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.edac.Golay23;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.NXDNMessage;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer2.SACCHFragment;
import io.github.dsheirer.module.decode.nxdn.layer3.type.AudioCodec;
import io.github.dsheirer.util.ByteUtil;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;

/**
 * NXDN audio frame data
 */
public class Audio extends NXDNMessage
{
    private static final FragmentedIntField VECTOR_C0 = FragmentedIntField.of(0, 4, 8, 12, 16, 20, 24, 28, 32,
            36, 40, 44, 48, 52, 56, 60, 64, 68, 1, 5, 9, 13, 17, 21);
    private static final FragmentedIntField VECTOR_C1 = FragmentedIntField.of(25, 29, 33, 37, 41, 45, 49, 53,
            57, 61, 65, 69, 2, 6, 10, 14, 18, 22, 26, 30, 34, 38, 42);
    private static final IntField VECTOR_U0 = IntField.length12(0);

    private final AudioCodec mAudioCodec;
    private final List<byte[]> mAudioFrames;
    private SACCHFragment mSACCHFragment;

    /**
     * Constructs an instance
     *
     * @param audioCodec code used for the channel
     * @param frames of raw AMBE audio
     * @param timestamp for the message
     * @param ran from the SR field
     * @param lich from the SR field
     */
    public Audio(AudioCodec audioCodec, List<byte[]> frames, long timestamp, int ran, LICH lich)
    {
        super(new CorrectedBinaryMessage(0), timestamp, ran, lich);
        mAudioCodec = audioCodec;
        mAudioFrames = frames;

        boolean valid = false;

        //Check that at least 1 audio frame is valid.
        for(byte[] frame: frames)
        {
            valid |= isValid(frame);
        }

        setValid(valid);
    }

    /**
     * Optional SACCH fragment
     */
    public SACCHFragment getSACCHFragment()
    {
        return mSACCHFragment;
    }

    /**
     * Indicates if this audio has an optional SACCH fragment
     */
    public boolean hasSACCHFragment()
    {
        return mSACCHFragment != null;
    }

    /**
     * Sets the optional SACCH fragment
     */
    public void setSACCHFragment(SACCHFragment fragment)
    {
        mSACCHFragment = fragment;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = getMessageBuilder().append("AUDIO FRAMES");

        for(byte[] frame: mAudioFrames)
        {
            sb.append(" ").append(ByteUtil.toHexString(frame));
        }

        return sb.toString();
    }

    /**
     * Audio frame data
     */
    public List<byte[]> getAudioFrames()
    {
        return mAudioFrames;
    }

    /**
     * AMBE+ audio codec format
     * @return full or half rate
     */
    public AudioCodec getAudioCodec()
    {
        return mAudioCodec;
    }

    /**
     * Performs error detection and correction to determine if the frame is valid
     */
    public static boolean isValid(byte[] audio)
    {
        CorrectedBinaryMessage cbm = CorrectedBinaryMessage.from(audio, ByteOrder.LITTLE_ENDIAN);
        CorrectedBinaryMessage vc0 = new CorrectedBinaryMessage(24);
        vc0.load(0, 24, cbm.getInt(VECTOR_C0));

        //TODO: use Golay24 here, but the Golay24 implementation isn't as robust as the Golay23 implementation
        int error1 = Golay23.checkAndCorrect(vc0, 0);

        if(error1 < 4)
        {
            CorrectedBinaryMessage vc1 = new CorrectedBinaryMessage(23);
            vc1.load(0, 23, cbm.getInt(VECTOR_C1));
            CorrectedBinaryMessage modulationVector = getModulationVector(vc0.getInt(VECTOR_U0));
            vc1.xor(modulationVector);
            int error2 = Golay23.checkAndCorrect(vc1, 0);
            return error2 < 4;
        }

        return false;
    }

    /**
     * Generates the bit modulation vector that is xor'd to vector C1 using U0 as the seed.
     *
     * @param seed for the modulation vector
     * @return modulation vector bit sequence to descramble vector C1
     */
    private static CorrectedBinaryMessage getModulationVector(int seed)
    {
        CorrectedBinaryMessage modulationVector = new CorrectedBinaryMessage(23);

        //alg 52
        int prX = 16 * seed;

        for(int x = 0; x < 23; x++)
        {
            //alg 53 - simplified [... - 65536 * floor((173 * pr(n-1) + 13849) / 65536)] to modulus operation
            prX = (173 * prX + 13849) % 65536;

            //alg 54 - values 32768 and above are a 1 and below is a 0 (default)
            if(prX >= 32768)
            {
                modulationVector.set(x);
            }
        }

        return modulationVector;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}

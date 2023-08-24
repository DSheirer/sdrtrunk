/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.Golay24;
import io.github.dsheirer.module.decode.dmr.bptc.BPTC_16_2;
import io.github.dsheirer.module.decode.dmr.message.IServiceOptionsProvider;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.FullLCMessage;
import io.github.dsheirer.module.decode.dmr.message.voice.embedded.EmbeddedEncryptionParameters;
import io.github.dsheirer.module.decode.dmr.message.voice.embedded.EmbeddedParameters;
import io.github.dsheirer.module.decode.dmr.message.voice.embedded.NonStandardShortBurst;
import io.github.dsheirer.module.decode.dmr.message.voice.embedded.NullShortBurst;
import io.github.dsheirer.module.decode.dmr.message.voice.embedded.ShortBurst;
import io.github.dsheirer.module.decode.dmr.message.voice.embedded.ShortBurstOpcode;
import io.github.dsheirer.module.decode.dmr.message.voice.embedded.TransmitInterrupt;
import io.github.dsheirer.module.decode.dmr.message.voice.embedded.UnknownShortBurst;

/**
 * Monitors audio call voice frame messaging to detect encrypted audio calls and extract the encryption parameters
 * that are embedded into the six voice frames that comprise a voice super-frame.
 * <p>
 * See patent: <a href="https://patents.google.com/patent/EP2347540B1/en">...</a> - embedding encryption parameters in voice super frame
 * See patent: <a href="https://patents.google.com/patent/US8271009B2">...</a> - TXI - interrupting voice transmissions
 */
public class VoiceSuperFrameProcessor
{
    private static final int[] IV = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 24, 25, 26, 27, 28, 29, 30, 31, 32,
            33, 34, 35, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] CRC4 = new int[]{56, 57, 58, 59};
    private boolean mEncrypted = false;
    private byte[] mFragmentA;
    private byte[] mFragmentB;
    private byte[] mFragmentC;
    private byte[] mFragmentD;
    private byte[] mFragmentE;

    /**
     * Constructor.
     */
    public VoiceSuperFrameProcessor()
    {
    }

    /**
     * Indicates if this collector is in collecting mode.
     *
     * @return true if collecting.
     */
    public boolean isCollecting()
    {
        return mEncrypted;
    }

    /**
     * Fully resets this collector
     */
    public void reset()
    {
        mEncrypted = false;
        softReset();
    }

    private void softReset()
    {
        mFragmentA = null;
        mFragmentB = null;
        mFragmentC = null;
        mFragmentD = null;
        mFragmentE = null;
    }

    /**
     * Indicates if the IV fragments from voice frames A-E have been collected and this is an encrypted call.
     *
     * @return true if the fragments are non-null.
     */
    private boolean isComplete()
    {
        return mEncrypted && mFragmentA != null && mFragmentB != null && mFragmentC != null && mFragmentD != null &&
                mFragmentE != null;
    }

    /**
     * Process full link control message to determine if the current call sequence is encrypted.
     * @param flc to inspect
     */
    public void process(FullLCMessage flc)
    {
        if(flc instanceof IServiceOptionsProvider provider && provider.getServiceOptions().isEncrypted())
        {
            mEncrypted = true;
        }
    }

    /**
     * Processes the voice frame to extract the IV fragments and the encryption parameters.
     *
     * @param voiceMessage to process.
     */
    public void process(VoiceMessage voiceMessage)
    {
        if(voiceMessage instanceof VoiceEMBMessage voiceEMB)
        {
            if(voiceEMB.getEMB().isValid() && voiceEMB.getEMB().isEncrypted())
            {
                mEncrypted = true;
            }
        }

        switch(voiceMessage.getSyncPattern())
        {
            case BASE_STATION_VOICE:
                mFragmentA = voiceMessage.getIvFragments();
                break;
            case BS_VOICE_FRAME_B:
                mFragmentB = voiceMessage.getIvFragments();
                break;
            case BS_VOICE_FRAME_C:
                mFragmentC = voiceMessage.getIvFragments();
                break;
            case BS_VOICE_FRAME_D:
                mFragmentD = voiceMessage.getIvFragments();
                break;
            case BS_VOICE_FRAME_E:
                mFragmentE = voiceMessage.getIvFragments();
                break;
            case BS_VOICE_FRAME_F:
                if(voiceMessage instanceof VoiceEMBMessage voiceFrame6)
                {
                    BinaryMessage frameFFragment = voiceFrame6.getFLCFragment();
                    ShortBurst shortBurst = extractShortBurst(frameFFragment);
                    EmbeddedParameters embeddedParameters = new EmbeddedParameters(shortBurst);

                    if(isComplete())
                    {
                        String iv = extractIV(voiceMessage.getIvFragments());
                        embeddedParameters.setIv(iv);
                    }

                    voiceFrame6.setEmbeddedParameters(embeddedParameters);
                }
                softReset();
                break;
        }
    }

    /**
     * Processes the short burst from the Voice Frame F FLC fragment payload and combines with the optionally available
     * initialization vector to return a parameters object.
     *
     * @param frameFFragment containing the short-burst FLC fragment
     * @return embedded parameters.
     */
    private ShortBurst extractShortBurst(BinaryMessage frameFFragment)
    {
        CorrectedBinaryMessage decoded = BPTC_16_2.decodeShortBurst(new CorrectedBinaryMessage(frameFFragment));

        if(decoded == null)
        {
            return new NonStandardShortBurst(BPTC_16_2.deinterleave(new CorrectedBinaryMessage(frameFFragment)));
        }

        ShortBurstOpcode opcode = ShortBurst.getOpcode(decoded);
        return switch(opcode)
        {
            case NULL -> new NullShortBurst(decoded);
            case ARC4_ENCRYPTION, AES128_ENCRYPTION, AES256_ENCRYPTION -> new EmbeddedEncryptionParameters(decoded);
            case TXI_DELAY -> new TransmitInterrupt(decoded);
            default -> new UnknownShortBurst(decoded);
        };
    }

    /**
     * Extracts the encryption initialization vector (IV) from the voice frame fragments, performs error detection and
     * correction and if the error correction passes, returns the hex string version of the 32-bit IV.
     *
     * @param mFragmentF voice frame 6 IV fragments.
     * @return 32-bit IV as hex string or null if the extraction process couldn't extract/correct the IV.
     */
    private String extractIV(byte[] mFragmentF)
    {
        CorrectedBinaryMessage reassembled = new CorrectedBinaryMessage(72);
        reassembled.setByte(0, combine(mFragmentA[0], mFragmentB[0]));
        reassembled.setByte(8, combine(mFragmentC[0], mFragmentD[0]));
        reassembled.setByte(16, combine(mFragmentE[0], mFragmentF[0]));
        reassembled.setByte(24, combine(mFragmentA[1], mFragmentB[1]));
        reassembled.setByte(32, combine(mFragmentC[1], mFragmentD[1]));
        reassembled.setByte(40, combine(mFragmentE[1], mFragmentF[1]));
        reassembled.setByte(48, combine(mFragmentA[2], mFragmentB[2]));
        reassembled.setByte(56, combine(mFragmentC[2], mFragmentD[2]));
        reassembled.setByte(64, combine(mFragmentE[2], mFragmentF[2]));

        int check1 = Golay24.checkAndCorrect(reassembled, 0);
        int check2 = Golay24.checkAndCorrect(reassembled, 24);
        int check3 = Golay24.checkAndCorrect(reassembled, 48);

        if(check1 == 2 || check2 == 2 || check3 == 2)
        {
            return null;
        }

        int iv = reassembled.getInt(IV);
        int checksum = reassembled.getInt(CRC4);

        boolean passes = crc4(iv, checksum);

        if(passes)
        {
            return String.format("%08X", iv);
        }
        else
        {
            return String.format("%08X", iv) + "(CRC-FAIL " + check1 + "/" + check2 + "/" + check3 + "/" +
                    reassembled.getCorrectedBitCount() + ")";
        }
    }

    /**
     * Combines the low nibble from the hi byte with the low nibble from the lo byte
     *
     * @param high byte containing a low order nibble
     * @param low byte containing a low order nibble
     * @return low order nibbles from hi and lo bytes combined.
     */
    private byte combine(byte high, byte low)
    {
        return (byte)(((high & 0xF) << 4) | (low & 0xF));
    }

    /**
     * Calculates a CRC value from the polynomial: x4 + x1 + 1  (0x13) using an initial fill of 0xF.
     * @param value to calculate CRC from
     * @param crc to compare
     * @return true if the calculated CRC from the value matches the crc argument value.
     */
    public static boolean crc4(int value, int crc)
    {
        long checksum = (value & 0x0FFFFFFFFL) << 4;
        checksum ^= 0xF; //Initial fill
        long polynomial = 0x013L << 31;
        long checkBit = 0x1L << 35;

        for(int x = 31; x >= 0; x--)
        {
            if((checksum & checkBit) == checkBit)
            {
                checksum ^= polynomial;
            }
            polynomial >>= 1;
            checkBit >>= 1;
        }

        return (int)checksum == crc;
    }
}

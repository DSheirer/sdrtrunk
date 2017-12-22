/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.util;

import org.apache.commons.lang3.Validate;

import java.util.Arrays;

public class XTEA
{
    //Key Schedule = (sqrt(5) ­ 1) * 2^31
    private static final int K = 0x9E3779B9;

    //Byte mask values
    private static final int MASK0 = 0xFF;
    private static final int MASK1 = 0xFF00;
    private static final int MASK2 = 0xFF0000;
    private static final int MASK3 = 0xFF000000;

    //Pre-calculated sub keys derived from encryption key
    private int mSubKeys1[] = new int[32];
    private int mSubKeys2[] = new int[32];

    /**
     * XTEA block cipher encryption using 128 bit encryption key and 64 bit plain text block size with 32 Feistel rounds.
     * Shorter keys and/or plaintext values can be zero padded to the specified lengths.
     *
     * Algorithm source code adapted from: https://people.rit.edu/rab3106/CryptoReport.pdf (as accessed Sep. 2016)
     *
     * The encryption key can be changed at any point by invoking the setKey() method.
     *
     * @param key for encryption
     */
    public XTEA(String key)
    {
        setKey(Arrays.copyOf(key.getBytes(), 16));
    }

    /**
     * Set the key for this block cipher. Key must be 128 bits or 16 bytes.  The key value can be zero padded to
     * achieve the specified key length.
     *
     * @param key encryption key to use
     */
    public void setKey(byte[] key)
    {
        Validate.isTrue(key.length == 16);

        int keyInts[] = new int[8];
        
        //Convert the key to an array of integers
        for (int i = 0; i < 16; i += 4)
        {
            keyInts[i / 4] = ((key[i + 3] << 0)) & MASK0 | ((key[i + 2]) << 8) & MASK1 |
                             ((key[i + 1]) << 16) & MASK2 | ((key[i + 0]) << 24) & MASK3;
        }

        //Calculate subkeys for each Feistel round
        int sum, cycle;

        for (sum = 0, cycle = 0; cycle < 32; cycle++)
        {
            //Calculate round 1 subkey
            mSubKeys1[cycle] = keyInts[sum & 3];
            
            //Add K to sum
            sum += K;
            
            //Calculate round 2 subkey
            mSubKeys2[cycle] = keyInts[(sum >>> 11) & 3];
        }
    }

    /**
     * Encrypts the plaintext argument.  Textual byte array must be 64 bits or 8 bytes in length and can be zero padded
     * to achieve the specified length.  Encrypted text byte array replaces the original plaintext byte array.
     *
     * @param plainText to encrypt
     * @return cipher text
     */
    public byte[] encrypt(byte[] plainText)
    {
        Validate.isTrue(plainText.length == 8);

        //Convert the 8 bytes of plaintext to 2 integers
        int result0 = (plainText[3] << 0) & MASK0 | (plainText[2] << 8) & MASK1 |
                      (plainText[1] << 16) & MASK2 | (plainText[0] << 24) & MASK3;
        int result1 = (plainText[7] << 0) & MASK0 | (plainText[6] << 8) & MASK1 |
                      (plainText[5] << 16) & MASK2 | (plainText[4] << 24) & MASK3;
        
        //Unrolled loop for all Feistel rounds
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 0) + mSubKeys1[0];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 1) + mSubKeys2[0];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 1) + mSubKeys1[1];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 2) + mSubKeys2[1];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 2) + mSubKeys1[2];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 3) + mSubKeys2[2];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 3) + mSubKeys1[3];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 4) + mSubKeys2[3];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 4) + mSubKeys1[4];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 5) + mSubKeys2[4];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 5) + mSubKeys1[5];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 6) + mSubKeys2[5];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 6) + mSubKeys1[6];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 7) + mSubKeys2[6];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 7) + mSubKeys1[7];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 8) + mSubKeys2[7];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 8) + mSubKeys1[8];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 9) + mSubKeys2[8];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 9) + mSubKeys1[9];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 10) + mSubKeys2[9];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 10) + mSubKeys1[10];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 11) + mSubKeys2[10];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 11) + mSubKeys1[11];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 12) + mSubKeys2[11];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 12) + mSubKeys1[12];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 13) + mSubKeys2[12];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 13) + mSubKeys1[13];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 14) + mSubKeys2[13];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 14) + mSubKeys1[14];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 15) + mSubKeys2[14];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 15) + mSubKeys1[15];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 16) + mSubKeys2[15];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 16) + mSubKeys1[16];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 17) + mSubKeys2[16];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 17) + mSubKeys1[17];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 18) + mSubKeys2[17];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 18) + mSubKeys1[18];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 19) + mSubKeys2[18];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 19) + mSubKeys1[19];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 20) + mSubKeys2[19];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 20) + mSubKeys1[20];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 21) + mSubKeys2[20];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 21) + mSubKeys1[21];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 22) + mSubKeys2[21];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 22) + mSubKeys1[22];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 23) + mSubKeys2[22];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 23) + mSubKeys1[23];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 24) + mSubKeys2[23];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 24) + mSubKeys1[24];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 25) + mSubKeys2[24];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 25) + mSubKeys1[25];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 26) + mSubKeys2[25];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 26) + mSubKeys1[26];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 27) + mSubKeys2[26];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 27) + mSubKeys1[27];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 28) + mSubKeys2[27];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 28) + mSubKeys1[28];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 29) + mSubKeys2[28];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 29) + mSubKeys1[29];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 30) + mSubKeys2[29];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 30) + mSubKeys1[30];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 31) + mSubKeys2[30];
        result0 += (result1 << 4 ^ result1 >>> 5) + result1 ^ (K * 31) + mSubKeys1[31];
        result1 += (result0 << 4 ^ result0 >>> 5) + result0 ^ (K * 32) + mSubKeys2[31];

        byte[] cipherText = new byte[8];

        cipherText[0] = (byte)(result0 >>> 24);
        cipherText[1] = (byte)(result0 >>> 16);
        cipherText[2] = (byte)(result0 >>> 8);
        cipherText[3] = (byte)(result0 >>> 0);
        cipherText[4] = (byte)(result1 >>> 24);
        cipherText[5] = (byte)(result1 >>> 16);
        cipherText[6] = (byte)(result1 >>> 8);
        cipherText[7] = (byte)(result1 >>> 0);

        return cipherText;
    }
}

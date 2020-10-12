package io.github.dsheirer.edac;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;

/*
 Adopted from https://github.com/boatbod/op25/blob/e5e8fe457240fb358a8163bcf6fe7dda5e4d8392/op25/gr-op25_repeater/lib/hamming.cc
 */
public class BPTC_17_12_3 {

    public static boolean decode17123(BinaryMessage d, int offset)
    {
        if(d==null) {
            return false;
        }

        // Calculate the checksum this column should have
        boolean c0 = d.get(0 + offset) ^ d.get(1 + offset) ^ d.get(2 + offset) ^ d.get(3 + offset) ^ d.get(6 + offset) ^ d.get(7 + offset) ^ d.get(9 + offset);
        boolean c1 = d.get(0 + offset) ^ d.get(1 + offset) ^ d.get(2 + offset) ^ d.get(3 + offset) ^ d.get(4 + offset) ^ d.get(7 + offset) ^ d.get(8 + offset) ^ d.get(10 + offset);
        boolean c2 = d.get(1 + offset) ^ d.get(2 + offset) ^ d.get(3 + offset) ^ d.get(4 + offset) ^ d.get(5 + offset) ^ d.get(8 + offset) ^ d.get(9 + offset) ^ d.get(11 + offset);
        boolean c3 = d.get(0 + offset) ^ d.get(1 + offset) ^ d.get(4 + offset) ^ d.get(5 + offset) ^ d.get(7 + offset) ^ d.get(10 + offset);
        boolean c4 = d.get(0 + offset) ^ d.get(1 + offset) ^ d.get(2 + offset) ^ d.get(5 + offset) ^ d.get(6 + offset) ^ d.get(8 + offset) ^ d.get(11 + offset);

        // Compare these with the actual bits
        byte n = 0x00;
        n |= (c0 != d.get(12 + offset)) ? 0x01 : 0x00;
        n |= (c1 != d.get(13 + offset)) ? 0x02 : 0x00;
        n |= (c2 != d.get(14 + offset)) ? 0x04 : 0x00;
        n |= (c3 != d.get(15 + offset)) ? 0x08 : 0x00;
        n |= (c4 != d.get(16 + offset)) ? 0x10 : 0x00;

        switch (n) {
            // Parity bit errors
            case 0x01: d.set(12 + offset, !d.get(12 + offset)); return true;
            case 0x02: d.set(13 + offset, !d.get(13 + offset)); return true;
            case 0x04: d.set(14 + offset, !d.get(14 + offset)); return true;
            case 0x08: d.set(15 + offset, !d.get(15 + offset)); return true;
            case 0x10: d.set(16 + offset, !d.get(16 + offset)); return true;

            // Data bit errors
            case 0x1B: d.set(0 + offset, !d.get(0 + offset));  return true;
            case 0x1F: d.set(1 + offset, !d.get(1 + offset));  return true;
            case 0x17: d.set(2 + offset, !d.get(2 + offset));  return true;
            case 0x07: d.set(3 + offset, !d.get(3 + offset));  return true;
            case 0x0E: d.set(4 + offset, !d.get(4 + offset));  return true;
            case 0x1C: d.set(5 + offset, !d.get(5 + offset));  return true;
            case 0x11: d.set(6 + offset, !d.get(6 + offset));  return true;
            case 0x0B: d.set(7 + offset, !d.get(7 + offset));  return true;
            case 0x16: d.set(8 + offset, !d.get(8 + offset));  return true;
            case 0x05: d.set(9 + offset, !d.get(9 + offset));  return true;
            case 0x0A: d.set(10 + offset,!d.get(10 + offset)); return true;
            case 0x14: d.set(11 + offset,!d.get(11 + offset)); return true;

            // No bit errors
            case 0x00: return true;

                // Unrecoverable errors
            default: return false;
        }
    }

}

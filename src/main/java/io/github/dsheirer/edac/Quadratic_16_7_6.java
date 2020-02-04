package io.github.dsheirer.edac;

import io.github.dsheirer.bits.BinaryMessage;

import java.util.BitSet;

public class Quadratic_16_7_6 {
    /*
   typedef struct {
   flag_t data[7];
   flag_t parity[9];
} quadres_16_7_codeword_t;

typedef struct {
   flag_t bits[9];
} quadres_16_7_parity_bits_t;

quadres_16_7_parity_bits_t *quadres_16_7_get_parity_bits(flag_t bits[7]);

flag_t quadres_16_7_check(quadres_16_7_codeword_t *codeword);

void quadres_16_7_init(void);
    */
    static boolean initialized = false;
    static int[] quadres_16_7_valid_data_paritys = new int[128];

// Returns the quadratic residue (16,7,6) parity bits for the given byte.
    static int quadres_16_7_get_parity_bits(boolean bits[]) {
        int parity = 0;
        // Multiplying the generator matrix with the given data bits.
        // See DMR AI spec. page 134.
        parity |= ((bits[1] ^ bits[2] ^ bits[3] ^ bits[4]) ? 256 : 0);
        parity |= ((bits[2] ^ bits[3] ^ bits[4] ^ bits[5]) ? 128 : 0);
        parity |= ((bits[0] ^ bits[3] ^ bits[4] ^ bits[5] ^ bits[6]) ? 64 : 0);
        parity |= ((bits[2] ^ bits[3] ^ bits[5] ^ bits[6]) ? 32 : 0);
        parity |= ((bits[1] ^ bits[2] ^ bits[6]) ? 16 : 0);
        parity |= ((bits[0] ^ bits[1] ^ bits[4]) ? 8 : 0);
        parity |= ((bits[0] ^ bits[1] ^ bits[2] ^ bits[5]) ? 4 : 0);
        parity |= ((bits[0] ^ bits[1] ^ bits[2] ^ bits[3] ^ bits[6]) ? 2 : 0);
        parity |= ((bits[0] ^ bits[2] ^ bits[4] ^ bits[5] ^ bits[6]) ? 1 : 0);
        return parity;
    }
    static void base_bytetobits(int byted, boolean[] bits) {
        bits[0] = ((byted & 128) > 0);
        bits[1] = ((byted & 64) > 0);
        bits[2] = ((byted & 32) > 0);
        bits[3] = ((byted & 16)  > 0);
        bits[4] = ((byted & 8)  > 0);
        bits[5] = ((byted & 4)  > 0);
        bits[6] = ((byted & 2)  > 0);
        bits[7] = ((byted & 1)  > 0);
    }
    static void quadres_16_7_calculate_valid_data_paritys() {
        int i;
        int parity_bits = 0;
        boolean [] bits = new boolean[8];

        for (i = 0; i < 128; i++) {
            base_bytetobits(i, bits);
            parity_bits = quadres_16_7_get_parity_bits(bits);
            quadres_16_7_valid_data_paritys[i] = parity_bits;
        }
    }

    // Returns 1 if the codeword is valid.
    public static boolean quadres_16_7_check(int codeword, int parity_val) {
        if(!initialized) {
            quadres_16_7_calculate_valid_data_paritys();
            initialized = true;
        }
        if(quadres_16_7_valid_data_paritys[codeword] == parity_val) {
            return true;
        }
        return false;
    }
}

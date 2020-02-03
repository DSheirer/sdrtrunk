package io.github.dsheirer.edac;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.BitSetFullException;
import io.github.dsheirer.bits.CorrectedBinaryMessage;

public class BPTC_196_96 {
    public static final int BPTC_LENGTH = 196;
    public static void main(String[] args) {
        String input = "0101011000000110110110000011010101011000100110000101000100110011011100110000010011000110100000111001001101000110001001101001100000110100101100001001111000010000011111010010000110001001000100011010";
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(BPTC_LENGTH);
        try
        {
            for(int i = 0; i < BPTC_LENGTH; i++) {
                message.add(input.charAt(i) == '0' ? false :true);
            }
            message = bptc_deinterleave(message);
            if(bptc_196_96_check_and_repair(message)){
                System.out.println("Output: " + message.toString());
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    public static CorrectedBinaryMessage bptc_deinterleave(CorrectedBinaryMessage msg) {
        int i;
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(BPTC_LENGTH);
        for (i = 0; i < BPTC_LENGTH; i++)
            message.set(i, msg.get((i*181) % BPTC_LENGTH));

        return message;
    }
    public static void bptc_196_96_hamming_15_11_3_get_parity_bits(int offset, BinaryMessage data_bits, boolean[] error_vector) {
        error_vector[0] = (data_bits.get(0 + offset) ^ data_bits.get(1 + offset) ^ data_bits.get(2 + offset) ^ data_bits.get(3 + offset) ^ data_bits.get(5 + offset) ^ data_bits.get(7 + offset) ^ data_bits.get(8 + offset));
        error_vector[1] = (data_bits.get(1 + offset) ^ data_bits.get(2 + offset) ^ data_bits.get(3 + offset) ^ data_bits.get(4 + offset) ^ data_bits.get(6 + offset) ^ data_bits.get(8 + offset) ^ data_bits.get(9 + offset));
        error_vector[2] = (data_bits.get(2 + offset) ^ data_bits.get(3 + offset) ^ data_bits.get(4 + offset) ^ data_bits.get(5 + offset) ^ data_bits.get(7 + offset) ^ data_bits.get(9 + offset) ^ data_bits.get(10 + offset));
        error_vector[3] = (data_bits.get(0 + offset) ^ data_bits.get(1 + offset) ^ data_bits.get(2 + offset) ^ data_bits.get(4 + offset) ^ data_bits.get(6 + offset) ^ data_bits.get(7 + offset) ^ data_bits.get(10 + offset));
    }
    public static void bptc_196_96_hamming_13_9_3_get_parity_bits(boolean[] data_bits, boolean[] error_vector) {
        error_vector[0] = (data_bits[0] ^ data_bits[1] ^ data_bits[3] ^ data_bits[5] ^ data_bits[6]);
        error_vector[1] = (data_bits[0] ^ data_bits[1] ^ data_bits[2] ^ data_bits[4] ^ data_bits[6] ^ data_bits[7]);
        error_vector[2] = (data_bits[0] ^ data_bits[1] ^ data_bits[2] ^ data_bits[3] ^ data_bits[5] ^ data_bits[7] ^ data_bits[8]);
        error_vector[3] = (data_bits[0] ^ data_bits[2] ^ data_bits[4] ^ data_bits[5] ^ data_bits[8]);
    }
    public static BinaryMessage bptc_196_96_generate(BinaryMessage data_bits) {
        BinaryMessage payload_info_bits = new BinaryMessage(196);
        boolean [] error_vector = new boolean[4];
        byte col, row;
        byte dbp;
        boolean[] column_bits = new boolean[9]; //flag_t

        dbp = 0;
        for (row = 0; row < 9; row++) {
            if (row == 0) {
                for (col = 3; col < 11; col++) {
                    // +1 because the first bit is R(3) and it's not used so we can ignore that.
                    payload_info_bits.set(col+1, data_bits.get(dbp++));
                }
            } else {
                for (col = 0; col < 11; col++) {
                    // +1 because the first bit is R(3) and it's not used so we can ignore that.
                    payload_info_bits.set(col+row*15+1, data_bits.get(dbp++));
                }
            }

            // +1 because the first bit is R(3) and it's not used so we can ignore that.
            bptc_196_96_hamming_15_11_3_get_parity_bits(row*15+1, payload_info_bits, error_vector);
            payload_info_bits.set(row*15+11+1,error_vector[0]);
            payload_info_bits.set(row*15+12+1,error_vector[1]);
            payload_info_bits.set(row*15+13+1,error_vector[2]);
            payload_info_bits.set(row*15+14+1,error_vector[3]);
        }

        for (col = 0; col < 15; col++) {
            for (row = 0; row < 9; row++)
                column_bits[row] = payload_info_bits.get(col+row*15+1);

            bptc_196_96_hamming_13_9_3_get_parity_bits(column_bits, error_vector);
            payload_info_bits.set(col+135+1,error_vector[0]);
            payload_info_bits.set(col+135+15+1,error_vector[1]);
            payload_info_bits.set(col+135+30+1,error_vector[2]);
            payload_info_bits.set(col+135+45+1,error_vector[3]);
        }

        //console_log(LOGLEVEL_CODING LOGLEVEL_DEBUG "bptc (196,96): constructed matrix:\n");
        //bptc_196_96_display_data_matrix(payload_info_bits.bits);

        return payload_info_bits;
    }
    public static void memcpy(BinaryMessage msgto, BinaryMessage msgfrom, int toindex, int fromindex, int size)
    {
        for(int i = 0; i <size; i++) {
            msgto.set(toindex + i, msgfrom.get(fromindex + i));
        }
    }
    public static CorrectedBinaryMessage bptc_196_96_extractdata(CorrectedBinaryMessage message) {
        BinaryMessage deinterleaved_bits = message;
        CorrectedBinaryMessage data_bits = new CorrectedBinaryMessage(96);
        memcpy(data_bits, deinterleaved_bits,0,4, 8);
        memcpy(data_bits, deinterleaved_bits, 8, 16, 11);;
        memcpy(data_bits, deinterleaved_bits, 19, 31, 11);;
        memcpy(data_bits, deinterleaved_bits, 30, 46, 11);;
        memcpy(data_bits, deinterleaved_bits, 41, 61, 11);;
        memcpy(data_bits, deinterleaved_bits, 52, 76, 11);;
        memcpy(data_bits, deinterleaved_bits, 63, 91, 11);;
        memcpy(data_bits, deinterleaved_bits, 74, 106, 11);;
        memcpy(data_bits, deinterleaved_bits, 85, 121, 11);;

        return data_bits;
    }
    public static boolean bptc_196_96_hamming_13_9_3_errorcheck(boolean[] data_bits, boolean[] error_vector) {
        bptc_196_96_hamming_13_9_3_get_parity_bits(data_bits, error_vector);

        error_vector[0] ^= data_bits[9];
        error_vector[1] ^= data_bits[10];
        error_vector[2] ^= data_bits[11];
        error_vector[3] ^= data_bits[12];

        if (error_vector[0] == false &&
                error_vector[1] == false &&
                        error_vector[2] == false &&
                                error_vector[3] == false)
            return true;

        return false;
    }
    static int bptc_196_96_find_hamming_15_11_3_error_position(boolean[] error_vector) {
        boolean hamming_15_11_generator_matrix[] = {
                true, false, false, true,
                true, true, false, true,
                true, true, true, true,
                true, true, true, false,
                false, true, true, true,
                true, false, true, false,
                false, true, false, true,
                true, false, true, true,
                true, true, false, false,
                false, true, true, false,
                false, false, true, true,

                true, false, false, false, // These are used to determine errors in the Hamming checksum bits.
                false, true, false, false,
                false, false, true, false,
                false, false, false, true
        };
        byte row;

        for (row = 0; row < 15; row++) {
            if (hamming_15_11_generator_matrix[row*4] == error_vector[0] &&
                    hamming_15_11_generator_matrix[row*4+1] == error_vector[1] &&
                            hamming_15_11_generator_matrix[row*4+2] == error_vector[2] &&
                                    hamming_15_11_generator_matrix[row*4+3] == error_vector[3])
                return row;
        }

        return -1;
    }

    static int bptc_196_96_find_hamming_13_9_3_error_position(boolean[] error_vector) {
        boolean [] hamming_13_9_generator_matrix = new boolean[]{
                true, true, true, true,
                true, true, true, false,
                false, true, true, true,
                false, true, true, true,
                false, true, false, true,
                true, false, true, true,
                true, true, false, false,
                false, true, true, false,
                false, false, true, true,

                true, false, false, false, // These are used to determine errors in the Hamming checksum bits.
                false, true, false, false,
                false, false, true, false,
                false, false, false, true
        };
        byte row;

        if (error_vector == null)
            return -1;

        for (row = 0; row < 13; row++) {
            if (hamming_13_9_generator_matrix[row*4] == error_vector[0] &&
                    hamming_13_9_generator_matrix[row*4+1] == error_vector[1] &&
                            hamming_13_9_generator_matrix[row*4+2] == error_vector[2] &&
                                    hamming_13_9_generator_matrix[row*4+3] == error_vector[3])
                return row;
        }

        return -1;
    }
    static boolean bptc_196_96_hamming_15_11_3_errorcheck(BinaryMessage data_bits, int offset, boolean[] error_vector) {
        if (data_bits == null || error_vector == null)
            return false;
        bptc_196_96_hamming_15_11_3_get_parity_bits(offset, data_bits, error_vector);

        error_vector[0] ^= data_bits.get(11 + offset);
        error_vector[1] ^= data_bits.get(12 + offset);
        error_vector[2] ^= data_bits.get(13 + offset);
        error_vector[3] ^= data_bits.get(14 + offset);

        if (error_vector[0] == false &&
                error_vector[1] == false &&
                        error_vector[2] == false &&
                                error_vector[3] == false)
            return true;

        return false;
    }
    public static boolean bptc_196_96_check_and_repair(BinaryMessage deinterleaved_bits) {
        boolean[] bptc_196_96_error_vector = new boolean[4];
        boolean[] column_bits = new boolean[13];
        byte row, col;
        int wrongbitnr = -1;
        boolean errors_found = false;
        boolean result = true;

        for (col = 0; col < 15; col++) {
            for (row = 0; row < 13; row++) {
                // +1 because the first bit is R(3) and it's not used so we can ignore that.
                column_bits[row] = deinterleaved_bits.get(col+row*15+1);
            }

            if (!bptc_196_96_hamming_13_9_3_errorcheck(column_bits, bptc_196_96_error_vector)) {
                errors_found = true;
                // Error check failed, checking if we can determine the location of the bit error.
                wrongbitnr = bptc_196_96_find_hamming_13_9_3_error_position(bptc_196_96_error_vector);
                if (wrongbitnr < 0) {
                    result = false;
                    //System.out.print(String.format("bptc (196,96): hamming(13,9) check error, can't repair column #%u\n", col));
                } else {
                    // +1 because the first bit is R(3) and it's not used so we can ignore that.
                    deinterleaved_bits.set(col+wrongbitnr*15+1, !deinterleaved_bits.get(col+wrongbitnr*15+1));

                    for (row = 0; row < 13; row++) {
                        // +1 because the first bit is R(3) and it's not used so we can ignore that.
                        column_bits[row] = deinterleaved_bits.get(col+row*15+1);
                    }

                    if (!bptc_196_96_hamming_13_9_3_errorcheck(column_bits, bptc_196_96_error_vector)) {
                        result = false;
                    }
                    //System.out.print(String.format("bptc (196,96): correct #%u\n", col));
                }
            }
        }

        for (row = 0; row < 9; row++) {
            // +1 because the first bit is R(3) and it's not used so we can ignore that.
            if (!bptc_196_96_hamming_15_11_3_errorcheck(deinterleaved_bits, row*15+1, bptc_196_96_error_vector)) {
                errors_found = true;
                // Error check failed, checking if we can determine the location of the bit error.
                wrongbitnr = bptc_196_96_find_hamming_15_11_3_error_position(bptc_196_96_error_vector);
                if (wrongbitnr < 0) {
                    result = false;
                    //System.out.println("    bptc (196,96): hamming(15,11) check error in row "+row+ ", can't repair\n");
                } else {
                    // +1 because the first bit is R(3) and it's not used so we can ignore that.
                    //System.out.println("    bptc (196,96): hamming(15,11) check error, fixing bit row #" + row + " col #" + wrongbitnr + "\n");
                    deinterleaved_bits.set(row*15+wrongbitnr+1, !deinterleaved_bits.get(row*15+wrongbitnr+1));

                    if (!bptc_196_96_hamming_15_11_3_errorcheck(deinterleaved_bits, row*15+1, bptc_196_96_error_vector)) {
                        result = true;
                        //System.out.println("    bptc (196,96): hamming(15,11) check error, couldn't repair row #"+ row+"\n");
                    }
                }
            }
        }
        /*
        if (result && !errors_found)
            System.out.println("    bptc (196,96): received data was error free\n");
        else if (result && errors_found)
            System.out.println("    bptc (196,96): received data had errors which were corrected\n");
        else if (!result)
            System.out.println("    bptc (196,96): received data had errors which couldn't be corrected\n");
            */
        return result;
    }
}

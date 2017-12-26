package io.github.dsheirer.dsp.filter.fir.remez;

public enum FIRLinearPhaseFilterType
{
	/**
	 * TYPE 1
	 *    Length: ODD
	 *     Order: EVEN
	 *  Symmetry: SYMMETRICAL
	 *  
	 * Valid for: LOW PASS, HIGH PASS
	 */
	TYPE_1_ODD_LENGTH_EVEN_ORDER_SYMMETRICAL,
	
	/**
	 * TYPE 2
	 *    Length: EVEN
	 *     Order: ODD
	 *  Symmetry: SYMMETRICAL
	 * Valid for: LOW PASS
	 * 
	 * Notes: 
	 * -Frequency response at FS/2 (Pi) must be 0 -- not suitable for High Pass.
	 */
	TYPE_2_EVEN_LENGTH_ODD_ORDER_SYMMETRICAL,
	
	/**
	 * TYPE 3
	 *    Length: ODD
	 *     Order: EVEN
	 *  Symmetry: ANTI-SYMMETRICAL
	 * Valid for: 
	 * 
	 * Notes:
	 * -Introduces a 90 degree phase shift such that h[N-n] = -h[n] for all n
	 * -Frequency response at 0 and FS/2 (Pi) must be 0 -- not suitable for Low or High Pass.
	 */
	TYPE_3_ODD_LENGTH_EVEN_ORDER_ANTI_SYMMETRICAL,
	
	/**
	 * TYPE 4
	 *    Length: EVEN
	 *     Order: ODD
	 *  Symmetry: ANTI-SYMMETRICAL
	 * Valid for: HIGH PASS
	 * 
	 * Notes:
	 * -Introduces a 90 degree phase shift such that h[N-n] = -h[n] for all n
	 * -Frequency response at 0 must be zero -- not suitable for Low Pass
	 */
	TYPE_4_EVEN_LENGTH_ODD_ORDER_ANTI_SYMMETRICAL,
	
    BANDPASS, 
    DIFFERENTIATOR, 
    HILBERT;
}

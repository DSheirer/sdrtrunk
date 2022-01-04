package io.github.dsheirer.vector.calibrate;

/**
 * Identifies the optimal implementation (Scalar vs Vector) identified through calibration testing
 */
public enum Implementation
{
    SCALAR,
    VECTOR_SIMD_PREFERRED,
    VECTOR_SIMD_64,
    VECTOR_SIMD_128,
    VECTOR_SIMD_256,
    VECTOR_SIMD_512,
    UNCALIBRATED;
}

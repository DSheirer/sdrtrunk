package io.github.dsheirer.vector.calibrate;

/**
 * Error encountered while calibrating
 */
public class CalibrationException extends Exception
{
    /**
     * Constructs a new instance
     * @param description of the error
     * @param error nested
     */
    public CalibrationException(String description, Exception error)
    {
        super(description, error);
    }
}

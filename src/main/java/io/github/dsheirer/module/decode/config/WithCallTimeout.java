package io.github.dsheirer.module.decode.config;

/**
 * Trait of decoder configurations that support providing information about call timeout.
 */
public interface WithCallTimeout
{
    /**
     * @return call timeout in seconds
     */
    int getCallTimeout();
}

package io.github.dsheirer.module.decode.p25;


import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;

/**
 * APCO25 message that exposes a service options configuration
 */
public interface IServiceOptionsProvider
{
    /**
     * Service Options
     * @return service options
     */
    ServiceOptions getServiceOptions();
}

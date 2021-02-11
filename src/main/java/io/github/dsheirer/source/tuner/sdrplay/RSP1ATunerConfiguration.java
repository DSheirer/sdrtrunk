package io.github.dsheirer.source.tuner.sdrplay;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import io.github.dsheirer.source.tuner.TunerType;

public class RSP1ATunerConfiguration extends SDRplayTunerConfiguration {


	
	  /**
     * Default constructor for JAXB
     */
    public RSP1ATunerConfiguration()
    {
    }

    public RSP1ATunerConfiguration(String uniqueID, String name)
    {
        super(uniqueID, name);
    }
	
    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public TunerType getTunerType()
    {
        return TunerType.SDRPLAY_RSP1A;
    }
  
}

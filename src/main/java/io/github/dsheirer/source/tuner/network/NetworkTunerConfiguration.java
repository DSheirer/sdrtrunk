package io.github.dsheirer.source.tuner.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;

/**
 * Network tuner configuration for using network connected tuner as a source
 */
public class NetworkTunerConfiguration extends TunerConfiguration {
  private static final Logger mLog = LoggerFactory.getLogger(NetworkTunerConfiguration.class);
  private String mHost;
  private short mPort;

  /**
   * Jackson constructor
   */
  public NetworkTunerConfiguration() {
    super(0, Long.MAX_VALUE);
  }

  /**
   * Constructs an instance
   * 
   * @param uniqueId to use for this configuration
   */
  public NetworkTunerConfiguration(String uniqueId) {
    this();
    setUniqueID(uniqueId);
  }

  @JsonIgnore
  @Override
  public TunerType getTunerType() {
    return TunerType.RTL_TCP; // TODO: support more than this.
  }

  @JacksonXmlProperty(isAttribute = true, localName = "host")
  public String getHost() {
    return mHost;
  }

  public void setHost(String host) {
    mHost = host;
  }

  @JacksonXmlProperty(isAttribute = true, localName = "port")
  public short getPort() {
    return mPort;
  }

  public void setPort(short port) {
    mPort = port;
  }

  public static NetworkTunerConfiguration create() {
    return new NetworkTunerConfiguration("Network " + System.currentTimeMillis());
  }
}

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
  private boolean mBiasTEnabled = false;
  private int mGain = 0;
  private boolean mGainAuto = true;
  private long mSampleRate = 2_048_000;
  private boolean mAutoReconnect = false;

  /**
   * Jackson constructor
   */
  public NetworkTunerConfiguration() {
    super(52000000, 2200000000l);
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

  @JacksonXmlProperty(isAttribute = true, localName = "bias_t_enabled")
  public boolean isBiasTEnabled() {
    return mBiasTEnabled;
  }

  public void setBiasTEnabled(boolean enabled) {
    mBiasTEnabled = enabled;
  }

  @JacksonXmlProperty(isAttribute = true, localName = "gain_auto")
  public boolean isGainAuto() {
    return mGainAuto;
  }

  public void setGainAuto(boolean gainAuto) {
    mGainAuto = gainAuto;
  }

  @JacksonXmlProperty(isAttribute = true, localName = "gain")
  public int getGain() {
    return mGain;
  }

  public void setGain(int gain) {
    mGain = gain;
  }

  @JacksonXmlProperty(isAttribute = true, localName = "sample_rate")
  public long getSampleRate() {
    return mSampleRate;
  }

  public void setSampleRate(long sampleRate) {
    mSampleRate = sampleRate;
  }

  @JacksonXmlProperty(isAttribute = true, localName = "auto_reconnect")
  public boolean isAutoReconnect() {
    return mAutoReconnect;
  }

  public void setAutoReconnect(boolean autoReconnect) {
    mAutoReconnect = autoReconnect;
  }

  public static NetworkTunerConfiguration create() {
    return new NetworkTunerConfiguration("Network " + System.currentTimeMillis());
  }
}

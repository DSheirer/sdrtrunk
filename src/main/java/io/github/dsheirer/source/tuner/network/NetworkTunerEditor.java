package io.github.dsheirer.source.tuner.network;

import javax.swing.JLabel;
import javax.swing.JSeparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import net.miginfocom.swing.MigLayout;

public class NetworkTunerEditor extends TunerEditor<NetworkTuner, NetworkTunerConfiguration> {
  private static final long serialVersionUID = 1L;
  private static final Logger mLog = LoggerFactory.getLogger(NetworkTunerEditor.class);
  private JLabel mHost;
  private JLabel mPort;

  public NetworkTunerEditor(UserPreferences userPreferences, TunerManager tunerManager,
      DiscoveredTuner discoveredTuner) {
    super(userPreferences, tunerManager, discoveredTuner);
    init();
    tunerStatusUpdated();
  }

  @Override
  public void setTunerLockState(boolean locked) {
    getFrequencyPanel().updateControls();
  }

  @Override
  public long getMinimumTunableFrequency() {
    return 52000000;
  }

  @Override
  public long getMaximumTunableFrequency() {
    return 2200000000l;
  }

  @Override
  protected void tunerStatusUpdated() {
    setLoading(true);

    if (hasTuner()) {
      getTunerIdLabel().setText(getTuner().getPreferredName());
    } else {
      getTunerIdLabel().setText(null);
    }

    String status = getDiscoveredTuner().getTunerStatus().toString();
    if (getDiscoveredTuner().hasErrorMessage()) {
      status += " - " + getDiscoveredTuner().getErrorMessage();
    }
    getTunerStatusLabel().setText(status);
    getButtonPanel().updateControls();
    getFrequencyPanel().updateControls();

    if (hasConfiguration()) {
      getHost().setText(getConfiguration().getHost());
      getPort().setText(getConfiguration().getPort() + "");
    }

    setLoading(false);
  }

  private void init() {
    setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill]", "[][][][][][][grow]"));

    add(new JLabel("Tuner:"));
    add(getTunerIdLabel(), "wrap");

    add(new JLabel("Status:"));
    add(getTunerStatusLabel(), "wrap");

    add(new JLabel("Host:"));
    add(getHost(), "wrap");

    add(new JLabel("Port:"));
    add(getPort(), "wrap");

    add(getButtonPanel(), "span,align left");
    add(new JSeparator(), "span,growx,push");

    add(new JLabel("Frequency (MHz):"));
    add(getFrequencyPanel(), "wrap");
  }

  private JLabel getHost() {
    if (mHost == null) {
      mHost = new JLabel();
    }

    return mHost;
  }

  private JLabel getPort() {
    if (mPort == null) {
      mPort = new JLabel();
    }

    return mPort;
  }

  @Override
  public void save() {
    if (hasConfiguration() && !isLoading()) {
      NetworkTunerConfiguration config = getConfiguration();
      config.setFrequency(getFrequencyControl().getFrequency());
      getConfiguration().setMinimumFrequency(getMinimumFrequencyTextField().getFrequency());
      getConfiguration().setMaximumFrequency(getMaximumFrequencyTextField().getFrequency());
      saveConfiguration();
    }
  }
}

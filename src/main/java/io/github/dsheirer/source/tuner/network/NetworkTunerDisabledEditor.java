package io.github.dsheirer.source.tuner.network;

import javax.swing.JLabel;
import javax.swing.JSeparator;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import net.miginfocom.swing.MigLayout;

/**
 * Minimal tuner editor for a network (rtl_tcp) tuner that has not been started. Mirrors
 * RTL2832UnknownTunerEditor's pattern: only the id/status labels and the button panel are shown -
 * no frequency/sample-rate/gain controls, since there's no live tuner to configure yet.
 */
public class NetworkTunerDisabledEditor extends TunerEditor<NetworkTuner, NetworkTunerConfiguration> {
  private static final long serialVersionUID = 1L;

  public NetworkTunerDisabledEditor(UserPreferences userPreferences, TunerManager tunerManager,
      DiscoveredTuner discoveredTuner) {
    super(userPreferences, tunerManager, discoveredTuner);
    init();
    tunerStatusUpdated();
  }

  private void init() {
    setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill][fill]", "[][][][grow]"));

    add(new JLabel("Tuner:"));
    add(getTunerIdLabel(), "wrap");

    add(new JLabel("Status:"));
    add(getTunerStatusLabel(), "wrap");

    add(getButtonPanel(), "span,align left");

    add(new JSeparator(), "span,growx,push");
  }

  @Override
  public long getMinimumTunableFrequency() {
    //Bogus value - matches NetworkTunerEditor's range since there's no live tuner to query yet.
    return 52000000;
  }

  @Override
  public long getMaximumTunableFrequency() {
    //Bogus value - matches NetworkTunerEditor's range since there's no live tuner to query yet.
    return 2200000000L;
  }

  @Override
  protected void save() {
    //No-op
  }

  @Override
  protected void tunerStatusUpdated() {
    setLoading(true);
    getTunerIdLabel().setText(getDiscoveredTuner().getId());

    String status = getDiscoveredTuner().getTunerStatus().toString();
    if (getDiscoveredTuner().hasErrorMessage()) {
      status += " - " + getDiscoveredTuner().getErrorMessage();
    }
    getTunerStatusLabel().setText(status);
    getButtonPanel().updateControls();
    setLoading(false);
  }

  @Override
  public void setTunerLockState(boolean locked) {
    getFrequencyPanel().updateControls();
  }
}

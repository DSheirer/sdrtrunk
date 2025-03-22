package io.github.dsheirer.source.tuner.network;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationManager;
import io.github.dsheirer.source.tuner.manager.DiscoveredNetworkTuner;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class AddNetworkTunerDialog extends JFrame {
  private final static Logger mLog = LoggerFactory.getLogger(AddNetworkTunerDialog.class);
  private UserPreferences mUserPreferences;
  private DiscoveredTunerModel mDiscoveredTunerModel;
  private TunerConfigurationManager mTunerConfigurationManager;
  private JLabel mTypeLabel;
  private JLabel mHostLabel;
  private JLabel mPortLabel;
  private JLabel mFrequencyLabel;
  private JComboBox mTypeComboBox;
  private JTextField mHostField;
  private JTextField mPortField;
  private JTextField mFrequencyField;
  private JButton mAddButton;
  private JButton mCancelButton;

  public AddNetworkTunerDialog(UserPreferences userPreferences, DiscoveredTunerModel discoveredTunerModel,
      TunerConfigurationManager tunerConfigurationManager) {
    Validate.notNull(userPreferences, "UserPreferences cannot be null");
    Validate.notNull(discoveredTunerModel, "TunerModel cannot be null");
    Validate.notNull(tunerConfigurationManager, "TunerConfigurationManager cannot be null");

    mDiscoveredTunerModel = discoveredTunerModel;
    mUserPreferences = userPreferences;
    mTunerConfigurationManager = tunerConfigurationManager;

    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setTitle("Add Network Tuner");
    setSize(new Dimension(500, 250));

    JPanel content = new JPanel();
    content.setLayout(new MigLayout("", "[align right][][]", "[][][][][grow][]"));

    mTypeLabel = new JLabel("Type:");
    content.add(mTypeLabel);

    String[] options = { "rtl-tcp" };
    mTypeComboBox = new JComboBox<>(options);
    content.add(mTypeComboBox, "span2,grow,wrap");

    mHostLabel = new JLabel("Host:");
    content.add(mHostLabel);

    mHostField = new JTextField("");
    mHostField.setToolTipText("Enter the hostname or IP address of the network tuner");
    content.add(mHostField, "span2,grow,wrap");

    mPortLabel = new JLabel("Port:");
    content.add(mPortLabel);

    mPortField = new JTextField("");
    mHostField.setToolTipText("Enter the port number of the network tuner");
    content.add(mPortField, "span2,grow,wrap");

    mFrequencyLabel = new JLabel("Frequency (Hz):");
    content.add(mFrequencyLabel);

    mFrequencyField = new JTextField("");
    mFrequencyField.setToolTipText("Enter the center frequency of the network tuner in Hertz (Hz)");
    content.add(mFrequencyField, "span2,grow,wrap");

    content.add(new JLabel(""), "wrap");
    content.add(new JLabel(""));

    mAddButton = new JButton("Add");
    // mAddButton.setEnabled(false);
    mAddButton.addActionListener(e -> {

      long frequency = getFrequency();

      if (frequency <= 0 || frequency > Integer.MAX_VALUE) {
        JOptionPane.showMessageDialog(AddNetworkTunerDialog.this,
            "Please provide a center frequency (1Hz to 2.14 GHz)",
            "Center Frequency Required",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      mLog.info("Adding network tuner = frequency [" + frequency + "] host [" + getHost() + ":" + getPort() + "]");

      try {
        NetworkTunerConfiguration config = NetworkTunerConfiguration.create();
        config.setFrequency(frequency);
        config.setHost(getHost());
        config.setPort(getPort());
        mTunerConfigurationManager.addTunerConfiguration(config);
        DiscoveredNetworkTuner discoveredNetworkTuner = new DiscoveredNetworkTuner(mUserPreferences, config);
        mDiscoveredTunerModel.addDiscoveredTuner(discoveredNetworkTuner);
      } catch (Exception ex) {
        mLog.error("Error adding network tuner", ex);
      }

      AddNetworkTunerDialog.this.setVisible(false);
    });
    content.add(mAddButton, "grow,push");

    mCancelButton = new JButton("Cancel");
    mCancelButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        AddNetworkTunerDialog.this.setVisible(false);
      }
    });
    content.add(mCancelButton, "grow,push");

    setContentPane(content);
  }

  private long getFrequency() {
    String text = mFrequencyField.getText();

    if (text != null && !text.isEmpty()) {
      try {
        return Long.parseLong(text);
      } catch (Exception e) {
        // Do nothing, we couldn't parse the frequency value
      }
    }

    return 0l;
  }

  private String getHost() {
    return mHostField.getText();
  }

  private short getPort() {
    String text = mPortField.getText();

    if (text != null && !text.isEmpty()) {
      try {
        return Short.parseShort(text);
      } catch (Exception e) {
        // Do nothing, we couldn't parse the port value
      }
    }

    return 0;
  }
}

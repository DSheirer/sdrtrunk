package io.github.dsheirer.source.tuner.network;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.dsheirer.source.tuner.configuration.TunerConfigurationManager;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class AddNetworkTunerDialog extends JFrame {
  private final static Logger mLog = LoggerFactory.getLogger(AddNetworkTunerDialog.class);
  private TunerConfigurationManager mTunerConfigurationManager;
  private JLabel mTypeLabel;
  private JLabel mHostLabel;
  private JLabel mPortLabel;
  private JComboBox mTypeComboBox;
  private JTextField mHostField;
  private JTextField mPortField;
  private JButton mAddButton;
  private JButton mCancelButton;

  public AddNetworkTunerDialog(TunerConfigurationManager tunerConfigurationManager) {
    Validate.notNull(tunerConfigurationManager, "TunerConfigurationManager cannot be null");

    mTunerConfigurationManager = tunerConfigurationManager;

    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setTitle("Add Network Tuner");
    setSize(new Dimension(500, 250));

    JPanel content = new JPanel();
    content.setLayout(new MigLayout("", "[align right][][]", "[][][][grow][]"));

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

    content.add(new JLabel(""), "wrap");
    content.add(new JLabel(""));

    mAddButton = new JButton("Add");
    mAddButton.setEnabled(false);
    mAddButton.addActionListener(e -> {

      // mTunerConfigurationManager.addTunerConfiguration();

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

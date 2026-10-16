/*
 * *****************************************************************************
 * Copyright (C) 2026 Jared Szechy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.source.tuner.network;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.dsheirer.source.tuner.manager.TunerManager;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class AddNetworkTunerDialog extends JFrame {
  private final static Logger mLog = LoggerFactory.getLogger(AddNetworkTunerDialog.class);
  private TunerManager mTunerManager;
  private JLabel mTypeLabel;
  private JLabel mHostLabel;
  private JLabel mPortLabel;
  private JLabel mFrequencyLabel;
  private JComboBox<String> mTypeComboBox;
  private JTextField mHostField;
  private JTextField mPortField;
  private JTextField mFrequencyField;
  private JButton mAddButton;
  private JButton mCancelButton;

  public AddNetworkTunerDialog(TunerManager tunerManager) {
    Validate.notNull(tunerManager, "TunerManager cannot be null");

    mTunerManager = tunerManager;

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

    mHostField = new JTextField("localhost");
    mHostField.setToolTipText("Enter the hostname or IP address of the network tuner");
    content.add(mHostField, "span2,grow,wrap");

    mPortLabel = new JLabel("Port:");
    content.add(mPortLabel);

    mPortField = new JTextField("1234");
    mPortField.setToolTipText("Enter the port number of the network tuner (1-65535)");
    content.add(mPortField, "span2,grow,wrap");

    mFrequencyLabel = new JLabel("Frequency (Hz):");
    content.add(mFrequencyLabel);

    mFrequencyField = new JTextField("100000000");
    mFrequencyField.setToolTipText("Enter the center frequency of the network tuner in Hertz (Hz)");
    content.add(mFrequencyField, "span2,grow,wrap");

    content.add(new JLabel(""), "wrap");
    content.add(new JLabel(""));

    mAddButton = new JButton("Add");
    // mAddButton.setEnabled(false);
    mAddButton.addActionListener(e -> {

      long frequency = getFrequency();
      String host = getHost();
      int port = getPort();

      if (frequency < 1_000_000L || frequency > 3_000_000_000L) {
        JOptionPane.showMessageDialog(AddNetworkTunerDialog.this,
            "Please provide a center frequency from 1 MHz to 3 GHz.",
            "Center Frequency Required",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      if (host.isBlank()) {
        JOptionPane.showMessageDialog(AddNetworkTunerDialog.this, "Please provide a host name or IP address.",
            "Host Required", JOptionPane.ERROR_MESSAGE);
        return;
      }

      if (port < 1 || port > 65535) {
        JOptionPane.showMessageDialog(AddNetworkTunerDialog.this, "Please provide a port from 1 to 65535.",
            "Valid Port Required", JOptionPane.ERROR_MESSAGE);
        return;
      }

      String id = "RTL TCP " + host + ":" + port;
      if (mTunerManager.getDiscoveredTunerModel().getDiscoveredTuner(id) != null) {
        JOptionPane.showMessageDialog(AddNetworkTunerDialog.this, "That network tuner already exists.",
            "Duplicate Network Tuner", JOptionPane.ERROR_MESSAGE);
        return;
      }

      mLog.info("Adding network tuner = frequency [" + frequency + "] host [" + host + ":" + port + "]");

      try {
        NetworkTunerConfiguration config = NetworkTunerConfiguration.create();
        config.setFrequency(frequency);
        config.setHost(host);
        config.setPort(port);
        mTunerManager.addNetworkTuner(config);
      } catch (Exception ex) {
        mLog.error("Error adding network tuner", ex);
        JOptionPane.showMessageDialog(AddNetworkTunerDialog.this, ex.getMessage(),
            "Unable to Add Network Tuner", JOptionPane.ERROR_MESSAGE);
        return;
      }

      AddNetworkTunerDialog.this.dispose();
    });
    content.add(mAddButton, "grow,push");

    mCancelButton = new JButton("Cancel");
    mCancelButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        AddNetworkTunerDialog.this.dispose();
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
    return mHostField.getText() == null ? "" : mHostField.getText().trim();
  }

  private int getPort() {
    String text = mPortField.getText();

    if (text != null && !text.isEmpty()) {
      try {
        return Integer.parseInt(text);
      } catch (Exception e) {
        // Do nothing, we couldn't parse the port value
      }
    }

    return 0;
  }
}

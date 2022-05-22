/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.recording;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationManager;
import io.github.dsheirer.source.tuner.manager.DiscoveredRecordingTuner;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

/**
 * Dialog to select a recording and specify a center frequency for use in adding a Recording Tuner
 */
public class AddRecordingTunerDialog extends JFrame
{
    private final static Logger mLog = LoggerFactory.getLogger(AddRecordingTunerDialog.class);
    private static final String SELECT_A_FILE = "Please select a recording file";
    private static final String LAST_FILE_BROWSE_LOCATION_KEY = "AddRecordingTunerDialog.lastBrowseLocation";
    private UserPreferences mUserPreferences;
    private DiscoveredTunerModel mDiscoveredTunerModel;
    private TunerConfigurationManager mTunerConfigurationManager;
    private JButton mSelectFileButton;
    private File mSelectedRecording;
    private JLabel mRecordingFileLabel;
    private JLabel mFrequencyLabel;
    private JTextField mFrequencyTextField;
    private JButton mAddButton;
    private JButton mCancelButton;
    private static final Pattern TUNER_RECORDING_PATTERN = Pattern.compile(".*_(\\d*)_baseband_\\d{8}_\\d{6}\\.wav");

    public AddRecordingTunerDialog(UserPreferences userPreferences, DiscoveredTunerModel discoveredTunerModel,
                                   TunerConfigurationManager tunerConfigurationManager)
    {
        Validate.notNull(userPreferences, "UserPreferences cannot be null");
        Validate.notNull(discoveredTunerModel, "TunerModel cannot be null");
        Validate.notNull(tunerConfigurationManager, "TunerConfigurationManager cannot be null");

        mDiscoveredTunerModel = discoveredTunerModel;
        mUserPreferences = userPreferences;
        mTunerConfigurationManager = tunerConfigurationManager;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setTitle("Select Recording File");
        setSize(new Dimension(500, 250));

        JPanel content = new JPanel();
        content.setLayout(new MigLayout("", "[align right][][]", "[][][grow][]"));

        mSelectFileButton = new JButton("Select ...");
        mSelectFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            String lastBrowsedDirectory = SystemProperties.getInstance().get(LAST_FILE_BROWSE_LOCATION_KEY, "");

            File browseDirectory;

            if(lastBrowsedDirectory != null && !lastBrowsedDirectory.isEmpty())
            {
                browseDirectory = new File(lastBrowsedDirectory);
            }
            else
            {
                browseDirectory = mUserPreferences.getDirectoryPreference().getDefaultRecordingDirectory().toFile();
            }

            fileChooser.setCurrentDirectory(browseDirectory);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new FileFilter()
            {
                @Override
                public boolean accept(File f)
                {
                    return f.getAbsolutePath().endsWith(".wav") || f.isDirectory();
                }

                @Override
                public String getDescription()
                {
                    return "Recordings (*.wav)";
                }
            });

            int returnVal = fileChooser.showOpenDialog(AddRecordingTunerDialog.this);

            File lastDirectory = fileChooser.getCurrentDirectory();

            if(lastDirectory != null)
            {
                SystemProperties.getInstance().set(LAST_FILE_BROWSE_LOCATION_KEY, lastDirectory.getAbsolutePath());
            }

            if(returnVal == JFileChooser.APPROVE_OPTION)
            {
                mSelectedRecording = fileChooser.getSelectedFile();

                mAddButton.setEnabled(mSelectedRecording != null);

                if(mSelectedRecording != null &&
                   (mFrequencyTextField.getText() == null || mFrequencyTextField.getText().isEmpty()))
                {
                    Matcher m = TUNER_RECORDING_PATTERN.matcher(mSelectedRecording.getName());

                    if(m.matches())
                    {
                        mFrequencyTextField.setText(m.group(1));
                    }
                }
            }
            else
            {
                mSelectedRecording = null;
            }

            mRecordingFileLabel.setText(mSelectedRecording != null ? mSelectedRecording.getName() : SELECT_A_FILE);
        });
        content.add(mSelectFileButton);

        mRecordingFileLabel = new JLabel(SELECT_A_FILE);
        content.add(mRecordingFileLabel, "span 2,wrap");

        mFrequencyLabel = new JLabel("Frequency (Hz):");
        content.add(mFrequencyLabel);

        mFrequencyTextField = new JTextField("");
        mFrequencyTextField.setToolTipText("Center frequency for the recording in Hertz (Hz)");
        content.add(mFrequencyTextField, "span 2,grow,wrap");

        content.add(new JLabel(""), "wrap");

        content.add(new JLabel(""));

        mAddButton = new JButton("Add");
        mAddButton.setEnabled(false);
        mAddButton.addActionListener(e -> {
            if(mSelectedRecording == null)
            {
                JOptionPane.showMessageDialog(AddRecordingTunerDialog.this,
                    "Please select a recording file",
                    "Select a Recording File",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            long frequency = getFrequency();

            if(frequency <= 0 || frequency > Integer.MAX_VALUE)
            {
                JOptionPane.showMessageDialog(AddRecordingTunerDialog.this,
                    "Please provide a recording center frequency (1 Hz to 2.14 GHz)",
                    "Center Frequency Required",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            mLog.info("Adding recording tuner = frequency [" + frequency +
                "] recording [" + mSelectedRecording.getAbsolutePath() + "]");


            try
            {
                RecordingTunerConfiguration config = RecordingTunerConfiguration.create();
                config.setFrequency(frequency);
                config.setPath(mSelectedRecording.getAbsolutePath());
                mTunerConfigurationManager.addTunerConfiguration(config);
                DiscoveredRecordingTuner discoveredRecordingTuner = new DiscoveredRecordingTuner(mUserPreferences, config);
                mDiscoveredTunerModel.addDiscoveredTuner(discoveredRecordingTuner);
            }
            catch(Exception ex)
            {
                mLog.error("Error adding recording tuner", ex);
            }

            AddRecordingTunerDialog.this.setVisible(false);
        });
        content.add(mAddButton, "grow,push");

        mCancelButton = new JButton("Cancel");
        mCancelButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                AddRecordingTunerDialog.this.setVisible(false);
            }
        });
        content.add(mCancelButton, "grow,push");

        setContentPane(content);
    }

    private long getFrequency()
    {
        String text = mFrequencyTextField.getText();

        if(text != null && !text.isEmpty())
        {
            try
            {
                return Long.parseLong(text);
            }
            catch(Exception e)
            {
                //Do nothing, we couldn't parse the frequency value
            }
        }

        return 0l;
    }
}

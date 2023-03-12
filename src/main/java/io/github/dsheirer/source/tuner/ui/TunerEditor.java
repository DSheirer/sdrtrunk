/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.source.tuner.ui;

import io.github.dsheirer.gui.control.JFrequencyControl;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.record.wave.IRecordingStatusListener;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.IDiscoveredTunerStatusListener;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.spectrum.SpectralDisplayPanel;
import io.github.dsheirer.util.SwingUtils;
import io.github.dsheirer.util.ThreadPool;
import java.awt.EventQueue;
import java.text.CharacterIterator;
import java.text.DecimalFormat;
import java.text.StringCharacterIterator;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Base tuner configuration editor.
 */
public abstract class TunerEditor<T extends Tuner,C extends TunerConfiguration> extends JPanel
        implements IDiscoveredTunerStatusListener, Listener<TunerEvent>
{
    private Logger mLog = LoggerFactory.getLogger(TunerEditor.class);
    private static final String BUTTON_STATUS_ENABLE = "Enable";
    private static final String BUTTON_STATUS_DISABLE = "Disable";
    private static final long serialVersionUID = 1L;
    private UserPreferences mUserPreferences;
    private TunerManager mTunerManager;
    private DiscoveredTuner mDiscoveredTuner;
    private C mTunerConfiguration;
    private FrequencyAndCorrectionChangeListener mFrequencyAndCorrectionChangeListener = new FrequencyAndCorrectionChangeListener();
    private JFrequencyControl mFrequencyControl;
    private JSpinner mFrequencyCorrectionSpinner;
    private JButton mEnabledButton;
    private JButton mViewSpectrumButton;
    private JButton mNewSpectrumButton;
    private JButton mRestartTunerButton;
    private JToggleButton mRecordButton;
    private ButtonPanel mButtonsPanel;
    private FrequencyPanel mFrequencyPanel;
    private JLabel mTunerIdLabel;
    private JCheckBox mAutoPPMCheckBox;
    private JLabel mMeasuredPPMLabel;
    private JLabel mRecordingStatusLabel;
    private JLabel mTunerStatusLabel;
    private JLabel mTunerLockedStatusLabel;
    private boolean mLoading = false;

    /**
     * Constructs an instance
     * @param tunerManager for requesting configuration saves.
     */
    public TunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        mUserPreferences = userPreferences;
        mTunerManager = tunerManager;
        mDiscoveredTuner = discoveredTuner;

        if(mDiscoveredTuner != null && mDiscoveredTuner.hasTunerConfiguration())
        {
            mTunerConfiguration = (C)mDiscoveredTuner.getTunerConfiguration();
        }

        if(mDiscoveredTuner != null)
        {
            mDiscoveredTuner.addTunerStatusListener(this);

            if(mDiscoveredTuner.hasTuner())
            {
                mDiscoveredTuner.getTuner().addTunerEventListener(this);

                if(!mDiscoveredTuner.hasTunerConfiguration())
                {
                    mLog.warn("Tuner does not have a tuner configuration ....");
                }
            }
        }
    }

    /**r
     * Indicates if the controls are currently being loaded with values.
     */
    protected boolean isLoading()
    {
        return mLoading;
    }

    /**
     * Changes the loading status
     */
    protected void setLoading(boolean loading)
    {
        mLoading = loading;
    }

    /**
     * Measured PPM value received from any decoders that may be running.
     */
    protected JLabel getMeasuredPPMLabel()
    {
        if(mMeasuredPPMLabel == null)
        {
            mMeasuredPPMLabel = new JLabel("");
            mMeasuredPPMLabel.setToolTipText("Displays the measured frequency error and PPM when provided by compatible channel decoders");
        }

        return mMeasuredPPMLabel;
    }

    /**
     * Tuner locked status label that can be turned on/off depending on tuner lock state.
     */
    protected JLabel getTunerLockedStatusLabel()
    {
        if(mTunerLockedStatusLabel == null)
        {
            mTunerLockedStatusLabel = new JLabel("Channel(s) active - frequency and sample rate controls are locked");
            mTunerLockedStatusLabel.setToolTipText("Indicates that the tuner is providing channel(s) and you can't " +
                    "change the tuner frequency or sample rate");
            mTunerLockedStatusLabel.setVisible(false);
        }

        return mTunerLockedStatusLabel;
    }

    /**
     * Tuner status label
     */
    protected JLabel getTunerStatusLabel()
    {
        if(mTunerStatusLabel == null)
        {
            mTunerStatusLabel = new JLabel(" ");
        }

        return mTunerStatusLabel;
    }


    /**
     * Label to display current file size for a wide-band recording
     */
    protected JLabel getRecordingStatusLabel()
    {
        if(mRecordingStatusLabel == null)
        {
            mRecordingStatusLabel = new JLabel(" ");
            mRecordingStatusLabel.setToolTipText("Shows the status of the latest baseband recording when active");
            mRecordingStatusLabel.setVisible(false);
        }

        return mRecordingStatusLabel;
    }

    /**
     * Check box for enable/disable automatic PPM adjustment from decoder(s) frequency error feedback.
     */
    protected JCheckBox getAutoPPMCheckBox()
    {
        if(mAutoPPMCheckBox == null)
        {
            mAutoPPMCheckBox = new JCheckBox("Enable decoder(s) to auto-adjust PPM");
            mAutoPPMCheckBox.setToolTipText("Allow decoders to measure channel frequency error and correct tuner PPM");
            mAutoPPMCheckBox.addActionListener(e ->
            {
                if(!isLoading())
                {
                    Tuner tuner = getTuner();
                    if(tuner != null)
                    {
                        boolean enabled = getAutoPPMCheckBox().isSelected();
                        getTuner().getTunerController().getFrequencyErrorCorrectionManager().setEnabled(enabled);
                        save();
                    }
                }
            });
        }

        return mAutoPPMCheckBox;
    }

    /**
     * Label for displaying the tuner ID
     */
    protected JLabel getTunerIdLabel()
    {
        if(mTunerIdLabel == null)
        {
            mTunerIdLabel = new JLabel(" ");
        }

        return mTunerIdLabel;
    }

    protected FrequencyPanel getFrequencyPanel()
    {
        if(mFrequencyPanel == null)
        {
            mFrequencyPanel = new FrequencyPanel();
            mFrequencyPanel.setToolTipText("Tuner frequency and PPM controls");
        }

        return mFrequencyPanel;
    }

    protected JSpinner getFrequencyCorrectionSpinner()
    {
        if(mFrequencyCorrectionSpinner == null)
        {
            SpinnerModel model = new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.1);
            mFrequencyCorrectionSpinner = new JSpinner(model);
            mFrequencyCorrectionSpinner.setToolTipText("Adjust the PPM value to compensate for tuner frequency error");
            mFrequencyCorrectionSpinner.setEnabled(false);
            JSpinner.NumberEditor editor = (JSpinner.NumberEditor) mFrequencyCorrectionSpinner.getEditor();
            DecimalFormat format = editor.getFormat();
            format.setMinimumFractionDigits(1);
            editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
            mFrequencyCorrectionSpinner.addChangeListener(mFrequencyAndCorrectionChangeListener);
        }

        return mFrequencyCorrectionSpinner;
    }

    protected ButtonPanel getButtonPanel()
    {
        if(mButtonsPanel == null)
        {
            mButtonsPanel = new ButtonPanel();
            mButtonsPanel.setToolTipText("Button controls for the selected tuner");
        }

        return mButtonsPanel;
    }

    protected JFrequencyControl getFrequencyControl()
    {
        if(mFrequencyControl == null)
        {
            mFrequencyControl = new JFrequencyControl();
        }

        return mFrequencyControl;
    }

    /**
     * Button requesting to show tuner in new spectral display
     */
    protected JButton getNewSpectrumButton()
    {
        if(mNewSpectrumButton == null)
        {
            mNewSpectrumButton = new JButton("New Spectrum Display");
            mNewSpectrumButton.setToolTipText("Show this tuner in a new (separate) spectral display window");
            mNewSpectrumButton.addActionListener(e ->
            {
                Tuner tuner = getTuner();

                if(tuner != null)
                {
                    ThreadPool.CACHED.submit(() -> mTunerManager.getDiscoveredTunerModel().broadcast(new TunerEvent(tuner,
                            TunerEvent.Event.REQUEST_NEW_SPECTRAL_DISPLAY)));
                }
            });
        }

        return mNewSpectrumButton;
    }

    /**
     * Button requesting to show tuner in main spectral display
     */
    protected JButton getViewSpectrumButton()
    {
        if(mViewSpectrumButton == null)
        {
            mViewSpectrumButton = new JButton("View Spectrum");
            mViewSpectrumButton.setToolTipText("Show this tuner in the spectral display");
            mViewSpectrumButton.addActionListener(e ->
            {
                Tuner tuner = getTuner();

                if(tuner != null)
                {
                    SystemProperties.getInstance().set(SpectralDisplayPanel.SPECTRAL_DISPLAY_ENABLED, true);

                    ThreadPool.CACHED.submit(() -> mTunerManager.getDiscoveredTunerModel().broadcast(new TunerEvent(tuner,
                            TunerEvent.Event.REQUEST_MAIN_SPECTRAL_DISPLAY)));
                }
            });
        }

        return mViewSpectrumButton;
    }

    /**
     * Button to change enable, disable, or error restart status.
     */
    protected JButton getEnabledButton()
    {
        if(mEnabledButton == null)
        {
            mEnabledButton = new JButton(BUTTON_STATUS_ENABLE);
            mEnabledButton.setToolTipText("Enable or disable the tuner for use by sdrtrunk");
            mEnabledButton.addActionListener(e ->
            {
                switch(getEnabledButton().getText())
                {
                    case BUTTON_STATUS_DISABLE:
                        mLog.info("Disabling " + getDiscoveredTuner().getTunerClass() + " tuner");
                        getDiscoveredTuner().setEnabled(false);
                        break;
                    case BUTTON_STATUS_ENABLE:
                        mLog.info("Enabling " + getDiscoveredTuner().getTunerClass() + " tuner");
                        getDiscoveredTuner().setEnabled(true);
                        break;
                    default:
                        mLog.info("None matched");
                }
            });
        }

        return mEnabledButton;
    }

    protected JButton getRestartTunerButton()
    {
        if(mRestartTunerButton == null)
        {
            mRestartTunerButton = new JButton("Restart Tuner");
            mRestartTunerButton.setVisible(false);
            mRestartTunerButton.setToolTipText("Attempt to restart this tuner to recover from error condition");
            mRestartTunerButton.addActionListener(e ->
            {
                if(!hasTuner() && getDiscoveredTuner().getTunerStatus() == TunerStatus.ERROR)
                {
                    mLog.info("Restarting " + getDiscoveredTuner().getTunerClass() + " tuner");
                    getDiscoveredTuner().restart();
                }
            });
        }

        return mRestartTunerButton;
    }

    /**
     * Turns off the recorder, if it's currently recording.
     */
    protected void turnOffRecorder()
    {
        if(getRecordButton().isSelected())
        {
            getRecordButton().setSelected(false);
        }
    }

    /**
     * Records the tuner's wide-band sample stream.
     */
    protected JToggleButton getRecordButton()
    {
        if(mRecordButton == null)
        {
            mRecordButton = new JToggleButton("Record");
            mRecordButton.setToolTipText("Create a baseband recording for this tuner");
            mRecordButton.setEnabled(false);
            mRecordButton.addActionListener(e ->
            {
                if(hasTuner())
                {
                    if(getRecordButton().isSelected())
                    {
                        getRecordingStatusLabel().setVisible(true);
                        getRecordingStatusLabel().setText(" ");
                        getTuner().getTunerController().startRecorder(mUserPreferences, new RecordingStatusListener(),
                                getDiscoveredTuner().getTunerClass().name());
                    }
                    else
                    {
                        getTuner().getTunerController().stopRecorder();
                    }
                }
            });
        }

        return mRecordButton;
    }

    protected abstract void save();

    /**
     * Access the tuner configuration
     */
    protected C getConfiguration()
    {
        return mTunerConfiguration;
    }

    /**
     * Indicates if this editor has a tuner configuration
     */
    protected boolean hasConfiguration()
    {
        return mTunerConfiguration != null;
    }

    /**
     * Discovered tuner for this editor
     */
    protected DiscoveredTuner getDiscoveredTuner()
    {
        return mDiscoveredTuner;
    }

    /**
     * Notification that the status of the discovered tuner has changed and that the editor implementation must
     * refresh the UI controls with the current tuner state.
     */
    protected abstract void tunerStatusUpdated();

    /**
     * Implements the tuner status listener interface to send notification to editor implementations that the
     * status of the discovered tuner has changed.
     * @param discoveredTuner that has a status change.
     * @param previous tuner status
     * @param current tuner status
     */
    @Override
    public void tunerStatusUpdated(DiscoveredTuner discoveredTuner, TunerStatus previous, TunerStatus current)
    {
        SwingUtils.run(() ->
        {
            tunerStatusUpdated();

            if(current == TunerStatus.ENABLED)
            {
                if(hasTuner())
                {
                    getTuner().addTunerEventListener(this);
                }
            }
        });
    }

    @Override
    public void receive(TunerEvent tunerEvent)
    {
        if(tunerEvent.getEvent().equals(TunerEvent.Event.UPDATE_MEASURED_FREQUENCY_ERROR))
        {
            SwingUtils.run(() -> getFrequencyPanel().updateFrequencyError());
        }
    }

    /**
     * Prepare this editor for disposal
     */
    public void dispose()
    {
        turnOffRecorder();

        getFrequencyControl().clearListeners();
        getFrequencyCorrectionSpinner().removeChangeListener(mFrequencyAndCorrectionChangeListener);

        if(mDiscoveredTuner != null)
        {
            mDiscoveredTuner.removeTunerStatusListener(this);

            if(mDiscoveredTuner.hasTuner())
            {
                mDiscoveredTuner.getTuner().removeTunerEventListener(this);
            }
        }
    }

    /**
     * Indicates if the discovered tuner has a usable tuner
     */
    public boolean hasTuner()
    {
        return mDiscoveredTuner != null && mDiscoveredTuner.hasTuner();
    }

    /**
     * Access the usable tuner from the discovered tuner
     * Note: use hasTuner() to check before invoking this method.
     * @return tuner or null.
     */
    public T getTuner()
    {
        if(hasTuner())
        {
            return (T)mDiscoveredTuner.getTuner();
        }

        return null;
    }

    /**
     * Request to save the state of this configuration.
     */
    protected void saveConfiguration()
    {
        mTunerManager.saveConfigurations();
    }

    /**
     * Sets the lock state for the tuner so that the frequency and sample rate controls can be enabled/disabled.
     *
     * Note: implementing classes should invoke: getFrequencyPanel().updateControls() method.
     *
     * @param locked true if the tuner is locked.
     */
    public abstract void setTunerLockState(boolean locked);

    /**
     * Tuner buttons panel
     */
    public class ButtonPanel extends JPanel
    {
        /**
         * Constructs an instance
         */
        public ButtonPanel()
        {
            setLayout(new MigLayout("insets 0,fill", "[][][][][][grow,fill]", ""));
            add(getEnabledButton());
            add(getRecordButton());
            add(getViewSpectrumButton());
            add(getNewSpectrumButton());
            add(getRestartTunerButton(), "wrap");
            add(getRecordingStatusLabel(), "span");
        }

        /**
         * Updates the state and text of the buttons based on the tuner status.
         */
        public void updateControls()
        {
            TunerStatus tunerStatus = getDiscoveredTuner().getTunerStatus();

            getRecordButton().setEnabled(tunerStatus.isAvailable() && getDiscoveredTuner().hasTuner());
            getRecordingStatusLabel().setText(" ");
            getViewSpectrumButton().setEnabled(tunerStatus.isAvailable() && getDiscoveredTuner().hasTuner());
            getNewSpectrumButton().setEnabled(tunerStatus.isAvailable() && getDiscoveredTuner().hasTuner());
            getRestartTunerButton().setVisible(tunerStatus == TunerStatus.ERROR);

            if(getDiscoveredTuner().isEnabled())
            {
                getEnabledButton().setText(BUTTON_STATUS_DISABLE);
            }
            else
            {
                getEnabledButton().setText(BUTTON_STATUS_ENABLE);
            }
        }
    }

    /**
     * Sub panel that displays frequency control, ppm spinner and control, and tuner locked status label.
     */
    public class FrequencyPanel extends JPanel
    {
        public FrequencyPanel()
        {
            setLayout(new MigLayout("insets 0,fill", "[][][][][grow,fill]", ""));
            add(getFrequencyControl(), "spany 2");
            add(new JLabel("PPM:"));
            add(getFrequencyCorrectionSpinner());
            add(getMeasuredPPMLabel(), "wrap");
            add(getAutoPPMCheckBox(), "span");
            add(getTunerLockedStatusLabel(), "span");
        }

        /**
         * Update the state of the frequency panel controls
         */
        public void updateControls()
        {
            getFrequencyControl().clearListeners();
            getFrequencyControl().addListener(mFrequencyAndCorrectionChangeListener);
            getFrequencyControl().setEnabled(hasTuner() && !getTuner().getTunerController().isLockedSampleRate());
            getTunerLockedStatusLabel().setVisible(hasTuner() && getTuner().getTunerController().isLockedSampleRate());
            getFrequencyCorrectionSpinner().setEnabled(hasTuner());
            getAutoPPMCheckBox().setEnabled(hasTuner());

            Tuner tuner = getTuner();

            if(tuner != null)
            {
                getFrequencyControl().setFrequency(tuner.getTunerController().getFrequency(), false);
                getFrequencyCorrectionSpinner().setValue(tuner.getTunerController().getFrequencyCorrection());
                getAutoPPMCheckBox().setSelected(tuner.getTunerController().getFrequencyErrorCorrectionManager().isEnabled());
                getFrequencyControl().addListener(getTuner().getTunerController());
                getTuner().getTunerController().addListener(getFrequencyControl());
                getMeasuredPPMLabel().setText(tuner.getTunerController().getMeasuredErrorStatus());
            }
            else
            {
                getFrequencyControl().setFrequency(0, false);
                getFrequencyCorrectionSpinner().setValue(0);
                getAutoPPMCheckBox().setSelected(false);
                getMeasuredPPMLabel().setText("");
            }
        }

        /**
         * Updates the measured frequency error label
         */
        public void updateFrequencyError()
        {
            SwingUtils.run(() ->
            {
                if(hasTuner())
                {
                    getMeasuredPPMLabel().setText(getTuner().getTunerController().getMeasuredErrorStatus());
                }
                else
                {
                    getMeasuredPPMLabel().setText("");
                }
            });
        }
    }

    /**
     * Implements status listener to receive updates for wide-band recordings
     */
    public class RecordingStatusListener implements IRecordingStatusListener
    {
        private DecimalFormat mSizeFormat = new DecimalFormat("0.0");

        @Override
        public void update(int fileCount, String file, long size)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Recording Size: ").append(humanReadableByteCount(size));
            sb.append(" File #").append(fileCount).append(": ").append(file);
            final String status = sb.toString();
            EventQueue.invokeLater(() -> getRecordingStatusLabel().setText(status));
        }

        public static String humanReadableByteCount(long bytes) {
            long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
            if (absB < 1024) {
                return bytes + " B";
            }
            long value = absB;
            CharacterIterator ci = new StringCharacterIterator("KMGTPE");
            for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
                value >>= 10;
                ci.next();
            }
            value *= Long.signum(bytes);
            return String.format("%.1f %cB", value / 1024.0, ci.current());
        }
    }

    /**
     * Monitors the frequency correction spinner for changed value.
     */
    private class FrequencyAndCorrectionChangeListener implements ChangeListener, ISourceEventProcessor
    {
        //This monitors the frequency correction spinner, applies the changes to the tuner, and saves configuration.
        @Override
        public void stateChanged(ChangeEvent e)
        {
            final double value = ((SpinnerNumberModel) getFrequencyCorrectionSpinner().getModel()).getNumber().doubleValue();

            if(hasTuner() && !isLoading())
            {
                try
                {
                    getTuner().getTunerController().setFrequencyCorrection(value);
                }
                catch(SourceException e1)
                {
                    mLog.error("Error setting frequency correction value", e1);
                }

                save();
            }
        }

        //This monitors the frequency control and saves configuration.
        @Override
        public void process(SourceEvent event) throws SourceException
        {
            if(hasTuner() && !isLoading())
            {
                save();
            }
        }
    }
}

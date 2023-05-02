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

package io.github.dsheirer.source.tuner.sdrplay.rspDuo;

import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.sdrplay.DiscoveredRspTuner;
import io.github.dsheirer.source.tuner.sdrplay.RspSampleRate;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerEditor;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.control.AgcMode;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.RspDuoAmPort;
import java.util.EnumSet;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SpinnerNumberModel;

/**
 * RSPduo Tuner 1 Editor
 */
public class RspDuoTuner1Editor extends RspTunerEditor<RspDuoTuner1Configuration>
{
    private static final Logger mLog = LoggerFactory.getLogger(RspDuoTuner1Editor.class);

    private JComboBox<RspSampleRate> mSampleRateCombo;
    private JCheckBox mRfDabNotchCheckBox;
    private JCheckBox mRfNotchCheckBox;
    private JCheckBox mAmNotchCheckBox;
    private JCheckBox mExternalReferenceOutputCheckBox;
    private JComboBox<RspDuoAmPort> mAmPortCombo;
    private JButton mTunerPreferencesButton;

    /**
     * Constructs an instance
     * @param userPreferences for settings
     * @param tunerManager for state updates
     * @param discoveredTuner to edit or control.
     */
    public RspDuoTuner1Editor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredRspTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill][fill]",
                "[][][][][][][][][][][][][][][][grow]"));

        add(new JLabel("Tuner:"));
        JPanel labelAndButtonPanel = new JPanel();
        labelAndButtonPanel.setLayout(new MigLayout("insets 0"));
        labelAndButtonPanel.add(getTunerIdLabel());
        labelAndButtonPanel.add(getTunerPreferencesButton());
        add(labelAndButtonPanel, "wrap");
        add(new JLabel("Status:"));
        add(getTunerStatusLabel(), "wrap");
        add(getButtonPanel(), "span,align left");

        add(new JSeparator(), "span,growx,push");

        add(new JLabel("Frequency (MHz):"));
        add(getFrequencyPanel(), "wrap");

        add(new JLabel("Sample Rate:"));
        add(getSampleRateCombo(), "wrap");

        add(new JLabel("IF AGC Mode:"));
        JPanel gainPanel = new JPanel();
        gainPanel.setLayout(new MigLayout("insets 0","[grow,fill][]",""));
        gainPanel.add(getAgcModeCombo());
        gainPanel.add(getGainOverloadButton());
        add(gainPanel, "wrap");

        add(new JLabel("Gain:"));
        add(getGainSlider());
        add(getGainValueLabel());

        add(new JSeparator(), "span,growx,push");

        add(new JLabel());
        add(getExternalReferenceOutputCheckBox(), "wrap");
        add(new JLabel());
        add(getAmNotchCheckBox(), "wrap");
        add(new JLabel());
        add(getRfDabNotchCheckBox(), "wrap");
        add(new JLabel());
        add(getRfNotchCheckBox(), "wrap");
        add(new JLabel("AM Port:"));
        add(getAmPortCombo(), "wrap");
    }

    /**
     * Access tuner controller
     */
    private RspDuoTuner1Controller getTunerController()
    {
        if(hasTuner())
        {
            return (RspDuoTuner1Controller) getTuner().getTunerController();
        }

        return null;
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
        getSampleRateCombo().setEnabled(!locked);
        updateSampleRateToolTip();
    }

    @Override
    protected void tunerStatusUpdated()
    {
        setLoading(true);

        getTunerIdLabel().setText(getDiscoveredTuner().getId());

        String status = getDiscoveredTuner().getTunerStatus().toString();
        if(getDiscoveredTuner().hasErrorMessage())
        {
            status += " - " + getDiscoveredTuner().getErrorMessage();
        }
        getTunerStatusLabel().setText(status);
        getButtonPanel().updateControls();
        mLog.info("Update tuner status - has tuner: " + hasTuner() + " is locked:" + getTuner().getTunerController().isLockedSampleRate());

        getFrequencyPanel().updateControls();

        clearSampleRates();
        if(hasTuner())
        {
            setSampleRates(getTunerController().getControlRsp().getSupportedSampleRates());
        }
        getSampleRateCombo().setEnabled(hasTuner() && !getTuner().getTunerController().isLockedSampleRate());
        getSampleRateCombo().setSelectedItem(hasTuner() ? getTunerController().getControlRsp().getSampleRateEnumeration() : null);
        updateSampleRateToolTip();

        getAgcModeCombo().setEnabled(hasTuner());
        if(hasTuner())
        {
            getAgcModeCombo().setSelectedItem(getTunerController().getControlRsp().getAgcMode());
            //Register to receive gain overload notifications
            getTunerController().getControlRsp().setGainOverloadListener(this);
        }

        getGainSlider().setEnabled(hasTuner());
        getGainValueLabel().setEnabled(hasTuner());
        getGainSlider().setValue(hasTuner() ? getTunerController().getControlRsp().getGain() : 0);

        getAmPortCombo().setEnabled(hasTuner() && !getTuner().getTunerController().isLockedSampleRate());
        try
        {
            getAmPortCombo().setSelectedItem(hasTuner() ? getTunerController().getControlRsp().getAmPort() : null);
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RSPduo tuner 1 AM Port in editor");
        }

        getRfDabNotchCheckBox().setEnabled(hasTuner());
        try
        {
            getRfDabNotchCheckBox().setSelected(hasTuner() && getTunerController().getControlRsp().isRfDabNotch());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RSPduo tuner 1 RF DAB Notch enabled state in editor");
        }

        getRfNotchCheckBox().setEnabled(hasTuner());
        try
        {
            getRfNotchCheckBox().setSelected(hasTuner() && getTunerController().getControlRsp().isRfNotch());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RSPduo tuner 1 RF Notch enabled state in editor");
        }

        getAmNotchCheckBox().setEnabled(hasTuner());
        try
        {
            getAmNotchCheckBox().setSelected(hasTuner() && getTunerController().getControlRsp().isAmNotch());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RSPduo tuner 1 AM Notch enabled state in editor");
        }

        getExternalReferenceOutputCheckBox().setEnabled(hasTuner());
        try
        {
            getExternalReferenceOutputCheckBox()
                    .setSelected(hasTuner() && getTunerController().getControlRsp().isExternalReferenceOutput());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RSPduo tuner 1 external reference output enabled state in editor");
        }

        setLoading(false);
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            getConfiguration().setFrequency(getFrequencyControl().getFrequency());
            double value = ((SpinnerNumberModel) getFrequencyCorrectionSpinner().getModel()).getNumber().doubleValue();
            getConfiguration().setFrequencyCorrection(value);
            getConfiguration().setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());
            getConfiguration().setSampleRate((RspSampleRate)getSampleRateCombo().getSelectedItem());
            getConfiguration().setAmNotch(getAmNotchCheckBox().isSelected());
            getConfiguration().setAmPort(getAmPortCombo().getSelectedItem() != null ? (RspDuoAmPort)getAmPortCombo().getSelectedItem() : null);
            getConfiguration().setExternalReferenceOutput(getExternalReferenceOutputCheckBox().isSelected());
            getConfiguration().setRfDabNotch(getRfDabNotchCheckBox().isSelected());
            getConfiguration().setRfNotch(getRfNotchCheckBox().isSelected());
            getConfiguration().setGain(getGainSlider().getValue());
            getConfiguration().setAgcMode((AgcMode)getAgcModeCombo().getSelectedItem());

            saveConfiguration();
        }
    }

    /**
     * Updates the sample rates listed in the combobox.
     * @param sampleRates to use.
     */
    private void setSampleRates(EnumSet<RspSampleRate> sampleRates)
    {
        if(!sampleRates.isEmpty())
        {
            for(RspSampleRate sampleRate: sampleRates)
            {
                getSampleRateCombo().addItem(sampleRate);
            }
        }
    }

    /**
     * Removes all sample rate options from the combo box.
     */
    private void clearSampleRates()
    {
        getSampleRateCombo().removeAllItems();
    }

    /**
     * Sample rate selection combobox control
     */
    private JComboBox<RspSampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new JComboBox<>();
            mSampleRateCombo.setEnabled(false);
            mSampleRateCombo.addActionListener(e -> {
                if(hasTuner() && !isLoading())
                {
                    RspSampleRate selected = (RspSampleRate)mSampleRateCombo.getSelectedItem();

                    try
                    {
                        getTunerController().setSampleRate(selected);
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Error setting sample rate for RSPduo tuner 1", se);
                    }
                }
            });
        }

        return mSampleRateCombo;
    }

    /**
     * AM port selection combobox control
     */
    private JComboBox<RspDuoAmPort> getAmPortCombo()
    {
        if(mAmPortCombo == null)
        {
            mAmPortCombo = new JComboBox<>(RspDuoAmPort.values());
            mAmPortCombo.setEnabled(false);
            mAmPortCombo.addActionListener(e -> {
                if(hasTuner() && !isLoading())
                {
                    RspDuoAmPort selected = (RspDuoAmPort)mAmPortCombo.getSelectedItem();

                    try
                    {
                        getTunerController().getControlRsp().setAmPort(selected);
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Error setting AM port for RSPduo tuner 1", se);
                    }
                }
            });
        }

        return mAmPortCombo;
    }

    /**
     * Checkbox control for RF DAB notch
     */
    private JCheckBox getRfDabNotchCheckBox()
    {
        if(mRfDabNotchCheckBox == null)
        {
            mRfDabNotchCheckBox = new JCheckBox("DAB Broadcast Band Filter (157-235 MHz)");
            mRfDabNotchCheckBox.setEnabled(false);
            mRfDabNotchCheckBox.addActionListener(e -> {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTunerController().getControlRsp().setRfDabNotch(mRfDabNotchCheckBox.isSelected());
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Unable to set RSPduo tuner 1 RF DAB notch enabled to " + mRfDabNotchCheckBox.isSelected(), se);
                    }
                }
            });
        }

        return mRfDabNotchCheckBox;
    }

    /**
     * Checkbox control for RF notch
     */
    private JCheckBox getRfNotchCheckBox()
    {
        if(mRfNotchCheckBox == null)
        {
            mRfNotchCheckBox = new JCheckBox("FM Broadcast Band Filter (78-114 MHz)");
            mRfNotchCheckBox.setEnabled(false);
            mRfNotchCheckBox.addActionListener(e -> {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTunerController().getControlRsp().setRfNotch(mRfNotchCheckBox.isSelected());
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Unable to set RSPduo tuner 1 RF notch enabled to " + mRfNotchCheckBox.isSelected(), se);
                    }
                }
            });
        }

        return mRfNotchCheckBox;
    }

    /**
     * Checkbox control for AM notch
     */
    private JCheckBox getAmNotchCheckBox()
    {
        if(mAmNotchCheckBox == null)
        {
            mAmNotchCheckBox = new JCheckBox("AM Broadcast Band Filter (415-1640 kHz)");
            mAmNotchCheckBox.setEnabled(false);
            mAmNotchCheckBox.addActionListener(e -> {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTunerController().getControlRsp().setAmNotch(mAmNotchCheckBox.isSelected());
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Unable to set RSPduo tuner 1 AM notch enabled to " + mAmNotchCheckBox.isSelected(), se);
                    }
                }
            });
        }

        return mAmNotchCheckBox;
    }

    /**
     * Checkbox control for External Reference Output
     */
    private JCheckBox getExternalReferenceOutputCheckBox()
    {
        if(mExternalReferenceOutputCheckBox == null)
        {
            mExternalReferenceOutputCheckBox = new JCheckBox("External Reference Output");
            mExternalReferenceOutputCheckBox.setEnabled(false);
            mExternalReferenceOutputCheckBox.addActionListener(e -> {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTunerController().getControlRsp().setExternalReferenceOutput(mExternalReferenceOutputCheckBox.isSelected());
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Unable to set RSPduo tuner 1 external reference output notch enabled to " +
                                mExternalReferenceOutputCheckBox.isSelected(), se);
                    }
                }
            });
        }

        return mExternalReferenceOutputCheckBox;
    }

    /**
     * Button to launch the User Preferences editor to show the RSPduo tuner preference selection.
     * @return constructed button.
     */
    private JButton getTunerPreferencesButton()
    {
        if(mTunerPreferencesButton == null)
        {
            mTunerPreferencesButton = new JButton("RSPduo Preferences");
            mTunerPreferencesButton.addActionListener(e -> MyEventBus.getGlobalEventBus()
                    .post(new ViewUserPreferenceEditorRequest(PreferenceEditorType.SOURCE_TUNERS)));
        }

        return mTunerPreferencesButton;
    }

    /**
     * Updates the sample rate tooltip according to the tuner controller's lock state.
     */
    private void updateSampleRateToolTip()
    {
        if(hasTuner() && getTuner().getTunerController().isLockedSampleRate())
        {
            getSampleRateCombo().setToolTipText("Sample Rate is locked.  Disable decoding channels to unlock.");
        }
        else if(hasTuner())
        {
            getSampleRateCombo().setToolTipText("Select a sample rate for the tuner");
        }
        else
        {
            getSampleRateCombo().setToolTipText("No tuner available");
        }
    }
}

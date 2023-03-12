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
package io.github.dsheirer.source.tuner.fcd.proplusV2;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.fcd.FCDTuner;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.SpinnerNumberModel;

/**
 * Funcube Dongle Pro Plus tuner editor
 */
public class FCD2TunerEditor extends TunerEditor<FCDTuner, FCD2TunerConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(FCD2TunerEditor.class);
    private static final long serialVersionUID = 1L;
    private JButton mTunerInfoButton;
    private JCheckBox mLnaGainCheckBox;
    private JCheckBox mMixerGainCheckBox;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager to save configuration
     * @param discoveredTuner to control
     */
    public FCD2TunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    private FCD2TunerController getController()
    {
        if(hasTuner())
        {
            return (FCD2TunerController)getTuner().getController();
        }

        return null;
    }

    @Override
    protected void tunerStatusUpdated()
    {
        setLoading(true);

        if(hasTuner())
        {
            getTunerIdLabel().setText(getTuner().getPreferredName());
        }
        else
        {
            getTunerIdLabel().setText(getDiscoveredTuner().getId());
        }

        String status = getDiscoveredTuner().getTunerStatus().toString();
        if(getDiscoveredTuner().hasErrorMessage())
        {
            status += " - " + getDiscoveredTuner().getErrorMessage();
        }
        getTunerStatusLabel().setText(status);
        getButtonPanel().updateControls();
        getFrequencyPanel().updateControls();
        getTunerInfoButton().setEnabled(hasTuner());
        getLnaGainCheckBox().setEnabled(hasTuner());
        getMixerGainCheckBox().setEnabled(hasTuner());

        if(hasTuner() && hasConfiguration())
        {
            getLnaGainCheckBox().setSelected(getConfiguration().getGainLNA());
            getMixerGainCheckBox().setSelected(getConfiguration().getGainMixer());
        }

        setLoading(false);
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill][fill]",
                "[][][][][][][][][][grow]"));

        add(new JLabel("Tuner:"));
        add(getTunerIdLabel());
        add(getTunerInfoButton());

        add(new JLabel("Status:"));
        add(getTunerStatusLabel(), "wrap");

        add(getButtonPanel(), "span,align left");

        add(new JSeparator(), "span,growx,push");
        add(new JLabel("Frequency (MHz):"));
        add(getFrequencyPanel(), "wrap");

        add(new JSeparator(), "span,growx,push");
        add(getLnaGainCheckBox());
        add(getMixerGainCheckBox());
    }

    public JButton getTunerInfoButton()
    {
        if(mTunerInfoButton == null)
        {
            mTunerInfoButton = new JButton("Tuner Info");
            mTunerInfoButton.setEnabled(false);
            mTunerInfoButton.addActionListener(e -> JOptionPane.showMessageDialog(FCD2TunerEditor.this,
                    getTunerInfo(), "Tuner Info", JOptionPane.INFORMATION_MESSAGE));
        }

        return mTunerInfoButton;
    }

    public JCheckBox getLnaGainCheckBox()
    {
        if(mLnaGainCheckBox == null)
        {
            mLnaGainCheckBox = new JCheckBox("LNA Gain");
            mLnaGainCheckBox.setEnabled(false);
            mLnaGainCheckBox.addActionListener(event ->
            {
                if(!isLoading())
                {
                    try
                    {
                        getController().setLNAGain(getLnaGainCheckBox().isSelected());
                        save();
                    }
                    catch(SourceException e)
                    {
                        mLog.error("Couldn't set LNA gain for FCD2", e);
                    }
                }
            });
        }

        return mLnaGainCheckBox;
    }

    public JCheckBox getMixerGainCheckBox()
    {
        if(mMixerGainCheckBox == null)
        {
            mMixerGainCheckBox = new JCheckBox("Mixer Gain");
            mMixerGainCheckBox.setEnabled(false);
            mMixerGainCheckBox.addActionListener(event ->
            {
                if(!isLoading())
                {
                    try
                    {
                        getController().setMixerGain(getMixerGainCheckBox().isSelected());
                        save();
                    }
                    catch(SourceException e)
                    {
                        mLog.error("Couldn't set mixer gain for FCD2", e);
                    }
                }
            });
        }

        return mMixerGainCheckBox;
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
    }

    private String getTunerInfo()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><h3>Funcube Dongle Pro Plus Tuner</h3>");

        if(hasTuner())
        {
            sb.append("<b>USB ID: </b>");
            sb.append(getController().getUSBID());
            sb.append("<br>");

            sb.append("<b>USB Address: </b>");
            sb.append(getController().getUSBAddress());
            sb.append("<br>");

            sb.append("<b>USB Speed: </b>");
            sb.append(getController().getUSBSpeed());
            sb.append("<br>");

            sb.append("<b>Cellular Band: </b>");
            sb.append(getController().getConfiguration().getBandBlocking());
            sb.append("<br>");

            sb.append("<b>Firmware: </b>");
            sb.append(getController().getConfiguration().getFirmware());
            sb.append("<br>");
        }

        return sb.toString();
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
            getConfiguration().setGainLNA(getLnaGainCheckBox().isSelected());
            getConfiguration().setGainMixer(getMixerGainCheckBox().isSelected());
            saveConfiguration();
        }
    }
}
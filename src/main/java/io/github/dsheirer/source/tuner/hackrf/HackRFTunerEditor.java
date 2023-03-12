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
package io.github.dsheirer.source.tuner.hackrf;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFLNAGain;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFSampleRate;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFVGAGain;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.usb.UsbException;

/**
 * HackRF Tuner Editor
 */
public class HackRFTunerEditor extends TunerEditor<HackRFTuner,HackRFTunerConfiguration>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(HackRFTunerEditor.class);
    private JButton mTunerInfo;
    private JComboBox<HackRFSampleRate> mSampleRateCombo;
    private JToggleButton mAmplifier;
    private JComboBox<HackRFLNAGain> mLnaGainCombo;
    private JComboBox<HackRFVGAGain> mVgaGainCombo;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager to save configuration
     * @param discoveredTuner to control
     */
    public HackRFTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill][fill]",
                "[][][][][][][][][][][grow]"));

        add(new JLabel("Tuner:"));
        add(getTunerIdLabel());
        add(getTunerInfoButton());

        add(new JLabel("Status:"));
        add(getTunerStatusLabel(), "wrap");

        add(getButtonPanel(), "span,align left");

        add(new JSeparator(), "span,growx,push");

        add(new JLabel("Frequency (MHz):"));
        add(getFrequencyPanel(), "wrap");

        add(new JLabel("Sample Rate:"));
        add(getSampleRateCombo(), "wrap");

        add(new JSeparator(), "span,growx,push");

        add(new JLabel("Gain Control"));
        add(getAmplifierToggle(), "wrap");

        add(new JLabel("LNA:"));
        add(getLnaGainCombo(), "wrap");

        add(new JLabel("VGA:"));
        add(getVgaGainCombo(), "wrap");
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
        getSampleRateCombo().setEnabled(hasTuner() && !getTuner().getTunerController().isLockedSampleRate());
        updateSampleRateToolTip();
        getTunerInfoButton().setEnabled(hasTuner());

        getAmplifierToggle().setEnabled(hasTuner());
        getLnaGainCombo().setEnabled(hasTuner());
        getVgaGainCombo().setEnabled(hasTuner());

        if(hasConfiguration())
        {
            getSampleRateCombo().setSelectedItem(getConfiguration().getSampleRate());
            getAmplifierToggle().setSelected(getConfiguration().getAmplifierEnabled());
            getLnaGainCombo().setSelectedItem(getConfiguration().getLNAGain());
            getVgaGainCombo().setSelectedItem(getConfiguration().getVGAGain());
        }

        setLoading(false);
    }

    private JComboBox getVgaGainCombo()
    {
        if(mVgaGainCombo == null)
        {
            mVgaGainCombo = new JComboBox<HackRFVGAGain>(HackRFVGAGain.values());
            mVgaGainCombo.setToolTipText("<html>VGA Gain.  Adjust to set the baseband gain</html>");
            mVgaGainCombo.setEnabled(false);
            mVgaGainCombo.addActionListener(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        HackRFVGAGain vgaGain = (HackRFVGAGain) mVgaGainCombo.getSelectedItem();

                        if(vgaGain == null)
                        {
                            vgaGain = HackRFVGAGain.GAIN_16;
                        }

                        getTuner().getController().setVGAGain(vgaGain);
                        save();
                    }
                    catch(UsbException e)
                    {
                        JOptionPane.showMessageDialog(HackRFTunerEditor.this, "HackRF Tuner Controller"
                                + " - couldn't apply the VGA gain setting - " + e.getLocalizedMessage());

                        mLog.error("HackRF Tuner Controller - couldn't apply VGA gain setting", e);
                    }
                }
            });
        }

        return mVgaGainCombo;
    }

    private JComboBox getLnaGainCombo()
    {
        if(mLnaGainCombo == null)
        {
            mLnaGainCombo = new JComboBox<HackRFLNAGain>(HackRFLNAGain.values());
            mLnaGainCombo.setToolTipText("<html>LNA Gain.  Adjust to set the IF gain</html>");
            mLnaGainCombo.setEnabled(false);
            mLnaGainCombo.addActionListener(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        HackRFLNAGain lnaGain = (HackRFLNAGain) mLnaGainCombo.getSelectedItem();

                        if(lnaGain == null)
                        {
                            lnaGain = HackRFLNAGain.GAIN_16;
                        }

                        getTuner().getController().setLNAGain(lnaGain);
                        save();
                    }
                    catch(UsbException e)
                    {
                        JOptionPane.showMessageDialog(HackRFTunerEditor.this, "HackRF Tuner Controller"
                                + " - couldn't apply the LNA gain setting - " + e.getLocalizedMessage());
                        mLog.error("HackRF Tuner Controller - couldn't apply LNA gain setting - ", e);
                    }
                }
            });
        }

        return mLnaGainCombo;
    }

    private JToggleButton getAmplifierToggle()
    {
        if(mAmplifier == null)
        {
            mAmplifier = new JToggleButton("Amplifier");
            mAmplifier.setToolTipText("Enable or disable the gain amplifier");
            mAmplifier.setEnabled(false);
            mAmplifier.addActionListener(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        getTuner().getController().setAmplifierEnabled(mAmplifier.isSelected());
                        save();
                    }
                    catch(UsbException e)
                    {
                        mLog.error("couldn't enable/disable amplifier", e);

                        JOptionPane.showMessageDialog(HackRFTunerEditor.this, "Couldn't change amplifier setting",
                                "Error changing amplifier setting", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        }

        return mAmplifier;
    }

    private JComboBox getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            HackRFSampleRate[] validRates = HackRFSampleRate.VALID_SAMPLE_RATES.toArray(new HackRFSampleRate[0]);
            mSampleRateCombo = new JComboBox<>(validRates);
            mSampleRateCombo.setEnabled(false);
            mSampleRateCombo.addActionListener(e ->
            {
                if(!isLoading())
                {
                    HackRFSampleRate sampleRate = (HackRFSampleRate)getSampleRateCombo().getSelectedItem();

                    try
                    {
                        getTuner().getController().setSampleRate(sampleRate);
                        save();
                    }
                    catch(SourceException | UsbException e2)
                    {
                        JOptionPane.showMessageDialog(HackRFTunerEditor.this, "HackRF Tuner Controller"
                                + " - couldn't apply the sample rate setting [" + sampleRate.getLabel() +
                                "] " + e2.getLocalizedMessage());

                        mLog.error("HackRF Tuner Controller - couldn't apply sample rate setting [" +
                                sampleRate.getLabel() + "]", e);
                    }
                }
            });
        }

        return mSampleRateCombo;
    }

    private JButton getTunerInfoButton()
    {
        if(mTunerInfo == null)
        {
            mTunerInfo = new JButton("Tuner Info");
            mTunerInfo.setToolTipText("Provides details and information about the tuner");
            mTunerInfo.setEnabled(false);
            mTunerInfo.addActionListener(e -> JOptionPane.showMessageDialog(HackRFTunerEditor.this,
                    getTunerInfo(), "Tuner Info", JOptionPane.INFORMATION_MESSAGE));
        }

        return mTunerInfo;
    }

    /**
     * Updates the sample rate tooltip according to the tuner controller's lock state.
     */
    private void updateSampleRateToolTip()
    {
        if(hasTuner() && getTuner().getController().isLockedSampleRate())
        {
            mSampleRateCombo.setToolTipText("Sample Rate is locked.  Disable decoding channels to unlock.");
        }
        else
        {
            mSampleRateCombo.setToolTipText("Select a sample rate for the tuner");
        }
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
        getSampleRateCombo().setEnabled(!locked);
        updateSampleRateToolTip();
    }

    private String getTunerInfo()
    {
        HackRFTunerController.BoardID board = HackRFTunerController.BoardID.INVALID;

        try
        {
            if(hasTuner())
            {
                board = getTuner().getController().getBoardID();
            }
        }
        catch(UsbException e)
        {
            mLog.error("couldn't read HackRF board identifier", e);
        }

        StringBuilder sb = new StringBuilder();

        sb.append("<html><h3>HackRF Tuner</h3>");
        sb.append("<b>Board: </b>");
        sb.append(board.getLabel());
        sb.append("<br>");

        HackRFTunerController.Serial serial = null;

        try
        {
            if(hasTuner())
            {
                serial = getTuner().getController().getSerial();
            }
        }
        catch(Exception e)
        {
            mLog.error("couldn't read HackRF serial number", e);
        }

        if(serial != null)
        {
            sb.append("<b>Serial: </b>");
            sb.append(serial.getSerialNumber());
            sb.append("<br>");

            sb.append("<b>Part: </b>");
            sb.append(serial.getPartID());
            sb.append("<br>");
        }
        else
        {
            sb.append("<b>Serial: Unknown</b><br>");
            sb.append("<b>Part: Unknown</b><br>");
        }

        String firmware = null;

        try
        {
            if(hasTuner())
            {
                firmware = getTuner().getController().getFirmwareVersion();
            }
        }
        catch(Exception e)
        {
            mLog.error("couldn't read HackRF firmware version", e);
        }

        if(firmware != null)
        {
            sb.append("<b>Firmware: </b>");
            sb.append(firmware);
            sb.append("<br>");
        }
        else
        {
            sb.append("<b>Firmware: Unknown</b><br>");
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
            getConfiguration().setSampleRate((HackRFSampleRate)getSampleRateCombo().getSelectedItem());
            getConfiguration().setAmplifierEnabled(getAmplifierToggle().isSelected());
            getConfiguration().setLNAGain((HackRFLNAGain)getLnaGainCombo().getSelectedItem());
            getConfiguration().setVGAGain((HackRFVGAGain)getVgaGainCombo().getSelectedItem());
            saveConfiguration();
        }
    }
}
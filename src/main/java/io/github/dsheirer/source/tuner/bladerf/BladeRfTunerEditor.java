/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.bladerf;

import io.github.dsheirer.bladerf.GainMode;
import io.github.dsheirer.bladerf.GainModeInfo;
import io.github.dsheirer.bladerf.Range;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import java.awt.Component;
import java.io.Serial;
import java.util.List;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.SpinnerNumberModel;

/**
 * bladeRF tuner editor
 */
public class BladeRfTunerEditor extends TunerEditor<BladeRfTuner, BladeRfTunerConfiguration>
{
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger mLog = LoggerFactory.getLogger(BladeRfTunerEditor.class);
    private JButton mTunerInfoButton;
    private JComboBox<BladeRfSampleRate> mSampleRateCombo;
    private JCheckBox mBiasTCheckBox;
    private JComboBox<GainModeInfo> mGainModeCombo;
    private JSlider mGainSlider;
    private JLabel mGainValueLabel;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving tuner configuration
     * @param discoveredTuner for the optionally usable bladeRF tuner and controller
     */
    public BladeRfTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return hasTuner() ? getTuner().getController().getMinimumTunableFrequency() :
                BladeRfTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return hasTuner() ? getTuner().getController().getMaximumTunableFrequency() :
                BladeRfTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill][fill]", "[][][][][][][][][][][][grow]"));

        add(new JLabel("Tuner:"));
        add(getTunerIdLabel());
        add(getTunerInfoButton());

        add(new JLabel("Status:"));
        add(getTunerStatusLabel(), "wrap");

        add(getButtonPanel(), "span,align left");

        add(new JSeparator(), "span,growx");

        add(new JLabel("Frequency (MHz):"));
        add(getFrequencyPanel(), "wrap");

        add(new JLabel("Sample Rate:"));
        add(getSampleRateCombo(), "wrap");

        add(new JLabel());
        add(getBiasTCheckBox(), "wrap");

        add(new JSeparator(), "span,growx");
        add(new JLabel("Gain Control"), "wrap");

        add(new JLabel("Mode:"));
        add(getGainModeCombo(), "wrap");

        add(new JLabel("Gain:"));
        add(getGainSlider());
        add(getGainValueLabel());

        //Empty row that takes all extra vertical space so that the controls stay stacked at the top of the editor.
        add(new JLabel(), "span,pushy");
    }

    @Override
    protected void tunerStatusUpdated()
    {
        setLoading(true);

        boolean started = hasStartedTuner();
        getTunerIdLabel().setText(started ? getTuner().getPreferredName() : getDiscoveredTuner().getId());

        String status = getDiscoveredTuner().getTunerStatus().toString();
        if(getDiscoveredTuner().hasErrorMessage())
        {
            status += " - " + getDiscoveredTuner().getErrorMessage();
        }
        getTunerStatusLabel().setText(status);
        getButtonPanel().updateControls();
        getFrequencyPanel().updateControls();
        getTunerInfoButton().setEnabled(started);

        if(started)
        {
            BladeRfTunerController controller = getTuner().getController();
            List<BladeRfSampleRate> rates = controller.getSampleRates();
            getSampleRateCombo().setModel(new DefaultComboBoxModel<>(rates.toArray(new BladeRfSampleRate[0])));
            getSampleRateCombo().setSelectedItem(controller.getBladeRfSampleRate());
            getSampleRateCombo().setEnabled(!controller.isLockedSampleRate());

            getBiasTCheckBox().setVisible(controller.isBiasTSupported());
            getBiasTCheckBox().setEnabled(true);

            List<GainModeInfo> modes = controller.getGainModes();
            getGainModeCombo().setModel(new DefaultComboBoxModel<>(modes.toArray(new GainModeInfo[0])));
            getGainModeCombo().setEnabled(true);

            Range range = controller.getGainRange();
            getGainSlider().setMinimum((int)range.min());
            getGainSlider().setMaximum((int)range.max());

            if(hasConfiguration())
            {
                getBiasTCheckBox().setSelected(getConfiguration().isBiasT());
                getGainSlider().setValue(getConfiguration().getGain());

                for(GainModeInfo mode : modes)
                {
                    if(mode.mode() == getConfiguration().getGainMode())
                    {
                        getGainModeCombo().setSelectedItem(mode);
                    }
                }
            }

            updateGainControls();
        }
        else
        {
            getSampleRateCombo().setModel(new DefaultComboBoxModel<>());
            getSampleRateCombo().setEnabled(false);
            getBiasTCheckBox().setEnabled(false);
            getBiasTCheckBox().setSelected(false);
            getGainModeCombo().setModel(new DefaultComboBoxModel<>());
            getGainModeCombo().setEnabled(false);
            getGainSlider().setEnabled(false);
            getGainValueLabel().setEnabled(false);
        }

        setLoading(false);
    }

    /**
     * Indicates if the tuner has finished starting, since a status update can arrive while it's starting or restarting.
     */
    private boolean hasStartedTuner()
    {
        return hasTuner() && getTuner().getController().getGainRange() != null;
    }

    /**
     * Enables the gain slider when the gain mode is manual and refreshes the gain value label.
     */
    private void updateGainControls()
    {
        GainModeInfo mode = (GainModeInfo)getGainModeCombo().getSelectedItem();
        boolean manual = hasTuner() && mode != null && mode.mode() == GainMode.MANUAL;
        getGainSlider().setEnabled(manual);
        getGainValueLabel().setEnabled(manual);
        getGainValueLabel().setText(manual ? getGainSlider().getValue() + " dB" : "AGC");
    }

    /**
     * Button that provides tuner information
     */
    private JButton getTunerInfoButton()
    {
        if(mTunerInfoButton == null)
        {
            mTunerInfoButton = new JButton("Info");
            mTunerInfoButton.setEnabled(false);
            mTunerInfoButton.addActionListener(_ -> JOptionPane.showMessageDialog(BladeRfTunerEditor.this,
                    getTunerInfo(), "Tuner Info", JOptionPane.INFORMATION_MESSAGE));
        }

        return mTunerInfoButton;
    }

    private JComboBox<BladeRfSampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new JComboBox<>();
            mSampleRateCombo.setEnabled(false);
            mSampleRateCombo.addActionListener(_ ->
            {
                if(hasTuner() && !isLoading())
                {
                    BladeRfSampleRate rate = (BladeRfSampleRate)mSampleRateCombo.getSelectedItem();

                    try
                    {
                        getTuner().getController().setSampleRate(rate);

                        //Adjust the min/max values for the sample rate.
                        adjustForSampleRate(rate.getRate());

                        save();
                    }
                    catch(Exception e1)
                    {
                        mLog.error("Error setting bladeRF sample rate", e1);
                        JOptionPane.showMessageDialog(BladeRfTunerEditor.this, "Couldn't set sample rate to " + rate);
                    }
                }
            });
        }

        return mSampleRateCombo;
    }

    /**
     * Checkbox to enable/disable Bias-T power for active antennas
     */
    private JCheckBox getBiasTCheckBox()
    {
        if(mBiasTCheckBox == null)
        {
            mBiasTCheckBox = new JCheckBox("Bias-T");
            mBiasTCheckBox.setEnabled(false);
            mBiasTCheckBox.setToolTipText("Enable Bias-T power output for active antennas.  Also powers the RX2 connector.");
            mBiasTCheckBox.addActionListener(_ ->
            {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTuner().getController().setBiasT(mBiasTCheckBox.isSelected());
                        save();
                    }
                    catch(Exception e1)
                    {
                        mLog.error("Error setting Bias-T", e1);
                        JOptionPane.showMessageDialog(mBiasTCheckBox, "Couldn't set Bias-T: " + e1.getMessage());
                    }
                }
            });
        }

        return mBiasTCheckBox;
    }

    private JComboBox<GainModeInfo> getGainModeCombo()
    {
        if(mGainModeCombo == null)
        {
            mGainModeCombo = new JComboBox<>();
            mGainModeCombo.setEnabled(false);
            mGainModeCombo.setRenderer(new DefaultListCellRenderer()
            {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                              boolean focused)
                {
                    Object label = value instanceof GainModeInfo mode ? mode.name() : value;
                    return super.getListCellRendererComponent(list, label, index, selected, focused);
                }
            });
            mGainModeCombo.addActionListener(_ ->
            {
                if(hasTuner() && !isLoading())
                {
                    GainModeInfo mode = (GainModeInfo)mGainModeCombo.getSelectedItem();

                    try
                    {
                        getTuner().getController().setGainMode(mode.mode());
                        updateGainControls();
                        save();
                    }
                    catch(Exception e1)
                    {
                        mLog.error("Error setting bladeRF gain mode", e1);
                        JOptionPane.showMessageDialog(mGainModeCombo, "Couldn't set gain mode to " + mode.name());
                    }
                }
            });
        }

        return mGainModeCombo;
    }

    private JSlider getGainSlider()
    {
        if(mGainSlider == null)
        {
            mGainSlider = new JSlider(JSlider.HORIZONTAL, 0, 60, BladeRfTunerController.GAIN_DEFAULT);
            mGainSlider.setEnabled(false);
            mGainSlider.setMajorTickSpacing(5);
            mGainSlider.setPaintTicks(true);
            mGainSlider.addChangeListener(_ ->
            {
                int gain = mGainSlider.getValue();

                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTuner().getController().setGain(gain);
                        save();
                    }
                    catch(Exception e)
                    {
                        mLog.error("Couldn't set bladeRF gain to:" + gain, e);
                        JOptionPane.showMessageDialog(mGainSlider, "Couldn't set gain value to " + gain);
                    }
                }

                updateGainControls();
            });
        }

        return mGainSlider;
    }

    private JLabel getGainValueLabel()
    {
        if(mGainValueLabel == null)
        {
            mGainValueLabel = new JLabel("0 dB");
            mGainValueLabel.setEnabled(false);
        }

        return mGainValueLabel;
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            getConfiguration().setFrequency(getFrequencyControl().getFrequency());
            getConfiguration().setMinimumFrequency(getMinimumFrequencyTextField().getFrequency());
            getConfiguration().setMaximumFrequency(getMaximumFrequencyTextField().getFrequency());
            double value = ((SpinnerNumberModel)getFrequencyCorrectionSpinner().getModel()).getNumber().doubleValue();
            getConfiguration().setFrequencyCorrection(value);
            getConfiguration().setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());

            if(getSampleRateCombo().getSelectedItem() instanceof BladeRfSampleRate rate)
            {
                getConfiguration().setSampleRate(rate.getRate());
            }

            if(getGainModeCombo().getSelectedItem() instanceof GainModeInfo mode)
            {
                getConfiguration().setGainMode(mode.mode());
            }

            getConfiguration().setGain(getGainSlider().getValue());
            getConfiguration().setBiasT(getBiasTCheckBox().isSelected());
            saveConfiguration();
        }
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
        getSampleRateCombo().setEnabled(!locked);
        getSampleRateCombo().setToolTipText(locked ? "Sample Rate is locked.  Disable decoding channels to unlock." :
                "Select a sample rate for the tuner");
    }

    private String getTunerInfo()
    {
        if(getDiscoveredTuner().getTunerStatus() == TunerStatus.ERROR)
        {
            return getDiscoveredTuner().getErrorMessage();
        }

        if(hasTuner())
        {
            BladeRfDeviceInformation info = getTuner().getController().getDeviceInfo();
            StringBuilder sb = new StringBuilder();
            sb.append("<html><h3>bladeRF Tuner</h3>");
            sb.append("<b>Serial: </b>").append(info.serial()).append("<br>");
            sb.append("<b>Board: </b>").append(info.board()).append("<br>");
            sb.append("<b>Firmware: </b>").append(info.firmware()).append("<br>");
            sb.append("<b>FPGA: </b>").append(info.fpga()).append("<br>");
            sb.append("<b>USB Speed: </b>").append(info.speed()).append("<br>");
            return sb.toString();
        }

        return null;
    }
}

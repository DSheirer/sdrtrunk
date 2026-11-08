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

package io.github.dsheirer.source.tuner.limesdr;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import java.io.Serial;
import java.util.List;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.SpinnerNumberModel;

/**
 * LimeSDR tuner editor
 */
public class LimeSdrTunerEditor extends TunerEditor<LimeSdrTuner, LimeSdrTunerConfiguration>
{
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger mLog = LoggerFactory.getLogger(LimeSdrTunerEditor.class);
    private JButton mTunerInfoButton;
    private JComboBox<LimeSdrSampleRate> mSampleRateCombo;
    private JComboBox<String> mAntennaCombo;
    private JSlider mGainSlider;
    private JLabel mGainValueLabel;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving tuner configuration
     * @param discoveredTuner for the optionally usable LimeSDR tuner and controller
     */
    public LimeSdrTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return hasTuner() ? getTuner().getController().getMinimumTunableFrequency() :
                LimeSdrTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return hasTuner() ? getTuner().getController().getMaximumTunableFrequency() :
                LimeSdrTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill][fill]", "[][][][][][][][][][][grow]"));

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

        add(new JLabel("Antenna:"));
        add(getAntennaCombo(), "wrap");

        add(new JSeparator(), "span,growx");

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
            LimeSdrTunerController controller = getTuner().getController();
            List<LimeSdrSampleRate> rates = controller.getSampleRates();
            getSampleRateCombo().setModel(new DefaultComboBoxModel<>(rates.toArray(new LimeSdrSampleRate[0])));
            getSampleRateCombo().setSelectedItem(controller.getLimeSdrSampleRate());
            getSampleRateCombo().setEnabled(!controller.isLockedSampleRate());

            List<String> antennas = controller.getAntennas();
            getAntennaCombo().setModel(new DefaultComboBoxModel<>(antennas.toArray(new String[0])));
            getAntennaCombo().setSelectedItem(controller.getAntenna());
            getAntennaCombo().setEnabled(true);

            getGainSlider().setValue(controller.getGain());
            getGainSlider().setEnabled(true);
            getGainValueLabel().setEnabled(true);
        }
        else
        {
            getSampleRateCombo().setModel(new DefaultComboBoxModel<>());
            getSampleRateCombo().setEnabled(false);
            getAntennaCombo().setModel(new DefaultComboBoxModel<>());
            getAntennaCombo().setEnabled(false);
            getGainSlider().setEnabled(false);
            getGainValueLabel().setEnabled(false);
        }

        updateGainLabel();
        setLoading(false);
    }

    /**
     * Indicates if the tuner has finished starting, since a status update can arrive while it's starting or restarting.
     */
    private boolean hasStartedTuner()
    {
        return hasTuner() && getTuner().getController().getAntenna() != null;
    }

    private void updateGainLabel()
    {
        getGainValueLabel().setText(getGainSlider().getValue() + " dB");
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
            mTunerInfoButton.addActionListener(_ -> JOptionPane.showMessageDialog(LimeSdrTunerEditor.this,
                    getTunerInfo(), "Tuner Info", JOptionPane.INFORMATION_MESSAGE));
        }

        return mTunerInfoButton;
    }

    private JComboBox<LimeSdrSampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new JComboBox<>();
            mSampleRateCombo.setEnabled(false);
            mSampleRateCombo.addActionListener(_ ->
            {
                if(hasTuner() && !isLoading())
                {
                    LimeSdrSampleRate rate = (LimeSdrSampleRate)mSampleRateCombo.getSelectedItem();

                    try
                    {
                        getTuner().getController().setSampleRate(rate);

                        //Adjust the min/max values for the sample rate.
                        adjustForSampleRate(rate.getRate());

                        save();
                    }
                    catch(Exception e1)
                    {
                        mLog.error("Error setting LimeSDR sample rate", e1);
                        JOptionPane.showMessageDialog(LimeSdrTunerEditor.this, "Couldn't set sample rate to " + rate);
                    }
                }
            });
        }

        return mSampleRateCombo;
    }

    private JComboBox<String> getAntennaCombo()
    {
        if(mAntennaCombo == null)
        {
            mAntennaCombo = new JComboBox<>();
            mAntennaCombo.setEnabled(false);
            mAntennaCombo.setToolTipText("Receive port: LNAW wideband (default), LNAL low band, LNAH high band");
            mAntennaCombo.addActionListener(_ ->
            {
                if(hasTuner() && !isLoading())
                {
                    String antenna = (String)mAntennaCombo.getSelectedItem();

                    try
                    {
                        getTuner().getController().setAntenna(antenna);
                        save();
                    }
                    catch(Exception e1)
                    {
                        mLog.error("Error setting LimeSDR antenna", e1);
                        JOptionPane.showMessageDialog(mAntennaCombo, "Couldn't set antenna to " + antenna);
                    }
                }
            });
        }

        return mAntennaCombo;
    }

    private JSlider getGainSlider()
    {
        if(mGainSlider == null)
        {
            mGainSlider = new JSlider(JSlider.HORIZONTAL, LimeSdrTunerController.GAIN_MINIMUM,
                    LimeSdrTunerController.GAIN_MAXIMUM, LimeSdrTunerController.GAIN_DEFAULT);
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
                        mLog.error("Couldn't set LimeSDR gain to:" + gain, e);
                        JOptionPane.showMessageDialog(mGainSlider, "Couldn't set gain value to " + gain);
                    }
                }

                updateGainLabel();
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

            if(getSampleRateCombo().getSelectedItem() instanceof LimeSdrSampleRate rate)
            {
                getConfiguration().setSampleRate(rate.getRate());
            }

            if(getAntennaCombo().getSelectedItem() instanceof String antenna)
            {
                getConfiguration().setAntenna(antenna);
            }

            getConfiguration().setGain(getGainSlider().getValue());
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
            LimeSdrDeviceInformation info = getTuner().getController().getDeviceInfo();
            StringBuilder sb = new StringBuilder();
            sb.append("<html><h3>LimeSDR Tuner</h3>");
            sb.append("<b>Board: </b>").append(info.board()).append("<br>");
            sb.append("<b>Serial: </b>").append(info.serial()).append("<br>");
            sb.append("<b>Hardware: </b>").append(info.hardware()).append("<br>");
            sb.append("<b>Firmware: </b>").append(info.firmware()).append("<br>");
            sb.append("<b>Gateware: </b>").append(info.gateware()).append("<br>");
            sb.append("<b>Connection: </b>").append(info.connection()).append("<br>");
            sb.append("<b>LimeSuiteNG: </b>").append(info.library()).append("<br>");
            return sb.toString();
        }

        return null;
    }
}

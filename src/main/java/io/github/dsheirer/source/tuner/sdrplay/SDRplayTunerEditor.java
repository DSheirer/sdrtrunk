/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.source.tuner.sdrplay;

import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceEvent.Event;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEditor;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEvent;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationModel;
import io.github.sammy1am.sdrplay.ApiException;
import io.github.sammy1am.sdrplay.jnr.TunerParamsT.Bw_MHzT;
import io.github.sammy1am.sdrplay.jnr.TunerParamsT.If_kHzT;
import io.github.sammy1am.sdrplay.jnr.TunerParamsT.LoModeT;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.util.Arrays;

import javax.swing.JCheckBox;
import javax.swing.JSlider;

public class SDRplayTunerEditor extends TunerConfigurationEditor
{
    private static final long serialVersionUID = 1L;

    private final static Logger mLog = LoggerFactory.getLogger(SDRplayTunerEditor.class);

    private JTextField mConfigurationName;
    private JButton mTunerInfo;
    private JComboBox<Integer> mComboSampleRate;
    private JComboBox<Integer> mComboDecFactor;
    private JSpinner mFrequencyCorrection;
    private JCheckBox mAutoPPMEnabled;
    
    private JLabel mLabelIfMode;
    private JLabel mLabelIFBandwidth;
    private JCheckBox mAGCEnabled;
    
    private JPanel mDeviceParams;
    private JCheckBox mRfNotch;
    private JCheckBox mDABNotch;
    private JCheckBox mBiasT;
    
    private JSlider mLNAState;
    DefaultComboBoxModel<Integer> mAllDecModel = new DefaultComboBoxModel<Integer>(new Integer[] {1, 2, 4, 8, 16, 32});
    DefaultComboBoxModel<Integer> mNoDecModel = new DefaultComboBoxModel<Integer>(new Integer[] {1});

    private boolean mLoading;

    private final SDRplayTunerController mController;

    public SDRplayTunerEditor(TunerConfigurationModel tunerConfigurationModel, SDRplayTuner tuner)
    {
        super(tunerConfigurationModel);

        mController = tuner.getController();

        init();
    }

    private SDRplayTunerConfiguration getConfiguration()
    {
        if(hasItem())
        {
            return (SDRplayTunerConfiguration)getItem();
        }

        return null;
    }

    private void init()
    {
        //setLayout(new MigLayout("fill,wrap 4", "[right][grow,fill][right][grow,fill]",
        setLayout(new MigLayout("fill,wrap 6", "[left][][][][][]",
            "[][][][][][][][][grow]"));

        add(new JLabel("SDRplay Tuner Configuration"), "span,align center");

        mConfigurationName = new JTextField();
        mConfigurationName.setEnabled(false);
        mConfigurationName.addFocusListener(new FocusListener()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                save();
            }

            @Override
            public void focusGained(FocusEvent e)
            {
            }
        });
        add(new JLabel("Name:"), "split 2");
        add(mConfigurationName, "growx, span 3");

        
        mTunerInfo = new JButton("Tuner Info");
        mTunerInfo.setEnabled(false);
        mTunerInfo.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                JOptionPane.showMessageDialog(SDRplayTunerEditor.this, getTunerInfo(),
                    "Tuner Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        add(mTunerInfo, "wrap");

        /**
         * Sample Rate
         */
        Integer[] validRates = {2_000_000, 3_000_000, 4_000_000, 5_000_000, 6_000_000, 7_000_000, 8_000_000, 9_000_000, 10_000_000};
        mComboSampleRate = new JComboBox<>(validRates);
        mComboSampleRate.setEnabled(false);
        mComboSampleRate.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int sampleRate = (Integer)mComboSampleRate.getSelectedItem();

                try
                {
                    mController.setSampleRate(sampleRate);
                    mLabelIfMode.setText(mController.getIfType().name());
                    mLabelIFBandwidth.setText(mController.getIFBandwidth().name());
                    save();
                }
                catch(SourceException e2)
                {
                    mLog.error("SDRplay Tuner Controller - couldn't apply sample rate setting [" +
                        sampleRate + "]", e);
                }
            }
        });
        add(new JLabel("Sample Rate:"), "split 2, left");
        add(mComboSampleRate, "left");

        /**
         * Show values for IF Mode and IF Bandwidth
         */
        add(new JLabel("IF Mode: "), "span 2, split 4");
        mLabelIfMode = new JLabel("");
        mLabelIfMode.setEnabled(false);
        add(mLabelIfMode, "left");
        
        add(new JLabel("IF Bandwidth: "));
        mLabelIFBandwidth = new JLabel("");
        mLabelIFBandwidth.setEnabled(false);
        add(mLabelIFBandwidth, "left, wrap");
        
        /**
         * PPM Frequency Correction - this PPM correction is implemented in sdrtrunk, not sdrplay device
         */
        SpinnerModel model = new SpinnerNumberModel(0.0,   //initial value
            -1000.0,   //min
            1000.0,   //max
            0.1); //step
        mFrequencyCorrection = new JSpinner(model);
        mFrequencyCorrection.setEnabled(false);
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor)mFrequencyCorrection.getEditor();

        DecimalFormat format = editor.getFormat();
        format.setMinimumFractionDigits(1);
        editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);

        mFrequencyCorrection.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                final double value = ((SpinnerNumberModel)mFrequencyCorrection
                    .getModel()).getNumber().doubleValue();

                try
                {
                    mController.setFrequencyCorrection(value);
                    save();
                }
                catch(SourceException e1)
                {
                    JOptionPane.showMessageDialog(SDRplayTunerEditor.this, "SDRplay Tuner Controller"
                        + " - couldn't apply frequency correction value: " + value +
                        e1.getLocalizedMessage());

                    mLog.error("SDRplay Tuner Controller - couldn't apply frequency correction "
                        + "value: " + value, e1);
                }
            }
        });
        add(new JLabel("PPM:"), "split 2");
        add(mFrequencyCorrection);

        /**
         * PPM Auto Correction - implemented in sdrtrunk code not sdrplay device
         */
        mAutoPPMEnabled = new JCheckBox("PPM Auto-Correction");
        mAutoPPMEnabled.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                boolean enabled = mAutoPPMEnabled.isSelected();
                mController.getFrequencyErrorCorrectionManager().setEnabled(enabled);
                save();
            }
            
        });
        add(mAutoPPMEnabled);
        
  
        
        /**
         * Automatic Gain Control
         * 
         */
        mAGCEnabled = new JCheckBox("AGC");
        mAGCEnabled.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                boolean enabled = mAGCEnabled.isSelected();
                mController.setAGCEnabled(enabled);
                save();
            }
            
        });
        add(mAGCEnabled);
        
        /******************************************************************************/
        /**
         * Decimation
         */
    
        Integer[] decFactors = {1, 2, 4, 8, 16, 32};
        mComboDecFactor = new JComboBox<>(decFactors);
        mComboDecFactor.setEnabled(false);
        mComboDecFactor.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent event)
            {
            	if(event.getStateChange() == ItemEvent.SELECTED)
            	{
	                //int decFactor = (Integer)mComboDecFactor.getSelectedItem();
            		int decFactor = (Integer)event.getItem();
	                try
	                {
	                    mController.setDecFactor(decFactor);
	                    save();
	                }
	                catch(ApiException ae)
	                {
	                    JOptionPane.showMessageDialog(SDRplayTunerEditor.this, "SDRplay Tuner Controller"
	                        + " - couldn't apply Decimation factor: " + decFactor +
	                        ae.getLocalizedMessage());
	
	                    mLog.error("SDRplay Tuner Controller - couldn't apply Decimation factor: " + decFactor, ae);
	                }
            	}
                
            }
        });
        add(new JLabel("Decimation:"));
        add(mComboDecFactor);
        
        // When sample rate changes, make sure 
        // only valid decimation factors are available.
        mComboSampleRate.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent event)
            {
            	if(event.getStateChange() == ItemEvent.SELECTED)
            	{
	            	mLog.debug("special mComboSampleRate listener");
	             	for(int i = 0; i < mComboDecFactor.getItemCount(); i++)
	            		System.out.printf("mComboDecFactor Item %d %s %b \n", i, mComboDecFactor.getItemAt(i).toString(), i == mComboDecFactor.getSelectedIndex());
	        		mComboDecFactor.setEnabled(false);
	        		int sel = mComboDecFactor.getSelectedIndex();
	        		mComboDecFactor.removeAllItems();
	        	    if((Integer)mComboSampleRate.getSelectedItem() <= 5_000_000) {
	        	    	for(Integer factor : decFactors)
	        	    		mComboDecFactor.addItem(factor);
	        	    	mComboDecFactor.setSelectedIndex(sel);
	                }
	                else {
	                	mComboDecFactor.addItem(decFactors[0]);
	                	mComboDecFactor.setSelectedIndex(0);
	                }
	        	    mComboDecFactor.setEnabled(true);
            	}
            }});
        
        /******************************************************************************/
        /**
         * RF Gain Reduction - aka. Low Noise Amplifier(LNA) Gain Reduction
         */
        add(new JLabel(""), "span 2");
        add(new JLabel("<html>RF Gain Reduction<br/>(LNA State)</html>"), "top");
        
        mLNAState = new JSlider();
        mLNAState.setEnabled(false);
        mLNAState.setMajorTickSpacing(1);
        mLNAState.setPaintTicks(true);
        mLNAState.setPaintLabels(true);
        mLNAState.setPaintTrack(false);
        mLNAState.setSnapToTicks(true);
        mLNAState.setMinimum(0);
        mLNAState.setMaximum(mController.getNumLNAStates()-1);
        mLNAState.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                final int value = mLNAState.getValue();

                try
                {
                    mController.setLNAState(value);
                    save();
                }
                catch(ApiException ae)
                {
                    JOptionPane.showMessageDialog(SDRplayTunerEditor.this, "SDRplay Tuner Controller"
                        + " - couldn't apply LNA State value: " + value +
                        ae.getLocalizedMessage());

                    mLog.error("SDRplay Tuner Controller - couldn't apply LNA State value: " + value, ae);
                }
            }
        });
        
        /**
         * Adjust maximum available LNA states when bandwidth frequency is changed
         * This value is very dependent upon the specific SDRplay device
         * 
         * TODO: This addListener call will likely slowly leak memory because it doesn't
         * have a corresponding removeListener call anywhere.  However, there doesn't seem to be a
         * editor.close() method available, so without resorting to finalizer(), there's nowhere
         * to appropriately remove us as a listener.
        */
        mController.addListener(new ISourceEventProcessor() {
            @Override
            public void process(SourceEvent event) throws SourceException {
                if (Event.NOTIFICATION_FREQUENCY_CHANGE.equals(event.getEvent())){
                    // Different LNA states are available for different frequencies, so 
                    // update the maximum state here
                    int newMax = mController.getNumLNAStates()-1;
                    if (mLNAState.getValue() > newMax) {
                        mLNAState.setValue(newMax);
                    }
                    mLNAState.setMaximum(newMax);
                    
   
                }
            }
        });
        add(mLNAState, "wrap");        
        
        add(new JSeparator(JSeparator.HORIZONTAL), "span,grow");
        
  
        /**
         * 
         * START DEVICE SPECIFIC PARAMETERS
         * 
         * Handle SDRPlay specific device parameters (RSP1, RSP1A, RSPdx, etc.)
         * For now focus on RSP1 and RSP1A
         * Each panel contained in mDeviceParams provides for editing device specific attributes
         * RSP1 has no device specific attributes, so is empty.
         */
        mDeviceParams = new JPanel(new CardLayout());
        JPanel rsp1Panel = new JPanel();
        JPanel rsp1aPanel = new JPanel();
        JPanel rspdxPanel = new JPanel();
        JPanel rsp2Panel = new JPanel();
        JPanel rspduoPanel = new JPanel();
        
        // FM Broadcast Notch
        mRfNotch = new JCheckBox("FM-BC Notch");
        mRfNotch.setEnabled(false);
        mRfNotch.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mController.setRfNotch(mRfNotch.isSelected());
                save();
            }
            
        });
        rsp1aPanel.add(mRfNotch);
        
        // Digital Audio Broadcast (DAB) Notch
        mDABNotch = new JCheckBox("DAB Notch");
        mDABNotch.setEnabled(false);
        mDABNotch.addActionListener(new ActionListener()
        {
        	@Override
        	public void actionPerformed(ActionEvent e)
        	{
        		mController.setDABNotch(mRfNotch.isSelected());
        		save();
        	}
        });
        rsp1aPanel.add(mDABNotch);
        
        // Bias T Enable
        mBiasT = new JCheckBox("BiasT");
        mBiasT.setEnabled(false);
        mBiasT.addActionListener(new ActionListener()
        {
        	@Override
        	public void actionPerformed(ActionEvent e)
        	{
        		mController.setDABNotch(mBiasT.isSelected());
        		save();
        	}
        });
        rsp1aPanel.add(mBiasT);
        
        mDeviceParams.add(rsp1aPanel, "RSP1A");
        mDeviceParams.add(rsp1Panel, "RSP1");
        mDeviceParams.add(rsp2Panel, "RSP2");
        mDeviceParams.add(rspduoPanel, "RSPduo");
        mDeviceParams.add(rspdxPanel, "RSPdx");
        CardLayout c1 = (CardLayout) mDeviceParams.getLayout();
        String dev = mController.getModel();
        if((Arrays.asList("RSP1A", "RSP1", "RSP2", "RSPduo", "RSPdx").contains(dev)))
		{
        	c1.show(mDeviceParams, dev);
        	add(mDeviceParams, "span");
		}
        
        
       
    }

    /**
     * Updates the sample rate tooltip according to the tuner controller's lock state.
     */
    private void updateSampleRateToolTip()
    {
        if(mController.isLocked())
        {
            mComboSampleRate.setToolTipText("Sample Rate is locked.  Stop channel decoding to unlock.");
        }
        else
        {
            mComboSampleRate.setToolTipText("Select a sample rate for the tuner");
        }
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        mComboSampleRate.setEnabled(!locked);
    }

    /**
     * Sets each of the tuner configuration controls to the enabled argument state
     */
    private void setControlsEnabled(boolean enabled)
    {
        if(mConfigurationName.isEnabled() != enabled)
        {
            mConfigurationName.setEnabled(enabled);
        }

        if(mTunerInfo.isEnabled() != enabled)
        {
            mTunerInfo.setEnabled(enabled);
        }

        if(mFrequencyCorrection.isEnabled() != enabled)
        {
            mFrequencyCorrection.setEnabled(enabled);
        }

        updateSampleRateToolTip();

        if(mController.isLocked())
        {
            mComboSampleRate.setEnabled(false);
        }
        else if(mComboSampleRate.isEnabled() != enabled)
        {
            mComboSampleRate.setEnabled(enabled);
        }
        
        if(mLNAState.isEnabled() != enabled)
        {
            mLNAState.setEnabled(enabled);
        }
        
        if(mDeviceParams.isEnabled() != enabled)
        {
        	mDeviceParams.setEnabled(enabled);
        }
        if(mRfNotch.isEnabled() != enabled)
        {
        	mRfNotch.setEnabled(enabled);
        }
        if(mDABNotch.isEnabled() != enabled)
        {
        	mDABNotch.setEnabled(enabled);
        }
        if(mBiasT.isEnabled() != enabled)
        {
        	mBiasT.setEnabled(enabled);
        }
        if(mLabelIfMode.isEnabled() != enabled)
        {
        	mLabelIfMode.setEnabled(enabled);
        }
        if(mLabelIFBandwidth.isEnabled() != enabled)
        {
        	mLabelIFBandwidth.setEnabled(enabled);
        }
        if(mComboDecFactor.isEnabled() != enabled)
        {
        	mComboDecFactor.setEnabled(enabled);
        }
        if(mAGCEnabled.isEnabled() != enabled)
        {
        	mAGCEnabled.setEnabled(enabled);
        }

    }

    private String getTunerInfo()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><h3>SDRplay Tuner</h3>");

        sb.append("<b>Model: </b>");
        sb.append(mController.getModel());
        sb.append("<br>");

        sb.append("<b>Serial: </b>");
        sb.append(mController.getSerial());
        sb.append("<br>");

        return sb.toString();
    }

    /**
     * Initializes each value as the Panel is loaded
     */
    @Override
    public void setItem(TunerConfiguration tunerConfiguration)
    {
        super.setItem(tunerConfiguration);

        //Toggle loading so that we don't fire a change event and schedule a settings file save
        mLoading = true;
        mLog.debug("setItem entered...");
        if(hasItem())
        {
        	mLog.debug("setItem hasItem() entered...");
            SDRplayTunerConfiguration config = getConfiguration();

            if(tunerConfiguration.isAssigned())
            {
            	mLog.debug("setItem tunerConfiguration.isAssigned entered...");
                setControlsEnabled(true);

                mConfigurationName.setText(config.getName());
                mComboDecFactor.setSelectedItem(config.getDecFactor()); //must be before setting mComboSampleRate!
                mComboSampleRate.setSelectedItem(config.getSampleRate()); //must be after setting mComboDecFactor!
                mFrequencyCorrection.setValue(config.getFrequencyCorrection());
                mAutoPPMEnabled.setSelected(config.getAutoPPMCorrectionEnabled());
                mAGCEnabled.setSelected(config.getAGCEnabled());
                mLNAState.setValue(config.getLNAState());

                mRfNotch.setSelected(config.getRfNotch());
                mDABNotch.setSelected(config.getDABNotch());
                mBiasT.setSelected(config.getBiasT());
                
                //Update enabled state to reflect when frequency and sample rate controls are locked
                mComboSampleRate.setEnabled(!mController.isLocked());
                
            }
            else
            {
                setControlsEnabled(false);
                mConfigurationName.setText(config.getName());
            }
        }
        else
        {
            setControlsEnabled(false);
            mConfigurationName.setText("");
        }

        mLoading = false;
    }

    /**
     * Saves each value for later use
     */
    @Override
    public void save()
    {
        if(hasItem() && !mLoading)
        {
            SDRplayTunerConfiguration config = getConfiguration();

            config.setName(mConfigurationName.getText());

            config.setSampleRate((Integer)mComboSampleRate.getSelectedItem());
            double value = ((SpinnerNumberModel)mFrequencyCorrection
                .getModel()).getNumber().doubleValue();
            config.setFrequencyCorrection(value);
            config.setAutoPPMCorrectionEnabled(mAutoPPMEnabled.isSelected());
            config.setDecFactor((Integer)mComboDecFactor.getSelectedItem());
            config.setLNAState(mLNAState.getValue());
            config.setAGCEnabled(mAGCEnabled.isSelected());
            config.setRfNotch(mRfNotch.isSelected());
            config.setDABNotch(mDABNotch.isSelected());
            config.setBiasT(mBiasT.isSelected());

            getTunerConfigurationModel().broadcast(
                new TunerConfigurationEvent(getConfiguration(), TunerConfigurationEvent.Event.CHANGE));
        }
    }
}
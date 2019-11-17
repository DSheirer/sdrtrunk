/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.gui.editor.Editor;
import io.github.dsheirer.gui.editor.EditorValidationException;
import io.github.dsheirer.gui.editor.ValidatingEditor;
import io.github.dsheirer.module.decode.AuxDecodeConfigurationEditor;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.source.SourceConfigurationEditor;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Editor panel for P25 Phase II decoder configuration
 */
public class P25P2DecoderEditor extends ValidatingEditor<Channel>
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2DecoderEditor.class);

    private static final long serialVersionUID = 1L;

    private JCheckBox mAutoDetectCheckBox;
    private JFormattedTextField mWacnText;
    private JFormattedTextField mSystemText;
    private JFormattedTextField mNacText;

    public P25P2DecoderEditor()
    {
        init();
    }

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0,wrap 2", "[right][grow,fill]", ""));

        mAutoDetectCheckBox = new JCheckBox("Auto-Detect System Settings");
        mAutoDetectCheckBox.setToolTipText("Auto-detect WACN, SYSTEM and NAC for systems that broadcast this information.");
        mAutoDetectCheckBox.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mWacnText.setEnabled(!mAutoDetectCheckBox.isSelected());
                mSystemText.setEnabled(!mAutoDetectCheckBox.isSelected());
                mNacText.setEnabled(!mAutoDetectCheckBox.isSelected());
                setModified(true);
            }
        });
        add(new JLabel(" "));
        add(mAutoDetectCheckBox);

        add(new JLabel("WACN:"));
        mWacnText = new JFormattedTextField();
        mWacnText.setToolTipText("Wide Area Communications Network (WACN) value in range: 0 - 1,048,575.  Set to 0 for auto-detect");
        mWacnText.setValue(Integer.valueOf(0));
        mWacnText.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mWacnText);

        add(new JLabel("SYSTEM:"));
        mSystemText = new JFormattedTextField();
        mSystemText.setToolTipText("System code in range: 0 - 4,095.  Set to 0 for auto-detect");
        mSystemText.setValue(Integer.valueOf(0));
        mSystemText.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mSystemText);

        add(new JLabel("NAC:"));
        mNacText = new JFormattedTextField();
        mSystemText.setToolTipText("Network Access Code (NAC): 0 - 4,095.  Set to 0 for auto-detect");
        mNacText.setValue(Integer.valueOf(0));
        mNacText.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(mNacText);
    }

    @Override
    public void save()
    {
        if(hasItem() && isModified())
        {
            DecodeConfigP25Phase2 p25 = new DecodeConfigP25Phase2();

            p25.setAutoDetectScrambleParameters(mAutoDetectCheckBox.isSelected());

            int wacn = ((Number)mWacnText.getValue()).intValue();

            if(wacn < 0 || wacn > 0xFFFFF)
            {
                JOptionPane.showConfirmDialog(P25P2DecoderEditor.this, "WACN [" + wacn +
                    "] must be in range: 0 - 1,048,575.  Please change and click 'Save' or a default value of 0 will " +
                    "be used", "WACN Value Error", JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);

                wacn = 0;
            }

            int system = ((Number)mSystemText.getValue()).intValue();

            if(system < 0 || system > 0xFFF)
            {
                JOptionPane.showConfirmDialog(P25P2DecoderEditor.this, "SYSTEM [" + system +
                    "] must be in range: 0 - 4,095.  Please change and click 'Save' or a default value of 0 will " +
                    "be used", "SYSTEM Value Error", JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);

                system = 0;
            }

            int nac = ((Number)mNacText.getValue()).intValue();

            if(nac < 0 || nac > 0xFFF)
            {
                JOptionPane.showConfirmDialog(P25P2DecoderEditor.this, "NAC [" + nac +
                    "] must be in range: 0 - 4,095.  Please change and click 'Save' or a default value of 0 will " +
                    "be used", "NAC Value Error", JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);

                nac = 0;
            }

            if(wacn != 0 && system != 0 && nac != 0)
            {
                p25.setScrambleParameters(new ScrambleParameters(wacn, system, nac));
            }

            getItem().setDecodeConfiguration(p25);
        }

        setModified(false);
    }

    @Override
    public void validate(Editor<Channel> editor) throws EditorValidationException
    {
        /**
         * Enforce CQPSK modulation uses a Tuner Source for I/Q sample data
         */
        if(editor instanceof SourceConfigurationEditor)
        {
            SourceConfiguration config = ((SourceConfigurationEditor)editor).getSourceConfiguration();

            if(!(config instanceof SourceConfigTuner || config instanceof SourceConfigTunerMultipleFrequency))
            {
                throw new EditorValidationException(editor,
                    "<html><body width='175'><h3>LSM Simulcast</h3>"
                        + "<p>P25 Phase II decoder can only be used with a tuner source.</p>");
            }
        }
        else if(editor instanceof AuxDecodeConfigurationEditor)
        {
            AuxDecodeConfiguration config = ((AuxDecodeConfigurationEditor)editor).getConfiguration();

            if(config != null && !config.getAuxDecoders().isEmpty())
            {
                throw new EditorValidationException(editor,
                    "<html><body width='175'><h3>Auxilary Decoders</h3>"
                        + "<p>The auxiliary decoders work with analog audio and are not "
                        + "compatible with the P25 Decoder.  Please uncheck any auxiliary "
                        + "decoders that you have selected for this channel</p>");
            }
        }
    }

    private void setControlsEnabled(boolean enabled)
    {
        mWacnText.setEnabled(enabled);
        mSystemText.setEnabled(enabled);
        mNacText.setEnabled(enabled);
        mAutoDetectCheckBox.setEnabled(enabled);
    }

    @Override
    public void setItem(Channel item)
    {
        super.setItem(item);

        if(hasItem())
        {
            setControlsEnabled(true);

            DecodeConfiguration config = getItem().getDecodeConfiguration();

            if(config instanceof DecodeConfigP25Phase2)
            {
                DecodeConfigP25Phase2 p25 = (DecodeConfigP25Phase2)config;

                ScrambleParameters scrambleParameters = p25.getScrambleParameters();

                if(scrambleParameters != null)
                {
                    mWacnText.setValue(scrambleParameters.getWACN());
                    mSystemText.setValue(scrambleParameters.getSystem());
                    mNacText.setValue(scrambleParameters.getNAC());
                }

                boolean autoDetect = p25.isAutoDetectScrambleParameters();
                mAutoDetectCheckBox.setSelected(autoDetect);
                mWacnText.setEnabled(!autoDetect);
                mSystemText.setEnabled(!autoDetect);
                mNacText.setEnabled(!autoDetect);

                setModified(false);
            }
            else
            {
                mAutoDetectCheckBox.setSelected(false);
                mWacnText.setEnabled(true);
                mWacnText.setValue(0);
                mSystemText.setEnabled(true);
                mSystemText.setValue(0);
                mNacText.setEnabled(true);
                mNacText.setValue(0);
                setModified(true);
            }
        }
        else
        {
            mWacnText.setValue(0);
            mSystemText.setValue(0);
            mNacText.setValue(0);
            mAutoDetectCheckBox.setSelected(false);
            setModified(false);
            setControlsEnabled(false);
        }
    }
}

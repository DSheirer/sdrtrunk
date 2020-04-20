/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.source.tuner;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.gui.control.MultipleFrequencyEditor;
import io.github.dsheirer.gui.editor.Editor;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;
import net.miginfocom.swing.MigLayout;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TunerSourceMultipleFrequencyEditor extends Editor<Channel>
{
    private static final long serialVersionUID = 1L;
    private static final String NO_PREFERRED_TUNER = "(none)";
    private static final String UNAVAILABLE_TUNER = " (unavailable)";
    private MultipleFrequencyEditor mFrequencyEditor = new MultipleFrequencyEditor(0.0, 6000.0);
    private JComboBox<String> mTunerNameComboBox;
    private TunerModel mTunerModel;
    private List<String> mCurrentTunerNames;

    public TunerSourceMultipleFrequencyEditor(TunerModel tunerModel)
    {
        mTunerModel = tunerModel;
        init();
    }

    private void init()
    {
        loadTunerNames();

        setLayout(new MigLayout("insets 0 0 0 0",
            "[grow,fill][][right][grow,fill,left]", "[][grow,fill]"));

        mFrequencyEditor.getFrequencyModel().addTableModelListener(new TableModelListener()
        {
            @Override
            public void tableChanged(TableModelEvent e)
            {
                setModified(true);
            }
        });
//        mFrequencyEditor.setSize(new Dimension(100, 30));
        add(mFrequencyEditor, "span 2 2");

        mTunerNameComboBox = new JComboBox<>(mCurrentTunerNames.toArray(new String[0]));
        mTunerNameComboBox.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });
        add(new JLabel("Preferred Tuner:"));
        add(mTunerNameComboBox);
    }

    private void loadTunerNames()
    {
        mCurrentTunerNames = new ArrayList<>();

        for(Tuner tuner: mTunerModel.getTuners())
        {
            mCurrentTunerNames.add(tuner.getName());
        }

        Collections.sort(mCurrentTunerNames);

        mCurrentTunerNames.add(0, NO_PREFERRED_TUNER);
    }

    private void updateTunerNameCombo(String preferredTuner)
    {
        if(preferredTuner != null && !mCurrentTunerNames.contains(preferredTuner))
        {
            List<String> updatedTunerNameList = new ArrayList<>(mCurrentTunerNames);
            String unavailableTuner = preferredTuner + UNAVAILABLE_TUNER;
            updatedTunerNameList.add(unavailableTuner);
            String[] tunerNameArray = updatedTunerNameList.toArray(new String[0]);
            mTunerNameComboBox.setModel(new DefaultComboBoxModel<>(tunerNameArray));
            mTunerNameComboBox.setSelectedItem(unavailableTuner);
        }
        else
        {
            String[] tunerNameArray = mCurrentTunerNames.toArray(new String[0]);
            mTunerNameComboBox.setModel(new DefaultComboBoxModel<>(tunerNameArray));

            if(preferredTuner == null)
            {
                mTunerNameComboBox.setSelectedItem(NO_PREFERRED_TUNER);
            }
            else
            {
                mTunerNameComboBox.setSelectedItem(preferredTuner);
            }
        }
    }

    public void save()
    {
        if(hasItem() && isModified())
        {
            SourceConfigTunerMultipleFrequency config = new SourceConfigTunerMultipleFrequency();

            String preferredTuner = (String)mTunerNameComboBox.getSelectedItem();

            if(preferredTuner != null)
            {
                if(preferredTuner.equalsIgnoreCase(NO_PREFERRED_TUNER))
                {
                    config.setPreferredTuner(null);
                }
                else if(preferredTuner.contains(UNAVAILABLE_TUNER))
                {
                    //don't do anything - the config already contains this unavailable tuner name
                }
                else
                {
                    config.setPreferredTuner(preferredTuner);
                }
            }

            config.setFrequencies(mFrequencyEditor.getFrequencyModel().getFrequencies());

            getItem().setSourceConfiguration(config);
        }

        setModified(false);
    }

    @Override
    public void setItem(Channel item)
    {
        super.setItem(item);

        mFrequencyEditor.getFrequencyModel().clear();

        if(hasItem())
        {
            SourceConfiguration config = getItem().getSourceConfiguration();

            if(config instanceof SourceConfigTunerMultipleFrequency)
            {
                SourceConfigTunerMultipleFrequency tunerConfig = (SourceConfigTunerMultipleFrequency) config;
                updateTunerNameCombo(tunerConfig.getPreferredTuner());
                mFrequencyEditor.getFrequencyModel().setFrequencies(tunerConfig.getFrequencies());
                setModified(false);
            }
        }
        else
        {
            setModified(false);
        }
    }
}

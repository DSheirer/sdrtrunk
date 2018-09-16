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
package io.github.dsheirer.module.decode.nbfm;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.gui.editor.Editor;
import io.github.dsheirer.gui.editor.EditorValidationException;
import io.github.dsheirer.gui.editor.ValidatingEditor;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import net.miginfocom.swing.MigLayout;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NBFMDecoderEditor extends ValidatingEditor<Channel>
{
    private static final long serialVersionUID = 1L;

    private JComboBox<DecodeConfigNBFM.Bandwidth> mComboBandwidth;

    public NBFMDecoderEditor()
    {
        init();
    }

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0,wrap 2", "[right][grow,fill]", ""));

        mComboBandwidth = new JComboBox<>();
        mComboBandwidth.setModel(new DefaultComboBoxModel<DecodeConfigNBFM.Bandwidth>(
            DecodeConfigNBFM.Bandwidth.values()));
        mComboBandwidth.setEnabled(false);
        mComboBandwidth.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setModified(true);
            }
        });

        add(new JLabel("Bandwidth:"));
        add(mComboBandwidth);
    }

    @Override
    public void validate(Editor<Channel> editor) throws EditorValidationException
    {
        //No validation
    }

    @Override
    public void save()
    {
        if(hasItem() && isModified())
        {
            DecodeConfigNBFM nbfm = new DecodeConfigNBFM();
            nbfm.setBandwidth((DecodeConfigNBFM.Bandwidth)mComboBandwidth.getSelectedItem());
            getItem().setDecodeConfiguration(nbfm);
        }

        setModified(false);
    }

    private void setControlsEnabled(boolean enabled)
    {
        mComboBandwidth.setEnabled(enabled);
    }

    @Override
    public void setItem(Channel item)
    {
        super.setItem(item);

        if(hasItem())
        {
            setControlsEnabled(true);

            DecodeConfiguration config = getItem().getDecodeConfiguration();

            if(config instanceof DecodeConfigNBFM)
            {
                DecodeConfigNBFM nbfm = (DecodeConfigNBFM)config;
                mComboBandwidth.setSelectedItem(nbfm.getBandwidth());
                setModified(false);
            }
            else
            {
                setModified(true);
            }
        }
        else
        {
            setControlsEnabled(false);
            setModified(false);
        }
    }
}

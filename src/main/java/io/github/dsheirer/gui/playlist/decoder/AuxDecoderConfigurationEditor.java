/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.decoder;

import io.github.dsheirer.gui.playlist.Editor;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Editor for auxiliary decoder configuration objects using a VBox to visually display a vertical list of boolean toggle
 * switches to turn on/off aux decoders for a custom set of decoder types.
 */
public class AuxDecoderConfigurationEditor extends Editor<AuxDecodeConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(AuxDecoderConfigurationEditor.class);

    private List<AuxDecoderControl> mControls = new ArrayList<>();

    public AuxDecoderConfigurationEditor(Collection<DecoderType> types)
    {
        for(DecoderType type: types)
        {
            AuxDecoderControl control = new AuxDecoderControl(type);
            mControls.add(control);
            getChildren().add(control);
        }
    }

    @Override
    public void setItem(AuxDecodeConfiguration item)
    {
        if(item == null)
        {
            item = new AuxDecodeConfiguration();
        }

        super.setItem(item);

        for(AuxDecoderControl control: mControls)
        {
            control.getToggleSwitch().setDisable(false);
            control.getToggleSwitch().setSelected(item.getAuxDecoders().contains(control.getDecoderType()));
        }

        modifiedProperty().set(false);
    }

    @Override
    public void save()
    {
        AuxDecodeConfiguration config = getItem();

        if(config == null)
        {
            config = new AuxDecodeConfiguration();
        }

        config.clearAuxDecoders();

        for(AuxDecoderControl control: mControls)
        {
            if(control.getToggleSwitch().isSelected())
            {
                config.addAuxDecoder(control.getDecoderType());
            }
        }

        setItem(config);
    }

    @Override
    public void dispose()
    {

    }

    public class AuxDecoderControl extends GridPane
    {
        private DecoderType mDecoderType;
        private ToggleSwitch mToggleSwitch;

        public AuxDecoderControl(DecoderType type)
        {
            mDecoderType = type;

            setPadding(new Insets(5,5,5,5));
            setHgap(10);

            GridPane.setConstraints(getToggleSwitch(), 0, 0);
            getChildren().add(getToggleSwitch());

            Label label = new Label(mDecoderType.getDisplayString());
            GridPane.setHalignment(label, HPos.LEFT);
            GridPane.setConstraints(label, 1, 0);
            getChildren().add(label);
        }

        private ToggleSwitch getToggleSwitch()
        {
            if(mToggleSwitch == null)
            {
                mToggleSwitch = new ToggleSwitch();
                mToggleSwitch.setDisable(true);
                mToggleSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
            }

            return mToggleSwitch;
        }

        public DecoderType getDecoderType()
        {
            return mDecoderType;
        }
    }
}

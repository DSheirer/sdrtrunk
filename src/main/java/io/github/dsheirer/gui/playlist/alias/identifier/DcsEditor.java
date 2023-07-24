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

package io.github.dsheirer.gui.playlist.alias.identifier;

import io.github.dsheirer.alias.id.dcs.Dcs;
import io.github.dsheirer.module.decode.dcs.DCSCode;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Editor for Digital Coded Squelch (DCS) alias identifiers
 */
public class DcsEditor extends IdentifierEditor<Dcs>
{
    private static final Logger mLog = LoggerFactory.getLogger(DcsEditor.class);
    private ComboBox<DCSCode> mDCSCodeComboBox;

    /**
     * Constructs an instance
     */
    public DcsEditor()
    {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);

        Label typeLabel = new Label("DCS Code");
        GridPane.setConstraints(typeLabel, 0, 0);
        gridPane.getChildren().add(typeLabel);

        GridPane.setConstraints(getDCSCodeComboBox(), 1, 0);
        gridPane.getChildren().add(getDCSCodeComboBox());

        getChildren().add(gridPane);
    }

    @Override
    public void setItem(Dcs item)
    {
        super.setItem(item);
        if(item.isValid())
        {
            getDCSCodeComboBox().getSelectionModel().select(item.getDCSCode());
        }
        modifiedProperty().set(false);
    }

    @Override
    public void save()
    {
        //no-op
    }

    @Override
    public void dispose()
    {
        //no-op
    }

    /**
     * Combo-box loaded with DCS codes
     * @return
     */
    private ComboBox<DCSCode> getDCSCodeComboBox()
    {
        if(mDCSCodeComboBox == null)
        {
            mDCSCodeComboBox = new ComboBox<>();
            mDCSCodeComboBox.getItems().addAll(DCSCode.STANDARD_CODES);
            mDCSCodeComboBox.getItems().addAll(DCSCode.INVERTED_CODES);
            mDCSCodeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                getItem().setDCSCode(getDCSCodeComboBox().getSelectionModel().getSelectedItem());
                modifiedProperty().set(true);
            });
        }

        return mDCSCodeComboBox;
    }
}

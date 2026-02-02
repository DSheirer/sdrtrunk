/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

import io.github.dsheirer.alias.id.ctcss.Ctcss;
import io.github.dsheirer.module.decode.ctcss.CTCSSCode;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Editor for Continuous Tone-Coded Squelch System (CTCSS) alias identifiers
 */
public class CtcssEditor extends IdentifierEditor<Ctcss>
{
    private static final Logger mLog = LoggerFactory.getLogger(CtcssEditor.class);
    private ComboBox<CTCSSCode> mCTCSSCodeComboBox;

    /**
     * Constructs an instance
     */
    public CtcssEditor()
    {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);

        Label typeLabel = new Label("CTCSS Tone");
        GridPane.setConstraints(typeLabel, 0, 0);
        gridPane.getChildren().add(typeLabel);

        GridPane.setConstraints(getCTCSSCodeComboBox(), 1, 0);
        gridPane.getChildren().add(getCTCSSCodeComboBox());

        getChildren().add(gridPane);
    }

    @Override
    public void setItem(Ctcss item)
    {
        super.setItem(item);

        if(item.isValid())
        {
            getCTCSSCodeComboBox().getSelectionModel().select(item.getCTCSSCode());
        }
        else
        {
            // Clear selection for new/invalid items - shows placeholder
            getCTCSSCodeComboBox().getSelectionModel().clearSelection();
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
     * Combo-box loaded with CTCSS codes
     * @return combo box
     */
    private ComboBox<CTCSSCode> getCTCSSCodeComboBox()
    {
        if(mCTCSSCodeComboBox == null)
        {
            mCTCSSCodeComboBox = new ComboBox<>();
            mCTCSSCodeComboBox.getItems().addAll(CTCSSCode.STANDARD_CODES);
            
            // Set prompt text shown when nothing is selected
            mCTCSSCodeComboBox.setPromptText("Select Tone...");
            
            // Custom cell factory to handle display
            mCTCSSCodeComboBox.setButtonCell(new ListCell<CTCSSCode>()
            {
                @Override
                protected void updateItem(CTCSSCode item, boolean empty)
                {
                    super.updateItem(item, empty);
                    if(empty || item == null)
                    {
                        setText("Select Tone...");
                    }
                    else
                    {
                        setText(item.toString());
                    }
                }
            });

            mCTCSSCodeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue != null)
                {
                    getItem().setCTCSSCode(newValue);
                    modifiedProperty().set(true);
                }
            });
        }

        return mCTCSSCodeComboBox;
    }
}
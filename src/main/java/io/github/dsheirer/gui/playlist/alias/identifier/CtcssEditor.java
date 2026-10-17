/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ****************************************************************************
 */
package io.github.dsheirer.gui.playlist.alias.identifier;

import io.github.dsheirer.alias.id.ctcss.Ctcss;
import io.github.dsheirer.module.decode.squelch.ctcss.CTCSSCode;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

/** Editor for Continuous Tone Coded Squelch (CTCSS) alias identifiers. */
public class CtcssEditor extends IdentifierEditor<Ctcss>
{
    private ComboBox<CTCSSCode> mCTCSSCodeComboBox;

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
            getCTCSSCodeComboBox().getSelectionModel().clearSelection();
        }
        modifiedProperty().set(false);
    }

    @Override
    public void save() {}

    @Override
    public void dispose() {}

    private ComboBox<CTCSSCode> getCTCSSCodeComboBox()
    {
        if(mCTCSSCodeComboBox == null)
        {
            mCTCSSCodeComboBox = new ComboBox<>();
            mCTCSSCodeComboBox.getItems().addAll(CTCSSCode.STANDARD_CODES);
            mCTCSSCodeComboBox.setPromptText("Select Tone...");
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

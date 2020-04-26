/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

import io.github.dsheirer.alias.id.lojack.LoJackFunctionAndID;
import io.github.dsheirer.module.decode.lj1200.LJ1200Message;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Editor for LoJack Function and ID alias identifiers
 */
public class LojackEditor extends IdentifierEditor<LoJackFunctionAndID>
{
    private static final Logger mLog = LoggerFactory.getLogger(LojackEditor.class);
    private ComboBox<LJ1200Message.Function> mFunctionComboBox;
    private TextField mIdentifierField;

    /**
     * Constructs an instance
     */
    public LojackEditor()
    {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);

        Label typeLabel = new Label("LoJack Function");
        GridPane.setConstraints(typeLabel, 0, 0);
        gridPane.getChildren().add(typeLabel);

        GridPane.setConstraints(getFunctionComboBox(), 1, 0);
        gridPane.getChildren().add(getFunctionComboBox());

        GridPane.setConstraints(getIdentifierField(), 2, 0);
        gridPane.getChildren().add(getIdentifierField());

        Label helpLabel = new Label("Format: 5 numbers, characters or (*)wildcard - e.g. AB*12");
        GridPane.setConstraints(helpLabel, 3, 0);
        gridPane.getChildren().add(helpLabel);

        getChildren().add(gridPane);
    }

    @Override
    public void setItem(LoJackFunctionAndID item)
    {
        super.setItem(item);

        getFunctionComboBox().setDisable(item == null);
        getIdentifierField().setDisable(item == null);

        if(item != null)
        {
            getFunctionComboBox().getSelectionModel().select(item.getFunction());
            getIdentifierField().setText(item.getID());
        }
        else
        {
            getFunctionComboBox().getSelectionModel().select(null);
            getIdentifierField().setText(null);
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

    private ComboBox<LJ1200Message.Function> getFunctionComboBox()
    {
        if(mFunctionComboBox == null)
        {
            mFunctionComboBox = new ComboBox<>();
            mFunctionComboBox.getItems().addAll(LJ1200Message.Function.values());
            mFunctionComboBox.setDisable(true);
            mFunctionComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                getItem().setFunction(newValue);
                modifiedProperty().set(true);
            });
        }

        return mFunctionComboBox;
    }

    private TextField getIdentifierField()
    {
        if(mIdentifierField == null)
        {
            mIdentifierField = new TextField();
            mIdentifierField.setDisable(true);
            mIdentifierField.textProperty()
                .addListener((observable, oldValue, newValue) -> {
                    getItem().setID(newValue);
                    modifiedProperty().set(true);
                });

            String text = "ID: 5 numbers or characters (e.g. 1BN47)\n" +
                "Wildcard: asterisk (*) for any character (e.g. AB*12)\n" +
                "Middle character in a reply ID code identifies the entity\n" +
                "Valid middle character entities are:\n" +
                " Tower: X,Y\n" +
                " Transponder: 0-9,A,C-H,J-N,P-W\n" +
                " Not Used: B,I,O,Z";
            Tooltip toolTip = new Tooltip(text);
            toolTip.wrapTextProperty().set(true);
            mIdentifierField.setTooltip(toolTip);
        }

        return mIdentifierField;
    }
}

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

package io.github.dsheirer.gui.playlist.alias.identifier;

import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.INXDNTypeD;
import io.github.dsheirer.gui.control.IntegerFormatter;
import io.github.dsheirer.preference.identifier.talkgroup.NXDNTalkgroupFormatter;
import javafx.beans.value.ChangeListener;
import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

/**
 * Editor for NXDN Type-D radio and talkgroup alias identifiers
 * @param <T> alias identifier to edit
 */
public class NXDNTypeDEditor<T extends AliasID & INXDNTypeD> extends IdentifierEditor<T>
{
    private final TextField mHomeRepeaterField = new TextField();
    private final TextField mIdentifierField = new TextField();
    private final TextFormatter<Integer> mHomeRepeaterFormatter =
            new IntegerFormatter(0, NXDNTalkgroupFormatter.TYPE_D_MAX_HOME_REPEATER);
    private final TextFormatter<Integer> mIdentifierFormatter =
            new IntegerFormatter(0, NXDNTalkgroupFormatter.TYPE_D_MAX_IDENTIFIER);
    private final ChangeListener<Integer> mHomeRepeaterListener = (observable, oldValue, newValue) -> {
        if(getItem() != null)
        {
            getItem().setHomeRepeater(newValue != null ? newValue : 0);
            modifiedProperty().set(true);
        }
    };
    private final ChangeListener<Integer> mIdentifierListener = (observable, oldValue, newValue) -> {
        if(getItem() != null)
        {
            getItem().setIdentifier(newValue != null ? newValue : 0);
            modifiedProperty().set(true);
        }
    };

    /**
     * Constructs an instance
     * @param identifierLabel for the identifier field: Radio or Talkgroup
     */
    public NXDNTypeDEditor(String identifierLabel)
    {
        mHomeRepeaterField.setTextFormatter(mHomeRepeaterFormatter);
        mHomeRepeaterField.setTooltip(new Tooltip("Home repeater: 0 - " + NXDNTalkgroupFormatter.TYPE_D_MAX_HOME_REPEATER));
        mIdentifierField.setTextFormatter(mIdentifierFormatter);
        mIdentifierField.setTooltip(new Tooltip(identifierLabel + ": 0 - " + NXDNTalkgroupFormatter.TYPE_D_MAX_IDENTIFIER));

        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(3);

        gridPane.add(new Label("NXDN Type-D"), 0, 0);

        Label homeRepeaterLabel = new Label("Home Repeater");
        GridPane.setHalignment(homeRepeaterLabel, HPos.RIGHT);
        gridPane.add(homeRepeaterLabel, 1, 0);
        gridPane.add(mHomeRepeaterField, 2, 0);

        Label identifierFieldLabel = new Label(identifierLabel);
        GridPane.setHalignment(identifierFieldLabel, HPos.RIGHT);
        gridPane.add(identifierFieldLabel, 3, 0);
        gridPane.add(mIdentifierField, 4, 0);

        getChildren().add(gridPane);
    }

    @Override
    public void setItem(T item)
    {
        //Clear the item and listeners so that loading the field values doesn't change the item
        mHomeRepeaterFormatter.valueProperty().removeListener(mHomeRepeaterListener);
        mIdentifierFormatter.valueProperty().removeListener(mIdentifierListener);

        super.setItem(item);

        mHomeRepeaterField.setDisable(item == null);
        mIdentifierField.setDisable(item == null);
        mHomeRepeaterFormatter.setValue(item != null ? item.getHomeRepeater() : null);
        mIdentifierFormatter.setValue(item != null ? item.getIdentifier() : null);

        mHomeRepeaterFormatter.valueProperty().addListener(mHomeRepeaterListener);
        mIdentifierFormatter.valueProperty().addListener(mIdentifierListener);

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
}

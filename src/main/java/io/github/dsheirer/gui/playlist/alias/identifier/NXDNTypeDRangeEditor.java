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
import io.github.dsheirer.alias.id.INXDNTypeDRange;
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
 * Editor for NXDN Type-D radio and talkgroup range alias identifiers
 * @param <T> alias identifier to edit
 */
public class NXDNTypeDRangeEditor<T extends AliasID & INXDNTypeDRange> extends IdentifierEditor<T>
{
    private final TextField mHomeRepeaterField = new TextField();
    private final TextField mMinField = new TextField();
    private final TextField mMaxField = new TextField();
    private final TextFormatter<Integer> mHomeRepeaterFormatter =
            new IntegerFormatter(0, NXDNTalkgroupFormatter.TYPE_D_MAX_HOME_REPEATER);
    private final TextFormatter<Integer> mMinFormatter =
            new IntegerFormatter(0, NXDNTalkgroupFormatter.TYPE_D_MAX_IDENTIFIER);
    private final TextFormatter<Integer> mMaxFormatter =
            new IntegerFormatter(0, NXDNTalkgroupFormatter.TYPE_D_MAX_IDENTIFIER);
    private final ChangeListener<Integer> mHomeRepeaterListener = (observable, oldValue, newValue) -> {
        if(getItem() != null)
        {
            getItem().setHomeRepeater(newValue != null ? newValue : 0);
            modifiedProperty().set(true);
        }
    };
    private final ChangeListener<Integer> mMinListener = (observable, oldValue, newValue) -> {
        if(getItem() != null)
        {
            getItem().setMinIdentifier(newValue != null ? newValue : 0);
            modifiedProperty().set(true);
        }
    };
    private final ChangeListener<Integer> mMaxListener = (observable, oldValue, newValue) -> {
        if(getItem() != null)
        {
            getItem().setMaxIdentifier(newValue != null ? newValue : 0);
            modifiedProperty().set(true);
        }
    };

    /**
     * Constructs an instance
     * @param rangeLabel for the range fields: Radio Range or Talkgroup Range
     */
    public NXDNTypeDRangeEditor(String rangeLabel)
    {
        String identifierTooltip = rangeLabel + ": 0 - " + NXDNTalkgroupFormatter.TYPE_D_MAX_IDENTIFIER;
        mHomeRepeaterField.setTextFormatter(mHomeRepeaterFormatter);
        mHomeRepeaterField.setTooltip(new Tooltip("Home repeater: 0 - " + NXDNTalkgroupFormatter.TYPE_D_MAX_HOME_REPEATER));
        mMinField.setTextFormatter(mMinFormatter);
        mMinField.setTooltip(new Tooltip(identifierTooltip));
        mMaxField.setTextFormatter(mMaxFormatter);
        mMaxField.setTooltip(new Tooltip(identifierTooltip));

        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(3);

        gridPane.add(new Label("NXDN Type-D"), 0, 0);

        Label homeRepeaterLabel = new Label("Home Repeater");
        GridPane.setHalignment(homeRepeaterLabel, HPos.RIGHT);
        gridPane.add(homeRepeaterLabel, 1, 0);
        gridPane.add(mHomeRepeaterField, 2, 0);

        Label rangeFieldLabel = new Label(rangeLabel);
        GridPane.setHalignment(rangeFieldLabel, HPos.RIGHT);
        gridPane.add(rangeFieldLabel, 3, 0);
        gridPane.add(mMinField, 4, 0);
        gridPane.add(new Label("-"), 5, 0);
        gridPane.add(mMaxField, 6, 0);

        getChildren().add(gridPane);
    }

    @Override
    public void setItem(T item)
    {
        //Clear the item and listeners so that loading the field values doesn't change the item
        mHomeRepeaterFormatter.valueProperty().removeListener(mHomeRepeaterListener);
        mMinFormatter.valueProperty().removeListener(mMinListener);
        mMaxFormatter.valueProperty().removeListener(mMaxListener);

        super.setItem(item);

        mHomeRepeaterField.setDisable(item == null);
        mMinField.setDisable(item == null);
        mMaxField.setDisable(item == null);
        mHomeRepeaterFormatter.setValue(item != null ? item.getHomeRepeater() : null);
        mMinFormatter.setValue(item != null ? item.getMinIdentifier() : null);
        mMaxFormatter.setValue(item != null ? item.getMaxIdentifier() : null);

        mHomeRepeaterFormatter.valueProperty().addListener(mHomeRepeaterListener);
        mMinFormatter.valueProperty().addListener(mMinListener);
        mMaxFormatter.valueProperty().addListener(mMaxListener);

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

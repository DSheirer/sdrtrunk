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

import io.github.dsheirer.alias.id.esn.Esn;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.gui.control.HexFormatter;
import io.github.dsheirer.gui.control.IntegerFormatter;
import io.github.dsheirer.gui.control.LtrFormatter;
import io.github.dsheirer.gui.control.PrefixIdentFormatter;
import io.github.dsheirer.preference.identifier.IntegerFormat;
import io.github.dsheirer.protocol.Protocol;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Editor for ESN alias identifiers
 */
public class EsnEditor extends IdentifierEditor<Esn>
{
    private static final Logger mLog = LoggerFactory.getLogger(EsnEditor.class);
    private TextField mEsnField;

    /**
     * Constructs an instance
     */
    public EsnEditor()
    {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);

        Label typeLabel = new Label("ESN");
        GridPane.setConstraints(typeLabel, 0, 0);
        gridPane.getChildren().add(typeLabel);

        GridPane.setConstraints(getEsnField(), 1, 0);
        gridPane.getChildren().add(getEsnField());

        getChildren().add(gridPane);
    }

    @Override
    public void setItem(Esn item)
    {
        super.setItem(item);
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

    private TextField getEsnField()
    {
        if(mEsnField == null)
        {
            mEsnField = new TextField();
            mEsnField.textProperty()
                .addListener((observable, oldValue, newValue) -> {
                    getItem().setEsn(getEsnField().getText());
                    modifiedProperty().set(true);
                });
        }

        return mEsnField;
    }
}

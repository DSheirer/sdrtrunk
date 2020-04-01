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

import io.github.dsheirer.alias.id.status.UserStatusID;
import io.github.dsheirer.gui.control.IntegerFormatter;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Editor for user status alias identifiers
 */
public class UserStatusEditor extends IdentifierEditor<UserStatusID>
{
    private static final Logger mLog = LoggerFactory.getLogger(UserStatusEditor.class);

    private TextField mUserStatusField;
    private TextFormatter<Integer> mIntegerTextFormatter;

    /**
     * Constructs an instance
     */
    public UserStatusEditor()
    {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(3);

        Label valueLabel = new Label("User Status");
        GridPane.setHalignment(valueLabel, HPos.RIGHT);
        GridPane.setConstraints(valueLabel, 0, 0);
        gridPane.getChildren().add(valueLabel);

        GridPane.setConstraints(getUserStatusField(), 1, 0);
        gridPane.getChildren().add(getUserStatusField());

        Label helpLabel = new Label("Format: 0 - 255");
        GridPane.setConstraints(helpLabel, 2, 0);
        gridPane.getChildren().add(helpLabel);

        getChildren().add(gridPane);
    }

    @Override
    public void setItem(UserStatusID item)
    {
        super.setItem(item);

        UserStatusID userStatus = getItem();

        getUserStatusField().setDisable(userStatus == null);

        if(userStatus != null)
        {
            getTextFormatter().setValue(userStatus.getStatus());
        }
        else
        {
            getTextFormatter().setValue(null);
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

    private TextField getUserStatusField()
    {
        if(mUserStatusField == null)
        {
            mUserStatusField = new TextField();
            mUserStatusField.setTextFormatter(getTextFormatter());
        }

        return mUserStatusField;
    }

    private TextFormatter<Integer> getTextFormatter()
    {
        if(mIntegerTextFormatter == null)
        {
            mIntegerTextFormatter = new IntegerFormatter(0, 255);
            mIntegerTextFormatter.valueProperty().addListener(new ChangeListener<Integer>()
            {
                @Override
                public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue)
                {
                    if(getItem() != null)
                    {
                        getItem().setStatus(newValue != null ? newValue : 0);
                        modifiedProperty().set(true);
                    }
                }
            });
        }

        return mIntegerTextFormatter;
    }
}

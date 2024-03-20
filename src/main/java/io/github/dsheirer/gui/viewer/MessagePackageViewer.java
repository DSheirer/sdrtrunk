/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.gui.viewer;

import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * JavaFX message package details viewer
 */
public class MessagePackageViewer extends VBox
{
    private Label mMessageLabel;

    /**
     * Constructs an instance
     */
    public MessagePackageViewer()
    {
        GridPane gridPane = new GridPane();
        Label messageLabel = new Label("Message:");
        gridPane.add(messageLabel, 0, 0);
        gridPane.add(getMessageLabel(), 1, 0);

        getChildren().add(gridPane);
    }

    public void set(MessagePackage messagePackage)
    {
        if(messagePackage != null)
        {
            getMessageLabel().setText(messagePackage.toString());

        }
        else
        {
            getMessageLabel().setText(null);
        }
    }

    private Label getMessageLabel()
    {
        if(mMessageLabel == null)
        {
            mMessageLabel = new Label();
            mMessageLabel.setMaxWidth(Double.MAX_VALUE);
        }

        return mMessageLabel;
    }
}

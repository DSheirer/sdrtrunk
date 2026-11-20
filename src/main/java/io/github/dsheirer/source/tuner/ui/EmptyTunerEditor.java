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
package io.github.dsheirer.source.tuner.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Placeholder shown when no tuner is selected.
 */
public class EmptyTunerEditor extends VBox
{
    public EmptyTunerEditor()
    {
        getStyleClass().add("empty-tuner-editor");
        setAlignment(Pos.CENTER);
        Label title = new Label("No tuner selected");
        title.getStyleClass().add("empty-tuner-title");
        Label hint = new Label("Select a tuner in the list to view and change its settings.");
        hint.setWrapText(true);
        getChildren().addAll(title, hint);
    }
}

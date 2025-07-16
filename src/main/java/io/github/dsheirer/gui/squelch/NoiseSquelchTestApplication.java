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

package io.github.dsheirer.gui.squelch;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NoiseSquelchTestApplication extends Application
{
    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage)
    {
        NoiseSquelchView view = new NoiseSquelchView();
        view.setShowing(true);
        Scene scene = new Scene(view, 600, 400);
        primaryStage.setTitle("Noise Squelch View");
        primaryStage.setScene(scene);
        primaryStage.show();

        NoiseSquelchSimulator simulator = new NoiseSquelchSimulator();
        view.setController(simulator);
    }
}

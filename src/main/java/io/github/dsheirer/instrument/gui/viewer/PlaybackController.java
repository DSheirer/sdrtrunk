/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.instrument.gui.viewer;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.nio.file.Path;

public class PlaybackController extends HBox
{
    private Button mPlay1Button;
    private Button mPlay10Button;
    private Button mPlay100Button;
    private Button mPlay1000Button;

    public PlaybackController()
    {
        getChildren().addAll(getPlay1Button(), getPlay10Button(), getPlay100Button(), getPlay1000Button());

    }

    public void load(Path path)
    {

    }

    public Button getPlay1Button()
    {
        if(mPlay1Button == null)
        {
            mPlay1Button = new Button("> 1");
        }

        return mPlay1Button;
    }

    public Button getPlay10Button()
    {
        if(mPlay10Button == null)
        {
            mPlay10Button = new Button(">> 10");
        }

        return mPlay10Button;
    }

    public Button getPlay100Button()
    {
        if(mPlay100Button == null)
        {
            mPlay100Button = new Button(">> 100");
        }

        return mPlay100Button;
    }

    public Button getPlay1000Button()
    {
        if(mPlay1000Button == null)
        {
            mPlay1000Button = new Button(">> 1000");
        }

        return mPlay1000Button;
    }

}

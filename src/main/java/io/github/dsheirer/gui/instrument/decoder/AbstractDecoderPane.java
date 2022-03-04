/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.gui.instrument.decoder;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.SampleType;
import javafx.scene.layout.VBox;

public abstract class AbstractDecoderPane<T> extends VBox implements Listener<T>
{
    abstract SampleType getSampleType();

    public void setSampleRate(double sampleRate)
    {
    }
}

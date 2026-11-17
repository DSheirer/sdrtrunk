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
package io.github.dsheirer.gui.playlist.source;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FrequencyFieldTest
{
    @BeforeAll
    public static void startToolkit() throws Exception
    {
        //Run without a display
        System.setProperty("glass.platform", "Headless");
        System.setProperty("prism.order", "sw");
        CompletableFuture<Void> started = new CompletableFuture<>();

        try
        {
            Platform.startup(() -> started.complete(null));
        }
        catch(IllegalStateException e)
        {
            started.complete(null); //Already started
        }

        started.get(10, TimeUnit.SECONDS);
    }

    @Test
    void setUsesDotDecimalOnCommaLocale()
    {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);

        try
        {
            assertEquals("1,5", new DecimalFormat("0.0").format(1.5));
            FrequencyField field = new FrequencyField();
            field.set(172_650_000L);
            assertEquals(172_650_000L, field.get());
        }
        finally
        {
            Locale.setDefault(previous);
        }
    }
}

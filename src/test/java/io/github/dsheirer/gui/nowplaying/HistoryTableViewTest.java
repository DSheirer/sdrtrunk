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
package io.github.dsheirer.gui.nowplaying;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests copying the selected history rows to the clipboard.
 */
public class HistoryTableViewTest
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
    public void copiesSelectedRowsToClipboard() throws Exception
    {
        CompletableFuture<String> copied = new CompletableFuture<>();

        Platform.runLater(() -> {
            try
            {
                TestView view = new TestView();
                view.add("a");
                view.add("b");
                view.add("c");

                //Newest first: c, b, a
                TableView<?> table = (TableView<?>)view.lookup(".table-view");
                table.getSelectionModel().select(0);
                table.getSelectionModel().select(2);

                Clipboard.getSystemClipboard().clear();
                Event.fireEvent(table, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.C, false,
                    true, false, false));
                copied.complete(Clipboard.getSystemClipboard().getString());
            }
            catch(Throwable t)
            {
                copied.completeExceptionally(t);
            }
        });

        assertEquals("c\tC\na\tA", copied.get(10, TimeUnit.SECONDS));
    }

    private static class TestView extends HistoryTableView<String,String>
    {
        TestView()
        {
            super(List.of(NowPlayingCells.column("name", "Name", null, Pos.CENTER_LEFT,
                    (Row<String> row) -> row.getItem(), null),
                NowPlayingCells.column("upper", "Upper", null, Pos.CENTER_LEFT,
                    (Row<String> row) -> row.getItem().toUpperCase(), null)),
                "test.history.table", null, null, "Filter", "Columns");
        }

        @Override
        protected boolean include(String item)
        {
            return true;
        }
    }
}

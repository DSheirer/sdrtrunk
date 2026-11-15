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
package io.github.dsheirer.gui.table;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests storing, restoring and resetting table column order and visibility.
 */
public class TableColumnMonitorTest
{
    private static final String KEY = "test.table";
    private Preferences mPreferences;

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

    @BeforeEach
    public void createPreferences()
    {
        mPreferences = Preferences.userRoot().node("sdrtrunk-test/" + UUID.randomUUID());
    }

    @AfterEach
    public void removePreferences() throws BackingStoreException
    {
        mPreferences.removeNode();
    }

    /**
     * Runs the test on the JavaFX application thread.
     */
    private static void onFx(Runnable runnable) throws Exception
    {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try
            {
                runnable.run();
                result.complete(null);
            }
            catch(Throwable t)
            {
                result.completeExceptionally(t);
            }
        });
        result.get(10, TimeUnit.SECONDS);
    }

    private static TableView<String> createTable(String... ids)
    {
        TableView<String> tableView = new TableView<>();

        for(String id: ids)
        {
            TableColumn<String,String> column = new TableColumn<>(id.toUpperCase());
            column.setId(id);
            tableView.getColumns().add(column);
        }

        return tableView;
    }

    private static List<String> ids(TableView<String> tableView)
    {
        return tableView.getColumns().stream().map(TableColumn::getId).toList();
    }

    private static List<String> visibleIds(TableView<String> tableView)
    {
        return tableView.getColumns().stream().filter(TableColumn::isVisible).map(TableColumn::getId).toList();
    }

    private TableColumnMonitor<String> monitor(TableView<String> tableView)
    {
        return new TableColumnMonitor<>(tableView, KEY, mPreferences, null, null);
    }

    @Test
    public void defaultsWhenNothingIsStored() throws Exception
    {
        onFx(() -> {
            TableView<String> table = createTable("a", "b", "c");
            TableColumnMonitor<String> monitor = monitor(table);
            assertEquals(List.of("a", "b", "c"), ids(table));
            assertEquals(List.of("a", "b", "c"), visibleIds(table));
            assertTrue(monitor.isDefault());
        });
    }

    @Test
    public void orderAndVisibilityAreRestored() throws Exception
    {
        onFx(() -> {
            TableView<String> table = createTable("a", "b", "c", "d");
            TableColumnMonitor<String> monitor = monitor(table);
            assertTrue(monitor.move(table.getColumns().get(0), 1));
            table.getColumns().get(3).setVisible(false);
            assertEquals(List.of("b", "a", "c", "d"), ids(table));
            assertFalse(monitor.isDefault());

            //A new session
            TableView<String> restored = createTable("a", "b", "c", "d");
            monitor(restored);
            assertEquals(List.of("b", "a", "c", "d"), ids(restored));
            assertEquals(List.of("b", "a", "c"), visibleIds(restored));
        });
    }

    @Test
    public void resetRestoresDefaultsAndKeepsWidths() throws Exception
    {
        onFx(() -> {
            TableView<String> table = createTable("a", "b", "c");
            TableColumnMonitor<String> monitor = monitor(table);
            monitor.move(table.getColumns().get(2), -1);
            table.getColumns().get(0).setVisible(false);
            mPreferences.putDouble(KEY + ".column.width.b", 123.0);

            monitor.reset();
            assertEquals(List.of("a", "b", "c"), ids(table));
            assertEquals(List.of("a", "b", "c"), visibleIds(table));
            assertTrue(monitor.isDefault());
            assertNull(mPreferences.get(KEY + ".column.order", null));
            assertNull(mPreferences.get(KEY + ".column.hidden", null));

            TableView<String> restored = createTable("a", "b", "c");
            monitor(restored);
            assertEquals(List.of("a", "b", "c"), ids(restored));
            assertEquals(123.0, restored.getColumns().get(1).getPrefWidth());
        });
    }

    @Test
    public void newColumnIsShownAtItsDefaultPosition() throws Exception
    {
        onFx(() -> {
            mPreferences.put(KEY + ".column.order", "c,a,removed");
            mPreferences.put(KEY + ".column.hidden", "a");
            TableView<String> table = createTable("a", "b", "c");
            monitor(table);
            assertEquals(List.of("c", "b", "a"), ids(table));
            assertEquals(List.of("c", "b"), visibleIds(table));
        });
    }

    @Test
    public void lastVisibleColumnCantBeHidden() throws Exception
    {
        onFx(() -> {
            TableView<String> table = createTable("a", "b");
            monitor(table);
            table.getColumns().get(0).setVisible(false);
            table.getColumns().get(1).setVisible(false);
            assertEquals(List.of("b"), visibleIds(table));
        });
    }

    @Test
    public void columnsNeedUniqueIds() throws Exception
    {
        onFx(() -> assertThrows(IllegalArgumentException.class, () -> monitor(createTable("a", "a"))));
    }
}

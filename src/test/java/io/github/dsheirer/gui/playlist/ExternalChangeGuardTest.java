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

package io.github.dsheirer.gui.playlist;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.web.playlist.PlaylistConfig;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The desktop editors' guard against overwriting changes made elsewhere: an editor without unsaved edits reloads,
 * and the editor's own save and changes that don't change the version are ignored.
 */
public class ExternalChangeGuardTest
{
    @BeforeAll
    static void startToolkit() throws Exception
    {
        System.setProperty("glass.platform", "Headless");
        CompletableFuture<Void> started = new CompletableFuture<>();

        try
        {
            Platform.startup(() -> started.complete(null));
        }
        catch(IllegalStateException alreadyStarted)
        {
            started.complete(null);
        }

        started.get(10, TimeUnit.SECONDS);
    }

    /**
     * Minimal editor that counts reloads
     */
    private static class TestEditor extends Editor<Channel>
    {
        final ExternalChangeGuard<Channel> mGuard = new ExternalChangeGuard<>(this, PlaylistConfig::version,
            "channel");
        final AtomicInteger mLoads = new AtomicInteger();

        @Override
        public void setItem(Channel item)
        {
            super.setItem(item);
            mLoads.incrementAndGet();
            modifiedProperty().set(false);
            mGuard.loaded(item);
        }

        @Override
        public void save()
        {
            mGuard.save(() -> {
                getItem().setSite("Saved here");
                //The model reports the editor's own change while it saves
                mGuard.changed(getItem());
            });
        }

        @Override
        public void dispose()
        {
        }
    }

    private static <T> T onFx(java.util.concurrent.Callable<T> callable) throws Exception
    {
        CompletableFuture<T> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try
            {
                future.complete(callable.call());
            }
            catch(Throwable t)
            {
                future.completeExceptionally(t);
            }
        });
        return future.get(10, TimeUnit.SECONDS);
    }

    @Test
    void reloadsWhenChangedElsewhereWithoutUnsavedEdits() throws Exception
    {
        Channel channel = new Channel("Dispatch");
        TestEditor editor = onFx(TestEditor::new);
        onFx(() -> {
            editor.setItem(channel);
            return null;
        });

        //Same version: ignored
        onFx(() -> {
            editor.mGuard.changed(channel);
            return null;
        });
        assertEquals(1, editor.mLoads.get());

        //Changed elsewhere, reported off the JavaFX thread
        channel.setSystem("Changed by the web");
        editor.mGuard.changed(channel);
        onFx(() -> null);
        assertEquals(2, editor.mLoads.get());

        //The editor's own save
        onFx(() -> {
            editor.modifiedProperty().set(true);
            editor.save();
            return null;
        });
        assertEquals(2, editor.mLoads.get());

        //Another item than the one in the editor
        onFx(() -> {
            Channel other = new Channel("Other");
            other.setSite("x");
            editor.mGuard.changed(other);
            return null;
        });
        assertEquals(2, editor.mLoads.get());
    }
}

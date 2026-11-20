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

package io.github.dsheirer.gui.control;

import io.github.dsheirer.source.SourceEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the frequency control's keyboard and scroll wheel interaction.
 */
public class FrequencyControlTest
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

    private static FrequencyControl createControl(List<Long> requests)
    {
        FrequencyControl control = new FrequencyControl();
        control.setRange(24_000_000, 1_766_000_000);
        control.setFrequency(851_012_500);
        control.addListener(requests::add);
        Scene scene = new Scene(new StackPane(control), 400, 120);
        scene.snapshot(null);
        return control;
    }

    private static void type(Node node, String text)
    {
        for(char c: text.toCharArray())
        {
            node.fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, String.valueOf(c), "", KeyCode.UNDEFINED, false, false,
                    false, false));
        }
    }

    private static void press(Node node, KeyCode code)
    {
        node.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
    }

    @Test
    public void typedEntryAppliedWithEnter() throws Exception
    {
        List<Long> requests = new ArrayList<>();
        long frequency = onFx(() -> {
            FrequencyControl control = createControl(requests);
            type(control, "462.5625");
            assertTrue(control.isEntryActive());
            assertEquals(851_012_500L, control.getFrequency(), "not applied until Enter");
            press(control, KeyCode.ENTER);
            return control.getFrequency();
        });

        assertEquals(462_562_500L, frequency);
        assertEquals(List.of(462_562_500L), requests);
    }

    @Test
    public void typedEntryCancelledWithEscape() throws Exception
    {
        List<Long> requests = new ArrayList<>();
        long frequency = onFx(() -> {
            FrequencyControl control = createControl(requests);
            type(control, "155.0");
            press(control, KeyCode.ESCAPE);
            return control.getFrequency();
        });

        assertEquals(851_012_500L, frequency);
        assertTrue(requests.isEmpty());
    }

    @Test
    public void outOfRangeEntryRejected() throws Exception
    {
        List<Long> requests = new ArrayList<>();
        long frequency = onFx(() -> {
            FrequencyControl control = createControl(requests);
            type(control, "2000");
            press(control, KeyCode.ENTER);
            return control.getFrequency();
        });

        assertEquals(851_012_500L, frequency);
        assertTrue(requests.isEmpty());
    }

    @Test
    public void arrowKeysMoveCursorAndStep() throws Exception
    {
        List<Long> requests = new ArrayList<>();
        long frequency = onFx(() -> {
            FrequencyControl control = createControl(requests);
            //Cursor starts on the 1 MHz digit: move right to the 100 kHz digit and step up twice
            assertEquals(6, control.getCursorPosition());
            press(control, KeyCode.RIGHT);
            press(control, KeyCode.UP);
            press(control, KeyCode.UP);
            //Move left twice to the 10 MHz digit and step down once
            press(control, KeyCode.LEFT);
            press(control, KeyCode.LEFT);
            press(control, KeyCode.DOWN);
            return control.getFrequency();
        });

        assertEquals(841_212_500L, frequency);
        assertEquals(List.of(851_112_500L, 851_212_500L, 841_212_500L), requests);
    }

    @Test
    public void scrollWheelStepsDigitUnderMouse() throws Exception
    {
        List<Long> requests = new ArrayList<>();
        long frequency = onFx(() -> {
            FrequencyControl control = createControl(requests);
            //Digits are in order from the 1000 MHz digit (position 9) to the 1 Hz digit (position 0)
            Node digit = control.lookupAll(".frequency-digit").stream().toList().get(9 - 3); //1 kHz digit
            var bounds = digit.localToScene(digit.getLayoutBounds());
            //Two wheel notches up, as JFXPanel reports them: 40 per notch with a multiplier of 40
            digit.fireEvent(new ScrollEvent(ScrollEvent.SCROLL, bounds.getCenterX(), bounds.getCenterY(),
                    bounds.getCenterX(), bounds.getCenterY(), false, false, false, false, false, false, 0, 80, 0, 80,
                    40, 40, ScrollEvent.HorizontalTextScrollUnits.NONE, 0, ScrollEvent.VerticalTextScrollUnits.NONE,
                    0, 0, null));
            return control.getFrequency();
        });

        assertEquals(851_014_500L, frequency);
        assertEquals(List.of(851_014_500L), requests, "one request for both notches");
    }

    @Test
    public void tunerNotificationUpdatesWithoutRequest() throws Exception
    {
        List<Long> requests = new ArrayList<>();
        FrequencyControl control = onFx(() -> createControl(requests));
        control.process(SourceEvent.frequencyChange(null, 155_250_000L));
        long frequency = onFx(control::getFrequency);
        assertEquals(155_250_000L, frequency);
        assertTrue(requests.isEmpty());
    }
}

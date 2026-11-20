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

import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongConsumer;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Frequency display and tuning control that shows a frequency in MHz with six fractional digits (1 Hz).
 *
 * Mouse: hover over a digit to reveal its up/down halves.  Click the upper half to increment the digit or the lower
 * half to decrement it (hold the button to repeat), or use the scroll wheel over the digit.  Steps carry into the
 * higher digits and stop at the minimum/maximum frequency.
 *
 * Keyboard: left/right move the digit cursor, up/down (or +/-) step the digit under the cursor.  Typing digits
 * starts a calculator style entry (e.g. 851.0125) that is applied with Enter and cancelled with Escape.  Shortcut-C
 * and Shortcut-V copy and paste the frequency.
 *
 * The control only reports user requested frequencies to the listeners.  {@link #setFrequency(long)} updates the
 * display without notifying them, which is how the tuner reports its actual frequency back to the control.
 */
public class FrequencyControl extends HBox implements ISourceEventProcessor
{
    private static final PseudoClass HOVER_UP = PseudoClass.getPseudoClass("hover-up");
    private static final PseudoClass HOVER_DOWN = PseudoClass.getPseudoClass("hover-down");
    private static final PseudoClass LEADING = PseudoClass.getPseudoClass("leading");
    private static final PseudoClass CURSOR = PseudoClass.getPseudoClass("cursor");
    private static final PseudoClass ENTRY = PseudoClass.getPseudoClass("entry");
    private static final PseudoClass TYPED = PseudoClass.getPseudoClass("typed");
    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");
    private static final Duration REPEAT_DELAY = Duration.millis(400);
    private static final Duration REPEAT_INTERVAL = Duration.millis(70);

    private final List<LongConsumer> mListeners = new CopyOnWriteArrayList<>();
    private final Digit[] mDigits = new Digit[FrequencyEntry.DIGIT_COUNT];
    private final FrequencyEntry.TypedEntry mTypedEntry = new FrequencyEntry.TypedEntry();
    private final Text mProbe = new Text("0");
    private final Label mUnitLabel = new Label("MHz");
    private final Tooltip mTooltip = new Tooltip();
    private final PauseTransition mInvalidFlash = new PauseTransition(Duration.millis(1200));
    private final Timeline mRepeat = new Timeline();
    private long mFrequency;
    private long mMinimum = 1;
    private long mMaximum = FrequencyEntry.MAXIMUM_DISPLAYABLE;
    private int mCursorPosition = FrequencyEntry.FRACTION_DIGITS;
    private double mScrollAccumulator;

    /**
     * Constructs an instance
     */
    public FrequencyControl()
    {
        getStyleClass().add("frequency-control");
        URL stylesheet = FrequencyControl.class.getResource("/frequency_control.css");

        if(stylesheet != null)
        {
            getStylesheets().add(stylesheet.toExternalForm());
        }

        setAlignment(Pos.CENTER_LEFT);
        setFocusTraversable(true);
        setAccessibleRole(AccessibleRole.SPINNER);
        setMaxWidth(Region.USE_PREF_SIZE);

        //Invisible probe text, styled like the digits, sizes every digit cell to the same width so that a
        //proportional font doesn't make the display jitter as the digits change.
        mProbe.getStyleClass().add("frequency-digit-text");
        mProbe.setManaged(false);
        mProbe.setVisible(false);
        getChildren().add(mProbe);

        for(int position = FrequencyEntry.DIGIT_COUNT - 1; position >= 0; position--)
        {
            Digit digit = new Digit(position);
            mDigits[position] = digit;
            getChildren().add(digit);

            if(position == FrequencyEntry.FRACTION_DIGITS)
            {
                Text point = new Text(".");
                point.getStyleClass().addAll("frequency-digit-text", "frequency-point");
                getChildren().add(point);
            }
            else if(position % 3 == 0 && position > 0)
            {
                Region gap = new Region();
                gap.getStyleClass().add("frequency-group-gap");
                getChildren().add(gap);
            }
        }

        mUnitLabel.getStyleClass().add("frequency-unit");
        getChildren().add(mUnitLabel);

        Tooltip.install(this, mTooltip);
        updateTooltip();

        mInvalidFlash.setOnFinished(e -> pseudoClassStateChanged(INVALID, false));
        mRepeat.setCycleCount(Timeline.INDEFINITE);

        addEventHandler(KeyEvent.KEY_PRESSED, this::keyPressed);
        addEventHandler(KeyEvent.KEY_TYPED, this::keyTyped);
        addEventHandler(MouseEvent.MOUSE_PRESSED, e -> requestFocus());
        focusedProperty().addListener((observable, wasFocused, focused) -> {
            if(!focused)
            {
                cancelEntry();
            }
            updateDigits();
        });
        disabledProperty().addListener((observable, wasDisabled, disabled) -> {
            if(disabled)
            {
                stopRepeat();
                cancelEntry();
            }
        });

        setOnContextMenuRequested(e -> {
            buildContextMenu().show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        updateDigits();
    }

    /**
     * Adds a listener to be notified of frequencies requested by the user.  Listeners are invoked on the JavaFX
     * application thread.
     */
    public void addListener(LongConsumer listener)
    {
        if(listener != null && !mListeners.contains(listener))
        {
            mListeners.add(listener);
        }
    }

    /**
     * Removes the listener.
     */
    public void removeListener(LongConsumer listener)
    {
        mListeners.remove(listener);
    }

    /**
     * Removes all listeners.
     */
    public void clearListeners()
    {
        mListeners.clear();
    }

    /**
     * Current frequency in Hertz.
     */
    public long getFrequency()
    {
        return mFrequency;
    }

    /**
     * Sets the displayed frequency without notifying listeners.  Safe to call from any thread.
     * @param frequency in Hertz
     */
    public void setFrequency(long frequency)
    {
        if(Platform.isFxApplicationThread())
        {
            mFrequency = Math.max(0, Math.min(FrequencyEntry.MAXIMUM_DISPLAYABLE, frequency));
            updateDigits();
        }
        else
        {
            Platform.runLater(() -> setFrequency(frequency));
        }
    }

    /**
     * Sets the range of frequencies that the user can request.
     * @param minimum frequency in Hertz
     * @param maximum frequency in Hertz
     */
    public void setRange(long minimum, long maximum)
    {
        mMinimum = Math.max(0, Math.min(minimum, maximum));
        mMaximum = Math.min(FrequencyEntry.MAXIMUM_DISPLAYABLE, Math.max(minimum, maximum));
        updateTooltip();
    }

    /**
     * Minimum frequency that the user can request.
     */
    public long getMinimum()
    {
        return mMinimum;
    }

    /**
     * Maximum frequency that the user can request.
     */
    public long getMaximum()
    {
        return mMaximum;
    }

    /**
     * Receives tuner frequency change notifications and shows the new frequency.  Requests are not re-broadcast.
     */
    @Override
    public void process(SourceEvent event)
    {
        if(event.getEvent() == SourceEvent.Event.NOTIFICATION_FREQUENCY_CHANGE && event.getValue() != null)
        {
            setFrequency(event.getValue().longValue());
        }
    }

    /**
     * Digit position under the keyboard cursor.
     */
    public int getCursorPosition()
    {
        return mCursorPosition;
    }

    /**
     * Steps the digit at the position and notifies listeners when the frequency changes.
     * @param position of the digit (0 = 1 Hz)
     * @param steps positive to increment, negative to decrement
     */
    public void step(int position, int steps)
    {
        if(isDisabled() || steps == 0)
        {
            return;
        }

        cancelEntry();
        long stepped = FrequencyEntry.step(mFrequency, position, steps, mMinimum, mMaximum);
        request(stepped);
    }

    /**
     * Requests the frequency on behalf of the user.  An out of range frequency is rejected and flagged.
     * @return true if the frequency was accepted.
     */
    public boolean request(long frequency)
    {
        if(frequency < mMinimum || frequency > mMaximum)
        {
            flagInvalid();
            return false;
        }

        if(frequency != mFrequency)
        {
            mFrequency = frequency;
            updateDigits();

            for(LongConsumer listener: mListeners)
            {
                listener.accept(frequency);
            }
        }

        return true;
    }

    /**
     * Indicates if a typed entry is in progress.
     */
    public boolean isEntryActive()
    {
        return !mTypedEntry.isEmpty();
    }

    private void keyTyped(KeyEvent event)
    {
        if(isDisabled() || event.isShortcutDown() || event.isAltDown())
        {
            return;
        }

        String character = event.getCharacter();

        if(character == null || character.length() != 1)
        {
            return;
        }

        char c = character.charAt(0);

        if((c >= '0' && c <= '9') || c == '.' || c == ',')
        {
            mTypedEntry.type(c);
            updateDigits();
            event.consume();
        }
        else if(c == '+' || c == '=')
        {
            step(mCursorPosition, 1);
            event.consume();
        }
        else if(c == '-')
        {
            step(mCursorPosition, -1);
            event.consume();
        }
    }

    private void keyPressed(KeyEvent event)
    {
        if(isDisabled())
        {
            return;
        }

        if(event.isShortcutDown())
        {
            if(event.getCode() == KeyCode.C)
            {
                copy();
                event.consume();
            }
            else if(event.getCode() == KeyCode.V)
            {
                paste();
                event.consume();
            }
            return;
        }

        switch(event.getCode())
        {
            case LEFT, KP_LEFT -> moveCursor(1);
            case RIGHT, KP_RIGHT -> moveCursor(-1);
            case HOME -> setCursorPosition(FrequencyEntry.DIGIT_COUNT - 1);
            case END -> setCursorPosition(0);
            case UP, KP_UP -> step(mCursorPosition, 1);
            case DOWN, KP_DOWN -> step(mCursorPosition, -1);
            case PAGE_UP -> step(mCursorPosition, 10);
            case PAGE_DOWN -> step(mCursorPosition, -10);
            case ENTER ->
            {
                if(!commitEntry())
                {
                    return;
                }
            }
            case ESCAPE ->
            {
                if(!isEntryActive())
                {
                    return;
                }
                cancelEntry();
            }
            case BACK_SPACE ->
            {
                if(!isEntryActive())
                {
                    return;
                }
                mTypedEntry.backspace();
                updateDigits();
            }
            default ->
            {
                return;
            }
        }

        event.consume();
    }

    /**
     * Applies the typed entry.
     * @return true if there was an entry to apply.
     */
    private boolean commitEntry()
    {
        if(!isEntryActive())
        {
            return false;
        }

        long frequency = mTypedEntry.getFrequency();
        mTypedEntry.clear();

        if(!request(frequency))
        {
            updateDigits();
        }

        return true;
    }

    private void cancelEntry()
    {
        if(isEntryActive())
        {
            mTypedEntry.clear();
            updateDigits();
        }
    }

    private void moveCursor(int direction)
    {
        cancelEntry();
        setCursorPosition(mCursorPosition + direction);
    }

    private void setCursorPosition(int position)
    {
        mCursorPosition = Math.max(0, Math.min(FrequencyEntry.DIGIT_COUNT - 1, position));
        updateDigits();
    }

    private void copy()
    {
        ClipboardContent content = new ClipboardContent();
        content.putString(FrequencyEntry.format(mFrequency));
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void paste()
    {
        Clipboard clipboard = Clipboard.getSystemClipboard();

        if(clipboard.hasString())
        {
            Long frequency = FrequencyEntry.parse(clipboard.getString());

            cancelEntry();

            if(frequency != null)
            {
                request(frequency);
            }
            else
            {
                flagInvalid();
            }
        }
    }

    private ContextMenu buildContextMenu()
    {
        MenuItem copy = new MenuItem("Copy Frequency");
        copy.setAccelerator(KeyCombination.keyCombination("Shortcut+C"));
        copy.setOnAction(e -> copy());

        MenuItem paste = new MenuItem("Paste Frequency");
        paste.setAccelerator(KeyCombination.keyCombination("Shortcut+V"));
        paste.setDisable(isDisabled() || !Clipboard.getSystemClipboard().hasString());
        paste.setOnAction(e -> paste());

        MenuItem minimum = new MenuItem("Tune to Minimum (" + FrequencyEntry.format(mMinimum) + " MHz)");
        minimum.setDisable(isDisabled());
        minimum.setOnAction(e -> request(mMinimum));

        MenuItem maximum = new MenuItem("Tune to Maximum (" + FrequencyEntry.format(mMaximum) + " MHz)");
        maximum.setDisable(isDisabled());
        maximum.setOnAction(e -> request(mMaximum));

        return new ContextMenu(copy, paste, new SeparatorMenuItem(), minimum, maximum);
    }

    private void flagInvalid()
    {
        pseudoClassStateChanged(INVALID, true);
        mInvalidFlash.playFromStart();
    }

    private void updateTooltip()
    {
        mTooltip.setText("Tuning range " + FrequencyEntry.format(mMinimum) + " - " +
                FrequencyEntry.format(mMaximum) + " MHz\n\n" +
                "Hover over a digit and use the scroll wheel, or click its upper or lower half, to step it.\n" +
                "Or type a frequency in MHz (e.g. 851.0125) and press Enter.  Escape cancels.\n" +
                "Arrow keys move between digits and step the selected digit.");
    }

    /**
     * Refreshes every digit's value and state.
     */
    private void updateDigits()
    {
        boolean entry = isEntryActive();
        long shown = entry ? mTypedEntry.getFrequency() : mFrequency;
        boolean focused = isFocused();
        pseudoClassStateChanged(ENTRY, entry);

        for(Digit digit: mDigits)
        {
            int position = digit.getPosition();
            digit.setValue(FrequencyEntry.getDigit(shown, position));
            digit.pseudoClassStateChanged(LEADING, FrequencyEntry.isLeadingZero(shown, position));
            digit.pseudoClassStateChanged(TYPED, entry && mTypedEntry.isTyped(position));
            digit.pseudoClassStateChanged(CURSOR, focused && !entry && position == mCursorPosition);
        }

        setAccessibleText(FrequencyEntry.format(mFrequency) + " MHz");
    }

    private void stopRepeat()
    {
        mRepeat.stop();
        mRepeat.getKeyFrames().clear();
    }

    /**
     * A single digit with up and down halves.
     */
    private class Digit extends VBox
    {
        private final int mPosition;
        private final Text mText = new Text("0");
        private final Region mUpHalf = new Region();
        private final Region mDownHalf = new Region();

        Digit(int position)
        {
            mPosition = position;
            getStyleClass().add("frequency-digit");
            setAlignment(Pos.CENTER);

            Region upArrow = new Region();
            upArrow.getStyleClass().addAll("frequency-arrow", "frequency-arrow-up");
            Region downArrow = new Region();
            downArrow.getStyleClass().addAll("frequency-arrow", "frequency-arrow-down");

            mText.getStyleClass().add("frequency-digit-text");

            mUpHalf.getStyleClass().addAll("frequency-half", "frequency-half-up");
            mDownHalf.getStyleClass().addAll("frequency-half", "frequency-half-down");

            StackPane cell = new StackPane(mUpHalf, mDownHalf, mText);
            cell.getStyleClass().add("frequency-digit-cell");
            StackPane.setAlignment(mUpHalf, Pos.TOP_CENTER);
            StackPane.setAlignment(mDownHalf, Pos.BOTTOM_CENTER);
            mUpHalf.maxHeightProperty().bind(cell.heightProperty().divide(2));
            mDownHalf.maxHeightProperty().bind(cell.heightProperty().divide(2));
            cell.minWidthProperty().bind(mProbe.layoutBoundsProperty().map(bounds -> bounds.getWidth() + 4));
            cell.prefWidthProperty().bind(cell.minWidthProperty());

            //Arrow size follows the digit font size, so it scales with the user's GUI scale
            upArrow.prefWidthProperty().bind(mProbe.layoutBoundsProperty().map(bounds -> bounds.getWidth() * 0.6));
            upArrow.prefHeightProperty().bind(mProbe.layoutBoundsProperty().map(bounds -> bounds.getHeight() * 0.18));
            upArrow.maxWidthProperty().bind(upArrow.prefWidthProperty());
            upArrow.maxHeightProperty().bind(upArrow.prefHeightProperty());
            downArrow.prefWidthProperty().bind(upArrow.prefWidthProperty());
            downArrow.prefHeightProperty().bind(upArrow.prefHeightProperty());
            downArrow.maxWidthProperty().bind(upArrow.prefWidthProperty());
            downArrow.maxHeightProperty().bind(upArrow.prefHeightProperty());

            getChildren().addAll(upArrow, cell, downArrow);

            addEventHandler(MouseEvent.MOUSE_MOVED, e -> setHalf(e.getY() < getHeight() / 2 ? 1 : -1));
            addEventHandler(MouseEvent.MOUSE_ENTERED, e -> setHalf(e.getY() < getHeight() / 2 ? 1 : -1));
            addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
                setHalf(0);
                stopRepeat();
            });
            addEventHandler(MouseEvent.MOUSE_PRESSED, this::pressed);
            addEventHandler(MouseEvent.MOUSE_RELEASED, e -> stopRepeat());
            addEventHandler(ScrollEvent.SCROLL, this::scrolled);
        }

        int getPosition()
        {
            return mPosition;
        }

        void setValue(int value)
        {
            mText.setText(String.valueOf(value));
        }

        private void setHalf(int half)
        {
            pseudoClassStateChanged(HOVER_UP, half > 0);
            pseudoClassStateChanged(HOVER_DOWN, half < 0);
        }

        private void pressed(MouseEvent event)
        {
            if(event.getButton() != MouseButton.PRIMARY || isDisabled())
            {
                return;
            }

            requestFocus();
            mCursorPosition = mPosition;
            int direction = event.getY() < getHeight() / 2 ? 1 : -1;
            setHalf(direction);
            step(mPosition, direction);

            stopRepeat();
            mRepeat.getKeyFrames().add(new KeyFrame(REPEAT_INTERVAL, e -> step(mPosition, direction)));
            mRepeat.setDelay(REPEAT_DELAY);
            mRepeat.playFromStart();
            event.consume();
        }

        private void scrolled(ScrollEvent event)
        {
            if(isDisabled() || event.getDeltaY() == 0)
            {
                return;
            }

            //Accumulate so that high resolution (touchpad) scrolling steps once per wheel notch
            double notch = event.getMultiplierY() > 1 ? event.getMultiplierY() : 40;
            mScrollAccumulator += event.getDeltaY() / notch;
            int steps = (int)mScrollAccumulator;

            if(steps != 0)
            {
                mScrollAccumulator -= steps;
                mCursorPosition = mPosition;
                step(mPosition, steps);
            }

            event.consume();
        }
    }
}

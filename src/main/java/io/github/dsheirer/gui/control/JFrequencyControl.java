/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
import io.github.dsheirer.source.InvalidFrequencyException;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Custom frequency controller for displaying or editing frequency values in MHz.
 */
public class JFrequencyControl extends JPanel implements ISourceEventProcessor
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(JFrequencyControl.class);
    private List<ISourceEventProcessor> mProcessors = new ArrayList<>();
    private Color mHighlightColor = Color.YELLOW;
    private long mFrequency;
    private HashMap<Integer,Digit> mDigits = new HashMap<Integer,Digit>();

    /**
     * Constructor
     */
    public JFrequencyControl()
    {
        init();
    }

    /**
     * Initializes the control/layout.
     */
    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0", "[]0[]", ""));

        Font font = new Font(Font.MONOSPACED, Font.BOLD, 30);

        for(int x = 9; x >= 0; x--)
        {
            Digit digit = null;

            try
            {
                digit = new Digit(x);
            }
            catch(ParseException e)
            {
                mLog.error("JFrequencyControl - parse exception constructing a digit - " + e);
            }

            if(digit != null)
            {
                mDigits.put(x, digit);

                add(digit);

                digit.setFont(font);

                if(x == 6)
                {
                    add(new JLabel(". "));
                }
            }
        }

        revalidate();
    }

    /**
     * Enables or disables the control.
     * @param enabled true if this component should be enabled, false otherwise
     */
    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);

        for(Digit digit : mDigits.values())
        {
            digit.setEnabled(enabled);
        }
    }

    /**
     * Sets the highlight color for the frequency digits when selected.
     * @param color the color to use for highlighting selected digits
     */
    public void setHighlightColor(Color color)
    {
        mHighlightColor = color;
    }

    /**
     * Receives a frequency change event invoked by another control.  We don't
     * rebroadcast this event, just set the control to indicate the new frequency.
     */
    @Override
    public void process(SourceEvent event)
    {
        switch(event.getEvent())
        {
            case NOTIFICATION_FREQUENCY_CHANGE:
                EventQueue.invokeLater(() -> setFrequency(event.getValue().longValue(), false));
                break;
        }
    }

    /**
     * Loads the frequency into the display and optionally fires a change event
     * to all registered listeners
     */
    public void setFrequency(long frequency, boolean fireChangeEvent)
    {
        mFrequency = frequency;

        for(Digit digit : mDigits.values())
        {
            digit.setFrequency(frequency, fireChangeEvent);
        }
    }

    /**
     * Current frequency value for the control.
     * @return frequency
     */
    public long getFrequency()
    {
        return mFrequency;
    }

    /**
     * Updates the frequency value according to the manually changed digit value.
     */
    private void updateFrequency()
    {
        long frequency = 0;

        for(Digit digit : mDigits.values())
        {
            frequency += digit.getFrequency();
        }

        mFrequency = frequency;
    }

    /**
     * Fires a source changed event
     * @throws SourceException of there's an error
     */
    private void fireSourceChanged() throws SourceException
    {
        updateFrequency();
        Iterator<ISourceEventProcessor> it = mProcessors.iterator();
        SourceEvent event = SourceEvent.frequencyRequest(mFrequency);
        while(it.hasNext())
        {
            it.next().process(event);
        }
    }

    /**
     * Adds the source event listener
     * @param processor
     */
    public void addListener(ISourceEventProcessor processor)
    {
        if(!mProcessors.contains(processor))
        {
            mProcessors.add(processor);
        }
    }

    /**
     * Remove all source event processors
     */
    public void clearListeners()
    {
        mProcessors.clear();
    }

    /**
     * Removes the source event processor
     * @param processor
     */
    public void removeListener(ISourceEventProcessor processor)
    {
        mProcessors.remove(processor);
    }

    /**
     * Custom single digit value editor.
     */
    public class Digit extends JTextField
    {
        private static final long serialVersionUID = 1L;
        private int mPower = 0;
        private long mValue = 0;

        /**
         * Constructor
         * @param position of the digit where a position of 1 is the ones unit of MHz and a position of -1 is the tenths
         * unit of MHz.
         * @throws ParseException if there is an error
         */
        private Digit(int position) throws ParseException
        {
            super("0");

            mPower = position;
            setToolTipText(getTooltip(mPower));
            Listener listener = new Listener();

            this.addKeyListener(listener);
            this.addMouseListener(listener);
            this.addMouseWheelListener(listener);
        }

        /**
         * Generates a custom tooltip for the current hover-over digit.
         * @param power of the digit in units of 10
         * @return custom tooptip.
         */
        private static String getTooltip(int power)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Center frequency ");
            switch(power)
            {
                case 0:
                    sb.append("1 Hertz");
                    break;
                case 1:
                    sb.append("10 Hertz");
                    break;
                case 2:
                    sb.append("100 Hertz");
                    break;
                case 3:
                    sb.append("1 Kilohertz");
                    break;
                case 4:
                    sb.append("10 Kilohertz");
                    break;
                case 5:
                    sb.append("100 Kilohertz");
                    break;
                case 6:
                    sb.append("1 Megahertz");
                    break;
                case 7:
                    sb.append("10 Megahertz");
                    break;
                case 8:
                    sb.append("100 Megahertz");
                    break;
                case 9:
                    sb.append("1 Gigahertz");
                    break;
            }

            sb.append(" units.  Type a number, use up/down arrows, or click the upper/lower digit control to adjust");

            return sb.toString();
        }

        /**
         * Sets this digit to the value of the column in frequency that corresponds
         * to the power (ie column) set for this digit.  Optionally, fires a
         * value change event to all listeners.
         */
        public void setFrequency(long frequency, boolean fireChangeEvent)
        {
            //Strip the digits higher than this one
            long lower = frequency % (long)(FastMath.pow(10, mPower + 1));
            //Set the value to int value of dividing by 10 to this power
            long value = (long)(lower / (long)(FastMath.pow(10, mPower)));
            set(value, fireChangeEvent);
        }

        /**
         * Frequency value.
         * @return frequency value.
         */
        public long getFrequency()
        {
            return mValue * (long)FastMath.pow(10, mPower);
        }

        /**
         * Increments the unit value and fires a change event
         */
        public void increment()
        {
            increment(true);
        }

        /**
         * Increments the unit value and fires a change event
         * @param fireChangeEvent true to fire an event
         */
        public void increment(boolean fireChangeEvent)
        {
            if(isEnabled())
            {
                set(mValue + 1, fireChangeEvent);
            }
        }

        /**
         * Decrements the unit value and fires a change event.
         */
        public void decrement()
        {
            decrement(true);
        }

        /**
         * Decrements the unit value and optionally fires a change event.
         * @param fireChangeEvent set to true to fire an event
         */
        public void decrement(boolean fireChangeEvent)
        {
            if(isEnabled())
            {
                set(mValue - 1, fireChangeEvent);
            }
        }

        /**
         * Convenience wrapper to change amount and fire change event
         */
        private void set(long amount)
        {
            set(amount, true);
        }

        /**
         * Changes the value and optionally fires change event to listeners
         */
        private void set(long amount, boolean fireChangeEvent)
        {
            long previous = mValue;
            boolean higherDigitIncremented = false;
            boolean higherDigitDecremented = false;

            mValue = amount;


            while(mValue < 0)
            {
                mValue += 10;
                Digit nextHigherDigit = mDigits.get(mPower + 1);

                if(nextHigherDigit != null)
                {
                    nextHigherDigit.decrement(false);
                    higherDigitDecremented = true;
                }

            }

            while(mValue > 9)
            {
                mValue -= 10;
                Digit nextHigherDigit = mDigits.get(mPower + 1);

                if(nextHigherDigit != null)
                {
                    nextHigherDigit.increment(false);
                    higherDigitIncremented = true;
                }
            }

            if(fireChangeEvent)
            {
                try
                {
                    fireSourceChanged();
                }
                catch(SourceException se)
                {
                    mValue = previous;

                    if(higherDigitIncremented)
                    {
                        Digit nextHigherDigit = mDigits.get(mPower + 1);

                        if(nextHigherDigit != null)
                        {
                            nextHigherDigit.decrement(false);
                        }
                    }

                    if(higherDigitDecremented)
                    {
                        Digit nextHigherDigit = mDigits.get(mPower + 1);

                        if(nextHigherDigit != null)
                        {
                            nextHigherDigit.increment(false);
                        }
                    }

                    if(se instanceof InvalidFrequencyException ife)
                    {
                        JOptionPane.showMessageDialog(this, ife.getMessage() + " for this tuner.");
                    }
                }
            }


            setText(String.valueOf(mValue));
            repaint();
        }

        /**
         * Multiple Listener interface implementation
         */
        private class Listener implements KeyListener, MouseListener, MouseWheelListener
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                int key = e.getKeyCode();

                switch(key)
                {
                    case KeyEvent.VK_0:
                    case KeyEvent.VK_NUMPAD0:
                        set(0);
                        Digit.this.transferFocus();
                        break;
                    case KeyEvent.VK_1:
                    case KeyEvent.VK_NUMPAD1:
                        set(1);
                        Digit.this.transferFocus();
                        break;
                    case KeyEvent.VK_2:
                    case KeyEvent.VK_NUMPAD2:
                        set(2);
                        Digit.this.transferFocus();
                        break;
                    case KeyEvent.VK_3:
                    case KeyEvent.VK_NUMPAD3:
                        set(3);
                        Digit.this.transferFocus();
                        break;
                    case KeyEvent.VK_4:
                    case KeyEvent.VK_NUMPAD4:
                        set(4);
                        Digit.this.transferFocus();
                        break;
                    case KeyEvent.VK_5:
                    case KeyEvent.VK_NUMPAD5:
                        set(5);
                        Digit.this.transferFocus();
                        break;
                    case KeyEvent.VK_6:
                    case KeyEvent.VK_NUMPAD6:
                        set(6);
                        Digit.this.transferFocus();
                        break;
                    case KeyEvent.VK_7:
                    case KeyEvent.VK_NUMPAD7:
                        set(7);
                        Digit.this.transferFocus();
                        break;
                    case KeyEvent.VK_8:
                    case KeyEvent.VK_NUMPAD8:
                        set(8);
                        Digit.this.transferFocus();
                        break;
                    case KeyEvent.VK_9:
                    case KeyEvent.VK_NUMPAD9:
                        set(9);
                        Digit.this.transferFocus();
                        break;
                    case KeyEvent.VK_LEFT:
                        Digit.this.transferFocusBackward();
                        break;
                    case KeyEvent.VK_RIGHT:
                        Digit.this.transferFocus();
                        break;
                    case KeyEvent.VK_UP:
                        increment();
                        break;
                    case KeyEvent.VK_DOWN:
                        decrement();
                        break;
                    case KeyEvent.VK_TAB:
                        break;
                    default:
                        set(mValue);
                        break;
                }

                repaint();
            }

            @Override
            public void keyPressed(KeyEvent e) {}
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void mouseClicked(MouseEvent e)
            {
                switch(e.getButton())
                {
                    case MouseEvent.BUTTON1:
                        increment();
                        break;
                    case MouseEvent.BUTTON2:
                        break;
                    case MouseEvent.BUTTON3:
                        decrement();
                        break;
                }
            }

            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e)
            {
                Digit.this.setBackground(mHighlightColor);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                Digit.this.setBackground(Color.WHITE);
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e)
            {
                set(mValue - e.getWheelRotation());
            }
        }
    }
}

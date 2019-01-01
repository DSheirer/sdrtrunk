/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.gui.control;

import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.swing.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Multiple frequency editor control for editing an ordered list of frequencies.  Frequency values are persisted as
 * long values in hertz, but presented to the user as double values in megahertz.
 *
 */
public class MultipleFrequencyEditor extends JPanel
{
    private final static Logger mLog = LoggerFactory.getLogger(MultipleFrequencyEditor.class);
    private static final Long DEFAULT_FREQUENCY = 150000000l;
    private DecimalFormat mDecimalFormat = new DecimalFormat("0.000000");
    private FrequencyModel mFrequencyModel = new FrequencyModel();
    private JTable mFrequencyTable;
    private JButton mAddButton;
    private JButton mRemoveButton;
    private JButton mMoveUpButton;
    private JButton mMoveDownButton;

    /**
     * Constructs the editor
     * @param minimum allowable frequency value
     * @param maximum allowable frequency value
     */
    public MultipleFrequencyEditor(double minimum, double maximum)
    {
        setLayout(new MigLayout("", "[grow,fill][]", "[top,grow,fill][][][][]"));

        mFrequencyTable = new JTable(mFrequencyModel);
        mFrequencyTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mFrequencyTable.getSelectionModel().addListSelectionListener(new SelectionListener());
        mFrequencyTable.setDefaultEditor(Long.class, new FrequencyEditor(minimum, maximum));
        mFrequencyTable.setDefaultRenderer(Long.class, new FrequencyRenderer());
        JScrollPane listScrollPane = new JScrollPane(mFrequencyTable);
        add(listScrollPane, "span 1 5,wrap");

        mAddButton = new JButton();
        mAddButton.setIcon(FontIcon.of(FontAwesome.PLUS));
        mAddButton.addActionListener(e -> {
            int selectedRow = mFrequencyTable.getSelectedRow();
            if(selectedRow >= 0)
            {
                int index = mFrequencyTable.convertRowIndexToModel(selectedRow);
                int nextIndex = index + 1;
                mFrequencyModel.add(DEFAULT_FREQUENCY, nextIndex);
                mFrequencyTable.setRowSelectionInterval(nextIndex, nextIndex);
                mFrequencyTable.editCellAt(nextIndex, 0);
            }
            else
            {
                mFrequencyModel.add(DEFAULT_FREQUENCY);
                int lastIndex = mFrequencyModel.getRowCount() - 1;
                mFrequencyTable.setRowSelectionInterval(lastIndex, lastIndex);
                mFrequencyTable.editCellAt(lastIndex, 0);
            }

            mFrequencyTable.setSurrendersFocusOnKeystroke(true);
            mFrequencyTable.getEditorComponent().requestFocus();

            updateButtons();
        });
        add(mAddButton, "wrap");

        mRemoveButton = new JButton();
        mRemoveButton.setIcon(FontIcon.of(FontAwesome.MINUS));
        mRemoveButton.setEnabled(false);
        mRemoveButton.addActionListener(e -> {
            int selectedRow = mFrequencyTable.getSelectedRow();
            int selectedIndex = mFrequencyTable.convertRowIndexToModel(selectedRow);
            mFrequencyModel.remove(selectedIndex);

            if(selectedIndex < mFrequencyModel.getRowCount())
            {
                int reselectedRow = mFrequencyTable.convertRowIndexToView(selectedIndex);
                mFrequencyTable.setRowSelectionInterval(reselectedRow, reselectedRow);
            }

            updateButtons();
        });
        add(mRemoveButton, "wrap");

        mMoveUpButton = new JButton();
        mMoveUpButton.setIcon(FontIcon.of(FontAwesome.ARROW_UP));
        mMoveUpButton.setEnabled(false);
        mMoveUpButton.addActionListener(e -> {
            int selectedRow = mFrequencyTable.getSelectedRow();
            int index = mFrequencyTable.convertRowIndexToModel(selectedRow);
            mFrequencyModel.moveUp(index);
            mFrequencyTable.getSelectionModel().setLeadSelectionIndex(index - 1);

            updateButtons();
        });
        add(mMoveUpButton, "wrap");

        mMoveDownButton = new JButton();
        mMoveDownButton.setIcon(FontIcon.of(FontAwesome.ARROW_DOWN));
        mMoveDownButton.setEnabled(false);
        mMoveDownButton.addActionListener(e -> {
            int selectedRow = mFrequencyTable.getSelectedRow();
            int index = mFrequencyTable.convertRowIndexToModel(selectedRow);
            mFrequencyModel.moveDown(index);
            mFrequencyTable.getSelectionModel().setLeadSelectionIndex(index + 1);

            updateButtons();
        });
        add(mMoveDownButton);
    }

    private void updateButtons()
    {
        int index = mFrequencyTable.getSelectedRow();

        if(index >= 0)
        {
            mMoveUpButton.setEnabled(index > 0);
            mMoveDownButton.setEnabled(index < mFrequencyModel.getRowCount() - 1);
            mRemoveButton.setEnabled(true);
        }
        else
        {
            mRemoveButton.setEnabled(false);
            mMoveUpButton.setEnabled(false);
            mMoveDownButton.setEnabled(false);
        }
    }

    /**
     * Formats the frequency value as MHz
     */
    private String format(long frequency)
    {
        return mDecimalFormat.format(frequency / 1e6d);
    }

    public class FrequencyModel extends AbstractTableModel
    {
        private List<Long> mFrequencies = new ArrayList<>();

        @Override
        public String getColumnName(int column)
        {
            return "Frequencies (MHz)";
        }

        @Override
        public int getRowCount()
        {
            return mFrequencies.size();
        }

        @Override
        public int getColumnCount()
        {
            return 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            return mFrequencies.get(rowIndex);
        }

        public void setValueAt(int index, Long value)
        {
            mFrequencies.set(index, value);
            fireTableRowsUpdated(index, index);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return true;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return Long.class;
        }

        /**
         * Adds a frequency to the model
         */
        public void add(long frequency, int index)
        {
            mFrequencies.add(index, frequency);
            fireTableRowsInserted(index, index);
        }

        /**
         * Adds a frequency to the model
         */
        public void add(long frequency)
        {
            mFrequencies.add(frequency);
            fireTableRowsInserted(mFrequencies.size() - 1, mFrequencies.size() - 1);
        }

        /**
         * Removes an indexed value from the model
         */
        public Long remove(int index)
        {
            Long value = mFrequencies.remove(index);
            fireTableRowsDeleted(index, index);
            return value;
        }

        /**
         * Moves the frequency up in the list
         */
        public void moveUp(int index)
        {
            if(index > 0 && index < mFrequencies.size())
            {
                Long valueA = mFrequencies.get(index);
                Long valueB = mFrequencies.get(index - 1);
                mFrequencies.set(index, valueB);
                mFrequencies.set(index - 1, valueA);

                fireTableRowsUpdated(index - 1, index);
            }
        }

        /**
         * Moves the frequency down in the list
         */
        public void moveDown(int index)
        {
            if(index >= 0 && index + 1 < mFrequencies.size())
            {
                Long valueA = mFrequencies.get(index);
                Long valueB = mFrequencies.get(index + 1);
                mFrequencies.set(index, valueB);
                mFrequencies.set(index + 1, valueA);

                fireTableRowsUpdated(index, index + 1);
            }
        }

        /**
         * Sets the frequencies for the model
         */
        public void setFrequencies(List<Long> frequencies)
        {
            mFrequencies = frequencies;
            fireTableDataChanged();
        }

        /**
         * List of long frequency values contained in the model
         */
        public List<Long> getFrequencies()
        {
            return mFrequencies;
        }
    }

    public class SelectionListener implements ListSelectionListener
    {
        @Override
        public void valueChanged(ListSelectionEvent event)
        {
            updateButtons();
        }
    }

    public class FrequencyEditor extends DefaultCellEditor
    {
        private JFormattedTextField mTextField;
        private NumberFormat mNumberFormat;
        private Double mMinimum;
        private Double mMaximum;

        public FrequencyEditor(double minValue, double maxValue)
        {
            super(new JFormattedTextField());
            mTextField = (JFormattedTextField)getComponent();
            mMinimum = minValue;
            mMaximum = maxValue;

            mNumberFormat = NumberFormat.getNumberInstance();
            mNumberFormat.setMinimumFractionDigits(1);
            mNumberFormat.setMaximumFractionDigits(6);
            mNumberFormat.setGroupingUsed(false);
            NumberFormatter numberFormatter = new NumberFormatter(mNumberFormat);
            numberFormatter.setFormat(mNumberFormat);
            numberFormatter.setMinimum(mMinimum);
            numberFormatter.setMaximum(mMaximum);
            numberFormatter.setAllowsInvalid(false);

            mTextField.setFormatterFactory(new DefaultFormatterFactory(numberFormatter));
            mTextField.setValue(mMinimum);
            mTextField.setHorizontalAlignment(JTextField.TRAILING);
            mTextField.setFocusLostBehavior(JFormattedTextField.PERSIST);
            mTextField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "check");
            mTextField.getActionMap().put("check", new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent event)
                {
                    //This should always be true since we use formatter.setAllowsInvalid(false) above.
                    if(mTextField.isEditValid())
                    {
                        int selectedRow = mFrequencyTable.getSelectedRow();
                        int selectedIndex = mFrequencyTable.convertRowIndexToModel(selectedRow);

                        try
                        {
                            Double frequencyMHz = Double.parseDouble(mTextField.getText());

                            long frequency = (long)(frequencyMHz * 1e6d);
                            mFrequencyModel.setValueAt(selectedIndex, frequency);
                        }
                        catch(Exception err)
                        {
                            mLog.error("Error parsing frequency: " + mTextField.getText(), err);
                        }

                        fireEditingStopped();
                    }
                }
            });
        }

        //Override to invoke setValue on the formatted text field.
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
        {
            JFormattedTextField ftf = (JFormattedTextField)super.getTableCellEditorComponent(table, value, isSelected, row, column);

            if(value instanceof Long)
            {
                ftf.setValue(((Long)value).doubleValue() / 1e6d);
            }
            else
            {
                ftf.setValue(value);
            }

            ftf.requestFocus();
            ftf.selectAll();
            ftf.setCaretPosition(0);
            return ftf;
        }

        //Override to ensure that the value remains a Double.
        public Object getCellEditorValue()
        {
            JFormattedTextField ftf = (JFormattedTextField)getComponent();
            Object o = ftf.getValue();
            if(o instanceof Double)
            {
                return o;
            }
            else if(o instanceof Number)
            {
                return ((Number)o).doubleValue();
            }
            else
            {
                try
                {
                    return Double.parseDouble(o.toString());
                }
                catch(Exception exc)
                {
                    return new Double(0);
                }
            }
        }
    }

    /**
     * Table cell renderer to display long hertz values as double megahertz values
     */
    public class FrequencyRenderer extends DefaultTableCellRenderer
    {

        public FrequencyRenderer()
        {
            super.setHorizontalAlignment(JLabel.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            JLabel cell = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if(value instanceof Long)
            {
                cell.setText(format((long)value));
            }
            else
            {
                cell.setText("??");
            }

            return cell;
        }
    }

    public static void main(String[] args)
    {
        EventQueue.invokeLater(() -> {
            final JFrame frame = new JFrame();
            frame.setSize(new Dimension(400, 400));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new MultipleFrequencyEditor(1.0, 6000.0));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

package io.github.dsheirer.module.decode.event.filter;

import com.jidesoft.swing.JideButton;
import io.github.dsheirer.filter.FilterEditorPanel;
import io.github.dsheirer.filter.FilterSet;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class EventFilterButton<T> extends JideButton
{
    private static final long serialVersionUID = 1L;

    public EventFilterButton(String dialogTitle, FilterSet<T> filterSet)
    {
        this("Filter", dialogTitle, filterSet);
    }

    public EventFilterButton(String buttonLabel, String dialogTitle, FilterSet<T> filterSet)
    {
        super(buttonLabel);

        addActionListener(
                new EventFilterActionHandler(dialogTitle, filterSet)
        );
    }

    public class EventFilterActionHandler implements ActionListener
    {
        private String title;
        private FilterSet<T> filterSet;

        public EventFilterActionHandler( String title, FilterSet<T> filterSet)
        {
            this.title = title;
            this.filterSet = filterSet;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            final JFrame editor = new JFrame();

            editor.setTitle(title);
            editor.setLocationRelativeTo(EventFilterButton.this);
            editor.setSize(600, 400);
            editor.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            editor.setLayout(new MigLayout("", "[grow,fill]",
                    "[grow,fill][][]"));

            FilterEditorPanel<T> panel = new FilterEditorPanel<T>(filterSet);

            JScrollPane scroller = new JScrollPane(panel);
            scroller.setViewportView(panel);

            editor.add(scroller, "wrap");

            editor.add(new JLabel("Right-click to select/deselect all nodes"), "wrap");

            JButton close = new JButton("Close");
            close.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    editor.dispose();
                }
            });

            editor.add(close);

            EventQueue.invokeLater(new Runnable()
            {
                public void run()
                {
                    editor.setVisible(true);
                }
            });
        }
    }
}
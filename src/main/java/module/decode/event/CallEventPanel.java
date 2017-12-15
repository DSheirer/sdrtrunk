/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package module.decode.event;

import icon.IconManager;
import module.ProcessingChain;
import net.miginfocom.swing.MigLayout;
import sample.Listener;

import javax.swing.*;
import java.awt.*;

public class CallEventPanel extends JPanel implements Listener<ProcessingChain>
{
    private static final long serialVersionUID = 1L;
    private JTable mTable;
    private CallEventModel mEmptyCallEventModel = new CallEventModel();
    private JScrollPane mEmptyScroller;
    private CallEventAliasCellRenderer mRenderer;

    /**
     * View for call event table
     * @param iconManager to display alias icons in table rows
     */
    public CallEventPanel(IconManager iconManager)
    {
        setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill]"));

        mTable = new JTable(mEmptyCallEventModel);
        mTable.setAutoCreateRowSorter(true);
        mTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        mRenderer = new CallEventAliasCellRenderer(iconManager);


        mEmptyScroller = new JScrollPane(mTable);

        add(mEmptyScroller);
    }

    @Override
    public void receive(ProcessingChain processingChain)
    {
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                mTable.setModel(processingChain != null ? processingChain.getCallEventModel() : mEmptyCallEventModel);

                if(processingChain != null)
                {
                    mTable.getColumnModel().getColumn(CallEventModel.FROM_ALIAS).setCellRenderer(mRenderer);
                    mTable.getColumnModel().getColumn(CallEventModel.TO_ALIAS).setCellRenderer(mRenderer);
                }
            }
        });
    }
}

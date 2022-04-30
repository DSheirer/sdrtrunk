package io.github.dsheirer.filter;

import net.miginfocom.swing.MigLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.List;

public class FilterEditorPanel<T> extends JPanel
{
    private static final long serialVersionUID = 1L;

    private JTree mTree;
    private DefaultTreeModel mModel;
    private FilterSet<T> mFilterSet;

    public FilterEditorPanel(FilterSet<T> filterSet)
    {
        setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill]"));

        mFilterSet = filterSet;

        init();
    }

    private void init()
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(mFilterSet);

        mModel = new DefaultTreeModel(root);

        addFilterSet(mFilterSet, root);

        mTree = new JTree(mModel);
        mTree.setShowsRootHandles(true);

        mTree.addMouseListener(new MouseHandler());

        mTree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);

        mTree.setCellRenderer(new EditorTreeCellRenderer());

        mTree.setCellEditor(new FilterTreeNodeEditor());

        mTree.setEditable(true);

        add(mTree);
    }

    private void addFilterSet(FilterSet<T> filterSet, DefaultMutableTreeNode parent)
    {
        List<IFilter<T>> filters = filterSet.getFilters();

        /* sort the filters in alphabetical order by name */
        filters.sort(Comparator.comparing(IFilter::getName));

        for(IFilter<T> filter : filters)
        {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(filter);

            mModel.insertNodeInto(child, parent, parent.getChildCount());

            if(filter instanceof FilterSet)
            {
                addFilterSet((FilterSet<T>) filter, child);
            }
            else if(filter instanceof Filter)
            {
                addFilter((Filter<T>) filter, child);
            }
        }
    }

    private void addFilter(Filter<T> filter, DefaultMutableTreeNode parent)
    {
        List<FilterElement<?>> elements = filter.getFilterElements();

        elements.sort(Comparator.comparing(FilterElement::getName));

        for(FilterElement<?> element : elements)
        {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(element);

            mModel.insertNodeInto(child, parent, parent.getChildCount());
        }
    }

    public class MouseHandler implements MouseListener
    {
        public MouseHandler()
        {
        }

        @Override
        public void mouseClicked(MouseEvent event)
        {
            if(SwingUtilities.isRightMouseButton(event))
            {
                int row = mTree.getRowForLocation(event.getX(),
                        event.getY());

                if(row != -1)
                {
                    mTree.setSelectionRow(row);

                    Object selectedNode =
                            mTree.getLastSelectedPathComponent();

                    if(selectedNode instanceof DefaultMutableTreeNode)
                    {
                        final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) selectedNode;

                        if(node.getChildCount() > 0)
                        {
                            JPopupMenu selectionMenu = new JPopupMenu();

                            JMenuItem selectAll = new JMenuItem("Select All");
                            selectAll.addActionListener(new ActionListener()
                            {
                                @Override
                                public void actionPerformed(ActionEvent arg0)
                                {
                                    setFilterEnabled(mModel, node, true);
                                }
                            });

                            selectionMenu.add(selectAll);

                            JMenuItem deselectAll = new JMenuItem("Deselect All");
                            deselectAll.addActionListener(new ActionListener()
                            {
                                @Override
                                public void actionPerformed(ActionEvent arg0)
                                {
                                    setFilterEnabled(mModel, node, false);
                                }
                            });

                            selectionMenu.add(deselectAll);

                            selectionMenu.show(mTree,
                                    event.getX(),
                                    event.getY());
                        }
                    }
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
        }
    }

    /**
     * Recursively sets the enabled state on all filter node children below the
     * specified parent node.
     *
     * @param model - model containing the tree nodes
     * @param node - parent node
     * @param enabled - true or false to enable or disable the filters
     */
    private void setFilterEnabled(DefaultTreeModel model,
                                  DefaultMutableTreeNode node,
                                  boolean enabled)
    {
        Object obj = node.getUserObject();

        if(obj instanceof FilterSet<?>)
        {
            ((FilterSet<?>) obj).setEnabled(enabled);
        }
        else if(obj instanceof Filter)
        {
            ((Filter<?>) obj).setEnabled(enabled);
        }
        else if(obj instanceof FilterElement)
        {
            ((FilterElement<?>) obj).setEnabled(enabled);
        }

        model.nodeChanged(node);

        /* Recursively set the children of this node */
        Enumeration<?> children = node.children();

        while(children.hasMoreElements())
        {
            Object child = children.nextElement();

            if(child instanceof DefaultMutableTreeNode)
            {
                setFilterEnabled(model, (DefaultMutableTreeNode) child, enabled);
            }
        }
    }

    public class EditorTreeCellRenderer extends DefaultTreeCellRenderer
    {
        private static final long serialVersionUID = 1L;

        public Component getTreeCellRendererComponent(JTree tree,
                                                      Object treeNode,
                                                      boolean selected,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus)
        {
            if(treeNode instanceof DefaultMutableTreeNode)
            {
                Object userObject = ((DefaultMutableTreeNode) treeNode).getUserObject();

                JCheckBox checkBox = null;

                if(userObject instanceof IFilter)
                {
                    IFilter<?> filter = (IFilter<?>) userObject;

                    checkBox = new JCheckBox(filter.getName());
                    checkBox.setSelected(filter.isEnabled());
                }
                else if(userObject instanceof FilterElement)
                {
                    FilterElement<?> element = (FilterElement<?>) userObject;

                    checkBox = new JCheckBox(element.getName());
                    checkBox.setSelected(element.isEnabled());
                }

                if(checkBox != null)
                {
                    if(selected)
                    {
                        checkBox.setForeground(getTextSelectionColor());
                        checkBox.setBackground(getBackgroundSelectionColor());
                    }
                    else
                    {
                        checkBox.setForeground(getTextNonSelectionColor());
                        checkBox.setBackground(getBackgroundNonSelectionColor());
                    }

                    return checkBox;
                }
            }

            return super.getTreeCellRendererComponent(tree, treeNode, selected,
                    expanded, leaf, row, hasFocus);
        }
    }

    public class FilterCheckBox extends JCheckBox
    {
        private static final long serialVersionUID = 1L;

        private IFilter<T> mFilter;

        public FilterCheckBox(IFilter<T> filter)
        {
            super(filter.getName());

            mFilter = filter;

            setSelected(mFilter.isEnabled());

            addItemListener(new ItemListener()
            {
                @Override
                public void itemStateChanged(ItemEvent e)
                {
                    mFilter.setEnabled(FilterCheckBox.this.isSelected());
                }
            });
        }
    }

    public class FilterElementCheckBox extends JCheckBox
    {
        private static final long serialVersionUID = 1L;

        private FilterElement<?> mFilter;

        public FilterElementCheckBox(FilterElement<?> filter)
        {
            super(filter.getName());

            mFilter = filter;

            setSelected(mFilter.isEnabled());

            addItemListener(new ItemListener()
            {
                @Override
                public void itemStateChanged(ItemEvent arg0)
                {
                    mFilter.setEnabled(FilterElementCheckBox.this.isSelected());
                }
            });
        }
    }

    public class FilterTreeNodeEditor implements TreeCellEditor
    {
        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public Component getTreeCellEditorComponent(JTree tree, Object node,
                                                    boolean isSelected, boolean expanded, boolean leaf, int row)
        {
            if(node instanceof DefaultMutableTreeNode)
            {
                Object userObject = ((DefaultMutableTreeNode) node).getUserObject();

                if(userObject instanceof IFilter)
                {
                    return new FilterCheckBox((IFilter) userObject);
                }
                else if(userObject instanceof FilterElement)
                {
                    return new FilterElementCheckBox((FilterElement<?>) userObject);
                }
            }

            return new JLabel(node.toString());
        }

        @Override
        public void addCellEditorListener(CellEditorListener l)
        {
        }

        @Override
        public void cancelCellEditing()
        {

        }

        @Override
        public Object getCellEditorValue()
        {
            return null;
        }

        @Override
        public boolean isCellEditable(EventObject anEvent)
        {
            return true;
        }

        @Override
        public void removeCellEditorListener(CellEditorListener l)
        {
        }

        @Override
        public boolean shouldSelectCell(EventObject anEvent)
        {
            return false;
        }

        @Override
        public boolean stopCellEditing()
        {
            return false;
        }
    }
}

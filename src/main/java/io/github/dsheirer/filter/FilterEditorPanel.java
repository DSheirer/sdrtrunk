/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.filter;

import java.awt.Component;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.List;
import net.miginfocom.swing.MigLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.CellEditorListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;

/**
 * Editor panel for managing the state of a filter set.
 *
 * @param <T> element type for the filter set.
 */
public class FilterEditorPanel<T> extends JPanel
{
    private static final long serialVersionUID = 1L;
    private JTree mTree;
    private DefaultTreeModel mModel;
    private FilterSet<T> mFilterSet;

    /**
     * Constructs an instance
     * @param filterSet to manage
     */
    public FilterEditorPanel(FilterSet<T> filterSet)
    {
        setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill]"));
        mFilterSet = filterSet;
        init();
    }

    /**
     * Initializes the panel and the tree model.
     */
    private void init()
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(mFilterSet);
        mModel = new DefaultTreeModel(root);
        addFilterSet(mFilterSet, root);

        mTree = new JTree(mModel);
        mTree.setShowsRootHandles(true);
        mTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        mTree.setCellRenderer(new EditorTreeCellRenderer());
        mTree.setCellEditor(new FilterTreeCellEditor());
        mTree.setEditable(true);
        add(mTree);
    }

    /**
     * Updates the filter set for this editor panel
     * @param filterSet to use
     */
    public void updateFilterSet(FilterSet<T> filterSet)
    {
        remove(mTree);
        mFilterSet = filterSet;
        init();
        revalidate();
    }

    /**
     * Adds the filter set as a child tree node to the parent.
     * @param filterSet to add to the tree
     * @param parent node for the filter set child tree node.
     */
    private void addFilterSet(FilterSet<T> filterSet, DefaultMutableTreeNode parent)
    {
        List<IFilter<T>> filters = filterSet.getFilters();

        for(IFilter<T> filter : filters)
        {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(filter);
            mModel.insertNodeInto(childNode, parent, parent.getChildCount());

            if(filter instanceof FilterSet childFilterSet)
            {
                addFilterSet(childFilterSet, childNode);
            }
            else if(filter instanceof Filter childFilter)
            {
                addFilter(childFilter, childNode);
            }
        }
    }

    /**
     * Adds the filter as a child tree node to the parent.
     *
     * @param filter to add
     * @param parent tree node
     */
    private void addFilter(Filter filter, DefaultMutableTreeNode parent)
    {
        List<FilterElement<?>> elements = filter.getFilterElements();

        elements.sort(Comparator.comparing(FilterElement::getName));

        for(FilterElement<?> element : elements)
        {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(element);
            mModel.insertNodeInto(child, parent, parent.getChildCount());
        }
    }

    /**
     * Accesses the tree's root tree node.
     * @return root node.
     */
    private DefaultMutableTreeNode getRoot()
    {
        if(mTree.getModel().getRoot() instanceof DefaultMutableTreeNode root)
        {
            return root;
        }

        return null;
    }

    /**
     * Recursively sets the enabled state on all filter node children below the specified parent node.
     *
     * @param model - model containing the tree nodes
     * @param node - parent node
     * @param enabled - true or false to enable or disable the filters
     */
    private void setFilterEnabled(DefaultTreeModel model, DefaultMutableTreeNode node, boolean enabled)
    {
        Object obj = node.getUserObject();

        if(obj instanceof FilterElement filterElement)
        {
            filterElement.setEnabled(enabled);
        }

        /* Recursively set the children of this node */
        Enumeration<?> children = node.children();

        while(children.hasMoreElements())
        {
            Object child = children.nextElement();

            if(child instanceof DefaultMutableTreeNode childNode)
            {
                setFilterEnabled(model, childNode, enabled);
            }
        }

        model.nodeChanged(node);
    }

    /**
     * Recursively searches the tree to find the tree node containing the specified filter element.
     *
     * @param node to start search
     * @param element to find
     */
    private DefaultMutableTreeNode findFilterElementNode(DefaultMutableTreeNode node, FilterElement<?> element)
    {
        if(node != null)
        {
            if(node.getUserObject() instanceof FilterElement<?> selfFilterElement && selfFilterElement.equals(element))
            {
                return node;
            }

            for(int x = 0; x < node.getChildCount(); x++)
            {
                if(node.getChildAt(x) instanceof DefaultMutableTreeNode child)
                {
                    DefaultMutableTreeNode found = findFilterElementNode(child, element);

                    if(found != null)
                    {
                        return found;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Updates the display value for the specified node and all parents in the nodes parentage.
     * @param node to update
     */
    private void updateParentage(DefaultMutableTreeNode node)
    {
        mModel.nodeChanged(node);

        TreeNode parent = node.getParent();

        while(parent != null)
        {
            mModel.nodeChanged(parent);
            parent = parent.getParent();
        }
    }

    /**
     * Recursively searches the tree to find the tree node containing the specified filter.
     *
     * @param node to start search
     * @param filter to find
     */
    private DefaultMutableTreeNode findFilterNode(DefaultMutableTreeNode node, IFilter<?> filter)
    {
        if(node != null)
        {
            if(node.getUserObject() instanceof IFilter<?> selfFilter && selfFilter.equals(filter))
            {
                return node;
            }

            for(int x = 0; x < node.getChildCount(); x++)
            {
                if(node.getChildAt(x) instanceof DefaultMutableTreeNode child)
                {
                    DefaultMutableTreeNode found = findFilterNode(child, filter);

                    if(found != null)
                    {
                        return found;
                    }
                }
            }
        }
        else
        {
            System.out.println("Can't evaluate - node is null");
        }

        return null;
    }

    /**
     * Custom cell renderer
     */
    public class EditorTreeCellRenderer extends DefaultTreeCellRenderer
    {
        private static final long serialVersionUID = 1L;

        public Component getTreeCellRendererComponent(JTree tree, Object treeNode, boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus)
        {
            if(treeNode instanceof DefaultMutableTreeNode)
            {
                Object userObject = ((DefaultMutableTreeNode) treeNode).getUserObject();
                JCheckBox checkBox = null;

                if(userObject instanceof IFilter filter)
                {
                    checkBox = new FilterCheckBox(filter);
                    checkBox.setSelected(filter.isEnabled());
                }
                else if(userObject instanceof FilterElement element)
                {
                    checkBox = new FilterElementCheckBox(element);
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

            return super.getTreeCellRendererComponent(tree, treeNode, selected, expanded, leaf, row, hasFocus);
        }
    }

    /**
     * Check box used in filter tree nodes
     */
    public class FilterCheckBox extends JCheckBox
    {
        private static final long serialVersionUID = 1L;
        private IFilter<?> mFilter;

        /**
         * Constructs an instance
         *
         * @param filter
         */
        public FilterCheckBox(IFilter<?> filter)
        {
            mFilter = filter;
            setSelected(mFilter.isEnabled());
            addItemListener(e -> {
                DefaultMutableTreeNode selfNode = findFilterNode(getRoot(), mFilter);

                if(selfNode != null)
                {
                    setFilterEnabled(mModel, selfNode, FilterCheckBox.this.isSelected());
                    updateLabel();
                    updateParentage(selfNode);
                }
            });
        }

        @Override
        public void setSelected(boolean b)
        {
            super.setSelected(b);
            updateLabel();
        }

        public void updateLabel()
        {
            setText(mFilter.getName() + " (" + mFilter.getEnabledCount() + "/" + mFilter.getElementCount() + ")");
        }
    }

    /**
     * Check box used in filter element tree nodes
     */
    public class FilterElementCheckBox extends JCheckBox
    {
        private static final long serialVersionUID = 1L;
        private FilterElement<?> mFilter;

        /**
         * Constructs an instance
         *
         * @param filter element
         */
        public FilterElementCheckBox(FilterElement<?> filter)
        {
            super(filter.getName());
            mFilter = filter;
            setSelected(mFilter.isEnabled());
            addItemListener(arg0 -> {
                mFilter.setEnabled(FilterElementCheckBox.this.isSelected());
                DefaultMutableTreeNode self = findFilterElementNode(getRoot(), mFilter);
                if(self != null)
                {
                    updateParentage(self);
                }
            });
        }
    }

    /**
     * Editor for filter tree nodes
     */
    public class FilterTreeCellEditor implements TreeCellEditor
    {
        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object node, boolean isSelected, boolean expanded, boolean leaf, int row)
        {
            if(node instanceof DefaultMutableTreeNode mutableTreeNode)
            {
                Object userObject = mutableTreeNode.getUserObject();

                if(userObject instanceof IFilter filter)
                {
                    return new FilterCheckBox(filter);
                }
                else if(userObject instanceof FilterElement filterElement)
                {
                    return new FilterElementCheckBox(filterElement);
                }
            }
            return new JLabel(node.toString());
        }

        @Override
        public void addCellEditorListener(CellEditorListener l) {}

        @Override
        public void cancelCellEditing() {}

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
        public void removeCellEditorListener(CellEditorListener l) {}

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

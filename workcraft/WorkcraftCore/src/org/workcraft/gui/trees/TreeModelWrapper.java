package org.workcraft.gui.trees;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TreeModelWrapper<T> implements TreeModel {

    private final TreeSource<T> source;
    private final Map<TreeModelListener, TreeListenerWrapper<T>> listeners = new HashMap<>();

    TreeModelWrapper(TreeSource<T> source) {
        this.source = source;
    }

    @Override
    public void addTreeModelListener(final TreeModelListener l) {
        source.addListener(wrap(l));
    }

    private TreeListenerWrapper<T> wrap(final TreeModelListener l) {
        return listeners.computeIfAbsent(l, TreeListenerWrapper::new);
    }

    @Override
    public Object getChild(Object parent, int index) {
        Object result = null;
        List<T> children = source.getChildren(cast(parent));
        if (index < children.size()) {
            result = children.get(index);
        }
        return result;
    }

    @Override
    public int getChildCount(Object parent) {
        return getChildren(parent).size();
    }

    private List<T> getChildren(Object parent) {
        return source.getChildren(cast(parent));
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return getChildren(parent).indexOf(child);
    }

    @Override
    public Object getRoot() {
        return source.getRoot();
    }

    @Override
    public boolean isLeaf(Object node) {
        return source.isLeaf(cast(node));
    }

    @SuppressWarnings("unchecked")
    private T cast(Object node) {
        return (T) node;
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        source.removeListener(wrap(l));
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        throw new org.workcraft.exceptions.NotSupportedException();
    }

}

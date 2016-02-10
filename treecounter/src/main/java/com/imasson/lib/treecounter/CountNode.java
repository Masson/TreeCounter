package com.imasson.lib.treecounter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * 用于表示统计数量的节点树上的一个节点，含有节点ID、节点数量
 *
 * @author xinteng.mxt@alibaba-inc.com
 */
@SuppressWarnings("unused")
public final class CountNode {

    private String id;
    private CountNode parent;
    private CountNode[] children;
    private ICountFetcher countFetcher;
    private TouchNodePolicy touchNodePolicy;

    private int count = 0;
    private boolean bypassed = false;

    protected ICountFetcher getCountFetcher() {
        return countFetcher;
    }

    protected TouchNodePolicy getTouchNodePolicy() {
        return touchNodePolicy;
    }

    @Nullable
    protected CountNode getParent() {
        return parent;
    }

    @NonNull
    protected CountNode[] getChildren() {
        return children;
    }

    @NonNull
    public String getId() {
        return id;
    }

    /**
     * 获取该节点上的真实数量值
     * @return 数量值
     */
    public int getCount() {
        return count;
    }

    public boolean isBypassed() {
        return bypassed;
    }

    public void setBypassed(boolean bypassed) {
        this.bypassed = bypassed;
    }

    public CountNode(@NonNull String id) {
        this(id, null, null, null);
    }

    public CountNode(@NonNull String id, CountNode[] children) {
        this(id, children, null, null);
    }

    public CountNode(@NonNull String id, ICountFetcher fetcher) {
        this(id, null, fetcher, null);
    }

    public CountNode(@NonNull String id, ICountFetcher fetcher, TouchNodePolicy policy) {
        this(id, null, fetcher, policy);
    }

    public CountNode(@NonNull String id, CountNode[] children, TouchNodePolicy policy) {
        this(id, children, null, policy);
    }

    private CountNode(@NonNull String id, CountNode[] children, ICountFetcher fetcher, TouchNodePolicy policy) {
        this.id = id;

        if (children != null) {
            int totalCount = 0;
            for (CountNode child : children) {
                child.parent = this;
                totalCount += child.getDisplayCount();
            }
            this.count = totalCount;
            this.children = children;
        } else {
            this.count = 0;
            this.children = new CountNode[0];
        }

        this.countFetcher = fetcher;
        this.touchNodePolicy = (policy != null) ? policy : TreeCounter.DoNothingPolicy;
    }


    /**
     * 获取用于显示的数量值，当设置了bypass会返回0
     * @return 数量值
     */
    public final int getDisplayCount() {
        return bypassed ? 0 : count;
    }

    /**
     * 是否为叶子节点
     */
    public final boolean isLeaf() {
        return children.length == 0;
    }

    public final int getDepth() {
        if (getParent() != null){
            return getParent().getDepth() + 1;
        } else {
            return 0;
        }
    }

    /**
     * 遍历以此节点为根的整棵树的每个节点，分支节点和叶子节点，但不包括自己
     * @param handler 节点处理器
     * @return 是否要终止迭代
     */
    public final boolean iterateDescendant(@NonNull CountNodeHandler handler) {
        for (CountNode child : children) {
            if (handler.handleCountNode(child)) {
                return true;
            }
            if (child.iterateDescendant(handler)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 遍历此节点的所有父节点
     * @param handler 节点处理
     * @return 是否要终止迭代
     */
    public final boolean iterateAscendant(@NonNull CountNodeHandler handler) {
        if (this.parent != null) {
            if (handler.handleCountNode(this.parent)) {
                return true;
            }
            if (this.parent.iterateAscendant(handler)) {
                return true;
            }
        }
        return false;
    }


    protected void applyCount(int newCount) {
        this.count = newCount >= 0 ? newCount : 0;

        if (countFetcher != null) {
            countFetcher.updateCount(newCount);
        }
    }

    protected int computeCount() {
        int totalCount = 0;
        for (CountNode child : children) {
            totalCount += child.getDisplayCount();
        }
        applyCount(totalCount);
        return totalCount;
    }

    protected void addNode(@NonNull CountNode node) {
        CountNode[] newChildren = new CountNode[children.length + 1];
        System.arraycopy(children, 0, newChildren, 0, children.length);
        newChildren[children.length] = node;
        node.parent = this;

        children = newChildren;
    }

    protected boolean removeNode(@NonNull CountNode node) {
        int indexToRemove = -1;
        for (int i = 0; i < children.length; i++) {
            if (children[i] == node) {
                indexToRemove = i;
                break;
            }
        }

        boolean hasRemoved = false;
        if (indexToRemove >= 0) {
            CountNode[] newChildren = new CountNode[children.length - 1];
            for (int i = 0; i < children.length - 1; i++) {
                int originIndex = i >= indexToRemove ? i + 1 : i;
                newChildren[i] = children[originIndex];
            }
            children = newChildren;
            hasRemoved = true;
        } else {
            for (CountNode child : children) {
                if (child.removeNode(node)) {
                    hasRemoved = true;
                    break;
                }
            }
        }

        return hasRemoved;
    }

    public String getDebugInfo() {
        return id
                + " " + String.valueOf(getDisplayCount())
                + " (" + String.valueOf(count) + ")"
                + " [" + (
                    getTouchNodePolicy() == TreeCounter.BypassPolicy ? "BypassPolicy" :
                    getTouchNodePolicy() == TreeCounter.DoNothingPolicy ? "DoNothingPolicy" :
                    getTouchNodePolicy() == TreeCounter.ZeroSelfPolicy ? "ZeroSelfPolicy" :
                    getTouchNodePolicy() == TreeCounter.ZeroDescendantPolicy ? "ZeroDescendantPolicy" : "CustomPolicy"
                    )
                + "]"
                + (isBypassed() ? "[Bypassed]" : "")
                + (isLeaf() ? "[Leaf]" : "[Children: " + children.length + "]")
                ;
    }
}

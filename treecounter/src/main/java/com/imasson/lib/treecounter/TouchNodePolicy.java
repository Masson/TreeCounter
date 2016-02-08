package com.imasson.lib.treecounter;

/**
 * 在节点碰触时处理节点的策略，但策略中不需要进行对节点的值的修改
 *
 * @author xinteng.mxt@alibaba-inc.com
 */
public interface TouchNodePolicy {

    /**
     * 当碰触节点时会被{@link TreeCounter}调用的方法，在这里会根据所需的策略更新节点树
     * @param counter 统计器，即节点所在的树
     * @param node 当前碰触的节点
     */
    void touchNode(TreeCounter counter, CountNode node);

    /**
     * 当节点的值更新之后会被{@link TreeCounter}调用的方法，在这里会根据所需的策略更新节点树
     * @param counter 统计器，即节点所在的树
     * @param node 当前碰触的节点
     * @param isReloadOperation 是否为重新加载的操作
     */
    void afterNodeApplyCount(TreeCounter counter, CountNode node, boolean isReloadOperation);
}

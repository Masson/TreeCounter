package com.imasson.lib.treecounter;

import android.support.annotation.NonNull;

/**
 * 用于针对某个节点进行处理的接口
 *
 * @author xinteng.mxt@alibaba-inc.com
 */
public interface CountNodeHandler {

    /**
     * 在这个方法中执行针对单个节点的处理
     * @param node 要处理的节点
     * @return 是否需要继续，用于终止迭代
     */
    boolean handleCountNode(@NonNull CountNode node);
}

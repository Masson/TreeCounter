package com.imasson.lib.treecounter;

/**
 * 用于获取和更新节点数量数据源的接口，通常作为Counter和Model的桥梁
 *
 * @author xinteng.mxt@alibaba-inc.com
 */
public interface ICountFetcher {

    /**
     * 从数据源获取数量
     */
    void loadCount(LoadCountCallback callback);

    /**
     * 从数据源更新数量
     */
    void updateCount(int count);

    interface LoadCountCallback {
        void onLoadCount(ICountFetcher fetcher, int count);
    }
}

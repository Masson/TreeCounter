package com.imasson.lib.treecounter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;


/**
 * <p>未读数量统计器，用于管理一棵节点树的未读数量，
 * 常用于多层级界面中，显示不同类型消息的未读数量及这些消息的未读数量合计（可参考微信的未读数量小圆点）。</p>
 *
 *
 *
 * @author xinteng.mxt@alibaba-inc.com
 */
@SuppressWarnings("unused")
public class TreeCounter {

    private CountNode mRootNode;
    private ConcurrentHashMap<String, CountNode> mCountNodeMap = new ConcurrentHashMap<>();
    private OnNodeUnreadCountChangeListener mListener;
    private PreferenceHandler mPreferenceHandler;

    public CountNode getRootNode() {
        return mRootNode;
    }

    public void setListener(OnNodeUnreadCountChangeListener l) {
        this.mListener = l;
    }

    public PreferenceHandler getPreferenceHandler() {
        return mPreferenceHandler;
    }


    private static final String LOG_TAG = "TreeCounter";
    private boolean mDebuggable = false;

    public void setDebuggable(boolean debuggable) {
        this.mDebuggable = debuggable;
    }


    public TreeCounter(@NonNull CountNode rootNode) {
        this(rootNode, null);
    }

    public TreeCounter(@NonNull CountNode rootNode, @Nullable PreferenceHandler preferenceHandler) {
        //noinspection ConstantConditions
        if (rootNode == null) {
            throw new IllegalArgumentException("Argument 'rootNode' should not be null!");
        }
        mRootNode = rootNode;
        mPreferenceHandler = preferenceHandler;
        buildNodeMap();
    }

    private void buildNodeMap() {
        mCountNodeMap.clear();
        mCountNodeMap.put(mRootNode.getId(), mRootNode);
        mRootNode.iterateDescendant(new CountNodeHandler() {
            @Override
            public boolean handleCountNode(@NonNull CountNode node) {
                mCountNodeMap.put(node.getId(), node);
                return false;
            }
        });
    }


    public void addNode(@NonNull CountNode node) {
        addNode(mRootNode.getId(), node);
    }

    public void addNode(@NonNull String parentId, @NonNull CountNode node) {
        //noinspection ConstantConditions
        if (parentId == null) return;
        //noinspection ConstantConditions
        if (node == null) return;

        final CountNode countNode = mCountNodeMap.get(parentId);
        if (countNode == null) {
            if (mDebuggable) Log.w(LOG_TAG, "Cannot find the parent node [id=" + parentId + "] to attach");
            return;
        }

        countNode.addNode(node);
        mCountNodeMap.put(node.getId(), node);
        if (!node.isLeaf()) {
            node.iterateDescendant(new CountNodeHandler() {
                @Override
                public boolean handleCountNode(@NonNull CountNode node) {
                    mCountNodeMap.put(node.getId(), node);
                    return false;
                }
            });
        }

        // TODO 改为局部刷新，提高性能
        reload();
    }

    public void removeNode(@NonNull String id) {
        //noinspection ConstantConditions
        if (id == null) return;

        if (id.equals(mRootNode.getId())) {
            if (mDebuggable) Log.w(LOG_TAG, "Cannot remove the root node!");
            return;
        }

        final CountNode countNode = mCountNodeMap.get(id);
        if (countNode == null) {
            if (mDebuggable) Log.w(LOG_TAG, "Cannot find the node [id=" + id + "] to attach.");
            return;
        }

        if (countNode.getParent() == null) {
            return;
        }

        countNode.getParent().removeNode(countNode);

        // TODO 改为局部刷新，提高性能
        reload();
    }

    @Nullable
    public CountNode findNode(@NonNull String id) {
        //noinspection ConstantConditions
        if (id == null) return null;

        return mCountNodeMap.get(id);
    }

    public void touchNode(@NonNull String id) {
        //noinspection ConstantConditions
        if (id == null) return;

        final CountNode countNode = mCountNodeMap.get(id);
        if (countNode == null) {
            if (mDebuggable) Log.w(LOG_TAG, "Cannot find the node [id=" + id + "] to touch.");
            return;
        }

        final TouchNodePolicy touchNodePolicy = countNode.getTouchNodePolicy();
        touchNodePolicy.touchNode(this, countNode);
    }

    public void reload() {
        if (mRootNode.isLeaf()) {
            // 根节点就是叶子，这是一种特殊情况，整个tree就只有一个节点，这个时候只需要加载这个节点就可以了。
            fetchNodeCount(mRootNode);
        } else {
            // 遍历根节点下的所有是叶子的节点，并执行加载。
            // 值得注意的是每次加载节点完成后，都会主动刷新该节点的所有父辈节点，所以一个父辈节点可能会被触发更新多次。
            mRootNode.iterateDescendant(new CountNodeHandler() {
                @Override
                public boolean handleCountNode(@NonNull CountNode node) {
                    if (node.isLeaf()) {
                        fetchNodeCount(node);
                    }
                    return false;
                }
            });
        }
    }

    private void fetchNodeCount(final CountNode node) {
        final ICountFetcher fetcher = node.getCountFetcher();
        if (fetcher != null) {
            fetcher.loadCount(new ICountFetcher.LoadCountCallback() {
                @Override
                public void onLoadCount(ICountFetcher fetcher, int count) {
                    applyCount(node, count, true);
                }
            });
        }
    }

    public int getCount() {
        return mRootNode.getDisplayCount();
    }

    public int getCount(String id) {
        final CountNode countNode = mCountNodeMap.get(id);
        if (countNode == null) {
            if (mDebuggable) Log.w(LOG_TAG, "Cannot find the node [id=" + id + "] to attach.");
            return 0;
        }
        return countNode.getDisplayCount();
    }

    public void addCount(String id, final int delta) {
        final CountNode countNode = mCountNodeMap.get(id);
        if (countNode == null) {
            if (mDebuggable) Log.w(LOG_TAG, "Cannot find the node [id=" + id + "] to addCount.");
            return;
        }

        if (!countNode.isLeaf()) {
            if (mDebuggable) Log.w(LOG_TAG, "Cannot add count to a CountNode that is not a leaf in node tree.");
            return;
        }

        if (delta == 0) {
            return;
        }

        applyCount(countNode, countNode.getCount() + delta, false);
    }

    public void reduceCount(String id, final int delta) {
        final CountNode countNode = mCountNodeMap.get(id);
        if (countNode == null) {
            if (mDebuggable) Log.w(LOG_TAG, "Cannot find the node [id=" + id + "] to reduceCount.");
            return;
        }

        if (!countNode.isLeaf()) {
            if (mDebuggable) Log.w(LOG_TAG, "Cannot remove count to a CountNode that is not a leaf in node tree.");
            return;
        }

        if (delta == 0) {
            return;
        }

        applyCount(countNode, countNode.getCount() - delta, false);
    }

    public void setBypass(String id, boolean bypassed) {
        final CountNode countNode = mCountNodeMap.get(id);
        if (countNode == null) {
            if (mDebuggable) Log.w(LOG_TAG, "Cannot find the node [id=" + id + "] to reduceCount.");
            return;
        }

        setBypass(countNode, bypassed);
    }

    private void setBypass(CountNode countNode, boolean bypassed) {
        if (countNode.isBypassed() != bypassed) {
            countNode.setBypassed(bypassed);
            countNode.iterateAscendant(new CountNodeHandler() {
                @Override
                public boolean handleCountNode(@NonNull CountNode node) {
                    node.computeCount();
                    return false;
                }
            });
            notifyCountChange(countNode);
        }
    }

    public void applyCount(String id, int count) {
        final CountNode countNode = mCountNodeMap.get(id);
        if (countNode == null) {
            if (mDebuggable) Log.w(LOG_TAG, "Cannot find the node [id=" + id + "] to reduceCount.");
            return;
        }

        if (!countNode.isLeaf()) {
            if (mDebuggable) Log.w(LOG_TAG, "Cannot remove count to a CountNode that is not a leaf in node tree.");
            return;
        }

        applyCount(countNode, count, false);
    }

    private void applyCount(CountNode countNode, final int count, final boolean isReloadOperation) {
        if (countNode.getCount() == count) {
            return;
        }

        countNode.applyCount(count);
        countNode.iterateAscendant(new CountNodeHandler() {
            @Override
            public boolean handleCountNode(@NonNull CountNode node) {
                node.computeCount();
                triggerAfterNodeAfterCount(node, isReloadOperation);
                return false;
            }
        });
        notifyCountChange(countNode);
    }

    private void triggerAfterNodeAfterCount(@NonNull CountNode node, boolean isReloadOperation) {
        final TouchNodePolicy touchNodePolicy = node.getTouchNodePolicy();
        if (touchNodePolicy != null) {
            touchNodePolicy.afterNodeApplyCount(TreeCounter.this, node, isReloadOperation);
        }
    }

    private void notifyCountChange(CountNode countNode) {
        if (mListener != null) {
            mListener.onNodeUnreadCountChanged(countNode);
            if (countNode.getParent() != null) {
                notifyCountChange(countNode.getParent());
            }
        }
    }

    private void updateNodeBypassed(String id, boolean bypassed) {
        if (mPreferenceHandler != null) {
            mPreferenceHandler.setConfigValue(id, bypassed);
        }
    }

    private boolean loadNodeBypassed(String id) {
        if (mPreferenceHandler != null) {
            return mPreferenceHandler.getBooleanConfigValue(id);
        }
        return false;
    }


    /**
     * 监听节点的未读数量变化的监听器接口
     */
    public interface OnNodeUnreadCountChangeListener {
        void onNodeUnreadCountChanged(CountNode node);
    }

    /**
     * 用于进行配置获取和更新的接口
     */
    public interface PreferenceHandler {
        boolean getBooleanConfigValue(String key);
        void setConfigValue(String key, boolean value);
        int getIntegerConfigValue(String key);
        void setConfigValue(String key, int value);
    }


    /**
     * <p>节点碰触策略：旁路策略</p>
     * <p>使用旁路策略，当节点触发时，节点的数量值不会被清零，但会设置旁路标志，
     * 使DisplayCount变为0，但Count不变，并且会触发其所有父节点按照DisplayCount重新计算数量值。
     * 当节点数量发生变化时（重加载操作不影响）会使旁路标记被清除。</p>
     * <p><b>常用场景：</b>父节点</p>
     * <p><b>注意：</b>使用该策略，必须设置{@link PreferenceHandler}，否则下次启动初始化时将无法恢复旁路设置。</p>
     */
    public static final TouchNodePolicy BypassPolicy = new TouchNodePolicy() {
        @Override
        public void touchNode(TreeCounter counter, CountNode node) {
            if (!node.isBypassed()) {
                counter.setBypass(node, true);
                counter.updateNodeBypassed(node.getId(), true);
            }
        }

        @Override
        public void afterNodeApplyCount(TreeCounter counter, CountNode node, boolean isReloadOperation) {
            if (isReloadOperation) {
                final boolean bypassed = counter.loadNodeBypassed(node.getId());
                if (node.isBypassed() != bypassed) {
                    node.setBypassed(true);
                }
            } else {
                if (node.isBypassed()) {
                    node.setBypassed(false);
                    counter.updateNodeBypassed(node.getId(), false);
                }
            }
        }
    };

    /**
     * <p>节点碰触策略：不处理策略</p>
     * <p>碰触节点后不更新节点的数量值。</p>
     * <p><b>常用场景：</b>叶子节点，且希望节点数量值完全手动控制</p>
     */
    public static final TouchNodePolicy DoNothingPolicy = new TouchNodePolicy() {
        @Override
        public void touchNode(TreeCounter counter, CountNode node) {
            // Do nothing.
        }

        @Override
        public void afterNodeApplyCount(TreeCounter counter, CountNode node, boolean isReloadOperation) {
            // Do nothing.
        }
    };

    /**
     * <p>节点碰触策略：自身清零策略</p>
     * <p>碰触节点后将自身的Count重置为0，同时会触发其所有父节点重新计算数量值。</p>
     * <p><b>常用场景：</b>叶子节点，认为用户进入即当成已读的界面上的未读消息数量</p>
     */
    public static final TouchNodePolicy ZeroSelfPolicy = new TouchNodePolicy() {
        @Override
        public void touchNode(TreeCounter counter, CountNode node) {
            counter.applyCount(node, 0, false);
        }

        @Override
        public void afterNodeApplyCount(TreeCounter counter, CountNode node, boolean isReloadOperation) {
            // Do nothing.
        }
    };

    /**
     * <p>节点碰触策略：自身清零策略</p>
     * <p>碰触节点后将其所有子节点的count重置为0，同时会触发其所有父节点重新计算数量值。</p>
     * <p><b>常用场景：</b>作为一个父节点，但在界面中仅当叶子节点来对待，其所有子节点仅用于方便统计不同种类的消息数量</p>
     * <p><b>注意：</b>该节点的所有子节点的碰触策略不能为{@link #BypassPolicy}</p>
     */
    public static final TouchNodePolicy ZeroDescendantPolicy = new TouchNodePolicy() {
        @Override
        public void touchNode(final TreeCounter counter, CountNode node) {
            node.iterateDescendant(new CountNodeHandler() {
                @Override
                public boolean handleCountNode(@NonNull CountNode node) {
                    node.applyCount(0);
                    counter.notifyCountChange(node);
                    return false;
                }
            });
            counter.applyCount(node, 0, false);
        }

        @Override
        public void afterNodeApplyCount(TreeCounter counter, CountNode node, boolean isReloadOperation) {
            // Do nothing.
        }
    };
}

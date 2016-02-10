package com.imasson.demo.treecount;

import com.imasson.lib.treecounter.CountNode;
import com.imasson.lib.treecounter.ICountFetcher;
import com.imasson.lib.treecounter.TreeCounter;

import java.util.Random;

/**
 * 作为业务层的一部分，包装了{@link TreeCounter}的实例
 */
public class TreeCountWrapper {

    private static Random sRandom = new Random();

    private class RandomCountFetcher implements ICountFetcher {
        private int mPersistedValue = -1;

        @Override
        public void loadCount(LoadCountCallback callback) {
            if (mPersistedValue == -1) {
                mPersistedValue = TreeCountWrapper.sRandom.nextInt(10);
            }
            callback.onLoadCount(this, mPersistedValue);
        }

        @Override
        public void updateCount(int count) {
            mPersistedValue = count;
        }
    }

    private TreeCounter mDemoTreeCounter;

    public TreeCountWrapper() {
        init();
    }

    public void init() {
        mDemoTreeCounter = new TreeCounter(
                new CountNode("node-root", new CountNode[] {
                        new CountNode("node-single-leaf", new RandomCountFetcher(), TreeCounter.ZeroSelfPolicy),
                        new CountNode("node-parent-bypass", new CountNode[] {
                                new CountNode("node-child-1", new RandomCountFetcher(), TreeCounter.ZeroSelfPolicy),
                                new CountNode("node-child-2", new RandomCountFetcher(), TreeCounter.ZeroSelfPolicy),
                                new CountNode("node-child-3", new RandomCountFetcher(), TreeCounter.ZeroSelfPolicy)
                        }, TreeCounter.BypassPolicy),
                        new CountNode("node-parent-zero-descendant", new CountNode[] {
                                new CountNode("node-child-4", new RandomCountFetcher(), TreeCounter.DoNothingPolicy),
                                new CountNode("node-child-5", new RandomCountFetcher(), TreeCounter.DoNothingPolicy)
                        }, TreeCounter.ZeroDescendantPolicy)
                }, TreeCounter.DoNothingPolicy));
        mDemoTreeCounter.reload();
    }

    public TreeCounter getTreeCounter() {
        return mDemoTreeCounter;
    }
}

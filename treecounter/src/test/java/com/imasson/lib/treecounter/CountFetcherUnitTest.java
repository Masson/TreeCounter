package com.imasson.lib.treecounter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 关于加载和保存节点数量值的逻辑测试
 * @see ICountFetcher
 */
public class CountFetcherUnitTest {

    /*
     * Test tree graph:
     * -------------------------------------------
     *    IdTestNodeRoot
     *     ├─ IdTestLeafNode_1
     *     └─ IdTestLeafNode_2
     * -------------------------------------------
     */

    private static final String IdTestNodeRoot      = "testNodeRoot";
    private static final String IdTestLeafNode_1    = "testLeafNode_1";
    private static final String IdTestLeafNode_2    = "testLeafNode_2";

    public static final int COUNT_INIT_NODE_1 = 3;
    public static final int COUNT_INIT_NODE_2 = 5;

    private TreeCounter treeCounter;
    private int fakePersistingCount1 = COUNT_INIT_NODE_1;
    private int fakePersistingCount2 = COUNT_INIT_NODE_2;

    private ICountFetcher countFetcher1 = new ICountFetcher() {

        @Override
        public void loadCount(LoadCountCallback callback) {
            callback.onLoadCount(this, fakePersistingCount1);
        }

        @Override
        public void updateCount(int count) {
            fakePersistingCount1 = count;
        }
    };

    private ICountFetcher countFetcher2 = new ICountFetcher() {

        @Override
        public void loadCount(LoadCountCallback callback) {
            callback.onLoadCount(this, fakePersistingCount2);
        }

        @Override
        public void updateCount(int count) {
            fakePersistingCount2 = count;
        }
    };

    @Before
    public void setup() throws Exception {
        treeCounter = new TreeCounter(
                new CountNode(IdTestNodeRoot, new CountNode[] {
                        new CountNode(IdTestLeafNode_1, countFetcher1, TreeCounter.DoNothingPolicy),
                        new CountNode(IdTestLeafNode_2, countFetcher2, TreeCounter.DoNothingPolicy)
                }));
        treeCounter.reload();
    }

    /**
     * 测试reload之后的结果是否正确
     * @throws Exception
     */
    @Test
    public void reloadTreeCount_isCorrect() throws Exception {
        assertEquals(fakePersistingCount1, treeCounter.getCount(IdTestLeafNode_1));
        assertEquals(fakePersistingCount2, treeCounter.getCount(IdTestLeafNode_2));
        assertEquals(fakePersistingCount1 + fakePersistingCount2, treeCounter.getCount());
    }

    /**
     * 测试更新节点数量值之后的，该值是否有保存成功
     * @throws Exception
     */
    @Test
    public void updateCountNode_isCorrect() throws Exception {
        final int newNodeCount1 = 7;
        treeCounter.applyCount(IdTestLeafNode_1, newNodeCount1);

        assertEquals(newNodeCount1, treeCounter.getCount(IdTestLeafNode_1));
        assertEquals(newNodeCount1 + fakePersistingCount2, treeCounter.getCount());
        assertEquals(newNodeCount1, fakePersistingCount1);
    }
}

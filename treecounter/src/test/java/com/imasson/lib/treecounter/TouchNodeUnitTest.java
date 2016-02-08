package com.imasson.lib.treecounter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 测试{@link TreeCounter}中的节点碰触逻辑，包含针对4个{@link TouchNodePolicy}的测试
 */
public class TouchNodeUnitTest {

    /*
     * Test tree graph:
     * -------------------------------------------
     *    IdTestNodeRoot [DoNothing]
     *     ├─ IdTestBypassNode [Bypass]
     *     │   ├─ IdTestLeafNode_1 [ZeroSelf]
     *     │   └─ IdTestLeafNode_2 [ZeroSelf]
     *     └─ IdTestZeroDescendantNode [ZeroDescendant]
     *         ├─ IdTestLeafNode_1 [DoNothing]
     *         └─ IdTestLeafNode_2 [DoNothing]
     * -------------------------------------------
     */

    private static final String IdTestNodeRoot              = "testNodeRoot";
    private static final String IdTestBypassNode            = "testBypassNode";
    private static final String IdTestZeroDescendantNode    = "testZeroDescendantNode";
    private static final String IdTestLeafNode_1            = "testLeafNode_1";
    private static final String IdTestLeafNode_2            = "testLeafNode_2";
    private static final String IdTestLeafNode_3            = "testLeafNode_3";
    private static final String IdTestLeafNode_4            = "testLeafNode_4";

    public static final int COUNT_INIT_NODE_1 = 3;
    public static final int COUNT_INIT_NODE_2 = 5;
    public static final int COUNT_INIT_NODE_3 = 4;
    public static final int COUNT_INIT_NODE_4 = 6;

    private TreeCounter treeCounter;
    private int fakePersistingCount1 = COUNT_INIT_NODE_1;
    private int fakePersistingCount2 = COUNT_INIT_NODE_2;
    private int fakePersistingCount3 = COUNT_INIT_NODE_3;
    private int fakePersistingCount4 = COUNT_INIT_NODE_4;

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

    private ICountFetcher countFetcher3 = new ICountFetcher() {

        @Override
        public void loadCount(LoadCountCallback callback) {
            callback.onLoadCount(this, fakePersistingCount3);
        }

        @Override
        public void updateCount(int count) {
            fakePersistingCount3 = count;
        }
    };

    private ICountFetcher countFetcher4 = new ICountFetcher() {

        @Override
        public void loadCount(LoadCountCallback callback) {
            callback.onLoadCount(this, fakePersistingCount4);
        }

        @Override
        public void updateCount(int count) {
            fakePersistingCount4 = count;
        }
    };

    @Before
    public void setup() throws Exception {
        treeCounter = new TreeCounter(
                new CountNode(IdTestNodeRoot, new CountNode[] {
                        new CountNode(IdTestBypassNode, new CountNode[] {
                                new CountNode(IdTestLeafNode_1, countFetcher1, TreeCounter.ZeroSelfPolicy),
                                new CountNode(IdTestLeafNode_2, countFetcher2, TreeCounter.ZeroSelfPolicy)
                        }, TreeCounter.BypassPolicy),
                        new CountNode(IdTestZeroDescendantNode, new CountNode[] {
                                new CountNode(IdTestLeafNode_3, countFetcher3, TreeCounter.DoNothingPolicy),
                                new CountNode(IdTestLeafNode_4, countFetcher4, TreeCounter.DoNothingPolicy)
                        }, TreeCounter.ZeroDescendantPolicy)
                }));
        treeCounter.reload();
    }

    @Test
    public void bypassPolicy_isCorrect() throws Exception {
        final int originParentCount = treeCounter.getCount(IdTestBypassNode);
        final int originChildCount1 = treeCounter.getCount(IdTestLeafNode_1);
        final int originChildCount2 = treeCounter.getCount(IdTestLeafNode_2);
        final int originTotalCount = treeCounter.getCount();

        treeCounter.touchNode(IdTestBypassNode);

        assertEquals(0, treeCounter.getCount(IdTestBypassNode));
        assertEquals(originChildCount1, treeCounter.getCount(IdTestLeafNode_1));
        assertEquals(originChildCount2, treeCounter.getCount(IdTestLeafNode_2));
        assertEquals(originTotalCount - originParentCount, treeCounter.getCount());

        treeCounter.addCount(IdTestLeafNode_1, 1);
        assertEquals(originParentCount + 1, treeCounter.getCount(IdTestBypassNode));
        assertEquals(originChildCount1 + 1, treeCounter.getCount(IdTestLeafNode_1));
        assertEquals(originChildCount2, treeCounter.getCount(IdTestLeafNode_2));
        assertEquals(originTotalCount + 1, treeCounter.getCount());
    }

    @Test
    public void doNothingPolicy_isCorrect() throws Exception {
        final int originTotalCount = treeCounter.getCount();
        final int originChildCount1 = treeCounter.getCount(IdTestBypassNode);
        final int originChildCount2 = treeCounter.getCount(IdTestZeroDescendantNode);

        treeCounter.touchNode(IdTestNodeRoot);

        assertEquals(originTotalCount, treeCounter.getCount());
        assertEquals(originChildCount1, treeCounter.getCount(IdTestBypassNode));
        assertEquals(originChildCount2, treeCounter.getCount(IdTestZeroDescendantNode));
    }

    @Test
    public void ZeroSelfPolicy_isCorrect() throws Exception {
        final int originTargetCount = treeCounter.getCount(IdTestLeafNode_1);
        final int originParentCount = treeCounter.getCount(IdTestBypassNode);
        final int originTotalCount = treeCounter.getCount();

        treeCounter.touchNode(IdTestLeafNode_1);

        assertEquals(0, treeCounter.getCount(IdTestLeafNode_1));
        assertEquals(originParentCount - originTargetCount, treeCounter.getCount(IdTestBypassNode));
        assertEquals(originTotalCount - originTargetCount, treeCounter.getCount());
    }

    @Test
    public void zeroDescendantPolicy_isCorrect() throws Exception {
        final int originTotalCount = treeCounter.getCount();
        final int originParentCount = treeCounter.getCount(IdTestZeroDescendantNode);

        treeCounter.touchNode(IdTestZeroDescendantNode);

        assertEquals(0, treeCounter.getCount(IdTestZeroDescendantNode));
        assertEquals(0, treeCounter.getCount(IdTestLeafNode_3));
        assertEquals(0, treeCounter.getCount(IdTestLeafNode_4));
        assertEquals(originTotalCount - originParentCount, treeCounter.getCount());
    }
}

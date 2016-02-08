package com.imasson.lib.treecounter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * {@link TreeCounter}的基本功能测试，
 * 包括TreeCount构建测试，和基于{@link TreeCounter#DoNothingPolicy}的数量值操作测试
 */
public class TreeCountBaseUnitTest {
    private static final ICountFetcher EmptyCountFetcher = new ICountFetcher() {
        @Override
        public void loadCount(LoadCountCallback callback) {
            callback.onLoadCount(this, 0);
        }

        @Override
        public void updateCount(int count) {
        }
    };


    /*
     * Test tree graph:
     * -------------------------------------------
     *    IdTestNodeRoot
     *     ├─ IdTestSingleNode
     *     ├─ IdTestParentNode
     *     │   ├─ IdTestLeafNode_1
     *     │   ├─ IdTestLeafNode_2
     *     │   └─ IdTestLeafNode_Add2
     *     └─ IdTestLeafNode_Add1
     * -------------------------------------------
     */

    private static final String IdTestNodeRoot      = "testNodeRoot";
    private static final String IdTestSingleNode    = "testSingleNode";
    private static final String IdTestParentNode    = "testParentNode";
    private static final String IdTestLeafNode_1    = "testLeafNode_1";
    private static final String IdTestLeafNode_2    = "testLeafNode_2";
    private static final String IdTestLeafNode_Add1 = "testLeafNode_Add1";
    private static final String IdTestLeafNode_Add2 = "testLeafNode_Add2";

    private TreeCounter treeCounter;

    @Before
    public void setup() throws Exception {
        treeCounter = new TreeCounter(
                new CountNode(IdTestNodeRoot, new CountNode[] {
                        new CountNode(IdTestSingleNode, EmptyCountFetcher, TreeCounter.DoNothingPolicy),
                        new CountNode(IdTestParentNode, new CountNode[] {
                                new CountNode(IdTestLeafNode_1, EmptyCountFetcher, TreeCounter.DoNothingPolicy),
                                new CountNode(IdTestLeafNode_2, EmptyCountFetcher, TreeCounter.DoNothingPolicy)
                        }, TreeCounter.DoNothingPolicy),
                }));
    }

    /**
     * 测试构建{@link TreeCounter}和节点树结构
     * @throws Exception
     */
    @Test
    public void constructTreeCount_isCompleted() throws Exception {
        assertNotNull(treeCounter.getRootNode());
        assertEquals(treeCounter.getRootNode().getId(), IdTestNodeRoot);

        final CountNode[] rootChildren = treeCounter.getRootNode().getChildren();
        assertEquals(rootChildren.length, 2);
        assertEquals(rootChildren[0].getId(), IdTestSingleNode);
        assertEquals(rootChildren[1].getId(), IdTestParentNode);

        final CountNode[] subChildren = rootChildren[1].getChildren();
        assertEquals(subChildren.length, 2);
        assertEquals(subChildren[0].getId(), IdTestLeafNode_1);
        assertEquals(subChildren[1].getId(), IdTestLeafNode_2);
    }

    /**
     * 测试针对某一node进行{@link TreeCounter#applyCount(String, int)}操作
     * @throws Exception
     */
    @Test
    public void applyCountValue_isCorrect() throws Exception {
        final CountNode targetNode = treeCounter.findNode(IdTestLeafNode_1);
        assertNotNull(targetNode);

        final int delta = 1;

        final int originTargetCount = targetNode.getCount();
        final int originTotalCount = treeCounter.getCount();
        final int originParentCount = targetNode.getParent().getCount();

        treeCounter.applyCount(IdTestLeafNode_1, originTargetCount + delta);

        // test target node
        assertEquals(originTargetCount + delta, targetNode.getCount());

        // test parent node
        assertEquals(originParentCount + delta, targetNode.getParent().getCount());

        // test root node
        assertEquals(originTotalCount + delta, treeCounter.getCount());
    }

    /**
     * 测试针对某一node进行{@link TreeCounter#addCount(String, int)}操作
     * @throws Exception
     */
    @Test
    public void addCountValue_isCorrect() throws Exception {
        final CountNode targetNode = treeCounter.findNode(IdTestLeafNode_1);
        assertNotNull(targetNode);

        treeCounter.applyCount(IdTestLeafNode_1, 5);

        final int delta = 1;

        final int originTargetCount = targetNode.getCount();
        final int originTotalCount = treeCounter.getCount();
        final int originParentCount = targetNode.getParent().getCount();

        treeCounter.addCount(IdTestLeafNode_1, delta);

        // test target node
        assertEquals(originTargetCount + delta, targetNode.getCount());

        // test parent node
        assertEquals(originParentCount + delta, targetNode.getParent().getCount());

        // test root node
        assertEquals(originTotalCount + delta, treeCounter.getCount());
    }

    /**
     * 测试针对某一node进行{@link TreeCounter#reduceCount(String, int)}操作
     * @throws Exception
     */
    @Test()
    public void reduceCountValue_isCorrect() throws Exception {
        final CountNode targetNode = treeCounter.findNode(IdTestLeafNode_1);
        assertNotNull(targetNode);

        treeCounter.applyCount(IdTestLeafNode_1, 5);

        final int delta = 1;

        final int originTargetCount = targetNode.getCount();
        final int originTotalCount = treeCounter.getCount();
        final int originParentCount = targetNode.getParent().getCount();

        treeCounter.reduceCount(IdTestLeafNode_1, delta);

        // test target node
        assertEquals(originTargetCount - delta, targetNode.getCount());

        // test parent node
        assertEquals(originParentCount - delta, targetNode.getParent().getCount());

        // test root node
        assertEquals(originTotalCount - delta, treeCounter.getCount());
    }

    /**
     * 测试在初始化完成之后再中途增加一个新的节点
     * @throws Exception
     */
    @Test
    public void addNodeAfterConstruction_isCorrect() throws Exception {
        final int newLeafCount = 3;

        // test node add on root
        treeCounter.addNode(new CountNode(IdTestLeafNode_Add1, EmptyCountFetcher, TreeCounter.DoNothingPolicy));
        int originTotalCount = treeCounter.getCount();
        treeCounter.applyCount(IdTestLeafNode_Add1, newLeafCount);
        assertEquals(originTotalCount + newLeafCount, treeCounter.getCount());

        // test node add as grandchild
        treeCounter.addNode(IdTestParentNode,
                new CountNode(IdTestLeafNode_Add2, EmptyCountFetcher, TreeCounter.DoNothingPolicy));
        CountNode parentNode = treeCounter.findNode(IdTestParentNode);
        originTotalCount = treeCounter.getCount();
        int originParentCount = parentNode.getCount();
        treeCounter.applyCount(IdTestLeafNode_Add2, newLeafCount);
        assertEquals(originParentCount + newLeafCount, parentNode.getCount());
        assertEquals(originTotalCount + newLeafCount, treeCounter.getCount());
    }

    /**
     * 测试通知节点数量值变化的接口是否有效
     * @throws Exception
     */
    @Test
    public void notifyCountChanged_doesSuccess() throws Exception {
        final boolean[] notifyFlags = new boolean[3];

        treeCounter.setListener(new TreeCounter.OnNodeUnreadCountChangeListener() {
            @Override
            public void onNodeUnreadCountChanged(CountNode node) {
                if (IdTestLeafNode_2.equals(node.getId())) {
                    notifyFlags[0] = true;
                } else if (IdTestParentNode.equals(node.getId())) {
                    notifyFlags[1] = true;
                } else if (IdTestNodeRoot.equals(node.getId())) {
                    notifyFlags[2] = true;
                }
            }
        });
        treeCounter.addCount(IdTestLeafNode_2, 3);

        assertTrue(notifyFlags[0]);
        assertTrue(notifyFlags[1]);
        assertTrue(notifyFlags[2]);
    }
}
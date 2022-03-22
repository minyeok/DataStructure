package hashtable;

public class CLHashTable {

    // 버킷 최대 크기
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    // 버킷의 키 적재율 임계치 (75%)
    private static final float ROAD_FACTOR_THRESHOLD = 0.75F;
    // 특정 인덱스의 키의 개수가 6개 이상이면 List 방식에서 Tree 방식으로 변경
    private static final int TREE_THRESHOLD = 6;
    // 특정 인덱스의 키의 개수가 2개 이하이면 Tree 방식에서 List 방식으로 변경
    private static final int LIST_THRESHOLD = 2;
    // 버킷의 크기 고정 여부, false인 경우는 적재율에 따라 늘어난다
    private final boolean isFixedBucket;

    private Object[] bucket;
    private int bucketSize;

    public CLHashTable() {
        this(16, false);
    }

    public CLHashTable(boolean isFixedBucket) {
        this(16, isFixedBucket);
    }

    public CLHashTable(int capacity, boolean isFixedBucket) {
        if (capacity < 0) {
            throw new RuntimeException("음수는 불가능 합니다");
        }
        this.isFixedBucket = isFixedBucket;
        this.bucket = new Object[tableSizeFor(capacity)];
    }

    private int tableSizeFor(int capacity) {
        // TODO : 2의 제곱수로 bucket 사이즈를 보정한다. (상향식)
        int n = capacity - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 15;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    static int secondaryHash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    private int hash(Object key) {
        return secondaryHash(key) & (bucket.length - 1);
    }

    private int probing(Object key) {
        return hash(key);
    }

    public Object get(Object key) {
        int index = probing(key);
        if (null == bucket[index]) {
            return null;
        }
        /**
         * 버킷에는 SinglyLinkedList 또는 RedBlackTree로 저장되어 있을 수 있다.
         */
        if (bucket[index] instanceof SinglyLinkedList) {
            SinglyLinkedList list = (SinglyLinkedList) bucket[index];
            return list.getValue(key);
        } else if (bucket[index] instanceof RedBlackTree) {
            RedBlackTree tree = (RedBlackTree) bucket[index];
            return tree.getValue(key);
        }
        return null;
    }

    public void remove(Object key) {
        int index = probing((key));
        if (null != bucket[index]) {
            /**
             * 버킷에는 SinglyLinkedList 또는 RedBlackTree로 저장되어 있을 수 있다.
             * 그러므로 접근 후 각 자료구조에 맞게 키를 제거한다.
             */
            if (bucket[index] instanceof SinglyLinkedList) {
                SinglyLinkedList list = (SinglyLinkedList) bucket[index];
                list.remove(key);
                if (list.isEmpty()) {
                    /**
                     * 만약에 SinglyLinkedList에 키가 0개이면 버킷을 null로 변경하여
                     * SinglyLinkedList를 제거한다.
                     */
                    bucket[index] = null;
                    --bucketSize;
                }
            } else if (bucket[index] instanceof RedBlackTree) {
                RedBlackTree tree = (RedBlackTree) bucket[index];
                tree.remove(key);
                // TODO : 특정 인덱스의 엔트리 개수가 임계치 이하이면 Tree 방식에서 List 방식으로 변경
                if (tree.getSize() <= LIST_THRESHOLD) {
                    treeToList(index, tree);
                }
            }
        }
    }
    public void put(Object key, Object value) {
        int hash = secondaryHash(key);
        putValue(hash, key, value);
    }

    private void putValue(int hash, Object key, Object value) {
        int index = hash & (bucket.length - 1);
        /**
         * index 위치에 최초로 엔트리가 삽입될 경우 SinglyLinkedList에 저장한다.
         */
        if (null == bucket[index]) {
            bucket[index] = new SinglyLinkedList();
            ++bucketSize;
        }

        if (bucket[index] instanceof SinglyLinkedList) {
            SinglyLinkedList list = (SinglyLinkedList) bucket[index];
            list.add(hash, key, value);
            // TODO : 특정 인덱스의 엔트리 개수가 임계치 이상이면 List 방식에서 Tree 방식으로 변경
            if (list.getSize() >= TREE_THRESHOLD) {
                listToTree(index, list);
            }
        } else if (bucket[index] instanceof RedBlackTree) {
            RedBlackTree tree = (RedBlackTree) bucket[index];
            tree.add(hash, key, value);
        }
        if (!isFixedBucket) {
            resizeBucket();
        }
    }

    private void listToTree(int bucketIndex, SinglyLinkedList list) {
        // TODO : List 방식에서 Tree 방식으로 변경한다.
        RedBlackTree redBlackTree = new RedBlackTree();
        bucket[bucketIndex] = redBlackTree;
        while (!list.isEmpty()) {
            Node node = list.removeFirst();
            redBlackTree.add(node.hash, node.key, node.value);
        }
    }

    private void treeToList(int bucketIndex, RedBlackTree tree) {
        // TODO : Tree 방식에서 List 방식으로 변경한다.
        SinglyLinkedList list = new SinglyLinkedList();
        bucket[bucketIndex] = list;
        while (!tree.isEmpty()) {
            TreeNode node = tree.removeFirst();
            list.add(node.hash, node.key, node.value);
        }
    }

    private void resizeBucket() {
        int prevBucketSize = bucket.length;
        if (MAXIMUM_CAPACITY <= prevBucketSize) {
            return;
        }

        if (bucketSize * 1.0F / bucket.length > ROAD_FACTOR_THRESHOLD) {
            int newBucketSize = tableSizeFor(prevBucketSize << 1);

            Object[] tempBucket = bucket;
            bucket = new Object[newBucketSize];
            bucketSize = 0;
            // TODO : 새로운 버킷이 할당되면 노드의 bucket 해시를 재계산하여 다시 할당한다.
            for (int i = 0; i < prevBucketSize; i++) {
                if (tempBucket[i] instanceof SinglyLinkedList) {
                    SinglyLinkedList list = (SinglyLinkedList) tempBucket[i];
                    while (!list.isEmpty()) {
                        Node node = list.removeFirst();
                        putValue(node.hash, node.key, node.value);
                    }
                } else if (bucket[i] instanceof RedBlackTree) {
                    RedBlackTree tree = (RedBlackTree) tempBucket[i];
                    while (!tree.isEmpty()) {
                        TreeNode node = tree.removeFirst();
                        putValue(node.hash, node.key, node.value);
                    }
                }
            }
        }
    }

    public void printHashTable() {
        System.out.println("-------------");
        for (int i = 0; i < bucket.length; i++) {
            if (bucket[i] instanceof SinglyLinkedList) {
                System.out.printf("| %d | %s %s\n", i, "List", bucket[i].toString());
            } else if (bucket[i] instanceof RedBlackTree) {
                RedBlackTree tree = (RedBlackTree) bucket[i];
                System.out.printf("| %d | %s ", i, "Tree Set");
                tree.traversal();
                System.out.println("");
            } else {
                System.out.printf("| %d | %s\n", i, "null");
            }
        }
        System.out.println("-------------");
    }


}

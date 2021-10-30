import java.util.Comparator;
import java.util.PriorityQueue;

public class Compress extends Huffman {
    // Таблица частот байт в файле
    private final int[] frequencyTable;
    //Сравнивание частот в дереве
    private final Comparator<Tree> freqComparator = Comparator.comparingInt(Tree::getFrequency);

    public Compress() {
        frequencyTable = new int[CODING_TABLE_SIZE];

        codingArray = new String[CODING_TABLE_SIZE];

        treeText = "";
    }

    //Принимает массив байт и по ним строит таблицу частот
    public void fillFrequencyTable(byte[] bytes) {
        for (byte b : bytes) {
            frequencyTable[b & MASK]++;
        }
    }

    //Создание дерева Хаффмана по таблице частот
    public void createHuffmanTree() {
        PriorityQueue<Tree> priorityQueue = new PriorityQueue<>(CODING_TABLE_SIZE, freqComparator);

        for (int i = 0; i < CODING_TABLE_SIZE; i++) {
            if (frequencyTable[i] > 0) {
                Node newNode = new Node(i, frequencyTable[i]);
                Tree newTree = new Tree(newNode);
                priorityQueue.add(newTree);
            }
        }

        while (true) {
            Tree tree1 = priorityQueue.poll();
            Tree tree2 = priorityQueue.poll();

            if (tree1 == null)
                return;

            if (tree2 == null) {
                huffmanTree = tree1;
                return;
            }

            Node newNode = new Node();
            newNode.addChild(tree1.getHead());
            newNode.addChild(tree2.getHead());

            priorityQueue.add(new Tree(newNode));
        }
    }

    //Создание представления дерева в виде строки
    private void createTreeText(Node node) {
        if (node == null)
            return;

        if (node.isLeaf()) {
            treeText += bitOne;
            treeText += String.format("%8s", Integer.toBinaryString(node.getCharacter() & 0xff)).replace(" ", "0");
        } else {
            treeText += bitZero;
            createTreeText(node.getLeft());
            createTreeText(node.getRight());
        }
    }

    public String getTreeText() {
        createTreeText(huffmanTree.getHead());

        return treeText;
    }

    public String getCompressedByte(byte b) {
        return codingArray[b & MASK];
    }
}

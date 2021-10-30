import java.util.HashMap;

public class Decompress extends Huffman {
    private final HashMap<String, String> hufTableReverse;

    Decompress(String treeText) {
        codingArray = new String[CODING_TABLE_SIZE];

        this.treeText = treeText;

        hufTableReverse = new HashMap<>();
    }

    //Заполнение дерева Хаффмана
    private void fillHuffmanTree(Node node, Node parent, boolean isLeft) {
        if (treeText.isEmpty()) {
            return;
        }

        if (node == null) {
            node = new Node();
            if (isLeft) {
                parent.setLeft(node);
            } else {
                parent.setRight(node);
            }
        }

        if (treeText.charAt(0) == '1') {
            treeText = treeText.substring(1);
            String tmp = treeText.substring(0, BYTE_SIZE);
            node.setCharacter(Integer.parseInt(tmp, 2));
            treeText = treeText.substring(BYTE_SIZE);
        } else {
            treeText = treeText.substring(1);
            fillHuffmanTree(node.getLeft(), node, true);
            fillHuffmanTree(node.getRight(), node, false);
        }
    }

    //Создание дерева Хаффмана
    public void createHuffmanTree() {
        huffmanTree = new Tree();

        fillHuffmanTree(huffmanTree.getHead(), null, true);
    }

    //Заполнение HashMap, в которой хранятся ключ: сжатый код, значение: исходный код
    public void fillHufTable() {
        for (int i = 0; i < CODING_TABLE_SIZE; i++) {
            if (codingArray[i] != null) {
                hufTableReverse.put(codingArray[i], String.format("%8s", Integer.toBinaryString(i & MASK)).replace(" ", "0"));
            }
        }
    }

    //Возвращает исходный код
    public String getChar(String compressedCode) {
        return hufTableReverse.get(compressedCode);
    }
}

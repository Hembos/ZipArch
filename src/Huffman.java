public class Huffman {
    protected final int MASK = 0b11111111;
    protected final String bitOne = "1";
    protected final String bitZero = "0";
    protected static final byte BYTE_SIZE = 8;
    protected static final short CODING_TABLE_SIZE = 255;
    protected Tree huffmanTree;
    protected String[] codingArray;
    protected String treeText;

    public void createCodingTable() {
        fillCodingTable(null, "", "");
    }

    //Создание таблицы новых кодов
    private void fillCodingTable(Node node, String code, String newCodeInEnd) {
        if (node == null) {
            node = huffmanTree.getHead();
        }

        if (node.isLeaf()) {
            codingArray[node.getCharacter()] = code + newCodeInEnd;
        } else {
            fillCodingTable(node.getLeft(), code + newCodeInEnd, "0");
            fillCodingTable(node.getRight(), code + newCodeInEnd, "1");
        }
    }
}

class Node {
    private int frequency;
    private int character;
    private Node left;
    private Node right;

    Node(int letter, int frequency) {
        this.character = letter;
        this.frequency = frequency;
    }

    Node() {

    }

    public boolean isLeaf() {
        return left == null && right == null;
    }

    public void addChild(Node newNode) {
        if (left == null) {
            left = newNode;
        } else {
            if (left.frequency <= newNode.frequency) {
                right = newNode;
            } else {
                right = left;
                left = newNode;
            }
        }

        frequency += newNode.frequency;
    }

    public int getFrequency() {
        return frequency;
    }

    public int getCharacter() {
        return character;
    }

    public Node getLeft() {
        return left;
    }

    public Node getRight() {
        return right;
    }

    public void setCharacter(int character) {
        this.character = character;
    }

    public void setLeft(Node left) {
        this.left = left;
    }

    public void setRight(Node right) {
        this.right = right;
    }
}

class Tree {
    private final Node head;

    Tree() {
        head = new Node();
    }

    Tree(Node head) {
        this.head = head;
    }

    public Node getHead() {
        return head;
    }

    public int getFrequency() {
        return head.getFrequency();
    }
}

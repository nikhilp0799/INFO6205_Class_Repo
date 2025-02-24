package com.phasmidsoftware.dsaipg.adt.pq;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class FibonacciHeap<T extends Comparable<T>> {
    private Node<T> min = null;
    private int size = 0;
    private List<T> spilledElements = new ArrayList<>();

    private static class Node<T> {
        T value;
        Node<T> prev, next, child, parent;
        boolean mark;
        int degree;

        public Node(T value) {
            this.value = value;
            this.prev = this;
            this.next = this;
        }
    }

    public boolean isEmpty() {
        return min == null;
    }

    public void clear() {
        min = null;
        size = 0;
        spilledElements.clear();
    }

    public int size() {
        return size;
    }

    public Node<T> insert(T value) {
        Node<T> node = new Node<>(value);
        min = mergeLists(min, node);
        size++;

        if (size > 4095) {
            T spilledElement = removeMin();
            if (spilledElement != null) {
                spilledElements.add(spilledElement);
                System.out.println("Fibonacci Heap - Spilled element added: " + spilledElement);
            }
        }


        return node;
    }

    public T removeMin() {
        if (isEmpty()) return null;

        Node<T> minNode = this.min;
        size--;

        if (size >= 4095 && minNode != null) {
            spilledElements.add(minNode.value);
            System.out.println("Fibonacci Heap - Spilled element added: " + minNode.value);
        }


        if (minNode.next == minNode) {
            min = null;
        } else {
            minNode.prev.next = minNode.next;
            minNode.next.prev = minNode.prev;
            min = minNode.next;
        }

        if (minNode.child != null) {
            Node<T> child = minNode.child;
            do {
                child.parent = null;
                child = child.next;
            } while (child != minNode.child);
            min = mergeLists(min, minNode.child);
        }

        if (!isEmpty()) {
            consolidate();
        }

        return minNode.value;
    }

    private Node<T> mergeLists(Node<T> a, Node<T> b) {
        if (a == null) return b;
        if (b == null) return a;

        Node<T> aNext = a.next;
        Node<T> bNext = b.next;

        a.next = bNext;
        bNext.prev = a;

        b.next = aNext;
        aNext.prev = b;

        return (a.value.compareTo(b.value) < 0) ? a : b;
    }

    private void consolidate() {
        int maxDegree = (int) Math.ceil(Math.log(size) / Math.log(2)) + 5;

        // Use ArrayList and ensure it grows dynamically when needed
        List<Node<T>> degreeTable = new ArrayList<>(Collections.nCopies(maxDegree, null));

        // Collect all nodes in the root list
        List<Node<T>> rootList = new ArrayList<>();
        Node<T> current = min;

        if (current != null) {
            do {
                rootList.add(current);
                current = current.next;
            } while (current != null && current != min);
        }

        // Perform degree-wise consolidation
        for (Node<T> node : rootList) {
            int degree = node.degree;

            while (degree >= degreeTable.size()) {
                degreeTable.add(null); // Ensure enough capacity
            }

            // Merge trees with the same degree
            while (degreeTable.get(degree) != null) {
                Node<T> other = degreeTable.get(degree);

                if (other.value.compareTo(node.value) < 0) {
                    Node<T> temp = node;
                    node = other;
                    other = temp;
                }

                // Link other under node
                linkTrees(other, node);
                degreeTable.set(degree, null);
                degree++;

                // Ensure the list is large enough before using .set()
                while (degree >= degreeTable.size()) {
                    degreeTable.add(null);
                }
            }

            degreeTable.set(degree, node);
        }

        // Step 3: Rebuild the root list and find new minimum
        min = null;
        for (Node<T> node : degreeTable) {
            if (node != null) {
                if (min == null || node.value.compareTo(min.value) < 0) {
                    min = node;
                }
            }
        }
    }





    private void linkTrees(Node<T> child, Node<T> parent) {
        child.next.prev = child.prev;
        child.prev.next = child.next;

        child.parent = parent;
        child.next = child.prev = child;
        child.mark = false;

        if (parent.child == null) {
            parent.child = child;
        } else {
            child.next = parent.child;
            child.prev = parent.child.prev;
            parent.child.prev.next = child;
            parent.child.prev = child;
        }

        parent.degree++;
    }

    public T getHighestPrioritySpilledElement() {
        if (spilledElements == null || spilledElements.isEmpty()) {
            return null;
        }
        return spilledElements.stream().max(Comparable::compareTo).orElse(null);
    }

    public int getSpilledElementsCount() {
        return (spilledElements == null) ? 0 : spilledElements.size();
    }

}
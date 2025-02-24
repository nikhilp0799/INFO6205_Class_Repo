package com.phasmidsoftware.dsaipg.adt.pq;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FourAryHeap<K> {
    private Object[] heap;
    private int last = 0;
    private final Comparator<K> comparator;
    private final boolean max;
    private final boolean useFloydsTrick;
    private List<K> spilledElements = new ArrayList<>();

    public FourAryHeap(int capacity, Comparator<K> comparator, boolean max, boolean useFloydsTrick) {
        this.max = max;
        this.comparator = comparator;
        this.heap = new Object[capacity + 1];
        this.useFloydsTrick = useFloydsTrick;
    }

    /**
     * Insert a new element into the heap.
     */
    public void insert(K key) {
        if (last >= 4095) {
            K spilledElement = removeTop();
            spilledElements.add(spilledElement);
            System.out.println("FourAryHeap - Spilled element added: " + spilledElement);
        }
        heap[++last] = key;
        swim(last);
    }


    /**
     * Removes and returns the top element of the heap.
     */
    public K removeTop() {
        if (last == 0) return null;
        K top = (K) heap[1]; // Root element
        swap(1, last--); // Swap root with last
        sink(1); // Restore heap property
        if (useFloydsTrick && last > 0) swim(1);
        heap[last + 1] = null; // Prevent loitering

        return top;
    }

    /**
     * Returns the current size of the heap.
     */
    public int size() {
        return last;
    }

    /**
     * Build the heap using Floyd's trick.
     */
    public void buildHeapWithFloydsTrick(List<K> elements) {
        if (elements.size() > heap.length - 1) {
            throw new IllegalStateException("Heap capacity exceeded");
        }
        for (K element : elements) {
            heap[++last] = element;
        }
        for (int i = last / 4; i >= 1; i--) {
            sink(i); // Apply Floyd's trick
        }
    }

    /**
     * Get the highest priority spilled element.
     */
    public K getHighestPrioritySpilledElement() {
        if (spilledElements.isEmpty()) return null;
        return spilledElements.stream().max(comparator).orElse(null);
    }

    /**
     * Get the count of spilled elements.
     */
    public int getSpilledElementsCount() {
        return spilledElements.size();
    }

    private void sink(int k) {
        while (4 * k - 2 <= last) {
            int j = 4 * k - 2;
            int bestChild = j;
            for (int i = 1; i < 4 && j + i <= last; i++) {
                if (less(bestChild, j + i)) bestChild = j + i;
            }
            if (!less(k, bestChild)) break;
            swap(k, bestChild);
            k = bestChild;
        }
    }

    private void swim(int k) {
        while (k > 1 && less(parent(k), k)) {
            swap(k, parent(k));
            k = parent(k);
        }
    }

    private int parent(int k) {
        return (k - 2) / 4 + 1;
    }

    private boolean less(int i, int j) {
        if (max) return comparator.compare((K) heap[i], (K) heap[j]) < 0;
        else return comparator.compare((K) heap[i], (K) heap[j]) > 0;
    }

    private void swap(int i, int j) {
        K temp = (K) heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }
}

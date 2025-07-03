package com.jamiewang.secure3d.util;

import java.util.Optional;

/**
 * Generic Interval Tree implementation for efficient range queries using Long values
 *
 * This class provides O(log n) insertion and search operations for interval-based data.
 * It uses a self-balancing AVL tree structure with interval augmentation for optimal
 * performance with large datasets. Optimized for Long-based ranges.
 *
 * @param <T> Type of data stored in the tree, must implement IntervalData interface
 */
public class IntervalTree<T extends IntervalData> {

    private Node root;
    private int size = 0;

    /**
     * Internal node class representing a node in the interval tree
     */
    private class Node {
        T data;
        Long start, end;
        Long maxEnd;
        Node left, right;
        int height = 1;

        Node(T data) {
            this.data = data;
            this.start = data.getStartRange();
            this.end = data.getEndRange();
            this.maxEnd = this.end;
        }
    }

    /**
     * Insert data into the interval tree
     *
     * @param data Data to insert, must not be null
     * @throws IllegalArgumentException if data is null or has invalid range
     */
    public void insert(T data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }

        if (data.getStartRange() == null || data.getEndRange() == null) {
            throw new IllegalArgumentException("Data must have valid start and end ranges");
        }

        root = insert(root, data);
        size++;
    }

    /**
     * Recursive insertion method
     */
    private Node insert(Node node, T data) {
        if (node == null) {
            return new Node(data);
        }

        Long start = data.getStartRange();

        if (start < node.start) {
            node.left = insert(node.left, data);
        } else {
            node.right = insert(node.right, data);
        }

        // Update height and maxEnd
        updateNode(node);

        // Balance the tree
        return balance(node);
    }

    /**
     * Find the card range that contains the given value
     * Since ranges don't overlap, this returns at most one result
     *
     * @param value Value to search for (will be converted to Long)
     * @return Optional containing the matching data, empty if not found
     */
    public Optional<T> findMostSpecific(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }

        Long valueAsLong = convertToLong(value);
        if (valueAsLong == null) {
            return Optional.empty();
        }

        Node result = findRange(root, valueAsLong);
        return result != null ? Optional.of(result.data) : Optional.empty();
    }

    /**
     * Find the card range that contains the given Long value
     *
     * @param value Long value to search for
     * @return Optional containing the matching data, empty if not found
     */
    public Optional<T> findMostSpecific(Long value) {
        if (value == null) {
            return Optional.empty();
        }

        Node result = findRange(root, value);
        return result != null ? Optional.of(result.data) : Optional.empty();
    }

    /**
     * Recursive method to find the interval that contains the value
     * Since ranges don't overlap, we can stop at the first match
     */
    private Node findRange(Node node, Long value) {
        if (node == null) {
            return null;
        }

        // Check if current node contains the value
        if (value >= node.start && value <= node.end) {
            return node; // Found the range, no need to continue
        }

        // Search left subtree if it might contain the value
        if (node.left != null && value <= node.left.maxEnd) {
            Node leftResult = findRange(node.left, value);
            if (leftResult != null) {
                return leftResult;
            }
        }

        // Search right subtree if current node start is <= value
        if (node.start <= value) {
            return findRange(node.right, value);
        }

        return null;
    }

    /**
     * Convert string value to Long for comparison
     */
    private Long convertToLong(String value) {
        try {
            // Pad to 16 digits if shorter
            String paddedValue = value.length() < 16 ?
                    value + "0".repeat(16 - value.length()) : value.substring(0, 16);
            return Long.parseLong(paddedValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Update node height and maxEnd after modifications
     */
    private void updateNode(Node node) {
        if (node == null) return;

        node.height = 1 + Math.max(getHeight(node.left), getHeight(node.right));

        node.maxEnd = node.end;
        if (node.left != null) {
            node.maxEnd = Math.max(node.maxEnd, node.left.maxEnd);
        }
        if (node.right != null) {
            node.maxEnd = Math.max(node.maxEnd, node.right.maxEnd);
        }
    }

    /**
     * Balance the tree using AVL rotations
     */
    private Node balance(Node node) {
        if (node == null) return null;

        int balanceFactor = getHeight(node.left) - getHeight(node.right);

        // Left heavy
        if (balanceFactor > 1) {
            if (getHeight(node.left.left) >= getHeight(node.left.right)) {
                node = rotateRight(node);
            } else {
                node.left = rotateLeft(node.left);
                node = rotateRight(node);
            }
        }
        // Right heavy
        else if (balanceFactor < -1) {
            if (getHeight(node.right.right) >= getHeight(node.right.left)) {
                node = rotateLeft(node);
            } else {
                node.right = rotateRight(node.right);
                node = rotateLeft(node);
            }
        }

        return node;
    }

    /**
     * Perform left rotation for AVL balancing
     */
    private Node rotateLeft(Node x) {
        Node y = x.right;
        x.right = y.left;
        y.left = x;
        updateNode(x);
        updateNode(y);
        return y;
    }

    /**
     * Perform right rotation for AVL balancing
     */
    private Node rotateRight(Node y) {
        Node x = y.left;
        y.left = x.right;
        x.right = y;
        updateNode(y);
        updateNode(x);
        return x;
    }

    /**
     * Get height of a node (0 for null nodes)
     */
    private int getHeight(Node node) {
        return node == null ? 0 : node.height;
    }

    /**
     * Get the number of elements in the tree
     *
     * @return Size of the tree
     */
    public int size() {
        return size;
    }

    /**
     * Get the height of the tree
     *
     * @return Height of the tree
     */
    public int getHeight() {
        return getHeight(root);
    }

    /**
     * Check if the tree is empty
     *
     * @return true if tree is empty, false otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Clear all elements from the tree
     */
    public void clear() {
        root = null;
        size = 0;
    }
}
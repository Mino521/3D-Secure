package com.jamiewang.secure3d.component;

import com.jamiewang.secure3d.entity.CardRangeEntity;
import com.jamiewang.secure3d.util.IntervalTree;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@Slf4j
public class SharedIntervalTreeComponent {

    private IntervalTree<CardRangeEntity> intervalTree;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile boolean isInitialized = false;

    @PostConstruct
    public void initialize() {
        log.info("Initializing shared interval tree component...");
        lock.writeLock().lock();
        try {
            intervalTree = new IntervalTree<>();
            isInitialized = true;
            log.info("Shared interval tree component initialized successfully");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Find the most specific card range for a given PAN
     *
     * This method provides thread-safe read access to the shared interval tree.
     * Multiple services can call this method concurrently without blocking each other.
     *
     * @param pan Primary Account Number as Long (16 digits)
     * @return Optional containing the most specific matching CardRange, empty if not found
     * @throws IllegalArgumentException if PAN is null
     */
    public Optional<CardRangeEntity> findCardRange(Long pan) {
        // Validate input
        if (pan == null) {
            log.debug("Invalid PAN provided: null");
            return Optional.empty();
        }

        if (pan < 0) {
            log.debug("Invalid PAN provided: negative value");
            throw new IllegalArgumentException("PAN must be positive");
        }

        // Check if tree is initialized
        if (!isInitialized) {
            log.warn("Interval tree not initialized yet");
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            Optional<CardRangeEntity> result = intervalTree.findMostSpecific(pan);

            if (result.isPresent()) {
                log.debug("Found card range for PAN {} in shared tree", pan);
            } else {
                log.debug("No card range found for PAN {} in shared tree", pan);
            }

            return result;

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Rebuild the interval tree with new card range data
     *
     * This method performs a complete rebuild of the interval tree with the provided
     * card ranges. It uses write lock to ensure no other operations interfere during
     * the rebuild process. All concurrent read operations will be blocked until
     * the rebuild is complete.
     *
     * @param cardRanges List of CardRange objects to rebuild the tree with
     * @throws IllegalArgumentException if cardRanges is null
     */
    public void rebuildTree(List<CardRangeEntity> cardRanges) {
        if (cardRanges == null) {
            throw new IllegalArgumentException("Card ranges list cannot be null");
        }

        log.info("Starting interval tree rebuild with {} card ranges", cardRanges.size());

        long startTime = System.currentTimeMillis();

        lock.writeLock().lock();
        try {
            // Create new tree instance
            IntervalTree<CardRangeEntity> newTree = new IntervalTree<>();

            // Insert all card ranges into the new tree
            int processedCount = 0;
            for (CardRangeEntity range : cardRanges) {
                if (range != null && isValidCardRange(range)) {
                    newTree.insert(range);
                    processedCount++;

                    // Log progress for large datasets
                    if (processedCount % 50000 == 0) {
                        log.info("Processed {} of {} card ranges", processedCount, cardRanges.size());
                    }
                } else {
                    log.warn("Skipping invalid card range: {}", range);
                }
            }

            // Replace the old tree with the new one atomically
            intervalTree = newTree;
            isInitialized = true;

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.info("Interval tree rebuild completed successfully");
            log.info("Processed {} valid ranges out of {} total in {} ms",
                    processedCount, cardRanges.size(), duration);
            log.info("Tree height: {}, Tree size: {}",
                    intervalTree.getHeight(), intervalTree.size());

        } catch (Exception e) {
            log.error("Error during interval tree rebuild", e);
            // Keep the old tree intact if rebuild fails
            throw new RuntimeException("Failed to rebuild interval tree", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get statistics about the current interval tree state
     *
     * @return TreeStatistics object containing size, height, and other metrics
     */
    public TreeStatistics getStatistics() {
        lock.readLock().lock();
        try {
            if (!isInitialized || intervalTree == null) {
                return new TreeStatistics(0, 0, false);
            }

            return new TreeStatistics(
                    intervalTree.size(),
                    intervalTree.getHeight(),
                    isInitialized
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Check if the interval tree is ready for operations
     *
     * @return true if tree is initialized and ready, false otherwise
     */
    public boolean isReady() {
        return isInitialized && intervalTree != null;
    }

    /**
     * Validate PAN format (positive Long value)
     */
    private boolean isValidPanFormat(Long pan) {
        return pan != null && pan > 0;
    }

    /**
     * Validate CardRange object
     */
    private boolean isValidCardRange(CardRangeEntity range) {
        if (range == null) {
            return false;
        }

        Long startRange = range.getStartRange();
        Long endRange = range.getEndRange();

        if (startRange == null || endRange == null) {
            return false;
        }

        if (startRange < 0 || endRange < 0) {
            return false;
        }

        // Basic range validation - start should be <= end
        return startRange <= endRange;
    }

    /**
     * Statistics class for interval tree metrics
     */
    public static class TreeStatistics {
        private final int size;
        private final int height;
        private final boolean initialized;

        public TreeStatistics(int size, int height, boolean initialized) {
            this.size = size;
            this.height = height;
            this.initialized = initialized;
        }

        public int getSize() {
            return size;
        }

        public int getHeight() {
            return height;
        }

        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public String toString() {
            return "TreeStatistics{" +
                    "size=" + size +
                    ", height=" + height +
                    ", initialized=" + initialized +
                    '}';
        }
    }
}

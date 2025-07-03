package com.jamiewang.secure3d.util;

/**
 * Interface for objects that can be stored in an IntervalTree
 *
 * Any class that implements this interface can be used with the generic
 * IntervalTree implementation for efficient range-based queries.
 */
public interface IntervalData {

    /**
     * Get the start of the interval range
     */
    Long getStartRange();

    /**
     * Get the end of the interval range
     */
    Long getEndRange();

}

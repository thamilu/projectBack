package com.eshop.app.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Utility for generic batch partitioning. */
public final class BatchUtils {

    private BatchUtils() {}

    /**
     * Partitions a source list into unmodifiable sublists of the specified batch size.
     *
     * @param source The source list to partition
     * @param batchSize The size of each partition batch
     * @param <T> The type of element in the list
     * @return An unmodifiable list containing partitioned unmodifiable sublists
     */
    public static <T> List<List<T>> partition(List<T> source, int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be > 0");
        }
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < source.size(); i += batchSize) {
            partitions.add(
                    Collections.unmodifiableList(
                            source.subList(i, Math.min(i + batchSize, source.size()))));
        }
        return Collections.unmodifiableList(partitions);
    }
}

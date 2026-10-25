package com.navan.travel.web.dto;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serializable;
import java.util.Objects;

/**
 * Custom Pageable implementation supporting arbitrary offset-based pagination (OFFSET x LIMIT y),
 * eliminating the page-chunking truncation bug where offset / limit rounds down.
 */
public class OffsetLimitRequest implements Pageable, Serializable {

    private final long offset;
    private final int limit;
    private final Sort sort;

    public OffsetLimitRequest(long offset, int limit, Sort sort) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must not be negative");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        this.offset = offset;
        this.limit = limit;
        this.sort = Objects.requireNonNullElseGet(sort, Sort::unsorted);
    }

    public OffsetLimitRequest(long offset, int limit) {
        this(offset, limit, Sort.unsorted());
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetLimitRequest(offset + limit, limit, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new OffsetLimitRequest(Math.max(0, offset - limit), limit, sort) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetLimitRequest(0, limit, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetLimitRequest((long) pageNumber * limit, limit, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OffsetLimitRequest that)) return false;
        return offset == that.offset && limit == that.limit && Objects.equals(sort, that.sort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, limit, sort);
    }

    @Override
    public String toString() {
        return "OffsetLimitRequest{" +
                "offset=" + offset +
                ", limit=" + limit +
                ", sort=" + sort +
                '}';
    }
}

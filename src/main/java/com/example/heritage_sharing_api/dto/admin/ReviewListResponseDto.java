package com.example.heritage_sharing_api.dto.admin;

import java.util.List;

public class ReviewListResponseDto {
    private List<ReviewListItemDto> items;
    private int currentPage;
    private int pageSize;
    private int totalPages;
    private long totalElements;
    private boolean hasNext;
    private boolean hasPrevious;

    public ReviewListResponseDto() {
    }

    public ReviewListResponseDto(List<ReviewListItemDto> items) {
        this.items = items;
    }

    public ReviewListResponseDto(List<ReviewListItemDto> items,
                                 int currentPage,
                                 int pageSize,
                                 int totalPages,
                                 long totalElements,
                                 boolean hasNext,
                                 boolean hasPrevious) {
        this.items = items;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    public List<ReviewListItemDto> getItems() {
        return items;
    }

    public void setItems(List<ReviewListItemDto> items) {
        this.items = items;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
}

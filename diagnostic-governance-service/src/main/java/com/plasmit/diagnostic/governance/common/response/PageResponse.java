package com.plasmit.diagnostic.governance.common.response;

import java.util.List;

public class PageResponse<T> {

    private List<T> rows;
    private PaginationMeta pagination;

    public PageResponse() {
    }

    public PageResponse(List<T> rows, PaginationMeta pagination) {
        this.rows = rows;
        this.pagination = pagination;
    }

    public List<T> getRows() {
        return rows;
    }

    public PaginationMeta getPagination() {
        return pagination;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }

    public void setPagination(PaginationMeta pagination) {
        this.pagination = pagination;
    }
}
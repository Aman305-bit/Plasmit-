package com.plasmit.diagnostic.integration.common.response;

public class PaginationMeta {

    private Integer page;
    private Integer limit;
    private Long total;
    private Integer totalPages;
    private Boolean hasNextPage;
    private Boolean hasPreviousPage;

    public PaginationMeta() {
    }

    public PaginationMeta(Integer page, Integer limit, Long total) {
        this.page = page;
        this.limit = limit;
        this.total = total;

        if (limit == null || limit <= 0) {
            this.totalPages = 0;
            this.hasNextPage = false;
            this.hasPreviousPage = false;
        } else {
            this.totalPages = (int) Math.ceil((double) total / limit);
            this.hasNextPage = page < this.totalPages;
            this.hasPreviousPage = page > 1;
        }
    }

    public Integer getPage() {
        return page;
    }

    public Integer getLimit() {
        return limit;
    }

    public Long getTotal() {
        return total;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public Boolean getHasNextPage() {
        return hasNextPage;
    }

    public Boolean getHasPreviousPage() {
        return hasPreviousPage;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public void setHasNextPage(Boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }

    public void setHasPreviousPage(Boolean hasPreviousPage) {
        this.hasPreviousPage = hasPreviousPage;
    }
}
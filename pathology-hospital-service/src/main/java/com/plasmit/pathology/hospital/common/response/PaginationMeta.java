package com.plasmit.pathology.hospital.common.response;

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
        this.totalPages = (int) Math.ceil((double) total / limit);
        this.hasNextPage = page < this.totalPages;
        this.hasPreviousPage = page > 1;
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
}
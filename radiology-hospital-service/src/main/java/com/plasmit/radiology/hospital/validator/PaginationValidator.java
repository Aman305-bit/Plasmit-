package com.plasmit.radiology.hospital.validator;

import com.plasmit.radiology.hospital.common.exception.ApiException;
import org.springframework.stereotype.Component;

@Component
public class PaginationValidator {

    public void validate(Integer page, Integer limit) {
        if (page == null || page < 1) {
            throw ApiException.validation("page must be greater than or equal to 1.");
        }

        if (limit == null || limit < 1) {
            throw ApiException.validation("limit must be greater than or equal to 1.");
        }

        if (limit > 100) {
            throw ApiException.validation("limit cannot be greater than 100.");
        }
    }
}
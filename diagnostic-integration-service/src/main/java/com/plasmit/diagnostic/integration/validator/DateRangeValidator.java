package com.plasmit.diagnostic.integration.validator;

import com.plasmit.diagnostic.integration.common.exception.ApiException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class DateRangeValidator {

    public void validateMaxRangeDays(LocalDate fromDate,
                                     LocalDate toDate,
                                     long maxDays) {

        if (fromDate == null) {
            throw ApiException.validation("fromDate is required.");
        }

        if (toDate == null) {
            throw ApiException.validation("toDate is required.");
        }

        if (fromDate.isAfter(toDate)) {
            throw ApiException.validation("fromDate must be before or equal to toDate.");
        }

        long days = ChronoUnit.DAYS.between(fromDate, toDate);

        if (days > maxDays) {
            throw ApiException.validation("Date range cannot be greater than " + maxDays + " days.");
        }
    }
}
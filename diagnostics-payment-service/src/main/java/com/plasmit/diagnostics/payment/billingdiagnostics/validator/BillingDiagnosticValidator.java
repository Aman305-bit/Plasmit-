package com.plasmit.diagnostics.payment.billingdiagnostics.validator;

import com.plasmit.diagnostics.payment.billingdiagnostics.dto.request.DiagnosticCartItemRequest;
import com.plasmit.diagnostics.payment.billingdiagnostics.dto.request.DiagnosticCartValidateRequest;
import com.plasmit.diagnostics.payment.billingdiagnostics.dto.request.DiagnosticInvoiceCreateRequest;
import com.plasmit.diagnostics.payment.common.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class BillingDiagnosticValidator {

    public void validateServiceSearch(Integer page, Integer limit) {
        if (page == null || page < 1) {
            throw ApiException.validation("page must be greater than or equal to 1.");
        }
        if (limit == null || limit < 1 || limit > 100) {
            throw ApiException.validation("limit must be between 1 and 100.");
        }
    }

    public void validateCart(DiagnosticCartValidateRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }
        if (request.payerType() == null || request.payerType().isBlank()) {
            throw ApiException.validation("payerType is required.");
        }
        validateItems(request.items());
    }

    public void validateInvoiceCreate(DiagnosticInvoiceCreateRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }
        if (request.patientId() == null) {
            throw ApiException.validation("patientId is required.");
        }
        if (request.patientName() == null || request.patientName().isBlank()) {
            throw ApiException.validation("patientName is required.");
        }
        if (request.payerType() == null || request.payerType().isBlank()) {
            throw ApiException.validation("payerType is required.");
        }
        validateItems(request.items());
    }

    public void validateHandoffId(Long handoffId) {
        if (handoffId == null || handoffId <= 0) {
            throw ApiException.validation("Valid handoffId is required.");
        }
    }

    private void validateItems(java.util.List<DiagnosticCartItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw ApiException.validation("At least one diagnostic item is required.");
        }

        Set<Long> serviceIds = new HashSet<>();

        for (DiagnosticCartItemRequest item : items) {
            if (item.serviceId() == null || item.serviceId() <= 0) {
                throw ApiException.validation("Valid serviceId is required.");
            }

            if (item.quantity() == null || item.quantity() < 1) {
                throw ApiException.validation("quantity must be at least 1.");
            }

            if (item.discountPaise() != null && item.discountPaise() < 0) {
                throw ApiException.validation("discountPaise cannot be negative.");
            }

            if (!serviceIds.add(item.serviceId())) {
                throw ApiException.validation("Duplicate serviceId not allowed: " + item.serviceId());
            }
        }
    }
}
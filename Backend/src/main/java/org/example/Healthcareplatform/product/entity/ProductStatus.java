package org.example.Healthcareplatform.product.entity;

/**
 * Moderation status for products. Existing products default to
 * APPROVED; new products created by vendors start as PENDING.
 */
public enum ProductStatus {
    PENDING,
    APPROVED,
    REJECTED
}

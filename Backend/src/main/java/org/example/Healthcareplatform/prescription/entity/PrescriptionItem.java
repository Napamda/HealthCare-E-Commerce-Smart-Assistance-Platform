package org.example.Healthcareplatform.prescription.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A medication that the pharmacist selected from the product catalog while
 * approving a prescription. The patient can add these items to their own cart
 * in one action instead of searching for each medication themselves.
 */
@Entity
@Table(name = "prescription_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prescription_id", nullable = false)
    private Long prescriptionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    /** Snapshot of the unit price at the time of approval. */
    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "dosage_instructions", length = 500)
    private String dosageInstructions;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;
}

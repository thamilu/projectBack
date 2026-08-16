package com.eshop.app.catalog.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

/** ProductWarranty entity representing warranty and return policy configurations. */
@Entity
@Table(name = "product_warranty")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductWarranty {

    @Id
    @Column(name = "product_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_product_warranty_product"))
    private Product product;

    @Min(value = 0)
    @Column(name = "warranty_months")
    private Integer warrantyMonths;

    @Size(max = 1000)
    @Column(name = "warranty_description", length = 1000)
    private String warrantyDescription;

    @Min(value = 0)
    @Column(name = "return_days")
    @Builder.Default
    private Integer returnDays = 30;

    @Size(max = 1000)
    @Column(name = "return_policy", length = 1000)
    private String returnPolicy;

    @Column(name = "is_returnable", nullable = false)
    @Builder.Default
    private boolean returnable = true;
}

package com.eshop.app.catalog.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

/** DigitalProduct entity representing digital product attributes. */
@Entity
@Table(name = "digital_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigitalProduct {

    @Id
    @Column(name = "product_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_digital_product_product"))
    private Product product;

    @Size(max = 1000)
    @Column(name = "download_url", length = 1000)
    private String downloadUrl;

    @Min(value = 0)
    @Column(name = "download_limit")
    private Integer downloadLimit;

    @Min(value = 0)
    @Column(name = "download_expiry_days")
    private Integer downloadExpiryDays;
}

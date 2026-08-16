package com.eshop.app.catalog.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import lombok.*;

/** ProductDimensions entity representing shipping dimensions and weights for products. */
@Entity
@Table(name = "product_dimensions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDimensions {

    @Id
    @Column(name = "product_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(
            name = "product_id",
            foreignKey = @ForeignKey(name = "fk_product_dimensions_product"))
    private Product product;

    @DecimalMin(value = "0.00")
    @Digits(integer = 8, fraction = 4)
    @Column(name = "weight", precision = 12, scale = 4)
    private BigDecimal weight;

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_unit", length = 5)
    @Builder.Default
    private WeightUnit weightUnit = WeightUnit.KG;

    @DecimalMin(value = "0.00")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "length", precision = 10, scale = 2)
    private BigDecimal length;

    @DecimalMin(value = "0.00")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "width", precision = 10, scale = 2)
    private BigDecimal width;

    @DecimalMin(value = "0.00")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "height", precision = 10, scale = 2)
    private BigDecimal height;

    @Enumerated(EnumType.STRING)
    @Column(name = "dimension_unit", length = 5)
    @Builder.Default
    private DimensionUnit dimensionUnit = DimensionUnit.CM;
}

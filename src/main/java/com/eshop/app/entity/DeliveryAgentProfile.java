package com.eshop.app.entity;

import com.eshop.app.enums.DeliveryAgentStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "delivery_agent_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
@EqualsAndHashCode(callSuper = true, exclude = "user")
public class DeliveryAgentProfile extends BaseEntity {



    @Column(name = "vehicle_type", length = 100)
    private String vehicleType;

    @Column(name = "vehicle_number", length = 50)
    private String vehicleNumber;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Column(name = "zone", length = 100)
    private String zone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DeliveryAgentStatus status = DeliveryAgentStatus.PENDING;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

}

package com.eshop.app.catalog.api.response;

import com.eshop.app.inventory.api.response.WarehouseDto;
import com.eshop.app.location.api.response.DeliveryZoneDto;
import com.eshop.app.location.api.response.PickupLocationDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityDto {
    private Boolean isGlobal;
    private List<String> availableCountries;
    private List<String> restrictedCountries;
    private List<RegionDto> regions;
    private List<WarehouseDto> warehouses;
    private List<DeliveryZoneDto> deliveryZones;
    private List<PickupLocationDto> pickupLocations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionDto {
        private String countryCode;
        private String countryName;
        private java.util.List<StateDto> states;
        private Boolean allStatesAvailable;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StateDto {
        private String stateCode;
        private String stateName;
        private java.util.List<String> cities;
        private java.util.List<String> zipCodes;
    }
}



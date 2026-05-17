package com.eshop.app.seed.seeders;

public record PostalCodeRow(
    String isoCode, String countryName, String phoneCode,
    String stateName, String stateCode, String districtName,
    String talukName, String pinCode, String localityName, String postOfficeName
) {}

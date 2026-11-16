package com.idonice.mini_wsa.dto;

import jakarta.validation.constraints.Size;

public record GeoLocationRequest(
        @Size(max = 2) String country,
        @Size(max = 100) String city
) {}

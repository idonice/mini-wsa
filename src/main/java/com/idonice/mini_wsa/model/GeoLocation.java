package com.idonice.mini_wsa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record GeoLocation(
        @Column(name = "geo_country") String country,
        @Column(name = "geo_city") String city
) {}

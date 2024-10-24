package com.es.geolocalization;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AtmStop {
    private String code;
    private String description;
    private List<String> availableLines;
    private GeoPoint position;
}
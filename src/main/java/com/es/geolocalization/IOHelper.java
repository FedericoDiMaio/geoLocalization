package com.es.geolocalization;

import java.io.InputStream;

public class IOHelper {
    public static InputStream loadClasspathResource(String path) {
        InputStream is = GeoLocalizationApplication.class.getClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalArgumentException("Resource NOT found in classpath: " + path);
        }
        return is;
    }
}
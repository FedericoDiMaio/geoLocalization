package com.es.geolocalization;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;
import spark.Request;
import spark.Response;

import java.io.InputStream;

import java.util.List;
import java.util.Map;

import static spark.Spark.*;

@Slf4j
public class GeoLocalizationApplication {
	private static final Gson gson = new Gson();
	private static final Yaml yaml = new Yaml();

	public static void main(String[] args) {
		GeocodingService geocodingService = createGeocodingService();
		AtmStops service = new AtmStops(geocodingService);
		configureExceptionHandling();
		configureRoutes(service);
		log.info("Service available at http://localhost:4567/atm/stops");
	}

	private static void configureExceptionHandling() {
		exception(Exception.class, (e, req, res) -> {
			log.error("Uncaught exception", e);
			handleServerError(res);
		});

		notFound((req, res) -> {
			res.type("application/json");
			return gson.toJson(Map.of("error", "Not Found"));
		});
	}

	private static void handleServerError(Response res) {
		res.status(500);
		res.type("application/json");
		res.body(gson.toJson(Map.of("error", "Internal Server Error")));
	}

	private static Object handleClientError(Response res, int statusCode, String errorMessage) {
		res.status(statusCode);
		return Map.of("error", errorMessage, "status", statusCode);
	}

	private static void configureRoutes(AtmStops service) {
		get("/atm/stops", (req, res) -> {
			try {
				String address = getAddressParameter(req);
				List<AtmStop> stops = service.stopsNearAddress(address);
				if (stops.isEmpty()) {
					return Map.of("message", "No stops found near the given address", "stops", stops);
				}
				return Map.of("stops", stops);
			} catch (IllegalArgumentException e) {
				return handleClientError(res, 400, "Invalid address parameter: " + e.getMessage());
			} catch (GeocodingException e) {
				return handleClientError(res, 422, "Unable to geocode address: " + e.getMessage());
			} catch (AddressOutOfMilanException e) {
				return handleClientError(res, 422, e.getMessage());
			}
		}, gson::toJson);
	}

	private static String getAddressParameter(Request req) {
		String address = req.queryParams("address");
		if (address == null || address.trim().isEmpty()) {
			throw new IllegalArgumentException("Address parameter is required");
		}
		return address.trim();
	}

	private static GeocodingService createGeocodingService() {
		Map<String, Object> config = loadConfiguration();
		Map<String, Object> appConfig = (Map<String, Object>) config.get("app");
		String geocodingServiceUrl = (String) appConfig.get("geocodingServiceUrl");
		String apiKey = (String) appConfig.get("geocodingApiKey");
		return new GeocodingService(geocodingServiceUrl, apiKey);
	}

	private static Map<String, Object> loadConfiguration() {
		try (InputStream configFile = IOHelper.loadClasspathResource("config.yml")) {
			return yaml.load(configFile);
		} catch (Exception e) {
			throw new RuntimeException("Failed to load configuration", e);
		}
	}

	static class GeocodingException extends RuntimeException {
		public GeocodingException(String message) {
			super(message);
		}
	}

	public static class AddressOutOfMilanException extends RuntimeException {
		public AddressOutOfMilanException(String message) {
			super(message);
		}
	}

}

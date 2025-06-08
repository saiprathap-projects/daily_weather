package Weather_application.basic.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final String API_KEY = "3cd98018f5dc8e61267f3931b1cbff89";

    @GetMapping
    public ResponseEntity<?> getWeather(@RequestParam(required = false) String city) {
        if (city == null || city.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing city parameter"));
        }

        String url = String.format(
            "http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric",
            city, API_KEY
        );

        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> apiResponse = restTemplate.getForObject(url, Map.class);

        if (apiResponse == null || !apiResponse.containsKey("main") || !apiResponse.containsKey("wind")) {
            return ResponseEntity.status(502).body(Map.of("error", "Invalid response from weather API"));
        }

        Map<String, Object> main = (Map<String, Object>) apiResponse.get("main");
        Map<String, Object> wind = (Map<String, Object>) apiResponse.get("wind");

        return ResponseEntity.ok(Map.of(
            "temperature", main.get("temp"),
            "humidity", main.get("humidity"),
            "wind_speed", wind.get("speed")
        ));
    }
}

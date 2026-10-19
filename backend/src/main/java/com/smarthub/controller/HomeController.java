package com.smarthub.controller;

import com.smarthub.dto.ActionResultDto;
import com.smarthub.dto.HomeDto;
import com.smarthub.dto.LocationDto;
import com.smarthub.service.HomeService;
import com.smarthub.service.HomeStateCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class HomeController {

    private final HomeService homeService;
    private final HomeStateCache cache;

    public HomeController(HomeService homeService, HomeStateCache cache) {
        this.homeService = homeService;
        this.cache = cache;
    }

    /** Riepilogo completo per la home dashboard. */
    @GetMapping("/home")
    public HomeDto home() {
        return homeService.home();
    }

    /** Spegne tutte le luci della casa. */
    @PostMapping("/home/actions/lights-off")
    public ActionResultDto allLightsOff() {
        return homeService.allLightsOff();
    }

    /** Location attiva (il backend gestisce una location alla volta, configurabile con SMARTTHINGS_LOCATION_ID). */
    @GetMapping("/locations")
    public List<LocationDto> locations() {
        return List.of(LocationDto.from(cache.location()));
    }
}

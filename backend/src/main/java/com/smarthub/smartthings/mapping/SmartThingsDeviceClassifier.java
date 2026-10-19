package com.smarthub.smartthings.mapping;

import com.smarthub.model.DeviceFamily;
import com.smarthub.model.DeviceType;
import com.smarthub.smartthings.api.StCategory;
import com.smarthub.smartthings.api.StComponent;
import com.smarthub.smartthings.api.StDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.smarthub.smartthings.api.StCapabilities.*;

/**
 * Decide il tipo normalizzato di un dispositivo SmartThings.
 * <p>
 * Ordine di classificazione (category-first):
 * <ol>
 *   <li>override esplicito in configurazione per deviceId;</li>
 *   <li>categoria SmartThings utilizzabile: prima quelle del component {@code main}, poi quelle degli altri
 *       component; le categorie specifiche vincono su quelle generiche ({@code Others}, {@code GenericSensor});</li>
 *   <li>fallback sulle capabilities, solo con combinazioni semanticamente forti: mai da una capability isolata
 *       che potrebbe appartenere a più tipi di dispositivo (es. {@code temperatureMeasurement});</li>
 *   <li>categoria generica, se presente, oppure {@link DeviceType#UNKNOWN}.</li>
 * </ol>
 * La categoria originale viene sempre preservata così com'è, anche se sconosciuta.
 * Classe senza stato e senza dipendenze da Spring, quindi testabile in isolamento.
 */
public final class SmartThingsDeviceClassifier {

    private static final Logger log = LoggerFactory.getLogger(SmartThingsDeviceClassifier.class);

    /** Da dove deriva la classificazione: utile nei log diagnostici. */
    public enum Source { OVERRIDE, SMARTTHINGS_CATEGORY, CAPABILITY_FALLBACK, NONE }

    /**
     * Esito della classificazione.
     *
     * @param smartThingsCategory categoria originale SmartThings, esattamente come ricevuta; null se assente
     * @param deviceType          tipo normalizzato Smart Hub
     * @param source              origine della decisione
     */
    public record Classification(String smartThingsCategory, DeviceType deviceType, Source source) {

        public DeviceFamily family() {
            return deviceType.family();
        }
    }

    private final Map<String, DeviceType> overrides;

    public SmartThingsDeviceClassifier(Map<String, DeviceType> overrides) {
        this.overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
    }

    public Classification classify(StDevice raw) {
        Classification result = decide(raw);
        if (log.isDebugEnabled()) {
            log.debug("Device '{}': category={} resolvedType={} family={} source={}",
                    raw.displayName(), result.smartThingsCategory() == null ? "missing" : result.smartThingsCategory(),
                    result.deviceType(), result.family(), result.source());
        }
        return result;
    }

    private Classification decide(StDevice raw) {
        DeviceType override = overrides.get(raw.deviceId());
        Candidate best = bestCategory(raw);
        String categoryName = best == null ? null : best.category().name();

        if (override != null) {
            return new Classification(categoryName, override, Source.OVERRIDE);
        }
        if (best != null && best.type() != null && !best.type().isWeakCategory()) {
            return new Classification(categoryName, best.type(), Source.SMARTTHINGS_CATEGORY);
        }
        DeviceType fallback = fromCapabilities(raw);
        if (fallback != null) {
            return new Classification(categoryName, fallback, Source.CAPABILITY_FALLBACK);
        }
        if (best != null && best.type() != null) {
            return new Classification(categoryName, best.type(), Source.SMARTTHINGS_CATEGORY);
        }
        return new Classification(categoryName, DeviceType.UNKNOWN, Source.NONE);
    }

    /** Una categoria candidata con il tipo che ne deriva (null se la categoria non è riconosciuta). */
    record Candidate(StCategory category, DeviceType type) {
    }

    /**
     * Sceglie la categoria da usare, in modo deterministico:
     * 1) categoria specifica del component main; 2) categoria specifica degli altri component;
     * 3) categoria generica (GenericSensor prima di Others); 4) categoria non riconosciuta, preservata come testo.
     * All'interno dello stesso component le categorie {@code user} hanno precedenza su {@code manufacturer}.
     */
    static Candidate bestCategory(StDevice raw) {
        List<Candidate> candidates = candidates(raw);
        Optional<Candidate> specific = candidates.stream()
                .filter(c -> c.type() != null && !c.type().isWeakCategory()).findFirst();
        if (specific.isPresent()) {
            return specific.get();
        }
        Optional<Candidate> generic = candidates.stream()
                .filter(c -> c.type() == DeviceType.GENERIC_SENSOR).findFirst()
                .or(() -> candidates.stream().filter(c -> c.type() == DeviceType.OTHER).findFirst());
        if (generic.isPresent()) {
            return generic.get();
        }
        return candidates.stream().findFirst().orElse(null);
    }

    private static List<Candidate> candidates(StDevice raw) {
        List<Candidate> result = new ArrayList<>();
        if (raw.components() == null) {
            return result;
        }
        // Prima il component main, poi gli altri nell'ordine ricevuto.
        StComponent main = raw.mainComponent().orElse(null);
        List<StComponent> ordered = new ArrayList<>();
        if (main != null) {
            ordered.add(main);
        }
        for (StComponent component : raw.components()) {
            if (component != null && component != main) {
                ordered.add(component);
            }
        }
        for (StComponent component : ordered) {
            if (component.categories() == null) {
                continue;
            }
            List<StCategory> sorted = new ArrayList<>(component.categories());
            sorted.sort((a, b) -> Boolean.compare(!"user".equalsIgnoreCase(a.categoryType()),
                    !"user".equalsIgnoreCase(b.categoryType())));
            for (StCategory category : sorted) {
                if (category != null && category.name() != null && !category.name().isBlank()) {
                    DeviceType type = DeviceType.fromSmartThingsCategory(category.name()).orElse(null);
                    result.add(new Candidate(category, type));
                }
            }
        }
        return result;
    }

    /**
     * Fallback da capabilities: solo combinazioni forti. Una singola capability non basta mai
     * a stabilire il tipo (es. temperatureMeasurement appartiene a frigoriferi, forni, condizionatori...).
     */
    static DeviceType fromCapabilities(StDevice raw) {
        if (raw.hasCapability(VIDEO_CAMERA) || raw.hasCapability(VIDEO_STREAM)) {
            return DeviceType.CAMERA;
        }
        if (raw.hasCapability(WINDOW_SHADE) || raw.hasCapability(WINDOW_SHADE_LEVEL)) {
            return DeviceType.BLIND;
        }
        boolean thermostatMode = raw.hasCapability(THERMOSTAT_MODE);
        boolean setpoint = raw.hasCapability(THERMOSTAT_HEATING_SETPOINT) || raw.hasCapability(THERMOSTAT_COOLING_SETPOINT);
        if (thermostatMode && setpoint) {
            return DeviceType.THERMOSTAT;
        }
        if (raw.hasCapability(SWITCH) && (raw.hasCapability(SWITCH_LEVEL) || raw.hasCapability(COLOR_CONTROL)
                || raw.hasCapability(COLOR_TEMPERATURE))) {
            return DeviceType.LIGHT;
        }
        if (raw.hasCapability(SWITCH)) {
            return DeviceType.SWITCH;
        }
        if (raw.hasCapability(MOTION) || raw.hasCapability(CONTACT) || raw.hasCapability(TEMPERATURE)
                || raw.hasCapability(HUMIDITY)) {
            return DeviceType.GENERIC_SENSOR;
        }
        return null;
    }
}

package com.smarthub.model;

/** Comando normalizzato già validato. */
public record DeviceCommand(CommandType type, Object value) {

    public int intValue() {
        return (int) Math.round(doubleValue());
    }

    public double doubleValue() {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Valore numerico non valido: " + s);
            }
        }
        throw new IllegalArgumentException("Il comando " + type + " richiede un valore numerico");
    }

    public String stringValue() {
        if (value == null) {
            throw new IllegalArgumentException("Il comando " + type + " richiede un valore");
        }
        return value.toString();
    }

    public ColorValue colorValue() {
        if (value instanceof java.util.Map<?, ?> map && map.get("hue") instanceof Number h
                && map.get("saturation") instanceof Number s) {
            return new ColorValue(clamp(h.intValue(), 0, 360), clamp(s.intValue(), 0, 100));
        }
        throw new IllegalArgumentException("SET_COLOR richiede { \"hue\": 0..360, \"saturation\": 0..100 }");
    }

    public static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}

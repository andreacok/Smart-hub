package com.smarthub.smartthings.api;

import java.util.List;
import java.util.Map;

/** Risposta paginata SmartThings: { items: [...], _links: { next: { href } } }. */
public record StPage<T>(List<T> items, Map<String, StLink> _links) {

    public record StLink(String href) {
    }

    public String nextHref() {
        if (_links == null) {
            return null;
        }
        StLink next = _links.get("next");
        return next == null || next.href() == null || next.href().isBlank() ? null : next.href();
    }
}

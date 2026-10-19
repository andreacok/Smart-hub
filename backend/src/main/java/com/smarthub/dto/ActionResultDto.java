package com.smarthub.dto;

import java.util.List;

/** Esito di un'azione su più dispositivi (es. "spegni tutto"). */
public record ActionResultDto(int succeeded, int failed, List<String> errors) {
}

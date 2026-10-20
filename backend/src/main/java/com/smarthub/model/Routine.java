package com.smarthub.model;

/**
 * Routine SmartThings normalizzata, indipendente dall'API di origine.
 * {@code manuallyExecutable} è true solo quando SmartThings espone un endpoint di esecuzione per quella entità.
 */
public record Routine(String id, String name, RoutineType type, boolean manuallyExecutable) {
}

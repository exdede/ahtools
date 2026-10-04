package dev.exdede.ahtools.core;

/**
 * One hotbar slot's contents. itemKey is the vanilla registry id
 * ("minecraft:observer"), which is what validation compares on, while
 * displayName is what chat calls the same item ("Observer"), which is what the
 * sale ledger has to match against.
 */
public record HeldStack(String itemKey, String displayName, int amount) {}

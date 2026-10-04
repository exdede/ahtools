package dev.exdede.ahtools.config;

import com.google.gson.JsonElement;

/**
 * One persisted value.
 *
 * Reading a bad value never throws: a config file is edited by hand often
 * enough that a single typo must not stop the mod from loading. Every subclass
 * falls back to its default instead.
 */
public abstract class Setting<T> {
    private final String name;
    private final String comment;
    private final T defaultValue;
    private T value;

    protected Setting(String name, String comment, T defaultValue) {
        this.name = name;
        this.comment = comment;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String name() { return name; }
    public String comment() { return comment; }
    public T defaultValue() { return defaultValue; }
    public T get() { return value; }

    public void set(T newValue) {
        this.value = newValue == null ? defaultValue : sanitize(newValue);
    }

    public void reset() { this.value = defaultValue; }

    /** Applied on every set, so a value from disk and a value from the GUI get the same treatment. */
    protected T sanitize(T candidate) { return candidate; }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement element);
}

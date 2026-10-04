package dev.exdede.ahtools.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * The constant list comes from the default value's own class, so a caller
 * never has to hand the class object in separately.
 */
public final class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final E[] values;

    @SuppressWarnings("unchecked")
    public EnumSetting(String name, E defaultValue, String comment) {
        super(name, comment, defaultValue);
        this.values = (E[]) defaultValue.getDeclaringClass().getEnumConstants();
    }

    public E[] values() { return values.clone(); }

    public void cycle() {
        set(values[(get().ordinal() + 1) % values.length]);
    }

    @Override
    public JsonElement toJson() { return new JsonPrimitive(get().name()); }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) return;
        String name = element.getAsString();
        for (E candidate : values) {
            if (candidate.name().equals(name)) {
                set(candidate);
                return;
            }
        }
    }
}

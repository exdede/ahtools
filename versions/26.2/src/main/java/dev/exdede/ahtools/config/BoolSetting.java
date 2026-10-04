package dev.exdede.ahtools.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, boolean defaultValue, String comment) {
        super(name, comment, defaultValue);
    }

    public void toggle() { set(!get()); }

    @Override
    public JsonElement toJson() { return new JsonPrimitive(get()); }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) set(element.getAsBoolean());
    }
}

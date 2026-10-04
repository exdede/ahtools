package dev.exdede.ahtools.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class IntSetting extends Setting<Integer> {
    private final int min;
    private final int max;

    public IntSetting(String name, int defaultValue, int min, int max, String comment) {
        super(name, comment, defaultValue);
        this.min = min;
        this.max = max;
        set(defaultValue);
    }

    public int min() { return min; }
    public int max() { return max; }

    @Override
    protected Integer sanitize(Integer candidate) {
        return Math.max(min, Math.min(max, candidate));
    }

    @Override
    public JsonElement toJson() { return new JsonPrimitive(get()); }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) return;
        try {
            set(element.getAsInt());
        }
        catch (NumberFormatException e) {
            reset();
        }
    }
}

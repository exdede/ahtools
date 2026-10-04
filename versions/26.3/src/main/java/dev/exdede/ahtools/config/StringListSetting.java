package dev.exdede.ahtools.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

public final class StringListSetting extends Setting<List<String>> {
    public StringListSetting(String name, List<String> defaultValue, String comment) {
        super(name, comment, List.copyOf(defaultValue));
    }

    @Override
    protected List<String> sanitize(List<String> candidate) {
        return List.copyOf(candidate);
    }

    @Override
    public JsonElement toJson() {
        JsonArray array = new JsonArray();
        for (String entry : get()) array.add(entry);
        return array;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonArray()) return;
        List<String> entries = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry.isJsonPrimitive()) entries.add(entry.getAsString());
        }
        set(entries);
    }
}

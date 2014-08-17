package trackvia.client.model;

import com.google.gson.*;

import java.lang.reflect.Type;

public class RecordDataSerializer implements JsonSerializer<RecordData> {
    @Override
    public JsonElement serialize(RecordData recordData, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject data = new JsonObject();

        recordData.remove(Identifiable.INTERNAL_ID_FIELD_NAME);

        for (String key : recordData.keySet()) {
            data.add(key, jsonSerializationContext.serialize(recordData.get(key)));
        }

        return data;
    }
}

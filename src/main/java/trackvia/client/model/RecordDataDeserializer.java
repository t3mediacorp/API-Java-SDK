package trackvia.client.model;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecordDataDeserializer implements JsonDeserializer<RecordData> {
    @Override
    public RecordData deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)
            throws JsonParseException {
        RecordData rd = new RecordData();
        JsonObject recordDataObject = jsonElement.getAsJsonObject();

        // 'id' is stored on every record.
        JsonElement idElement = recordDataObject.get("id");
        rd.put("id", idElement.getAsLong());

        // All other non-'id' fields.
        Set<Map.Entry<String, JsonElement>> entries = recordDataObject.entrySet();
        for (Map.Entry<String, JsonElement> entry : entries) {
            String fieldName = entry.getKey();
            if (!"id".equalsIgnoreCase(fieldName)) {
                rd.put(fieldName, deserialize(entry.getValue()));
            }
        }
        return rd;
    }

    private Object deserialize(JsonElement jsonElement) {
        Object result;

        if (jsonElement.isJsonPrimitive()) {
            result = deserialize(jsonElement.getAsJsonPrimitive());
        } else if (jsonElement.isJsonNull()) {
            result = null;
        } else if (jsonElement.isJsonArray()) {
            result = deserialize(jsonElement.getAsJsonArray());
        } else {

            throw new IllegalArgumentException("JSON Object deserialization is unsupported");
        }

        return result;
    }

    private Object deserialize(JsonPrimitive jsonPrimitive) {
        String stringValue = jsonPrimitive.getAsString();
        Object result;

        // gson converts all numbers to double type.  differentiate
        // between integer and decimal numbers.
        if (jsonPrimitive.isNumber()) {
            try {
                result = Long.parseLong(stringValue);
            } catch (NumberFormatException e) {
                result = Double.parseDouble(stringValue);
            }
        } else if (jsonPrimitive.isBoolean()) {
            result = jsonPrimitive.getAsBoolean();
        } else {
            result = stringValue;
        }

        return result;
    }

    private Object deserialize(JsonArray jsonArray) {
        List<Object> result = new ArrayList<Object>();

        for (JsonElement jsonElement : jsonArray) {
            result.add(deserialize(jsonElement));
        }

        return result;
    }
}

package trackvia.client.model;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * Deserializes Trackvia JSON data, storing it in RecordData's Map<String, Object>
 *
 * @see trackvia.client.model.RecordData
 *
 * Deserialization observes these mapping rules:
 *
 *      Trackvia type                           Mapped as this type, set as the map's value
 *      -----------------------------------------------------------------------------------
 *      DateTime("datetime")                    java.util.Date
 *      User("user")                            String
 *      Identifier("identifier")                Long
 *      ShortAnswer("shortAnswer")              String
 *      Email("email")                          String
 *      UserStatus("userStatus")                String
 *      TimeZone("timeZone")                    String
 *      Paragraph("paragraph")                  String
 *      Number("number")                        Double
 *      Percentage("percentage")                Double
 *      Currency("currency")                    Double
 *      AutoIncrement("autoIncrement")          Long
 *      DropDown("dropDown")                    List<String>
 *      CheckBox("checkbox")                    List<String>
 *      Date("date")                            java.util.Date
 *      Document("document")                    Long
 *      Image("image")                          Long
 *      URL("url")                              String
 */

public class RecordDataDeserializer implements JsonDeserializer<RecordData> {
    private DateFormat iso8601Formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    @Override
    public RecordData deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)
            throws JsonParseException {
        RecordData rd = new RecordData();
        JsonObject recordDataObject = jsonElement.getAsJsonObject();

        // All other non-'id' fields.
        Set<Map.Entry<String, JsonElement>> entries = recordDataObject.entrySet();
        for (Map.Entry<String, JsonElement> entry : entries) {
            String fieldName = entry.getKey();
            rd.put(fieldName, deserialize(entry.getValue()));
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

        // By default, Gson converts all numbers to double type.  Differentiate
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
            try {
                result = this.iso8601Formatter.parse(stringValue);
            } catch (ParseException e) {
                result = stringValue;
            }
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

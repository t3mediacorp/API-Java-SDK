package trackvia.client.model;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * De-serializes a Trackvia JSON record object, applying a case-insensitive "method & field"
 * name-matching strategy.
 *
 * De-serialization uses record field-metadata to correctly interpret each field value,
 * and properly map it to a corresponding Java type.
 *
 * <pre>
 *     {@code
 *
 *      Trackvia type                           Expected Java type on the domain class
 *      -------------------------------------------------------------------------------
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
 *     }
 *
 * </pre>
 *
 * Limitations:
 * <ul>
 *     <li>Nested, embedded JSON objects are not supported (e.g., Map within a Map).</li>
 * </ul>
 *
 * No support for nested/embedded json structure is not a showstopper, turning into a little more code
 * for the application integrator to write, translating "foreign key" identifiers into additional client.getRecord()
 * calls.
 *
 * @see trackvia.client.model.TrackviaDataType
 *
 */
public class DomainRecordSetDeserializer<T> extends DomainRecordDeserializerBase<T> implements JsonDeserializer<DomainRecordSet<T>> {
    protected Class<T> domainClass;
    protected Map<String, Method> methodNameToMethodIndex;

    public DomainRecordSetDeserializer(Class<T> domainClass) {
        super(domainClass);
    }

    @Override
    public DomainRecordSet<T> deserialize(JsonElement jsonElement, Type type,
            JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        final JsonObject responsePayload = jsonElement.getAsJsonObject();
        final JsonArray structureArray = responsePayload.getAsJsonArray("structure");
        final JsonArray dataArray = responsePayload.getAsJsonArray("data");
        final int recordCount = responsePayload.getAsJsonPrimitive("totalCount").getAsInt();

        // deserialize 'structure' elements
        final Type structureType = new TypeToken<List<FieldMetadata>>() {}.getType();
        final List<FieldMetadata> structure = jsonDeserializationContext.deserialize(structureArray, structureType);
        final List<T> data = new ArrayList<T>();

        // deserialize 'data' elements
        final Map<String, FieldMetadata> fieldNameToFieldMetaMap = new HashMap<String, FieldMetadata>();
        for (FieldMetadata fm : structure) {
            final String normalizedName = fm.getName().toUpperCase();
            fieldNameToFieldMetaMap.put(normalizedName, fm);
        }

        for (JsonElement recordJsonElement : dataArray) {
            data.add(deserializeDomainRecord(fieldNameToFieldMetaMap, recordJsonElement.getAsJsonObject()));
        }

        return new DomainRecordSet<T>(structure, data, recordCount);
    }
}

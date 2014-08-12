package trackvia.client.model;

import com.google.gson.*;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class DomainRecordDeserializerBase<T> {
    protected Class<T> domainClass;
    protected Map<String, Method> methodNameToMethodIndex;

    public DomainRecordDeserializerBase(Class<T> domainClass) {
        this.domainClass = domainClass;

        buildMethodIndex(domainClass);
    }

    protected void buildMethodIndex(Class<T> clazz) {
        this.methodNameToMethodIndex = new HashMap<String, Method>();

        for (Method method : clazz.getMethods()) {
            if (method.getName().startsWith("set") && method.getName().length() > "set".length()) {
                final int startIndex = "set".length();
                final String normalizedName = method.getName().substring(startIndex).toUpperCase();

                this.methodNameToMethodIndex.put(normalizedName, method);
            }
        }
    }

    protected T deserializeDomainRecord(final Map<String, FieldMetadata> fm, final JsonObject recordDataObject) {
        final T recordData;

        try {
            recordData = this.domainClass.newInstance();
        } catch (Exception e) {
            throw new JsonParseException(String.format("Error creating a new instance of domain class: %s",
                    this.domainClass.getName()), e);
        }

        // 'id' is stored on every record.
        final JsonElement idElement = recordDataObject.get("id");
        setDomainFieldValue(recordData, "id", idElement.getAsLong());

        // All other non-'id' fields.
        final Set<Map.Entry<String, JsonElement>> entries = recordDataObject.entrySet();
        for (Map.Entry<String, JsonElement> entry : entries) {
            final String fieldName = entry.getKey();

            if (!"id".equalsIgnoreCase(fieldName)) {
                final String normalizedName = fieldName.toUpperCase();
                final TrackviaDataType trackviaType = fm.get(normalizedName).getType();

                setDomainFieldValue(recordData, fieldName, deserialize(trackviaType, entry.getValue()));
            }
        }

        return recordData;

    }

    protected Object deserialize(TrackviaDataType type, JsonElement jsonElement) {
        Object value = null;
        Object intermediateValue = deserialize(jsonElement);

        // Any further transformation of the value happens next.
        switch (type) {
            case AutoIncrement:
            case User:
            case Identifier:
            case ShortAnswer:
            case Email:
            case UserStatus:
            case TimeZone:
            case Paragraph:
            case Number:
            case Percentage:
            case Currency:
            case DropDown:
            case CheckBox:
            case Document:
            case Image:
            case URL:
                value = intermediateValue;
                break;

            case Date:
            case DateTime:
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                try {
                    value = sdf.parse((String) intermediateValue);
                } catch (ParseException e) {
                    throw new JsonParseException(String.format(
                            "Error converting Trackvia DateTime value '%s' to java.util.Date", intermediateValue), e);
                }
                break;

            default:
                throw new JsonParseException(String.format("Unrecognized Trackvia Type: %s", type));
        }

        return value;
    }

    protected void setDomainFieldValue(T recordData, String fieldName, Object value) throws JsonParseException {
        final String normalizedName = fieldName.toUpperCase();
        Method m = this.methodNameToMethodIndex.get(normalizedName);
        try {
            m.invoke(recordData, value);
        } catch (Exception e) {
            throw new JsonParseException(String.format("Error invoking setter (%s(%s)): %s", m.getName(),
                    value.toString(), e.getMessage()), e);
        }
    }

    protected Object deserialize(JsonElement jsonElement) {
        final Object result;

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

    protected Object deserialize(JsonPrimitive jsonPrimitive) {
        final String stringValue = jsonPrimitive.getAsString();
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
            result = stringValue;
        }

        return result;
    }

    protected Object deserialize(JsonArray jsonArray) {
        final List<Object> result = new ArrayList<Object>();

        for (JsonElement jsonElement : jsonArray) {
            result.add(deserialize(jsonElement));
        }

        return result;
    }
}

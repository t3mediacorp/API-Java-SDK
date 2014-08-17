package trackvia.client.model;

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * Serializes a multiple records, each represented as an application-defined "domain class", given as
 * parameterized-type <T>.
 *
 * Java types map to Trackvia types as follows:
 *
 *      Java type                            Mappable (supporting) Trackvia type
 *      ------------------------------------------------------------------------
 *      Short                                Number
 *      Integer                              Number
 *      Long                                 Number, Identifier, AutoIncrement, Document, Image
 *      Float                                Number, Currency, Percentage
 *      Double                               Number, Currency, Percentage
 *      BigDecimal                           Number, Currency, Percentage
 *      String                               ShortAnswer, Paragraph
 *      java.util.Date                       ShortAnswer, Paragraph, DateTime, Date
 *      String                               User, Email, UserStatus, TimeZone, URL
 *      java.text.TimeZone                   TimeZone
 *      List<String>                         DropDown, CheckBox
 *
 * This implementation uses the Gson default serializer 100%, serving as an extension point, including
 * the default "naming policy" which literally uses the Java field-name for the Trackvia field name.
 *
 * The TrackviaClient uses a Gson exclusion strategy, to omit serialization of any Identifiable fields,
 * because a) the internal "Record ID" field has a name that's not a legal Java identifier and b)
 * setting the internal "Record ID" is not a legal API operation.
 *
 * @see trackvia.client.model.TrackviaSerializationExclusionStrategy
 */
public class DomainRecordDataBatchSerializer<T> implements JsonSerializer<DomainRecordDataBatch<T>> {
    protected Class<T> domainClass;

    public DomainRecordDataBatchSerializer(Class<T> domainClass) {
        this.domainClass = domainClass;
    }

    @Override
    public JsonElement serialize(DomainRecordDataBatch<T> t, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject wrapperObject = new JsonObject();
        JsonArray dataArray = new JsonArray();
        wrapperObject.add("data", dataArray);

        if (t.getData().size() > 0) {
            Class<T> domainClass = (Class<T>) t.getData().get(0).getClass();
            for (T domainRecord : t.getData()) {
                dataArray.add(jsonSerializationContext.serialize(domainRecord, domainClass));
            }
        }

        return wrapperObject;
    }
}

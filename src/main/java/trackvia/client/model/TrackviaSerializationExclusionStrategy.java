package trackvia.client.model;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class TrackviaSerializationExclusionStrategy implements ExclusionStrategy {
    @Override
    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
        return fieldAttributes.getDeclaringClass().equals(Identifiable.class);
    }

    @Override
    public boolean shouldSkipClass(Class<?> aClass) {
        return false;
    }
}

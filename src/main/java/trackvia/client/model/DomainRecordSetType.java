package trackvia.client.model;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class DomainRecordSetType<T> implements ParameterizedType {
    private Class<T> parameterClass;

    public DomainRecordSetType(Class<T> parameterClass) {
        this.parameterClass = parameterClass;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return new Type[] { this.parameterClass };
    }

    @Override
    public Type getRawType() {
        return DomainRecordSet.class;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }
}

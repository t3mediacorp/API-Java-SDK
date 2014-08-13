package trackvia.client.model;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class DomainRecordType<T>  implements ParameterizedType {
    private Class<T> parameterClass;

    public DomainRecordType(Class<T> parameterClass) {
        this.parameterClass = parameterClass;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return new Type[] { this.parameterClass };
    }

    @Override
    public Type getRawType() {
        return DomainRecord.class;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }
}

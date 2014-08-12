package trackvia.client.model;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class DomainRecord<T> {
    private List<FieldMetadata> structure;
    private T data;

    public DomainRecord() {}

    public DomainRecord(final List<FieldMetadata> structure, final T data) {
        this.structure = structure;
        this.data = data;
    }

    public List<FieldMetadata> getStructure() {
        return structure;
    }

    public void setStructure(List<FieldMetadata> structure) {
        this.structure = structure;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof DomainRecord)) return false;

        DomainRecord<T> otherRecord = (DomainRecord<T>) o;

        // Ensure both records have the same set of fields.  Having to do this isn't strictly
        // necessary to ensure equality.
        Set fieldNames1 = new TreeSet<String>();
        for (FieldMetadata fm : this.structure) {
            fieldNames1.add(fm.getName());
        }
        Set fieldNames2 = new TreeSet<String>();
        for (FieldMetadata fm : otherRecord.structure) {
            fieldNames2.add(fm.getName());
        }
        if (!fieldNames1.equals(fieldNames2)) return false;

        // Finally, delegate equality of data to the domain-class implementation.
        return getData().equals(otherRecord.getData());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) getData().hashCode();

        return result;
    }
}

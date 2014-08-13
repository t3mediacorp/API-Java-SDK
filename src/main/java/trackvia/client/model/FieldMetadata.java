package trackvia.client.model;

import java.util.ArrayList;
import java.util.List;

public class FieldMetadata {
    private String name;
    private String type;
    private Boolean required;
    private Boolean unique;
    private List<String> choices;

    public FieldMetadata() {}

    public FieldMetadata(final String name, final String type, final Boolean required, final Boolean unique, final List<String> choices) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.unique = unique;
        this.choices = new ArrayList<String>(choices);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TrackviaDataType getType() {
        return TrackviaDataType.get(type);
    }

    public void setType(TrackviaDataType type) {
        this.type = type.type();
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Boolean getUnique() {
        return unique;
    }

    public void setUnique(Boolean unique) {
        this.unique = unique;
    }

    public List<String> getChoices() {
        return choices;
    }

    public void setChoices(List<String> choices) {
        this.choices = choices;
    }
}

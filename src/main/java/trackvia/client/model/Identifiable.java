package trackvia.client.model;

/**
 * Makes a record identifiable, required by the Trackvia Service.
 */
public interface Identifiable {
    final String INTERNAL_ID_FIELD_NAME = "id";

    Long getId();
    void setId(Long id);
}

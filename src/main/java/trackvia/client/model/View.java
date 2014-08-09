package trackvia.client.model;

public class View {
    private String id;
    private String name;
    private String applicationName;

    public View() {}

    public View(final String id, final String name, final String applicationName) {
        this.id = id;
        this.name = name;
        this.applicationName = applicationName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof View)) return false;

        View otherView = (View) o;

        if (otherView.id == null) return false;
        if (!otherView.id.equals(id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) ((id != null) ? (id.hashCode()) : (0));
        return result;
    }
}

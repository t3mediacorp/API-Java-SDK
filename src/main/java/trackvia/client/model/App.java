package trackvia.client.model;

public class App {
    private String id;
    private String name;

    public String getId() {
        return id;
    }

    public App() {}

    public App(final String id, final String name) {
        this.id = id;
        this.name = name;
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

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof App)) return false;

        App otherApp = (App) o;

        if (otherApp.id == null) return false;
        if (!otherApp.id.equals(id)) return false;

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

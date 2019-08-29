package enrichmentapi.data;

public enum DataType {
    ENTITY,
    SIGNATURE;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

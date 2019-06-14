package enrichmentapi.dto.in;

import java.util.Set;

import static java.util.Collections.emptySet;

public abstract class InputDto {
    private Set<String> signatures = emptySet();
    private int limit = 500;
    private int offset = 0;
    private String database;
    private double significance = 0.05;

    public Set<String> getSignatures() {
        return signatures;
    }

    public void setSignatures(Set<String> signatures) {
        this.signatures = signatures;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public double getSignificance() {
        return significance;
    }

    public void setSignificance(double significance) {
        this.significance = significance;
    }
}

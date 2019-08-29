package enrichmentapi.dto.in;

import java.util.Set;

import static java.util.Collections.emptySet;

public abstract class InputDto {
    private String database;
    private Set<String> signatures = emptySet();
    private int limit = 500;
    private int offset = 0;
    private double significance = 0.05;

    InputDto(String database, Set<String> signatures, Integer limit, Integer offset, Double significance) {
        this.database = database;
        if (signatures != null) {
            this.signatures = signatures;
        }
        if (limit != null) {
            this.limit = limit;
        }
        if (offset != null) {
            this.offset = offset;
        }
        if (significance != null) {
            this.significance = significance;
        }
    }

    public Set<String> getSignatures() {
        return signatures;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public String getDatabase() {
        return database;
    }

    public double getSignificance() {
        return significance;
    }
}

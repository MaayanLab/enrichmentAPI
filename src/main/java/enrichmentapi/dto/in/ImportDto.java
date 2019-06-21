package enrichmentapi.dto.in;

import enrichmentapi.data.DatasetType;

public abstract class ImportDto {
    private String name;
    private DatasetType datasetType;
    private String databaseUrl;
    private String databaseUsername;
    private String databasePassword;
    private boolean deletePreviousVersion = true;

    ImportDto(String name, DatasetType datasetType, Boolean deletePreviousVersion,
              String databaseUrl, String databaseUsername, String databasePassword) {
        this.name = name;
        this.datasetType = datasetType;
        this.databaseUrl = databaseUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
        if (deletePreviousVersion != null) {
            this.deletePreviousVersion = deletePreviousVersion;
        }
    }

    public boolean isDeletePreviousVersion() {
        return deletePreviousVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public DatasetType getDatasetType() {
        return datasetType;
    }

    public String getDatabaseUsername() {
        return databaseUsername;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

}
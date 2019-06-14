package enrichmentapi.dto.out;

import java.util.Objects;

public class DatasetInfoDto {
    private final String uuid;
    private final String datatype;

    public DatasetInfoDto(String uuid, String datatype) {
        this.uuid = uuid;
        this.datatype = datatype;
    }

    public String getUuid() {
        return uuid;
    }

    public String getDatatype() {
        return datatype;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DatasetInfoDto)) return false;
        DatasetInfoDto that = (DatasetInfoDto) o;
        return Objects.equals(uuid, that.uuid) &&
                Objects.equals(datatype, that.datatype);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, datatype);
    }

    @Override
    public String toString() {
        return "DatasetInfoDto{" +
                "uuid='" + uuid + '\'' +
                ", datatype='" + datatype + '\'' +
                '}';
    }
}

package enrichmentapi.dto.out;

import java.util.Collection;
import java.util.Objects;

public class DatasetInfoListDto {
    private final Collection<DatasetInfoDto> repositories;

    public DatasetInfoListDto(Collection<DatasetInfoDto> repositories) {
        this.repositories = repositories;
    }

    public Collection<DatasetInfoDto> getRepositories() {
        return repositories;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DatasetInfoListDto)) return false;
        DatasetInfoListDto that = (DatasetInfoListDto) o;
        return repositories.containsAll(that.repositories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repositories);
    }
}

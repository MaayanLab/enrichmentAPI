package enrichmentapi.postgres;

import enrichmentapi.data.DatasetType;
import enrichmentapi.dto.in.ImportDto;
import enrichmentapi.exceptions.EnrichmentapiException;
import org.jetbrains.annotations.Nullable;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

public final class PostgresImportManager {

    private PostgresImportManager() {
    }

    @Nullable
    public static JdbcTemplate createJdbcTemplate(ImportDto dto) {
        if (dto.getDatabaseUrl() != null) {
            final DriverManagerDataSource driverManagerDataSource
                    = new DriverManagerDataSource(dto.getDatabaseUrl(), dto.getDatabaseUsername(), dto.getDatabasePassword());
            return new JdbcTemplate(driverManagerDataSource);
        } else {
            return null;
        }
    }

    public static void saveLibraryToPostgres(UUID libraryUuid, DatasetType type, JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("INSERT INTO libraries (uuid, dataset, dataset_type) values (?, ?, ?)",
                ps -> {
                    ps.setObject(1, libraryUuid);
                    ps.setInt(2, 1);
                    ps.setString(3, type.toString());
                });
    }

    public static void saveEntitiesToPostgres(Map<Number, String> map, JdbcTemplate jdbcTemplate) {
        final Iterator<Map.Entry<Number, String>> iterator = map.entrySet().iterator();
        jdbcTemplate.batchUpdate("INSERT INTO entities (uuid, meta) VALUES (?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        final Map.Entry<Number, String> next = iterator.next();
                        ps.setObject(1, UUID.fromString(next.getValue()));
                        ps.setObject(2, createJsonMeta());
                    }

                    @Override
                    public int getBatchSize() {
                        return map.size();
                    }
                });
    }

    public static void saveSignaturesToPostgres(Map<Number, String> map, JdbcTemplate jdbcTemplate, UUID libraryId) {
        final Iterator<Map.Entry<Number, String>> iterator = map.entrySet().iterator();
        jdbcTemplate.batchUpdate("INSERT INTO signatures (uuid, libid, meta) VALUES (?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        final Map.Entry<Number, String> next = iterator.next();
                        ps.setObject(1, UUID.fromString(next.getValue()));
                        ps.setObject(2, libraryId);
                        ps.setObject(3, createJsonMeta());
                    }

                    @Override
                    public int getBatchSize() {
                        return map.size();
                    }
                });
    }

    public static void saveSignatureArrayToPostgres(String[] signatures, JdbcTemplate jdbcTemplate, UUID libraryId) {
        final AtomicInteger index = new AtomicInteger(0);
        jdbcTemplate.batchUpdate("INSERT INTO signatures (uuid, libid, meta) VALUES (?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setObject(1, UUID.fromString(signatures[index.getAndIncrement()]));
                        ps.setObject(2, libraryId);
                        ps.setObject(3, createJsonMeta());
                    }

                    @Override
                    public int getBatchSize() {
                        return signatures.length;
                    }
                });
    }

    private static PGobject createJsonMeta() {
        PGobject jsonObject = new PGobject();
        jsonObject.setType("jsonb");
        try {
            jsonObject.setValue("{\"Name\": \"" + randomAlphabetic(20) + "\"}");
        } catch (SQLException e) {
            throw new EnrichmentapiException("", e);
        }
        return jsonObject;
    }
}

package enrichmentapi.rest;

import enrichmentapi.data.DatasetType;
import enrichmentapi.dto.in.IngestionImportDto;
import enrichmentapi.exceptions.EnrichmentapiException;
import enrichmentapi.ignite.IgniteImporter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.joining;

@RestController
@RequestMapping("/origin/api/v1")
public class IngestionController {

    private final Map<String, Map<String, Short>> dictionaries = new HashMap<>();
    private final Map<String, Map<Short, String>> revDictionaries = new HashMap<>();
    private final Map<String, Map<String, short[]>> genesetLibraries = new HashMap<>();

    private final Map<String, List<String>> repEntities = new HashMap<>();
    private final Map<String, Map<String, short[]>> repSignatures = new HashMap<>();

    private final IgniteImporter igniteImporter;

    public IngestionController(IgniteImporter igniteImporter) {
        this.igniteImporter = igniteImporter;
    }

    @PostMapping("/create")
    public void createRepository(HttpServletRequest request, HttpServletResponse response) {
        final JSONObject json = readPayload(request);
        String cacheName = json.getString("repository_uuid");

        Set<String> entities = new HashSet<>();
        if (json.optJSONArray("entities") != null) {
            final JSONArray queryEntities = json.getJSONArray("entities");
            int n = queryEntities.length();

            for (int i = 0; i < n; ++i) {
                entities.add(queryEntities.getString(i));
            }
        }
        String[] entityNames = entities.toArray(new String[0]);

        String datasetType = (String) json.get("data_type");
        if (datasetType.equals("rank_matrix")) {
            createRankMatrix(cacheName, entityNames);
        } else if (datasetType.equals("geneset_library")) {
            createGenesetLibrary(cacheName, entityNames);
        } else {
            throw new EnrichmentapiException("Datatype is not supported. Supported data repositories: geneset_library, rank_matrix");
        }
        sendStatus(response, "repository was successfully created");
    }

    @PostMapping("/append")
    public void appendRepository(HttpServletRequest request, HttpServletResponse response) {
        final JSONObject json = readPayload(request);
        String cacheName = json.getString("repository_uuid");
        if (dictionaries.containsKey(cacheName)) {
            appendGenesetLibraryRepository(cacheName, json);
        } else if (repEntities.containsKey(cacheName)) {
            appendRankRepository(cacheName, json);
        } else {
            throw new EnrichmentapiException("The uuid does not exist. The repository must first be created before data can be added.");
        }
        sendStatus(response, "data was successfully appended");
    }

    @PostMapping("/removesamples")
    public void removeSamples(HttpServletRequest request, HttpServletResponse response) {
        final JSONObject json = readPayload(request);
        String cacheName = json.getString("repository_uuid");
        if (dictionaries.containsKey(cacheName)) {
            removeSamplesGenesetLibraryRepository(cacheName, json);
        } else if (repEntities.containsKey(cacheName)) {
            removeSamplesRankRepository(cacheName, json);
        } else {
            throw new EnrichmentapiException("The uuid does not exist. The repository must first be created before data can be added.");
        }
        sendStatus(response, "samples were successfully removed from repository");
    }

    @PostMapping("/removerepository")
    public void removeRepository(HttpServletRequest request, HttpServletResponse response) {
        final JSONObject json = readPayload(request);
        String cacheName = json.getString("repository_uuid");
        removeRepository(cacheName);
        sendStatus(response, "repository was successfully removed");
    }

    @PostMapping("/persist")
    public void persistRepository(HttpServletRequest request, HttpServletResponse response) {
        final JSONObject json = readPayload(request);
        String cacheName = json.getString("repository_uuid");
        if (dictionaries.containsKey(cacheName)) {
            persistGenesetLibraryRepository(cacheName);
        } else if (repEntities.containsKey(cacheName)) {
            persistRankRepository(cacheName);
        } else {
            throw new EnrichmentapiException("The uuid does not exist. The repository must first be created before data can be added.");
        }
        sendStatus(response, "repository was successfully persisted");
    }

    @PostMapping("/listrepositories")
    public void listRepositories(HttpServletResponse response) {
        try {
            StringBuilder sb = new StringBuilder("{\"rank_repositories\": [");

            for (String rep : repSignatures.keySet()) {
                sb.append("{\"uuid\":\"").append(rep).append("\", \"entity_count\" : ").append(repEntities.get(rep).size()).append(", \"signature_count\":").append(repSignatures.get(rep).size()).append("},");
            }
            sb.append("], \"genelist_repositories\": [");

            for (String rep : genesetLibraries.keySet()) {
                sb.append("{\"uuid\":\"").append(rep).append("\", \"entity_count\" : ").append(dictionaries.get(rep).size()).append(", \"signature_count\":").append(genesetLibraries.get(rep).size()).append("},");
            }
            sb.append("]}");

            String json = sb.toString();
            json = json.replace(",]", "]");

            PrintWriter out = response.getWriter();
            out.write(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static short[] ranksHash(float[] temp) {

        float[] sc = new float[temp.length];
        System.arraycopy(temp, 0, sc, 0, temp.length);
        Arrays.sort(sc);

        Map<Float, Short> hm = new HashMap<>(sc.length);
        for (short i = 0; i < sc.length; i++) {
            hm.put(sc[i], i);
        }

        short[] ranks = new short[sc.length];

        for (int i = 0; i < temp.length; i++) {
            ranks[i] = (short) (hm.get(temp[i]) + 1);
        }
        return ranks;
    }

    private void sendStatus(HttpServletResponse response, String message) {
        try {
            PrintWriter out = response.getWriter();
            String json = "{\"status\": \"" + message + "\"}";
            out.write(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createGenesetLibrary(String cacheName, String[] entityNames) {
        if (entityNames.length > 65000) {
            throw new EnrichmentapiException("Too many unique entities for short representation. The system supports 65000 unique entites, but " + Arrays.toString(entityNames) + " were detected.");
        } else {
            Map<String, short[]> genesets = new HashMap<>();
            Map<String, Short> dictionary = new HashMap<>();
            Map<Short, String> revDictionary = new HashMap<>();

            Set<String> uidlist = new HashSet<>();

            short idx = Short.MIN_VALUE;

            for (String entity : entityNames) {
                if (!uidlist.contains(entity)) {
                    dictionary.put(entity, idx);
                    revDictionary.put(idx, entity);
                    idx++;
                    uidlist.add(entity);
                }
            }

            genesetLibraries.put(cacheName, genesets);
            dictionaries.put(cacheName, dictionary);
            revDictionaries.put(cacheName, revDictionary);
        }
    }

    private void createRankMatrix(String cacheName, String[] entityNames) {
        if (entityNames.length > 65000) {
            throw new EnrichmentapiException("Too many unique entities for short representation. The system supports 65000 unique entites, but " + Arrays.toString(entityNames) + " were detected.");
        } else {
            List<String> entities = new ArrayList<>();
            Map<String, short[]> signature = new HashMap<>();

            Collections.addAll(entities, entityNames);

            repEntities.put(cacheName, entities);
            repSignatures.put(cacheName, signature);
        }
    }

    private void appendGenesetLibraryRepository(String cacheName, JSONObject json) {
        Map<String, Short> dictionary = dictionaries.get(cacheName);
        Map<String, short[]> geneset = genesetLibraries.get(cacheName);

        final JSONArray querySignatures = json.getJSONArray("signatures");

        for (int i = 0; i < querySignatures.length(); i++) {
            JSONObject jo = querySignatures.getJSONObject(i);

            String signaturecacheName = jo.getString("uuid");
            JSONArray entities = jo.getJSONArray("entities");

            List<Short> arrl = new ArrayList<>();

            System.out.println(entities.length());

            for (int j = 0; j < entities.length(); j++) {
                if (dictionary.containsKey(entities.getString(j))) {
                    arrl.add(dictionary.get(entities.getString(j)));
                }
            }

            short[] set = new short[arrl.size()];
            for (int k = 0; k < arrl.size(); k++) {
                set[k] = arrl.get(k);
            }

            geneset.put(signaturecacheName, set);
        }

    }

    private void appendRankRepository(String cacheName, JSONObject json) {
        List<String> entities = repEntities.get(cacheName);

        Map<String, short[]> signatures = repSignatures.get(cacheName);

        try {
            final JSONArray querySignatures = json.getJSONArray("signatures");

            for (int i = 0; i < querySignatures.length(); i++) {
                JSONObject jo = querySignatures.getJSONObject(i);

                String signaturecacheName = jo.getString("uuid");
                JSONArray values = jo.getJSONArray("entity_values");

                List<Float> arrl = new ArrayList<>();

                for (int j = 0; j < values.length(); j++) {
                    arrl.add((float) values.getDouble(i));
                }

                float[] set = new float[arrl.size()];
                for (int k = 0; k < arrl.size(); k++) {
                    set[k] = arrl.get(k);
                }

                short[] rank = ranksHash(set);

                if (rank.length == entities.size()) {
                    signatures.put(signaturecacheName, rank);
                } else {
                    throw new EnrichmentapiException("appending " + signaturecacheName + " rank data failed. the size of the data vector did not fit the number of entities submitted on repository creation.");
                }
            }

            repSignatures.put(cacheName, signatures);
        } catch (Exception e) {
            throw new EnrichmentapiException("Could not read the JSON to append signature.", e);
        }
    }

    private void removeSamplesGenesetLibraryRepository(String cacheName, JSONObject json) {
        removeSamples(cacheName, json, genesetLibraries);
    }

    private void removeSamplesRankRepository(String cacheName, JSONObject json) {
        removeSamples(cacheName, json, repSignatures);
    }

    private void removeSamples(String cacheName, JSONObject json, Map<String, Map<String, short[]>> source) {
        try {
            if (json.getJSONArray("signatures") != null) {
                final JSONArray signatures = json.getJSONArray("signatures");
                int n = signatures.length();

                for (int i = 0; i < n; ++i) {
                    source.get(cacheName).remove(signatures.getString(i));
                }
            }
        } catch (Exception e) {
            throw new EnrichmentapiException("failed to remove samples from genelist repository", e);
        }
    }

    private void removeRepository(String cacheName) {
        if (dictionaries.containsKey(cacheName)) {
            genesetLibraries.remove(cacheName);
            revDictionaries.remove(cacheName);
            dictionaries.remove(cacheName);
        } else if (repEntities.containsKey(cacheName)) {
            repSignatures.remove(cacheName);
        } else {
            throw new EnrichmentapiException("The uuid does not exist. The repository must first be created before data can be added.");
        }
    }

    private void persistGenesetLibraryRepository(String cacheName) {
        Map<String, Object> setdata = new HashMap<>();
        setdata.put("geneset", genesetLibraries.get(cacheName));
        setdata.put("dictionary", dictionaries.get(cacheName));
        setdata.put("revDictionary", revDictionaries.get(cacheName));

        igniteImporter.importOverlap(setdata, new IngestionImportDto(cacheName, DatasetType.GENESET_LIBRARY));

        removeRepository(cacheName);
    }

    private void persistRankRepository(String cacheName) {
        if (repSignatures.containsKey(cacheName)) {
            Map<String, short[]> sigs = repSignatures.get(cacheName);
            String[] sigcacheNames = sigs.keySet().toArray(new String[0]);
            String[] entitycacheNames = repEntities.get(cacheName).toArray(new String[0]);

            short[][] rankMatrix = new short[entitycacheNames.length][sigcacheNames.length];
            for (int i = 0; i < sigcacheNames.length; i++) {
                short[] rank = sigs.get(sigcacheNames[i]);
                for (int j = 0; j < entitycacheNames.length; j++) {
                    rankMatrix[j][i] = rank[j];
                }
            }

            Map<String, Object> matrixSo = new HashMap<>();
            matrixSo.put("entity_id", entitycacheNames);
            matrixSo.put("signature_id", sigcacheNames);
            matrixSo.put("rank", rankMatrix);

            igniteImporter.importRank(matrixSo, new IngestionImportDto(cacheName, DatasetType.RANK_MATRIX));

            removeRepository(cacheName);

        } else {
            throw new EnrichmentapiException("Repository does not exist");
        }
    }

    private JSONObject readPayload(HttpServletRequest request) {
        try {
            return new JSONObject(request.getReader().lines().collect(joining()));
        } catch (Exception e) {
            throw new EnrichmentapiException("error in processing request");
        }
    }
}




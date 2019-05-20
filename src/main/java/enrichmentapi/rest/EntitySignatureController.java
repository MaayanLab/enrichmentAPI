package enrichmentapi.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import enrichmentapi.calc.Enrichment;
import enrichmentapi.calc.IgniteSoImporter;
import enrichmentapi.dto.PairInputDto;
import enrichmentapi.dto.SingleInputDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/v1")
public class EntitySignatureController {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Enrichment enrichment;
    private final IgniteSoImporter igniteSoImporter;

    public EntitySignatureController(Enrichment enrichment,
                                     IgniteSoImporter igniteSoImporter) {
        this.enrichment = enrichment;
        this.igniteSoImporter = igniteSoImporter;
    }

    @PostMapping("/listdata")
    public ResponseEntity listData() {
        return ResponseEntity.ok(enrichment.listData());
    }

    @PostMapping(path = "/enrich/overlap", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity createSignature(@RequestBody String input) throws IOException {
        final SingleInputDto parameters = objectMapper.readerFor(SingleInputDto.class).readValue(input);
        return ResponseEntity.ok(enrichment.overlap(parameters));
    }

    @PostMapping(path = "/enrich/rank", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity createRank(@RequestBody String input) throws IOException {
        final SingleInputDto parameters = objectMapper.readerFor(SingleInputDto.class).readValue(input);
        return ResponseEntity.ok(enrichment.rank(parameters));
    }

    @PostMapping(path = "/enrich/ranktwosided", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity createRankTwoSided(@RequestBody String input) throws IOException {
        final PairInputDto parameters = objectMapper.readerFor(PairInputDto.class).readValue(input);
        return ResponseEntity.ok(enrichment.rankTwoSided(parameters));
    }

    @PostMapping("/download-so")
    public ResponseEntity loadToRedis(@RequestBody Map<String, String> values) {
        igniteSoImporter.importSo(values.get("type"), values.get("fileName"), values.get("name"));
        return ResponseEntity.ok("OK");
    }

}

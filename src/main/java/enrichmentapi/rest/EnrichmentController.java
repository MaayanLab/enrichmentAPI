package enrichmentapi.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import enrichmentapi.calc.Enrichment;
import enrichmentapi.dto.in.PairInputDto;
import enrichmentapi.dto.in.SingleInputDto;
import enrichmentapi.dto.out.DatasetInfoListDto;
import enrichmentapi.dto.out.OverlapDto;
import enrichmentapi.dto.out.RankDto;
import enrichmentapi.dto.out.RankTwoSidedDto;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping(path = "/api/v1")
public class EnrichmentController {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Enrichment enrichment;

    public EnrichmentController(Enrichment enrichment) {
        this.enrichment = enrichment;
    }

    @PostMapping("/listdata")
    public DatasetInfoListDto listData() {
        return enrichment.listData();
    }

    @PostMapping(path = "/enrich/overlap", consumes = MediaType.TEXT_PLAIN_VALUE)
    public OverlapDto createOverlap(@RequestBody String input) throws IOException {
        final SingleInputDto parameters = objectMapper.readerFor(SingleInputDto.class).readValue(input);
        return enrichment.overlap(parameters);
    }

    @PostMapping(path = "/enrich/rank", consumes = MediaType.TEXT_PLAIN_VALUE)
    public RankDto createRank(@RequestBody String input) throws IOException {
        final SingleInputDto parameters = objectMapper.readerFor(SingleInputDto.class).readValue(input);
        return enrichment.rank(parameters);
    }

    @PostMapping(path = "/enrich/ranktwosided", consumes = MediaType.TEXT_PLAIN_VALUE)
    public RankTwoSidedDto createRankTwoSided(@RequestBody String input) throws IOException {
        final PairInputDto parameters = objectMapper.readerFor(PairInputDto.class).readValue(input);
        return enrichment.rankTwoSided(parameters);
    }
}

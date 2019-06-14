package enrichmentapi.rest;

import enrichmentapi.dto.in.SoImportDto;
import enrichmentapi.dto.in.TestImportDto;
import enrichmentapi.ignite.IgniteImporter;
import enrichmentapi.ignite.IgniteTestImporter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/v1")
public class ImportController {

    private final IgniteImporter igniteImporter;
    private final IgniteTestImporter igniteTestImporter;

    public ImportController(IgniteImporter igniteImporter, IgniteTestImporter igniteTestImporter) {
        this.igniteImporter = igniteImporter;
        this.igniteTestImporter = igniteTestImporter;
    }

    @PostMapping("/download-so")
    public void loadToIgnite(@RequestBody SoImportDto importDto) throws IOException, ClassNotFoundException {
        igniteImporter.importSo(importDto);
    }

    @PostMapping("/download-so-test")
    public Map<String, List<String>> loadToIgniteTest(@RequestBody TestImportDto importDto) {
        final Map<String, List<String>> result = new HashMap<>();
        final List<String> entities = igniteTestImporter.importSo(importDto);
        result.put("entities", entities);
        return result;
    }
}

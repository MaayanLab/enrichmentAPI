package enrichmentapi.rest;

import enrichmentapi.dto.in.DatasetDeletionDto;
import enrichmentapi.dto.in.SoImportDto;
import enrichmentapi.dto.in.TestImportDto;
import enrichmentapi.dto.out.DatasetInfoDto;
import enrichmentapi.dto.out.TestImportResultDto;
import enrichmentapi.ignite.IgniteImporter;
import enrichmentapi.ignite.IgniteTestImporter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

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
    public DatasetInfoDto loadToIgnite(@RequestBody SoImportDto importDto) throws IOException, ClassNotFoundException {
        return igniteImporter.importSo(importDto);
    }

    @PostMapping("/download-so-test")
    public TestImportResultDto loadToIgniteTest(@RequestBody TestImportDto importDto) {
        return igniteTestImporter.importSo(importDto);
    }

    @PostMapping("/delete")
    public void deleteDataset(@RequestBody DatasetDeletionDto dto) {
        igniteImporter.deleteDataset(dto);
    }
}

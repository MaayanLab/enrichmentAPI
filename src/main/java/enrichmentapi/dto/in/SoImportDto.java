package enrichmentapi.dto.in;

public class SoImportDto extends ImportDto {
    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}

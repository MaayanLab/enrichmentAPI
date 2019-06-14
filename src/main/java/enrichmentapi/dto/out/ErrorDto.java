package enrichmentapi.dto.out;

public class ErrorDto {

    private final String error;

    public ErrorDto(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }
}

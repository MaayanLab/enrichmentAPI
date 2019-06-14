package enrichmentapi.exceptions;

public class EnrichmentapiException extends RuntimeException {

    public EnrichmentapiException(String message) {
        super(message);
    }

    public EnrichmentapiException(String message, Throwable throwable) {
        super(message, throwable);
    }

}

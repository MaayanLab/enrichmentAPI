package enrichmentapi.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DatasetNotExistException extends ResponseStatusException {

    private static final String EXCEPTION_MESSAGE = "Dataset with name %s does not exist.";

    public DatasetNotExistException(String datasetName) {
        super(HttpStatus.NOT_FOUND, String.format(EXCEPTION_MESSAGE, datasetName));
    }

}

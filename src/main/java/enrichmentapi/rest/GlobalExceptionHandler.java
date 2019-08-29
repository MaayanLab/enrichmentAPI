package enrichmentapi.rest;

import enrichmentapi.dto.out.ErrorDto;
import enrichmentapi.exceptions.EnrichmentapiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(EnrichmentapiException.class)
    public ErrorDto handleException(EnrichmentapiException exception) {
        return new ErrorDto(exception.getMessage());
    }

}

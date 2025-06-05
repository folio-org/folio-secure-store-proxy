package org.folio.ssp.resource;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import lombok.extern.log4j.Log4j2;
import org.folio.ssp.model.error.Error;
import org.folio.ssp.model.error.ErrorCode;
import org.folio.ssp.model.error.ErrorResponse;
import org.folio.ssp.model.error.Parameter;
import org.folio.tools.store.exception.SecretNotFoundException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.UnwrapException;

@Log4j2
@UnwrapException({CompletionException.class})
public class ExceptionHandler {

  @ServerExceptionMapper
  public RestResponse<ErrorResponse> handleSecureStoreNotFoundException(SecretNotFoundException e, UriInfo uriInfo) {
    log.debug("Exception occurred while calling the endpoint: path = {}, exc = {}", uriInfo.getPath(), e);

    var error = new Error()
        .message(e.getMessage())
        .code(ErrorCode.NOT_FOUND_ERROR)
        .type(e.getClass().getSimpleName());

    return ResponseBuilder.create(RestResponse.Status.NOT_FOUND, singleError(error)).build();
  }

  @ServerExceptionMapper
  public RestResponse<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e,
    UriInfo uriInfo) {
    log.debug("Exception occurred while calling the endpoint: path = {}, exc = {}", uriInfo.getPath(), e);

    var violations = e.getConstraintViolations();

    var error = new Error().code(ErrorCode.VALIDATION_ERROR)
      .type(ConstraintViolationException.class.getSimpleName());

    if (isEmpty(violations)) {
      error = error.message(e.getMessage());
    } else {
      error = error.message("Validation failed")
        .parameters(parametersFromViolations(violations));
    }

    return ResponseBuilder.create(RestResponse.Status.BAD_REQUEST, singleError(error)).build();
  }

  private static ErrorResponse singleError(Error error) {
    return new ErrorResponse()
      .totalRecords(1)
      .errors(List.of(error));
  }

  private static List<Parameter> parametersFromViolations(Set<ConstraintViolation<?>> violations) {
    return violations.stream().map(violation -> {
      var propertyPath = violation.getPropertyPath();
      var message = violation.getMessage();
      return new Parameter().key(propertyPath.toString()).value(message);
    }).toList();
  }
}

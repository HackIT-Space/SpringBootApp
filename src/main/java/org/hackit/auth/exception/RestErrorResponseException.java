package org.hackit.auth.exception;

import static org.springframework.http.HttpStatusCode.valueOf;

import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class RestErrorResponseException extends ErrorResponseException {

    public RestErrorResponseException(final ProblemDetail problemDetail) {
        super(valueOf(problemDetail.getStatus()), problemDetail, null);
    }
}

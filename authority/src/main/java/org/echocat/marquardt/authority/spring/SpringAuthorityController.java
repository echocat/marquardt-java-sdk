/*
 * echocat Marquardt Java SDK, Copyright (c) 2015 echocat
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.echocat.marquardt.authority.spring;

import org.echocat.marquardt.authority.Authority;
import org.echocat.marquardt.authority.domain.Session;
import org.echocat.marquardt.authority.domain.User;
import org.echocat.marquardt.authority.exceptions.ExpiredSessionException;
import org.echocat.marquardt.common.domain.ClientInformation;
import org.echocat.marquardt.common.domain.Credentials;
import org.echocat.marquardt.common.domain.SignUpAccountData;
import org.echocat.marquardt.common.domain.Signature;
import org.echocat.marquardt.common.domain.certificate.Role;
import org.echocat.marquardt.common.exceptions.AlreadyLoggedInException;
import org.echocat.marquardt.common.exceptions.ClientNotAuthorizedException;
import org.echocat.marquardt.common.exceptions.InvalidCertificateException;
import org.echocat.marquardt.common.exceptions.LoginFailedException;
import org.echocat.marquardt.common.exceptions.NoSessionFoundException;
import org.echocat.marquardt.common.exceptions.UserAlreadyExistsException;
import org.echocat.marquardt.common.web.JsonWrappedCertificate;
import org.echocat.marquardt.common.web.RequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.echocat.marquardt.common.web.RequestHeaders.X_CERTIFICATE;

/**
 * Blueprint of an authority controller for the Spring MVC framework.
 * See examples to see how it is used.
 *
 * @param <USER>        Your authority's user implementation.
 * @param <SESSION>     Your authority's session implementation.
 */
public class SpringAuthorityController<USER extends User<? extends Role>,
        SESSION extends Session,
        CLIENT_INFORMATION extends ClientInformation,
        CREDENTIALS extends Credentials,
        SIGNUP_ACCOUNT_DATA extends SignUpAccountData<CREDENTIALS>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringAuthorityController.class);

    private final Authority<USER, SESSION, CREDENTIALS, SIGNUP_ACCOUNT_DATA> _authority;
    private final RequestValidator _requestValidator = new RequestValidator();

    public SpringAuthorityController(final Authority<USER, SESSION, CREDENTIALS, SIGNUP_ACCOUNT_DATA> authority) {
        _authority = authority;
    }

    @RequestMapping(value = "/initializeSignUp", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.CREATED)
    @ResponseBody
    public JsonWrappedCertificate initializeSignUp(@RequestBody final CLIENT_INFORMATION clientInformation) {
        return createCertificateResponse(_authority.initializeSignUp(clientInformation));
    }

    @RequestMapping(value = "/finalizeSignUp", method = RequestMethod.POST)
    @ResponseBody
    public JsonWrappedCertificate finalizeSignUp(@RequestHeader(X_CERTIFICATE) final byte[] certificate, @RequestBody final SIGNUP_ACCOUNT_DATA accountData, final HttpServletRequest request) {
        final byte[] signedBytesFromRequest = _requestValidator.extractSignedBytesFromRequest(request);
        final Signature signature = _requestValidator.extractSignatureFromHeader(request);
        return createCertificateResponse(_authority.finalizeSignUp(certificate, signedBytesFromRequest, signature, accountData));
    }

    @RequestMapping(value = "/signIn", method = RequestMethod.POST)
    @ResponseBody
    public JsonWrappedCertificate signIn(@RequestBody final CREDENTIALS credentials) {
        return createCertificateResponse(_authority.signIn(credentials));
    }

    @RequestMapping(value = "/refresh", method = RequestMethod.POST)
    @ResponseBody
    public JsonWrappedCertificate refresh(@RequestHeader(X_CERTIFICATE) final byte[] certificate, final HttpServletRequest request) {
        final byte[] signedBytesFromRequest = _requestValidator.extractSignedBytesFromRequest(request);
        final Signature signature = _requestValidator.extractSignatureFromHeader(request);
        return createCertificateResponse(_authority.refresh(certificate, signedBytesFromRequest, signature));
    }

    @RequestMapping(value = "/signOut", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void signOut(@RequestHeader(X_CERTIFICATE) final byte[] certificate, final HttpServletRequest request) {
        final byte[] signedBytesFromRequest = _requestValidator.extractSignedBytesFromRequest(request);
        final Signature signature = _requestValidator.extractSignatureFromHeader(request);
        _authority.signOut(certificate, signedBytesFromRequest, signature);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT, reason = "User already exists.")
    public void handleUserExistsException(final UserAlreadyExistsException ex) {
        LOGGER.info(ex.getMessage());
    }

    @ExceptionHandler(LoginFailedException.class)
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Login failed.")
    public void handleAlreadyLoggedInException(final LoginFailedException ex) {
        LOGGER.info(ex.getMessage());
    }

    @ExceptionHandler(AlreadyLoggedInException.class)
    @ResponseStatus(value = HttpStatus.PRECONDITION_FAILED, reason = "Already logged in.")
    public void handleAlreadyLoggedInException(final AlreadyLoggedInException ex) {
        LOGGER.info(ex.getMessage());
    }

    @ExceptionHandler(InvalidCertificateException.class)
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Invalid jsonWrappedCertificate.")
    public void handleInvalidCertificateException(final InvalidCertificateException ex) {
        LOGGER.info(ex.getMessage());
    }

    @ExceptionHandler(ExpiredSessionException.class)
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Session is expired.")
    public void handleInvalidSessionException(final ExpiredSessionException ex) {
        LOGGER.info(ex.getMessage());
    }

    @ExceptionHandler(NoSessionFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No session exists.")
    public void handleNoSessionFoundException(final NoSessionFoundException ex) {
        LOGGER.info(ex.getMessage());
    }

    @ExceptionHandler(ClientNotAuthorizedException.class)
    @ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Client is not authorized.")
    public void handleClientNotAuthorizedException(final ClientNotAuthorizedException ex) {
        LOGGER.info(ex.getMessage());
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleIOException(final IOException ex) {
        LOGGER.error("Caught unhandled IOException.", ex);
    }

    private JsonWrappedCertificate createCertificateResponse(final byte[] certificate) {
        return new JsonWrappedCertificate(certificate);
    }
}
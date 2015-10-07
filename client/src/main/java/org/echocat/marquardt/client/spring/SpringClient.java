/*
 * echocat Marquardt Java SDK, Copyright (c) 2015 echocat
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.echocat.marquardt.client.spring;

import org.echocat.marquardt.client.Client;
import org.echocat.marquardt.client.util.Md5Creator;
import org.echocat.marquardt.client.util.ResponseStatusTranslation;
import org.echocat.marquardt.common.CertificateValidator;
import org.echocat.marquardt.common.domain.Credentials;
import org.echocat.marquardt.common.domain.DeserializingFactory;
import org.echocat.marquardt.common.domain.Signable;
import org.echocat.marquardt.common.domain.certificate.Certificate;
import org.echocat.marquardt.common.domain.certificate.Role;
import org.echocat.marquardt.common.exceptions.InvalidCertificateException;
import org.echocat.marquardt.common.keyprovisioning.KeyPairProvider;
import org.echocat.marquardt.common.serialization.RolesDeserializer;
import org.echocat.marquardt.common.util.DateProvider;
import org.echocat.marquardt.common.web.JsonWrappedCertificate;
import org.echocat.marquardt.common.web.SignatureHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Collection;

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;

/**
 * Spring implementation of the client.
 *
 * @param <SIGNABLE> type of the payload contained in the certificate.
 */
public class SpringClient<SIGNABLE extends Signable, ROLE extends Role> implements Client<SIGNABLE> {

    private final RestTemplate _restTemplate = new RestTemplate();
    private final RestTemplate _headerSignedRestTemplate = new RestTemplate();
    private final String _baseUri;
    private final DeserializingFactory<SIGNABLE> _deserializingFactory;
    private final CertificateValidator<SIGNABLE, ROLE> _certificateValidator;
    private final RequestSigner _requestSigner = new RequestSigner();
    private final KeyPairProvider _clientKeyProvider;
    private DateProvider _dateProvider = new DateProvider();

    /**
     * Create a client instance.
     *
     * @param baseUri              base uri of the authority.
     * @param deserializingFactory factory used to deserialize the payload with type SIGNABLE.
     * @param roleRolesDeserializer RolesDeserializer for your roles implementation.
     * @param clientKeyProvider    key provider that returns the client's public/private key pair.
     * @param trustedKeys          a collection of pre-shared, trusted keys used by the authority to sign certificates. The client uses this list to verify the authenticity of certificates.
     */
    public SpringClient(final String baseUri,
                        final DeserializingFactory<SIGNABLE> deserializingFactory,
                        final RolesDeserializer<ROLE> roleRolesDeserializer,
                        final KeyPairProvider clientKeyProvider,
                        final Collection<PublicKey> trustedKeys) {
        _baseUri = baseUri;
        _deserializingFactory = deserializingFactory;
        _clientKeyProvider = clientKeyProvider;
        _headerSignedRestTemplate.getInterceptors().add(
                new ClientHttpRequestInterceptor() {
                    @Override
                    public ClientHttpResponse intercept(final HttpRequest httpRequest,
                                                        final byte[] bytes,
                                                        final ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
                        HttpHeaders headers = httpRequest.getHeaders();
                        headers.add(SignatureHeaders.CONTENT.getHeaderName(), encodeBase64URLSafeString(Md5Creator.create(bytes)));
                        headers.add("X-Signature", new String(_requestSigner.getSignature(httpRequest, _clientKeyProvider.getPrivateKey())));
                        return clientHttpRequestExecution.execute(httpRequest, bytes);
                    }
                });
        _certificateValidator = new CertificateValidator<SIGNABLE, ROLE>(trustedKeys) {
            @Override
            protected DeserializingFactory<SIGNABLE> deserializingFactory() {
                return _deserializingFactory;
            }

            @Override
            protected RolesDeserializer<ROLE> roleCodeDeserializer() {
                return roleRolesDeserializer;
            }
        };
        _certificateValidator.setDateProvider(_dateProvider);
    }

    /**
     * Used for internal (testing) purposes only.
     */
    public void setDateProvider(DateProvider dateProvider) {
        _dateProvider = dateProvider;
        _certificateValidator.setDateProvider(_dateProvider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Certificate<SIGNABLE> signup(final Credentials credentials) throws IOException {
        final ResponseEntity<JsonWrappedCertificate> response;
        try {
            response = _restTemplate.postForEntity(_baseUri + "/auth/signup/", credentials, JsonWrappedCertificate.class);
        } catch (HttpClientErrorException e) {
            throw ResponseStatusTranslation.from(e.getStatusCode().value()).translateToException(e.getMessage());
        }
        final HttpStatus expectedStatus = HttpStatus.CREATED;
        if (response.getStatusCode() == expectedStatus) {
            return extractCertificateFrom(response);
        } else {
            throw new IllegalStateException("Got HTTP status '" + response.getStatusCode() + "'. Expected was: " + expectedStatus.value() + ". Cannot handle unexpected status code");
        }
    }

    private Certificate<SIGNABLE> extractCertificateFrom(ResponseEntity<JsonWrappedCertificate> response) {
        final byte[] certificate = response.getBody().getCertificate();
        final Certificate<SIGNABLE> deserializedCertificate = _certificateValidator.deserializeAndValidateCertificate(certificate);
        if (!deserializedCertificate.getClientPublicKey().equals(_clientKeyProvider.getPublicKey())) {
            throw new InvalidCertificateException("certificate key does not match my public key");
        }
        return deserializedCertificate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Certificate<SIGNABLE> signin(final Credentials credentials) throws IOException {
        final ResponseEntity<JsonWrappedCertificate> response;
        try {
            response = _restTemplate.postForEntity(_baseUri + "/auth/signin/", credentials, JsonWrappedCertificate.class);
        } catch (HttpClientErrorException e) {
            throw ResponseStatusTranslation.from(e.getStatusCode().value()).translateToException(e.getMessage());
        }
        final HttpStatus expectedStatus = HttpStatus.OK;
        if (response.getStatusCode() == expectedStatus) {
            return extractCertificateFrom(response);
        } else {
            throw new IllegalStateException("Got HTTP status '" + response.getStatusCode() + "'. Expected was: " + expectedStatus.value() + ". Cannot handle unexpected status code");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Certificate<SIGNABLE> refresh(Certificate<SIGNABLE> certificateToRefesh) throws IOException {
        final ResponseEntity<JsonWrappedCertificate> response;
        try {
            response = _headerSignedRestTemplate.exchange(_baseUri + "/auth/refresh/", HttpMethod.POST, httpEntityWithCertificateHeader(certificateToRefesh), JsonWrappedCertificate.class);
        } catch (HttpClientErrorException e) {
            throw ResponseStatusTranslation.from(e.getStatusCode().value()).translateToException(e.getMessage());
        }
        return extractCertificateFrom(response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean signout(Certificate<SIGNABLE> certificate) throws IOException {
        final ResponseEntity<Void> response;
        try {
            response = _headerSignedRestTemplate.exchange(_baseUri + "/auth/signout/", HttpMethod.POST, httpEntityWithCertificateHeader(certificate), Void.class);
        } catch (HttpClientErrorException e) {
            throw ResponseStatusTranslation.from(e.getStatusCode().value()).translateToException(e.getMessage());
        }
        return response.getStatusCode() == HttpStatus.NO_CONTENT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <REQUEST, RESPONSE> RESPONSE sendSignedPayloadTo(final String url,
                                                            final String httpMethod,
                                                            final REQUEST payload,
                                                            final Class<RESPONSE> responseType,
                                                            final Certificate<SIGNABLE> certificate) throws IOException {
        try {
            final ResponseEntity<RESPONSE> exchange =
                    _headerSignedRestTemplate.exchange(
                            url, HttpMethod.valueOf(httpMethod.toUpperCase()), httpEntityWithCertificateHeader(payload, certificate), responseType);
            return exchange.getBody();
        } catch (HttpClientErrorException ignored) {
            throw ResponseStatusTranslation.from(ignored.getStatusCode().value()).translateToException(ignored.getMessage());
        }
    }

    private HttpEntity<Object> httpEntityWithCertificateHeader(Certificate<SIGNABLE> certificateToRefesh) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.set(SignatureHeaders.X_CERTIFICATE.getHeaderName(), encodeBase64URLSafeString(certificateToRefesh.getContent()));
        return new HttpEntity<>(headers);
    }

    private HttpEntity<Object> httpEntityWithCertificateHeader(Object object, Certificate<SIGNABLE> certificateToRefesh) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.set(SignatureHeaders.X_CERTIFICATE.getHeaderName(), encodeBase64URLSafeString(certificateToRefesh.getContent()));
        return new HttpEntity<>(object, headers);
    }
}
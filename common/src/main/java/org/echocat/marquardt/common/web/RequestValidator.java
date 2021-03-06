/*
 * echocat Marquardt Java SDK, Copyright (c) 2015 echocat
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.echocat.marquardt.common.web;

import com.google.common.primitives.Ints;
import org.apache.commons.io.IOUtils;
import org.echocat.marquardt.common.domain.Signature;
import org.echocat.marquardt.common.exceptions.SignatureValidationFailedException;

import javax.annotation.WillNotClose;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PublicKey;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.echocat.marquardt.common.web.RequestHeaders.X_SIGNATURE;

/**
 * Client signed header validator. Clients must sign their requests (including their certificate) with a
 * signature to ensure the origin of the request. This enables checking that the (authority signed) certificate
 * is only used by the client that requested the certificate. This is ensured due to the fact that the PublicKey
 * contained in the certificate can validate the signature. Since the client should use its own PrivateKey/PublicKey
 * pair he is the only one capable of producing this signature.
 * <p>
 * Simple: No man-in-the-middle attack is possible since the sender must be the same as the one that
 * obtained the certificate.
 */
public class RequestValidator {

    /**
     * Validate a request header that contains a Signature with this.
     *
     * @param request           The request to validate.
     * @param keyToValidateWith Client's PublicKey. Should be taken from the X-Certificate header.
     * @return True if the signature is valid. False if not.
     */
    public boolean isValid(HttpServletRequest request, PublicKey keyToValidateWith) {
        final ByteArrayOutputStream bytesToSign = new ByteArrayOutputStream();
        try {
            byte[] byteArray = extractSignedBytesFromRequest(request, bytesToSign);
            final Signature signature = extractSignatureFromHeader(request);
            return signature.isValidFor(byteArray, keyToValidateWith);
        } catch (IOException e) {
            throw new SignatureValidationFailedException(e.getMessage());
        } finally {
            IOUtils.closeQuietly(bytesToSign);
        }
    }

    /**
     * Extract the signed bytes from the request.
     *
     * @param request the http request
     * @return the signed byte sequence
     * @see SignatureHeaders
     */
    public byte[] extractSignedBytesFromRequest(HttpServletRequest request) {
        final ByteArrayOutputStream bytesToSign = new ByteArrayOutputStream();
        try {
            return extractSignedBytesFromRequest(request, bytesToSign);
        } catch (IOException e) {
            throw new SignatureValidationFailedException("could not extract signed bytes from header", e);
        } finally {
            IOUtils.closeQuietly(bytesToSign);
        }
    }

    private byte[] extractSignedBytesFromRequest(HttpServletRequest request, @WillNotClose ByteArrayOutputStream bytesToSign) throws IOException {
        writeRequestTo(request, bytesToSign);
        return bytesToSign.toByteArray();
    }

    /**
     * Extract the signature from the request.
     *
     * @param request the http request
     * @return the signature
     */
    public Signature extractSignatureFromHeader(HttpServletRequest request) {
        final String header = request.getHeader(X_SIGNATURE);
        if (header == null) {
            throw new IllegalArgumentException("Expected non-empty signature header.");
        }
        return new Signature(decodeBase64(header));
    }

    private void writeRequestTo(HttpServletRequest request, ByteArrayOutputStream bytesToSign) throws IOException {
        final byte[] requestBytes = (request.getMethod() + " " + request.getRequestURI()).getBytes();
        bytesToSign.write(Ints.toByteArray(requestBytes.length));
        bytesToSign.write(requestBytes);
        for (SignatureHeaders headerToInclude : SignatureHeaders.values()) {
            final String header = request.getHeader(headerToInclude.getHeaderName());
            if (header != null) {
                final byte[] headerBytes = (headerToInclude.getHeaderName() + ":" + header).getBytes();
                bytesToSign.write(Ints.toByteArray(headerBytes.length));
                bytesToSign.write(headerBytes);
            }
        }
    }
}
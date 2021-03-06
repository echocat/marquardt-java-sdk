/*
 * echocat Marquardt Java SDK, Copyright (c) 2015 echocat
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.echocat.marquardt.example.domain;

import org.echocat.marquardt.authority.domain.Session;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Entity
public class PersistentSession implements Session {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    @NotNull
    private UUID userId;

    @NotNull
    @Column(length = 1000)
    private byte[] certificate;

    @SuppressWarnings("UseOfObsoleteDateTimeApi")
    @NotNull
    private Date expiresAt;

    @NotNull
    private byte[] publicKey;

    @NotNull
    private String mechanism;
    private String clientId;

    @SuppressWarnings("unused")
    public Long getId() {
        return id;
    }

    @SuppressWarnings("unused")
    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public UUID getUserId() {
        return userId;
    }

    @Override
    public void setUserId(final UUID userId) {
        this.userId = userId;
    }

    @Override
    public byte[] getCertificate() {
        return certificate;
    }

    @Override
    public void setCertificate(final byte[] certificate) {
        this.certificate = Arrays.copyOf(certificate, certificate.length);
    }

    @SuppressWarnings("UseOfObsoleteDateTimeApi")
    @Override
    public Date getExpiresAt() {
        return expiresAt;
    }

    @SuppressWarnings("UseOfObsoleteDateTimeApi")
    @Override
    public void setExpiresAt(final Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public byte[] getPublicKey() {
        return publicKey;
    }

    @Override
    public void setPublicKey(final byte[] publicKey) {
        this.publicKey = Arrays.copyOf(publicKey, publicKey.length);
    }

    @Override
    public String getMechanism() {
        return mechanism;
    }

    @Override
    public void setMechanism(final String mechanism) {
        this.mechanism = mechanism;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public void setClientId(String clientId) {
        this.clientId =  clientId;
    }
}

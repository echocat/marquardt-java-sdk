/*
 * echocat Marquardt Java SDK, Copyright (c) 2015 echocat
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.echocat.marquardt.example.persistence;

import org.echocat.marquardt.authority.persistence.SessionStore;
import org.echocat.marquardt.example.domain.PersistentSession;
import org.echocat.marquardt.example.persistence.jpa.PersistentSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class PersistentSessionStore implements SessionStore<PersistentSession> {

    private final PersistentSessionRepository _sessionRepository;

    @Autowired
    public PersistentSessionStore(final PersistentSessionRepository sessionRepository) {
        _sessionRepository = sessionRepository;
    }

    @Override
    public Optional<PersistentSession> findByCertificate(final byte[] certificate) {
        return _sessionRepository.findByCertificate(certificate);
    }

    @Override
    public boolean existsActiveSession(final UUID userId, final byte[] clientPublicKey, @SuppressWarnings("UseOfObsoleteDateTimeApi") final Date dateToCheck) {
        return _sessionRepository.countByUserIdAndPublicKeyAndExpiresAtGreaterThan(userId, clientPublicKey, dateToCheck) > 0;
    }

    @Override
    public PersistentSession save(final PersistentSession session) {
        return _sessionRepository.save(session);
    }

    @Override
    public PersistentSession createTransient() {
        return new PersistentSession();
    }

    @Override
    public void delete(final PersistentSession session) {
        _sessionRepository.delete(session);
    }

    public void deleteAll() {
        _sessionRepository.deleteAll();
    }

}

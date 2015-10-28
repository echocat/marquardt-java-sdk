/*
 * echocat Marquardt Java SDK, Copyright (c) 2015 echocat
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.echocat.marquardt.authority.persistence;

import org.echocat.marquardt.authority.domain.User;
import org.echocat.marquardt.authority.domain.UserToSignableMapper;
import org.echocat.marquardt.common.domain.Credentials;
import org.echocat.marquardt.common.domain.certificate.Role;
import org.echocat.marquardt.common.domain.Signable;

import java.util.Optional;
import java.util.UUID;

public interface UserStore<USER extends User<? extends Role>,
                           SIGNABLE extends Signable,
                           SIGNUP_CREDENTIALS extends Credentials> extends UserToSignableMapper<USER, SIGNABLE> {

    Optional<USER> findByCredentials(Credentials credentials);

    Optional<USER> findByUuid(UUID userId);

    USER createFromCredentials(SIGNUP_CREDENTIALS credentials);
}

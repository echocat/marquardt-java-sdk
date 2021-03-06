/*
 * echocat Marquardt Java SDK, Copyright (c) 2015 echocat
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.echocat.marquardt.common.domain;

/**
 * Used to transport credentials for SignIn and SignUp from Client to Authority.
 *
 * Please make sure passwords are encrypted or use secure transport channels.
 */
public interface Credentials extends ClientInformation {

    /**
     * Unique identifier of the User.
     *
     * @return unique identifier
     */
    String getIdentifier();

    /**
     * Password of User. Make sure this is encrypted or sent via a secure channel.
     *
     * @return password of user
     */
    String getPassword();
}
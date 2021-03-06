/*
 * echocat Marquardt Java SDK, Copyright (c) 2015 echocat
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.echocat.marquardt.authority.testdomain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.echocat.marquardt.common.domain.Credentials;
import org.echocat.marquardt.common.serialization.PublicKeyDeserializer;

import java.security.PublicKey;

public class TestUserCredentials extends TestClientInformation implements Credentials {

    private final String _email;
    private final String _password;

    @JsonCreator
    public TestUserCredentials(@JsonProperty("email") final String email,
                               @JsonProperty("password") final String password,
                               @JsonProperty("publicKey") @JsonDeserialize(using = PublicKeyDeserializer.class) final PublicKey publicKey,
                               @JsonProperty("clientId") final String clientId) {
        super(publicKey, clientId);
        _email = email;
        _password = password;
    }

    @Override
    @JsonProperty("email")
    public String getIdentifier() {
        return _email;
    }

    @Override
    @JsonProperty("password")
    public String getPassword() {
        return _password;
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass() || !super.equals(o)) {
            return false;
        }
        final TestUserCredentials other = (TestUserCredentials) o;
        return new EqualsBuilder().append(getIdentifier(), other.getIdentifier()).append(getPassword(), other.getPassword()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(super.hashCode()).append(getIdentifier()).append(getPassword()).toHashCode();
    }
}
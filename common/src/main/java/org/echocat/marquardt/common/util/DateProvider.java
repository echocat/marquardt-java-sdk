/*
 * echocat Marquardt Java SDK, Copyright (c) 2015 echocat
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.echocat.marquardt.common.util;

import java.util.Date;

/**
 * For internal (testing) purposes.
 */
public class DateProvider {

    @SuppressWarnings("UseOfObsoleteDateTimeApi")
    public Date now() {
        return new Date();
    }
}
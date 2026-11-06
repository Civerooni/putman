/*******************************************************************************
 * Copyright (c) 2026 Shayne Civitarese and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package civerooni.com.putman.ui.parts;

import civerooni.com.putman.core.model.ResponseModel;

class ResponseReplay {
    final ResponseModel response;
    final int format;

    ResponseReplay(ResponseModel response, int format) {
        this.response = response;
        this.format = format;
    }
}

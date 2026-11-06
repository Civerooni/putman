/*******************************************************************************
 * Copyright (c) 2026 Shayne Civitarese and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package civerooni.com.putman.core.model;

public class ResponseModel {
    private final int statusCode;
    private final String body;
    private final String error;

    public ResponseModel(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
        this.error = null;
    }

    private ResponseModel(String error) {
        this.statusCode = 0;
        this.body = null;
        this.error = error;
    }

    public static ResponseModel error(String message) {
        return new ResponseModel(message);
    }

    public static ResponseModel empty() {
        return new ResponseModel(-1, null);
    }

    public boolean isEmpty() { return statusCode == -1 && body == null && error == null; }

    public boolean isError() { return error != null; }
    public int getStatusCode() { return statusCode; }
    public String getBody() { return body; }
    public String getError() { return error; }
}

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

import java.util.ArrayList;
import java.util.List;

public class RequestModel {
    private HttpMethod method = HttpMethod.GET;
    private String url = "";
    private List<HeaderEntry> headers = new ArrayList<>();
    private List<HeaderEntry> params = new ArrayList<>();
    private String body = "";

    public HttpMethod getMethod() { return method; }
    public void setMethod(HttpMethod method) { this.method = method; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public List<HeaderEntry> getHeaders() { return headers; }
    public List<HeaderEntry> getParams() { return params; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}

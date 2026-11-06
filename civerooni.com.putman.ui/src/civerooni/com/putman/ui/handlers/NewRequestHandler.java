/*******************************************************************************
 * Copyright (c) 2026 Shayne Civitarese and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package civerooni.com.putman.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;

import civerooni.com.putman.ui.parts.RequestEditorPart;

public class NewRequestHandler {

    @Execute
    public void execute(IEventBroker broker) {
        broker.post(RequestEditorPart.NEW_REQUEST_TOPIC, Boolean.TRUE);
    }
}

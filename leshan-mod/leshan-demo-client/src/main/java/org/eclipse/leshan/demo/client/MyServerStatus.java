/*******************************************************************************
 * Copyright (c) 2022    Sierra Wireless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.demo.client;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.request.argument.Arguments;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyServerStatus extends BaseInstanceEnabler implements Destroyable {
    private static final Logger LOG = LoggerFactory.getLogger(MyServerStatus.class);

    // Inizializzazione delle risorse
    private static final int CALCOLA_RTT = 0;
    private static final int CALCOLA_THR = 1;
    private static final int CALCOLA_LOSS = 2;
    private static final int ULTIMO_RTT = 3;
    private static final int ULTIMO_THR = 4;
    private static final int ULTIMA_LOSS = 5;
    private static final int STOP_RTT = 6;
    private static final int STOP_THR = 7;
    private static final int STOP_LOSS = 8;

    private static final List<Integer> supportedResources = Arrays.asList(CALCOLA_RTT, CALCOLA_THR, CALCOLA_LOSS,
            ULTIMO_RTT, ULTIMO_THR, ULTIMA_LOSS, STOP_RTT, STOP_THR, STOP_LOSS);

    private final Timer timer;
    private final int TIMEOUT_OBSERVE = 5000;

    public MyServerStatus() {
        this.timer = new Timer("MyServerStatus");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                fireResourceChange(ULTIMO_RTT);
                fireResourceChange(ULTIMO_THR);
                fireResourceChange(ULTIMA_LOSS);
            }
        }, TIMEOUT_OBSERVE, TIMEOUT_OBSERVE);
    }

    @Override
    public ReadResponse read(LwM2mServer server, int resourceid) {
        LOG.info("Read on ServerStatus resource /{}/{}/{}", getModel().id, getId(), resourceid);
        switch (resourceid) {
        // Lettura degli ultimi valori acquisiti
        case ULTIMO_RTT:
            return ReadResponse.success(resourceid, ServerStatus.RTT.last());
        case ULTIMO_THR:
            return ReadResponse.success(resourceid, ServerStatus.Throughput.last());
        case ULTIMA_LOSS:
            return ReadResponse.success(resourceid, ServerStatus.Loss.last());
        }
        return super.read(server, resourceid);
    }

    @Override
    public ExecuteResponse execute(LwM2mServer server, int resourceid, Arguments arguments) {
        String withArguments = "";
        if (!arguments.isEmpty())
            withArguments = " with arguments " + arguments;
        LOG.info("Execute on ServerStatus resource /{}/{}/{} {}", getModel().id, getId(), resourceid, withArguments);

        switch (resourceid) {
        // Execute dei calcoli delle metriche
        case CALCOLA_RTT:
            ServerStatus.RTT.start();
            break;
        case CALCOLA_THR:
            ServerStatus.Throughput.start();
            break;
        case CALCOLA_LOSS:
            ServerStatus.Loss.start();
            break;

        // Execute della pubblicazione dei calcoli su Grafana
        case STOP_RTT:
            ServerStatus.RTT.stop();
            break;
        case STOP_THR:
            ServerStatus.Throughput.stop();
            break;
        case STOP_LOSS:
            ServerStatus.Loss.stop();
            break;
        }
        return ExecuteResponse.success();
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    @Override
    public void destroy() {
        timer.cancel();
    }
}

/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.server.handler;

import java.io.IOException;

import net.customware.gwt.dispatch.server.ActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.ActionException;
import net.customware.gwt.dispatch.shared.Result;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.opensocial.container.client.rpc.AbstractAction;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * This class abstracts all the job for getting the CoreSession, make sure session is cleaned after being used and all
 * Tx stuff goes well
 *
 * @author Stéphane Fourrier
 */
public abstract class AbstractActionHandler<T extends AbstractAction<R>, R extends Result> implements
        ActionHandler<T, R> {

    private static final Log log = LogFactory.getLog(AbstractActionHandler.class);

    public final R execute(T action, ExecutionContext context) throws ActionException {
        try (CoreSession session = CoreInstance.openCoreSession(action.getRepositoryName())) {
            R result = doExecute(action, context, session);
            session.save();
            return result;
        } catch (Exception e) {
            throw new ActionException("Unable to get session", e);
        }
    }

    public void rollback(T action, R result, ExecutionContext context) throws ActionException {
        TransactionHelper.setTransactionRollbackOnly();
    }

    /**
     * Real job takes place here
     *
     * @throws Exception
     */
    protected abstract R doExecute(T action, ExecutionContext context, CoreSession session) throws Exception;

    protected Space getSpaceFromId(String spaceId, CoreSession session) throws ClientException {
        SpaceManager spaceManager = getSpaceManager();
        return spaceManager.getSpaceFromId(spaceId, session);
    }

    protected SpaceManager getSpaceManager() throws ClientException {
        try {
            return Framework.getService(SpaceManager.class);
        } catch (Exception e) {
            throw new ClientException("Unable to get Space Manager", e);
        }
    }

    protected static Blob getBlob(FileItem item) {
        String ctype = item.getContentType();
        if (ctype == null) {
            ctype = "application/octet-stream";
        }
        Blob blob;
        if (item.isInMemory()) {
            blob = new ByteArrayBlob(item.get(), ctype);
        } else {
            try {
                blob = new FileBlob(item.getInputStream(), ctype);
            } catch (IOException e) {
                throw WebException.wrap("Failed to get blob data", e);
            }
        }
        blob.setFilename(item.getName());
        return blob;
    }

}

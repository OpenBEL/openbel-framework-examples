/**
 *  Copyright 2013 OpenBEL Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openbel.framework.ws.client.example;

import static java.lang.String.*;
import static java.lang.System.*;
import static org.openbel.framework.ws.client.ObjectFactory.createFindKamEdgesRequest;
import static org.openbel.framework.ws.client.RelationshipType.*;

import java.net.*;
import java.util.*;
import javax.xml.namespace.*;

import org.openbel.framework.ws.client.*;

/**
 * Example demonstrating retrieval of causal edges for a KAM in the catalog.
 */
public class GetCausalEdges {

    private static WebAPI WAPI;
    private final static String URL_STR;
    private final static String NS;
    static {
        URL_STR = "http://localhost:8080/openbel-ws/belframework.wsdl";
        NS = "http://belframework.org/ws/schemas";
    }

    public static void main(String... args) throws MalformedURLException {
        final String local = "WebAPIService";
        QName qn = new QName(NS, local);
        WebAPIService ws = new WebAPIService(new URL(URL_STR), qn);
        WAPI = ws.getWebAPISoap11();

        GetCatalogResponse catalog = WAPI.getCatalog(null);
        Kam kam = catalog.getKams().get(0);
        LoadKamRequest lkreq = new LoadKamRequest();
        lkreq.setKam(kam);
        out.println("Loading KAM.");
        LoadKamResponse lkres = WAPI.loadKam(lkreq);
        KamHandle handle = lkres.getHandle();
        out.println("Loaded KAM (handle: " + handle.getHandle() + ")");

        EdgeFilter edgfltr = new EdgeFilter();
        RelationshipTypeFilterCriteria r = new RelationshipTypeFilterCriteria();
        List<RelationshipType> relationships = r.getValueSet();
        relationships.add(CAUSES_NO_CHANGE);
        relationships.add(DECREASES);
        relationships.add(DIRECTLY_DECREASES);
        relationships.add(DIRECTLY_INCREASES);
        relationships.add(INCREASES);
        edgfltr.getRelationshipCriteria().add(r);

        FindKamEdgesRequest fereq = createFindKamEdgesRequest();
        fereq.setFilter(edgfltr);
        fereq.setHandle(handle);

        out.println("Finding edges.");
        FindKamEdgesResponse feres = WAPI.findKamEdges(fereq);
        List<KamEdge> edges = feres.getKamEdges();
        out.println(format("Found %d edges.", edges.size()));

        out.println("Releasing KAM.");
        ReleaseKamRequest rkreq = new ReleaseKamRequest();
        rkreq.setKam(handle);
        WAPI.releaseKam(rkreq);
        out.println("KAM released.");
    }
}

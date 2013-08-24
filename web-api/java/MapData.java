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

import static org.openbel.framework.ws.client.ObjectFactory.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;

import org.openbel.framework.ws.client.*;

/**
 * Example demonstrating mapping namespace data to {@link KamNode kam nodes}.
 */
public class MapData {

    private static WebAPI WAPI;
    private final static String URL_STR;
    private final static String NS;
    private final static String HGNC =
            "http://resource.belframework.org/belframework/1.0/namespace/hgnc-approved-symbols.belns";
    static {
        URL_STR = "http://localhost:8080/openbel-ws/belframework.wsdl";
        NS = "http://belframework.org/ws/schemas";
    }

    public static void main(String... args) throws MalformedURLException {
        // must provide KAM name
        if (args.length != 1) {
            System.out.println("usage: KAM_NAME");
            System.exit(1);
        }

        // read first argument as KAM name
        final String kamName = args[0];

        // create Web API service
        final String local = "WebAPIService";
        final QName qn = new QName(NS, local);
        final WebAPIService ws = new WebAPIService(new URL(URL_STR), qn);
        WAPI = ws.getWebAPISoap11();

        // Load Kam
        final Kam kam = new Kam();
        kam.setName(kamName);
        final LoadKamRequest lkreq = createLoadKamRequest();
        lkreq.setKam(kam);

        LoadKamResponse lkres = null;
        KAMLoadStatus status = KAMLoadStatus.IN_PROCESS;
        while (KAMLoadStatus.IN_PROCESS.equals(status)) {
            lkres = WAPI.loadKam(lkreq);
            status = lkres.getLoadStatus();
        }
        if (KAMLoadStatus.FAILED.equals(status) || lkres == null) {
            System.out.println("Failed to load kam " + kamName);
            return;
        }

        // Create node filter
        final NodeFilter filter = createNodeFilter();
        final FunctionTypeFilterCriteria ft = new FunctionTypeFilterCriteria();
        ft.getValueSet().add(FunctionType.PROTEIN_ABUNDANCE);
        filter.getFunctionTypeCriteria().add(ft);

        // Map Data (single entrez gene id)
        final String symbol = "PPARG";
        final KamHandle kamHandle = lkres.getHandle();
        final FindKamNodesByNamespaceValuesRequest req =
                createFindKamNodesByNamespaceValuesRequest();
        req.setHandle(kamHandle);
        final Namespace hgncNs = createNamespace();
        hgncNs.setPrefix("HGNC");
        hgncNs.setResourceLocation(HGNC);

        final NamespaceValue nv = createNamespaceValue();
        nv.setNamespace(hgncNs);
        // req.setNodeFilter(filter);
        nv.setValue(symbol);
        req.getNamespaceValues().add(nv);
        final FindKamNodesByNamespaceValuesResponse res =
                WAPI.findKamNodesByNamespaceValues(req);
        final List<KamNode> nodes = res.getKamNodes();

        // report how many kam nodes were mapped to
        System.out.println(nodes.size() + " KAM nodes were found.");

        // report KAM node and Get Supporting Terms
        final GetSupportingTermsRequest gstreq =
                new GetSupportingTermsRequest();
        if (!nodes.isEmpty()) {
            for (final KamNode n : nodes) {
                System.out.println("KAM node label:\n    " + n.getLabel());

                gstreq.setKamNode(n);
                final GetSupportingTermsResponse gstres = WAPI
                        .getSupportingTerms(gstreq);
                final List<BelTerm> terms = gstres.getTerms();
                for (final BelTerm t : terms) {
                    System.out.println("    BEL term label:\n        "
                            + t.getLabel());
                }

                System.out.print("\n");
            }
        }
    }
}

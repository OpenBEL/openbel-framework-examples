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

import static java.lang.String.format;
import static java.lang.System.*;

import java.net.*;
import javax.xml.namespace.*;

import org.openbel.framework.ws.client.*;

/**
 * Example demonstrating searching for equivalences.
 */
public class FindEquivalence {

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

        FindNamespaceEquivalenceRequest req;
        req = new FindNamespaceEquivalenceRequest();

        Namespace tgtNS = new Namespace();
        tgtNS.setPrefix("EG");
        tgtNS.setResourceLocation("http://resource.belframework.org/"
                + "belframework/1.0/namespace/entrez-gene-ids-hmr.belns");
        tgtNS.setId("m80Uzc2rk+ILu/P78PeOpg==");
        req.setTargetNamespace(tgtNS);
        Namespace srcNS = new Namespace();
        srcNS.setPrefix("HGNC");
        srcNS.setId("m80Uzc2rk+J7iWRI4xhYxA==");
        srcNS.setResourceLocation("http://resource.belframework.org/"
                + "belframework/1.0/namespace/hgnc-approved-symbols.belns");

        NamespaceValue srcValue = new NamespaceValue();
        srcValue.setNamespace(srcNS);
        srcValue.setValue("AKT1");

        req.setNamespaceValue(srcValue);
        FindNamespaceEquivalenceResponse resp;
        resp = WAPI.findNamespaceEquivalence(req);
        NamespaceValue nsValue = resp.getNamespaceValue();

        if (nsValue != null) {
            String prefix = srcNS.getPrefix();
            String value = srcValue.getValue();
            String src = format("%s:%s", prefix, value);

            Namespace ns = nsValue.getNamespace();
            prefix = ns.getPrefix();
            value = nsValue.getValue();
            String equiv = format("%s:%s", prefix, value);

            out.println(format("%s is equivalent to %s", src, equiv, args));
        }
    }
}

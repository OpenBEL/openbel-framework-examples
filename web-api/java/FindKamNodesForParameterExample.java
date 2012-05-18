import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.selventa.belframework.ws.client.BelTerm;
import com.selventa.belframework.ws.client.FindEquivalencesRequest;
import com.selventa.belframework.ws.client.FindEquivalencesResponse;
import com.selventa.belframework.ws.client.FindKamNodesByPatternsRequest;
import com.selventa.belframework.ws.client.FindKamNodesByPatternsResponse;
import com.selventa.belframework.ws.client.GetNamespacesRequest;
import com.selventa.belframework.ws.client.GetNamespacesResponse;
import com.selventa.belframework.ws.client.GetSupportingTermsRequest;
import com.selventa.belframework.ws.client.GetSupportingTermsResponse;
import com.selventa.belframework.ws.client.Kam;
import com.selventa.belframework.ws.client.KamNode;
import com.selventa.belframework.ws.client.LoadKamRequest;
import com.selventa.belframework.ws.client.LoadKamResponse;
import com.selventa.belframework.ws.client.Namespace;
import com.selventa.belframework.ws.client.NamespaceValue;
import com.selventa.belframework.ws.client.WebAPI;
import com.selventa.belframework.ws.client.WebAPIService;

/**
 * BELFramework WebAPI that shows how to retrieve KamNodes based on
 * a parameter and all of its equivalences.  Specifically the WebAPI
 * operations used are:<ul>
 * <li>LoadKam</li>
 * <li>GetNamespaces</li>
 * <li>FindEquivalences</li>
 * <li>FindKamNodesByPatterns</li>
 * <li>GetSupportingTerms</li></ul>
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class FindKamNodesForParameterExample {

    private static final String API_URL = "http://localhost:8080/openbel-ws/belframework.wsdl";
    private static final String HGNC_LOCATION = "http://resource.belframework.org/belframework/1.0/namespace/hgnc-approved-symbols.belns";

    public static void main(String[] args) throws MalformedURLException {
        final WebAPIService ws = new WebAPIService(new URL(API_URL), new QName(
                "http://belframework.org/ws/schemas", "WebAPIService"));
        final WebAPI api = ws.getWebAPISoap11();

        // Load Kam
        final Kam kam = new Kam();
        kam.setName("small");
        final LoadKamRequest lkreq = new LoadKamRequest();
        lkreq.setKam(kam);
        final LoadKamResponse lkres = api.loadKam(lkreq);

        // Find HGNC Namespace
        final GetNamespacesRequest gnsreq = new GetNamespacesRequest();
        gnsreq.setHandle(lkres.getHandle());
        final GetNamespacesResponse gnsres = api.getNamespaces(gnsreq);

        Namespace hgncNs = null;
        for (final Namespace ns : gnsres.getNamespaces()) {
            if (HGNC_LOCATION.equals(ns.getResourceLocation())) {
                hgncNs = ns;
                break;
            }
        }

        // Check if HGNC was found, error if not
        if (hgncNs == null) {
            throw new IllegalStateException("Cannot find HGNC.");
        }

        // Find equivalences for JAK3
        final FindEquivalencesRequest fereq = new FindEquivalencesRequest();
        final NamespaceValue hgncJAK3 = new NamespaceValue();
        hgncJAK3.setNamespace(hgncNs);
        hgncJAK3.setValue("JAK3");
        fereq.setNamespaceValue(hgncJAK3);

        final FindEquivalencesResponse jak3Equivalences = api.findEquivalences(fereq);

        // Build list of namespace parameters, including HGNC:JAK3 and its equivalences.
        final List<String> namespaceParameters = new ArrayList<String>(
                jak3Equivalences.getNamespaceValues().size() + 1);
        namespaceParameters.add("JAK3");
        for (final NamespaceValue jak3Equivalent : jak3Equivalences.getNamespaceValues()) {
            namespaceParameters.add(jak3Equivalent.getValue());
        }

        // Find all KamNodes in Kam
        final FindKamNodesByPatternsRequest fknbpreq = new FindKamNodesByPatternsRequest();
        fknbpreq.getPatterns().add(".*");
        fknbpreq.setHandle(lkres.getHandle());
        final FindKamNodesByPatternsResponse fknbpres = api.findKamNodesByPatterns(fknbpreq);
        final List<KamNode> allKamNodes = fknbpres.getKamNodes();

        // Loop through supporting terms for each KamNode and determine if it contains HGNC:JAK3
        // or any of its equivalent namespace parameters.
        final List<KamNode> targetNodes = new ArrayList<KamNode>();
        GetSupportingTermsRequest gstreq = new GetSupportingTermsRequest();
        for (final KamNode kamNode : allKamNodes) {
            gstreq.setKamNode(kamNode);
            GetSupportingTermsResponse gstres = api.getSupportingTerms(gstreq);
            final List<BelTerm> terms = gstres.getTerms();
            for (final BelTerm term : terms) {
                boolean found = false;
                for (final String namespaceParameter : namespaceParameters) {
                    if (term.getLabel().contains(namespaceParameter)) {
                        found = true;
                        break;
                    }
                }

                if (found) {
                    targetNodes.add(kamNode);
                }
            }
        }

        System.out.println("Found " + targetNodes.size() + " KAM nodes containing JAK3 or equivalent.");
    }
}


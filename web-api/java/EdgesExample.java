import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import javax.xml.namespace.QName;

import com.selventa.belframework.ws.client.EdgeFilter;
import com.selventa.belframework.ws.client.FindKamEdgesRequest;
import com.selventa.belframework.ws.client.FindKamEdgesResponse;
import com.selventa.belframework.ws.client.GetCatalogResponse;
import com.selventa.belframework.ws.client.Kam;
import com.selventa.belframework.ws.client.KamEdge;
import com.selventa.belframework.ws.client.LoadKamRequest;
import com.selventa.belframework.ws.client.LoadKamResponse;
import com.selventa.belframework.ws.client.RelationshipType;
import com.selventa.belframework.ws.client.RelationshipTypeFilterCriteria;
import com.selventa.belframework.ws.client.WebAPI;
import com.selventa.belframework.ws.client.WebAPIService;

public class EdgesExample {

    private static final String API_URL = "http://localhost:8080/openbel-ws/belframework.wsdl";

    public static void main(String[] args) throws MalformedURLException {
        final WebAPIService ws = new WebAPIService(new URL(API_URL), new QName(
                "http://belframework.org/ws/schemas", "WebAPIService"));
        final WebAPI api = ws.getWebAPISoap11();

        GetCatalogResponse gcres = api.getCatalog(null);
        for (final Kam kam : gcres.getKams()) {
            System.out.println(kam.getName());
        }

        // Load Kam
        final Kam kam = new Kam();
        kam.setName("SMALL");
        final LoadKamRequest lkreq = new LoadKamRequest();
        lkreq.setKam(kam);
        final LoadKamResponse lkres = api.loadKam(lkreq);

        final RelationshipTypeFilterCriteria rtc = new RelationshipTypeFilterCriteria();
        rtc.getValueSet().addAll(Arrays.asList(RelationshipType.values()));
        final EdgeFilter edgeFilter = new EdgeFilter();
        edgeFilter.getRelationshipCriteria().add(rtc);

        final FindKamEdgesRequest fereq = new FindKamEdgesRequest();
        fereq.setFilter(edgeFilter);
        fereq.setHandle(lkres.getHandle());
        final FindKamEdgesResponse feres = api.findKamEdges(fereq);

        for (final KamEdge ke : feres.getKamEdges()) {
            System.out.println(ke.getSource().getLabel() + " "
                    + ke.getRelationship().name() + " "
                    + ke.getTarget().getLabel());
        }
    }
}


import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.xml.namespace.QName;

import com.selventa.belframework.ws.client.BelDocument;
import com.selventa.belframework.ws.client.FindKamNodesByPatternsRequest;
import com.selventa.belframework.ws.client.FindKamNodesByPatternsResponse;
import com.selventa.belframework.ws.client.GetBelDocumentsRequest;
import com.selventa.belframework.ws.client.GetBelDocumentsResponse;
import com.selventa.belframework.ws.client.GetCatalogResponse;
import com.selventa.belframework.ws.client.GetNamespacesRequest;
import com.selventa.belframework.ws.client.GetNamespacesResponse;
import com.selventa.belframework.ws.client.Kam;
import com.selventa.belframework.ws.client.KamHandle;
import com.selventa.belframework.ws.client.LoadKamRequest;
import com.selventa.belframework.ws.client.LoadKamResponse;
import com.selventa.belframework.ws.client.Namespace;
import com.selventa.belframework.ws.client.ReleaseKamRequest;
import com.selventa.belframework.ws.client.WebAPI;
import com.selventa.belframework.ws.client.WebAPIService;

/**
 * This example leverages the web services API to print simple statistics for a KAM.
 */
public class KamSummarizerExample {

    private WebAPI api;

    private boolean listCatalog = false;
    private String kamName = null;
    private String serviceURL = "http://localhost:8080/openbel-ws/belframework.wsdl"; //default to localhost:8080

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");


    public KamSummarizerExample() throws MalformedURLException {
        super();
    }

    protected void setupWebService() throws MalformedURLException {
        //set up web services location
        WebAPIService ws = new WebAPIService(new URL(this.serviceURL), new QName("http://belframework.org/ws/schemas", "WebAPIService"));
        api = ws.getWebAPISoap11();
    }

    protected void printApplcationInfo() {
        final StringBuilder bldr = new StringBuilder();
        bldr.append("\n");
        bldr.append("BEL Framework V1.2.0: ").append("KAM Summarizer\n");
        bldr.append("Copyright (c) 2011-2012, Selventa. All Rights Reserved.\n");
        bldr.append("\n");
        System.out.println(bldr.toString());
    }

    protected void printCatalog() {
        System.out.println("Available KAMS:");
        System.out.println("\tName\tLast Compiled\tDescription");
        System.out.println("\t------\t-------------\t-----------");
        for (Kam kam : getCatalog()) {
            System.out.println(String.format("\t%s\t%s\t%s", kam.getName(), sdf.format(kam.getLastCompiled().toGregorianCalendar().getTime()), kam.getDescription()));
        }
        System.out.print("\n");
    }

    /**
     * Loops through the KAM catalog, find KAM with the specified name
     * @param name
     * @return
     */
    protected Kam findKam(String name) {
        Kam kam = null;
        for (Kam k : getCatalog()) {
            if( k.getName().equals(name) ) {
                kam = k;
            }
        }
        return kam;
    }

    protected void summarizeKam(String name) {

        Kam kam = findKam(name); //find the KAM object
        if( kam != null ) {
            System.out.println("Loading KAM into server's memory...\n");
            //load the KAM into server's memory
            LoadKamRequest loadKamRequest = new LoadKamRequest();
            loadKamRequest.setKam(kam);
            LoadKamResponse response = api.loadKam(loadKamRequest);

            System.out.println("Summarizing KAM: " + kam.getName());
            System.out.println("\tDescription: " + kam.getDescription());
            System.out.println("\tLast compiled: " + sdf.format(kam.getLastCompiled().toGregorianCalendar().getTime()));
            System.out.println();

            //get a handle to the key to reference our KAM in server
            KamHandle kamHandle = response.getHandle();

            //print the total number of namespaces in the KAM
            System.out.println("\tNumber of namespaces: " + getNamespaces(kamHandle).size());

            //print the total number of BEL documents in the KAM
            System.out.println("\tNumber of documents: " + getBelDocuments(kamHandle).size());

            //find all protein nodes in the kam
            FindKamNodesByPatternsRequest findNodesRequest = new FindKamNodesByPatternsRequest();
            findNodesRequest.setHandle(kamHandle);
            FindKamNodesByPatternsResponse re;

            //total number of nodes in the KAM
            findNodesRequest.getPatterns().add(".*");
            re = api.findKamNodesByPatterns(findNodesRequest);
            System.out.println("\tTotal number of nodes: " + re.getKamNodes().size());

            //total number of protein nodes int he KAM
            findNodesRequest.getPatterns().clear();
            findNodesRequest.getPatterns().add("proteinAbundance(.*)");
            re = api.findKamNodesByPatterns(findNodesRequest);
            System.out.println("\tNumber of protein nodes: " + re.getKamNodes().size());

            //total number or RNA nodes in the KAM
            findNodesRequest.getPatterns().clear();
            findNodesRequest.getPatterns().add("rnaAbundance(.*)");
            re = api.findKamNodesByPatterns(findNodesRequest);
            System.out.println("\tNumber of RNA nodes: " + re.getKamNodes().size());
            System.out.println();
            //release the KAM from server's memory
            ReleaseKamRequest releaseKamRequest = new ReleaseKamRequest();
            releaseKamRequest.setKam(kamHandle);
            api.releaseKam(releaseKamRequest);
            System.out.println("KAM is released from server's memory successfully.");
        } else {
            System.out.println("Unable to find specified KAM: " + name);
        }

    }

    /**
     * Gets a list of namespaces used in a KAM
     * @param kam
     * @return
     */
    protected List<Namespace> getNamespaces(KamHandle kamHandle) {
        GetNamespacesRequest getNamespacesRequest = new GetNamespacesRequest();
        getNamespacesRequest.setHandle(kamHandle);
        GetNamespacesResponse response = api.getNamespaces(getNamespacesRequest);
        return response.getNamespaces();
    }

    /**
     * Gets a list of documents used in a KAM
     * @param kam
     * @return
     */
    protected List<BelDocument> getBelDocuments(KamHandle kamHandle) {
        GetBelDocumentsRequest getBelDocumentsRequest = new GetBelDocumentsRequest();
        getBelDocumentsRequest.setHandle(kamHandle);
        GetBelDocumentsResponse response = api.getBelDocuments(getBelDocumentsRequest);
        return response.getDocuments();
    }

    public void run() throws Exception {

        printApplcationInfo();

        setupWebService();

        if( isListCatalog() ) {
            printCatalog();
        }

        if( getKamName() != null ) {
            summarizeKam(getKamName());
        }
    }

    protected List<Kam> getCatalog() {
        //retrieve a list of KAMs using our web service API
        GetCatalogResponse response = api.getCatalog(null);
        return response.getKams();
    }

    public static void main(String[] args) {

        try {
            KamSummarizerExample app = new KamSummarizerExample();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("-l") || arg.equals("--list-catalog")) {
                    app.setListCatalog(true);
                } else if (arg.equals("-k") || arg.equals("--kam-name")) {
                    if ((i + 1) < args.length) {
                       app.setKamName(args[i + 1]);
                    } else {
                        printUsageThenExit();
                    }
                } else if (arg.equals("-u") || arg.equals("--service-url")) {
                    if ((i + 1) < args.length) {
                        app.setServiceURL(args[i + 1]);
                     } else {
                         printUsageThenExit();
                     }
                 }
            }
            if( !app.isListCatalog() && app.getKamName() == null ) {
                printUsageThenExit();
            }
            app.run();
        } catch (Exception e) {
            System.out.println("Unable to run application: " + e.getMessage());
        }
    }

    private static void printUsageThenExit() {
        System.out.println("Usage:\n"
                + "  -l       --list-catalog       Lists the KAMs in the KAM Store\n"
                + "  -k KAM,  --kam-name KAM       The kam to summarize\n"
                + "  -u URL,  --service-url URL    The URL to the web service end point\n"
                );
        System.exit(1);
    }

    public boolean isListCatalog() {
        return listCatalog;
    }

    public void setListCatalog(boolean listCatalog) {
        this.listCatalog = listCatalog;
    }

    public String getKamName() {
        return kamName;
    }

    public void setKamName(String kamName) {
        this.kamName = kamName;
    }

    public String getServiceURL() {
        return serviceURL;
    }

    public void setServiceURL(String serviceURL) {
        this.serviceURL = serviceURL;
    }
}


package com.selventa.belframework.api.examples;

import static java.lang.System.err;
import static java.lang.System.exit;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import com.selventa.belframework.api.KamStore;
import com.selventa.belframework.api.KamStoreImpl;
import com.selventa.belframework.api.examples.XGMMLObjects.Edge;
import com.selventa.belframework.api.examples.XGMMLObjects.Node;
import com.selventa.belframework.common.cfg.SystemConfiguration;
import com.selventa.belframework.df.DBConnection;
import com.selventa.belframework.df.DatabaseService;
import com.selventa.belframework.df.DatabaseServiceImpl;
import com.selventa.belframework.kamstore.data.jdbc.KAMStoreDaoImpl.BelTerm;
import com.selventa.belframework.kamstore.data.jdbc.KAMStoreDaoImpl.Namespace;
import com.selventa.belframework.kamstore.model.Kam;
import com.selventa.belframework.kamstore.model.Kam.KamEdge;
import com.selventa.belframework.kamstore.model.Kam.KamNode;
import com.selventa.belframework.kamstore.model.KamStoreException;
import com.selventa.belframework.kamstore.model.SpeciesKam;

public class SpeciesKamExample {

    private static final String HGNC =
            "http://resource.belframework.org/belframework/1.0/namespace/hgnc-approved-symbols.belns";

    private final String kamName;

    /**
     * Holds a reference to the system configuration
     */
    private SystemConfiguration systemConfiguration;
    private KamStore kamStore;
    private DBConnection dbConnection;

    /**
     * Constructs the KamFilterExample
     */
    public SpeciesKamExample(String kamName) {
        this.kamName = kamName;
    }

    public void run() throws Exception {

        // set up the KAM store by supplying database information
        setUpKamStore();

        Kam kam = kamStore.getKam(kamName);
        Namespace ns = kamStore.getNamespace(kam, HGNC);

        if (ns == null) {
            err.println("Kam '" + kamName + "' does not reference namespace '" + HGNC + "'.");
            exit(1);
        }

        SpeciesKam humanKam = kamStore.getKamForSpecies(kamName, ns);
        writeXGMML(humanKam);
    }

    private void writeXGMML(final Kam kam) throws IOException,
            KamStoreException {
        // Set up a writer to write the XGMML
        PrintWriter writer = new PrintWriter(new File(kam.getKamInfo()
                .getName() + ".xgmml"));

        // Write xgmml <graph> element header
        XGMMLUtility.writeStart("Species-specific KAM for "
                + kam.getKamInfo().getName(), writer);

        // Iterate over the path nodes and capture in XGMML
        final Collection<KamNode> nodes = kam.getNodes();
        for (KamNode pathNode : nodes) {
            Node xNode = new Node();
            xNode.id = pathNode.getId();
            xNode.label = pathNode.getLabel();
            xNode.function = pathNode.getFunctionType();

            List<BelTerm> supportingTerms = kamStore
                    .getSupportingTerms(pathNode);

            XGMMLUtility.writeNode(xNode, supportingTerms, writer);
        }

        // Iterate over the path nodes, find the edges, and capture in XGMML
        for (KamEdge edge : kam.getEdges()) {
            Edge xEdge = new Edge();
            xEdge.id = edge.getId();
            xEdge.rel = edge.getRelationshipType();
            xEdge.source = edge.getSourceNode().getId();
            xEdge.target = edge.getTargetNode().getId();

            KamNode knsrc = edge.getSourceNode();
            KamNode kntgt = edge.getTargetNode();

            Node src = new Node();
            src.function = knsrc.getFunctionType();
            src.label = knsrc.getLabel();
            Node tgt = new Node();
            tgt.function = kntgt.getFunctionType();
            tgt.label = kntgt.getLabel();

            XGMMLUtility.writeEdge(src, tgt, xEdge, writer);
        }

        // Close out the writer
        XGMMLUtility.writeEnd(writer);
        writer.close();
    }

    /**
     * Sets up the KAM store using the database information specified in the
     * SystemConfiguration.
     *
     * @throws SQLException
     * @throws IOException
     */
    protected void setUpKamStore() throws SQLException, IOException {
        setUpSystemConfiguration();
        // Setup a database connector to the KAM Store.
        DatabaseService dbService = new DatabaseServiceImpl();
        dbConnection = dbService.dbConnection(systemConfiguration.getKamURL(), systemConfiguration.getKamUser(), systemConfiguration.getKamPassword());

        // Connect to the KAM Store. This establishes a connection to the
        // KamStore database and sets up the system to read and process
        // Kams.
        kamStore = new KamStoreImpl(dbConnection);
    }

    /**
     * Reads the system configuration from the default location
     *
     * @throws IOException
     */
    protected void setUpSystemConfiguration() throws IOException {
        SystemConfiguration.createSystemConfiguration(null);
        systemConfiguration = SystemConfiguration.getSystemConfiguration();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("usage: xgmml.sh [KAM Name]");
            System.exit(1);
        }

        SpeciesKamExample app = new SpeciesKamExample(args[0]);
        app.run();
    }
}

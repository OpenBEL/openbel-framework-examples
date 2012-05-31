package org.openbel.framework.examples.api.speciesexport;

import static org.openbel.framework.common.BELUtilities.isNumeric;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import org.openbel.framework.api.DefaultDialect;
import org.openbel.framework.api.DefaultSpeciesDialect;
import org.openbel.framework.api.Kam;
import org.openbel.framework.api.Kam.KamEdge;
import org.openbel.framework.api.Kam.KamNode;
import org.openbel.framework.api.KamSpecies;
import org.openbel.framework.api.KamStore;
import org.openbel.framework.api.KamStoreException;
import org.openbel.framework.api.KamStoreImpl;
import org.openbel.framework.common.cfg.SystemConfiguration;
import org.openbel.framework.common.enums.RelationshipType;
import org.openbel.framework.core.df.DBConnection;
import org.openbel.framework.core.df.DatabaseService;
import org.openbel.framework.core.df.DatabaseServiceImpl;
import org.openbel.framework.examples.api.speciesexport.XGMMLObjects.Edge;
import org.openbel.framework.examples.api.speciesexport.XGMMLObjects.Node;
import org.openbel.framework.internal.KAMStoreDaoImpl.BelTerm;

/**
 * {@link KamSpeciesExample} captures an example of orthologizing a
 * {@link Kam kam} containing {@link RelationshipType#ORTHOLOGOUS orthologous}
 * relationships.  
 * 
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class KamSpeciesExample {

    private final String kamName;

    /**
     * Holds a reference to the system configuration
     */
    private SystemConfiguration systemConfiguration;
    private KamStore kamStore;
    private DBConnection dbConnection;

    private int taxId;

    /**
     * Constructs the {@link KamSpeciesExample} given the:
     * <ol>
     * <li>Name of the {@link Kam kam} containing orthologous relationships</li>
     * <li>Species tax id to orthologize to</li>
     * </ol>
     */
    public KamSpeciesExample(final String kamName, final int taxId) {
        this.kamName = kamName;
        this.taxId = taxId;
    }

    public void run() throws Exception {

        // set up the KAM store by supplying database information
        setUpKamStore();
        
        // load full kam with orthologous information
        final Kam kam = kamStore.getKam(kamName);
        
        // orthologize kam to species 
        final KamSpecies kamSpecies = new KamSpecies(kam,
                new DefaultSpeciesDialect(kam.getKamInfo(), kamStore, taxId, true),
                new DefaultDialect(kam.getKamInfo(), kamStore, true), kamStore);
        
        // write out XGMML graph
        writeXGMML(kamSpecies);
    }

    private void writeXGMML(final Kam kam) throws IOException,
            KamStoreException {
        // Set up a writer to write the XGMML
        PrintWriter writer = new PrintWriter(new File(kam.getKamInfo()
                .getName() + ".xgmml"));

        // Write xgmml <graph> element header
        XGMMLUtility.writeStart("Species-specific KAM for "
                + kam.getKamInfo().getName(), writer);

        // Iterate nodes and capture in XGMML
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

        // Iterate edges and capture in XGMML
        final Collection<KamEdge> edges = kam.getEdges();
        for (KamEdge edge : edges) {
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

    /**
     * Main method to execute the {@link KamSpeciesExample example}.  The
     * program takes the following as input:
     * <ol>
     * <li>{@link Kam kam} name</li>
     * <li>Species tax id, parsed to an {@code int}</li>
     * <ol>
     * 
     * @param args {@link String}[]
     * @throws Exception Thrown if an error occurred while retrieving the
     * {@link Kam kam} or exporting it to XGMML
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Incorrect number of arguments.");
            System.err.println("usage: xgmml.sh [KAM Name] [Tax Id]");
            System.exit(1);
        }

        final String kamName = args[0];
        final String taxId = args[1];
        if (!isNumeric(taxId)) {
            System.err.print("Tax Id is not a number.");
            System.err.println("usage: xgmml.sh [KAM Name] [Tax Id]");
            System.exit(1);
        }
        
        KamSpeciesExample app = new KamSpeciesExample(kamName,
                Integer.parseInt(taxId));
        app.run();
    }
}

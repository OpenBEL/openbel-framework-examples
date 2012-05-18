package com.selventa.belframework.api.examples;

import static com.selventa.belframework.common.enums.BELFrameworkVersion.VERSION_LABEL;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import com.selventa.belframework.api.KamStore;
import com.selventa.belframework.api.KamStoreImpl;
import com.selventa.belframework.api.examples.xgmml.XGMMLObjects.Edge;
import com.selventa.belframework.api.examples.xgmml.XGMMLObjects.Node;
import com.selventa.belframework.common.cfg.SystemConfiguration;
import com.selventa.belframework.common.enums.RelationshipType;
import com.selventa.belframework.df.DBConnection;
import com.selventa.belframework.df.DatabaseService;
import com.selventa.belframework.df.DatabaseServiceImpl;
import com.selventa.belframework.kamstore.data.jdbc.KAMStoreDaoImpl.BelTerm;
import com.selventa.belframework.kamstore.model.EdgeDirectionType;
import com.selventa.belframework.kamstore.model.Kam;
import com.selventa.belframework.kamstore.model.Kam.KamEdge;
import com.selventa.belframework.kamstore.model.Kam.KamNode;
import com.selventa.belframework.kamstore.model.KamStoreException;

/**
 * PathFinder leverages the KAM API to demonstrate path finding capabilities.
 * 
 * <p>
 * This pathfinder uses Dijkstra's algorithm to find the shortest path from
 * source to target {@link KamNode} id.
 * </p>
 * 
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class PathFinder {

    /**
     * Holds the name of the KAM to export.
     */
    private final String kamName;
    
    /**
     * Holds the source node id to pathfind from.
     */
    private final Integer sourceNodeId;

    /**
     * Holds the target node id to pathfind to.
     */
    private final Integer targetNodeId;
    
    /**
     * Holds the output file to write the path XGMML to.
     */
    private final String outputFile;
    
    /**
     * Holds a reference to the system configuration.
     */
    private SystemConfiguration config;
    
    /**
     * Holds a kam store.
     */
    private KamStore kamStore;
    
    /**
     * Constructs the PathFinder with a kam name, source / target node id,
     * and XGMML output file.
     * 
     * @param kamName {@link String}, the kam name
     * @param sourceNodeId {@link Integer}, the source node id
     * @param targetNodeId {@link Integer}, the target node id
     * @param outputFile {@link String}, the XGMML output file
     */
    public PathFinder(String kamName, Integer sourceNodeId,
            Integer targetNodeId, String outputFile) {
        if (kamName == null || sourceNodeId == null || targetNodeId == null
                || outputFile == null) {
            throw new IllegalArgumentException("input(s) were null.");
        }
        
        this.kamName = kamName;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.outputFile = outputFile;
        
        try {
            SystemConfiguration.createSystemConfiguration(null);
            config = SystemConfiguration.getSystemConfiguration();
        } catch (IOException e) {
            System.out.println("Terminated.");
            System.exit(1);
        }
    }
    
    /**
     * Run the path find implementation and output the results.
     * 
     * @throws SQLException Thrown if a SQL error occurred working with the
     * kam
     * @throws KamStoreException Thrown if the {@link KamStoreImpl} could not find
     * the kam
     * @throws IOException Thrown if an IO error occurred saving the found
     * path to an XGMML file
     */
    public void runPathFind() throws SQLException,
            KamStoreException, IOException {
        System.out.println("Calculating shortest path from " + sourceNodeId
                + " to " + targetNodeId + "...");
        
        // Setup a database connector to the KAM Store.
        DatabaseService dbService = new DatabaseServiceImpl();
        DBConnection dbc = dbService.dbConnection(
                config.getKamURL(),
                config.getKamUser(),
                config.getKamPassword());

        // Connect to the KAM Store. This establishes a connection to the
        // KamStore database and sets up the system to read and process
        // Kams. 
        kamStore = new KamStoreImpl(dbc);

        // Use the KamStore object to load the Kam which the user requested. If the Kam
        // is not found a KamStoreException will be thrown
        Kam kam = kamStore.getKam(kamName);
        
        KamNode sourceNode = kam.findNode(sourceNodeId);
        KamNode targetNode = kam.findNode(targetNodeId);
        
        // verify that the source node exists, otherwise fail.
        if (sourceNode == null) {
            System.out.println("Source node cannot be found in kam: " + kamName);
            System.out.println("Terminated.");
            System.exit(1);
        }
        
        // verify that the target node exists, otherwise fail.
        if (targetNode == null) {
            System.out.println("Target node cannot be found in kam: " + kamName);
            System.out.println("Terminated.");
            System.exit(1);
        }
        
        // do the dijkstra 
        Map<KamNode, KamNode> predecessors = doPathFind(kam, sourceNode,
                targetNode);
        
        if (!predecessors.containsKey(targetNode)) {
            if (sourceNode != null && targetNode != null) {
                System.out.println("A path from [ " + sourceNode.getLabel()
                        + " ] to [ " + targetNode.getLabel()
                        + " ] could not be found.");
            }
        } else {
            // construct path from target to source
            List<KamNode> pathNodes = new ArrayList<Kam.KamNode>();
            pathNodes.add(targetNode);
            
            KamNode predecessor = predecessors.get(targetNode);
            while (predecessor != null) {
                pathNodes.add(predecessor);
                predecessor = predecessors.get(predecessor);
            }
            
            Collections.reverse(pathNodes);
            
            System.out.println("Path found:");
            int i = 1;
            for (KamNode pathNode : pathNodes) {
                System.out.println("  (" + i + ") Path node id: " + pathNode.getId());
                System.out.println("    " + pathNode.getLabel());
                i++;
            }
            
            writeXGMML(kam, pathNodes);
            System.out.println("\nXGMML saved to file: " + new File(outputFile).getAbsolutePath());
        }
        
        // We are done with the Kam so we can close it out. This releases any
        // cached
        // data and connections to the Kam Store database
        kamStore.close(kam);

        // Close the database connection
        dbc.getConnection().close();
        
        // Report that they process is complete
        System.out.println("\nTerminated.");
    }
    
    /**
     * Provides the main loop of the Dijkstra shortest path algorithm.
     * 
     * @param kam {@link Kam}, the kam to path find on
     * @param sourceNode {@link KamNode}, the kam node to path find from
     * @param targetNode {@link KamNode}, the kam node to path find to
     * @return {@link Map} of {@link KamNode} to {@link KamNode}, the
     * kam node predecessors map that allows tracing back the found path
     */
    private Map<KamNode, KamNode> doPathFind(Kam kam, KamNode sourceNode,
            KamNode targetNode) {
        final Map<KamNode, Integer> distances = new HashMap<KamNode, Integer>();
        final Map<KamNode, KamNode> predecessors = new HashMap<Kam.KamNode, Kam.KamNode>();
        final Set<KamNode> settled = new HashSet<KamNode>();
        final Queue<KamNode> unsettled = new PriorityQueue<KamNode>(512, new Comparator<KamNode>() {
            @Override
            public int compare(KamNode kn1, KamNode kn2) {
                Integer kn1d = distances.get(kn1);
                Integer kn2d = distances.get(kn2);
                return kn1d.compareTo(kn2d);
            }
        });
        
        unsettled.add(sourceNode);
        distances.put(sourceNode, 0);
        
        while (!unsettled.isEmpty()) {
            KamNode minDistance = findMinimum(unsettled);
            settled.add(minDistance);
            relax(distances, predecessors, settled, unsettled, kam, minDistance);
        }
        
        return predecessors;
    }
    
    /**
     * Find the minimum distance {@link KamNode} on the unsettled queue.  Since
     * a priority queue is used as the queue implementation, the minimum
     * distance node will always be the head of the queue, so pop it.
     * 
     * @param unsettled {@link Queue} of {@link KamNode}, the unsettled kam
     * nodes
     * @return {@link KamNode} the minimum distance kam node
     */
    private KamNode findMinimum(Queue<KamNode> unsettled) {
        return unsettled.remove();
    }

    /**
     * Relax all edges from the new <tt>minDistance</tt> node.
     * 
     * @param distances {@link Map} of {@link KamNode} to {@link Integer}
     * distance
     * @param predecessor {@link Map} of {@link KamNode} to {@link KamNode}
     * to capture a path trail
     * @param settled {@link Set} of {@link KamNode}, the nodes that have
     * already been settled
     * @param unsettled {@link Queue} of {@link KamNode}, the queue of
     * unsettled nodes which is sorted by minimum distance
     * @param kam {@link Kam}, the kam used for path find
     * @param minDistance {@link KamNode}, the current minimum distance
     * node that is being relaxed
     */
    private void relax(Map<KamNode, Integer> distances,
            Map<KamNode, KamNode> predecessor, Set<KamNode> settled,
            Queue<KamNode> unsettled, Kam kam, KamNode minDistance) {
        
        // relax forward direction
        Set<KamEdge> edges = kam.getAdjacentEdges(minDistance, EdgeDirectionType.FORWARD);
        for (KamEdge edge : edges) {
            KamNode target = edge.getTargetNode();
            
            // if we have visited this node, skip it
            if (settled.contains(target)) {
                continue;
            }
            
            relaxEdge(distances, predecessor, settled, unsettled, kam,
                    minDistance, edge, target);
        }
        
        // relax reverse direction
        edges = kam.getAdjacentEdges(minDistance, EdgeDirectionType.REVERSE);
        for (KamEdge edge : edges) {
            KamNode source = edge.getSourceNode();
            
            // if we have visited this node, skip it
            if (settled.contains(source)) {
                continue;
            }
            
            relaxEdge(distances, predecessor, settled, unsettled, kam,
                    minDistance, edge, source);
        }
    }
    
    /**
     * Explore the new node and edge and see if this path's distance has
     * improved.  If it has then update distance, predecessor, and indicate
     * the node as settled.
     * 
     * @param distances {@link Map} of {@link KamNode} to {@link Integer}
     * distance
     * @param predecessor {@link Map} of {@link KamNode} to {@link KamNode}
     * to capture a path trail
     * @param settled {@link Set} of {@link KamNode}, the nodes that have
     * already been settled
     * @param unsettled {@link Queue} of {@link KamNode}, the queue of
     * unsettled nodes which is sorted by minimum distance
     * @param kam {@link Kam}, the kam used for path find
     * @param minDistance {@link KamNode}, the current minimum distance
     * node that is being relaxed
     * @param edge {@link KamEdge}, the kam edge to use in new distance
     * calculation
     * @param edgeNode {@link KamNode}, the kam edge's node to evaluate
     */
    private void relaxEdge(Map<KamNode, Integer> distances,
            Map<KamNode, KamNode> predecessor, Set<KamNode> settled,
            Queue<KamNode> unsettled, Kam kam, KamNode minDistance,
            KamEdge edge, KamNode edgeNode) {
        Integer dt = distance(edgeNode, distances);
        Integer dref = distances.get(minDistance);
        
        // distance to dt is less if traveling through dref, so promote
        Integer dnew = dref + weight(edge);
        if (dt > dnew) {
            distances.put(edgeNode, dnew);
            predecessor.put(edgeNode, minDistance);
            unsettled.add(edgeNode);
        }
    }
    
    /**
     * Return the distance for <tt>node</tt>.  If <tt>node</tt> does not exist
     * in the <tt>distances</tt> map then return {@link Integer#MAX_VALUE}.
     * 
     * @param node {@link KamNode}, the kam node to retrieve distance for
     * @param distances {@link Map} of {@link KamNode} to {@link Integer}
     * distance
     * @return <tt>int</tt> the kam nodes distance or {@link Integer#MAX_VALUE}
     * if a distance hasn't yet been seen for this node
     */
    private int distance(KamNode node, Map<KamNode, Integer> distances) {
        Integer distance = distances.get(node);
        return distance == null ? Integer.MAX_VALUE : distance;
    }
    
    /**
     * Calculate the weight of a {@link KamEdge} based on its type.
     * 
     * @param edge {@link KamEdge}, the kam eddge to weight
     * @return <tt>int</tt> the weight of the {@link KamEdge}
     */
    private int weight(KamEdge edge) {
        RelationshipType rel = edge.getRelationshipType();
        if (rel.isDirect()) {
            return 1;
        } else if (rel.isCausal()) {
            return 2;
        }
        
        return 3;
    }
    
    /**
     * Write the XGMML representation of the discovered path.
     * 
     * @param kam {@link Kam}, the kam used for path find
     * @param pathNodes {@link List} of {@link KamNode}, the path's kam nodes
     * @throws IOException Thrown if an IO error occurs while writing out the
     * XGMML file
     * @throws KamStoreException Thrown if an KamStore error occurs while
     * retrieving the supporting terms for a {@link KamNode}
     */
    private void writeXGMML(Kam kam, List<KamNode> pathNodes)
            throws IOException, KamStoreException {
        // Set up a writer to write the XGMML
        PrintWriter writer = new PrintWriter(new File(outputFile));

        // Write xgmml <graph> element header
        XGMMLUtility.writeStart("Path from " + sourceNodeId + " to "
                + targetNodeId, writer);

        // Iterate over the path nodes and capture in XGMML
        for (KamNode pathNode : pathNodes) {
            Node xNode = new Node();
            xNode.id = pathNode.getId();
            xNode.label = pathNode.getLabel();
            xNode.function = pathNode.getFunctionType();

            List<BelTerm> supportingTerms = kamStore
                    .getSupportingTerms(pathNode);

            XGMMLUtility.writeNode(xNode, supportingTerms, writer);
        }
        
        Set<KamNode> pathNodeSet = new HashSet<Kam.KamNode>(pathNodes);

        // Iterate over the path nodes, find the edges, and capture in XGMML
        for (KamEdge edge : kam.getEdges()) {
            if (pathNodeSet.contains(edge.getSourceNode())
                    && pathNodeSet.contains(edge.getTargetNode())) {
                Edge xEdge = new Edge();
                xEdge.id = edge.getId();
                xEdge.rel = edge.getRelationshipType();
                xEdge.source = edge.getSourceNode().getId();
                xEdge.target = edge.getTargetNode().getId();
                
                XGMMLUtility.writeEdge(xEdge, writer);
            }
        }

        // Close out the writer
        XGMMLUtility.writeEnd(writer);
        writer.close();
    }
    
    /**
     * Main method to launch the PathFinder with the user configuration
     * provided in <tt>args</tt>.
     * 
     * @param args <tt>String[]</tt>, the main command-line arguments
     */
    public static void main(String[] args) {
        final StringBuilder bldr = new StringBuilder();
        bldr.append("\n");
        bldr.append(VERSION_LABEL).append(": PathFinder Utility\n");
        bldr.append("Copyright (c) 2011-2012, Selventa. All Rights Reserved.\n");
        bldr.append("\n");
        System.out.println(bldr.toString());

        if(args.length < 2) {
            printUsageThenExit();
        }
        
        String kamName = null;
        Integer sourceNodeId = null;
        Integer targetNodeId = null;
        String outputFile = null;
        for(int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            if (arg.equals("-k") || arg.equals("--kam-name")) {
                if ((i + 1) < args.length) {
                    kamName = args[i + 1];
                } else {
                    printUsageThenExit();
                }
            } else if (arg.equals("-s") || arg.equals("--source-node-id")) {
                if ((i + 1) < args.length) {
                    try {
                        sourceNodeId = Integer.parseInt(args[i + 1]);
                    } catch (NumberFormatException e) {
                        printUsageThenExit();
                    }
                } else {
                    printUsageThenExit();
                }
            } else if (arg.equals("-t") || arg.equals("--target-node-id")) {
                if ((i + 1) < args.length) {
                    try {
                        targetNodeId = Integer.parseInt(args[i + 1]);
                    } catch (NumberFormatException e) {
                        printUsageThenExit();
                    }
                } else {
                    printUsageThenExit();
                }
            } else if (arg.equals("-o") || arg.equals("--output-file")) {
                if((i + 1) < args.length) {
                    outputFile = args[i + 1];
                } else {
                    printUsageThenExit();
                }
            }
        }

        if (kamName == null || sourceNodeId == null || targetNodeId == null) {
            printUsageThenExit();
        }
        
        if (outputFile == null) {
            outputFile = "path_" + sourceNodeId + "_" + targetNodeId + ".xgmml";
        }
        
        //Run path finder.
        try {
            new PathFinder(kamName, sourceNodeId, targetNodeId, outputFile)
                    .runPathFind();
        } catch (Exception e) {
            System.out.println("Error exporting KAM - " + e.getMessage());
        }
    }
    
    /**
     * Print the PathFinder command-line arguments and exit the JVM.
     */
    private static void printUsageThenExit() {
        System.out.println(
                "Usage:\n" +
                "  -k KAM,     --kam-name KAM        The kam to pathfind in.\n" +
                "  -s NODE ID, --source-node-id NODE ID  The source node id to pathfind from.\n" +
                "  -t NODE ID, --target-node-id NODE ID  The target node id to pathfind to.\n" +
                "  -o FILE,    --output-file FILE     The file to save the XGMML path to.");
        System.exit(1);
    }
}

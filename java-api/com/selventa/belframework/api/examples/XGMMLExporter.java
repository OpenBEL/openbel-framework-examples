package com.selventa.belframework.api.examples;

import static com.selventa.belframework.common.BELUtilities.nulls;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

import com.selventa.belframework.api.KamStore;
import com.selventa.belframework.api.examples.XGMMLObjects.Edge;
import com.selventa.belframework.api.examples.XGMMLObjects.Node;
import com.selventa.belframework.common.InvalidArgument;
import com.selventa.belframework.kamstore.data.jdbc.KAMStoreDaoImpl.BelTerm;
import com.selventa.belframework.kamstore.model.Kam;
import com.selventa.belframework.kamstore.model.Kam.KamEdge;
import com.selventa.belframework.kamstore.model.Kam.KamNode;
import com.selventa.belframework.kamstore.model.KamStoreException;

/**
 * XGMMLExporter leverages the KAM API to export a KAM in XGMML graph format.
 *
 * @see <a href="http://en.wikipedia.org/wiki/XGMML">http://en.wikipedia.org/wiki/XGMML</a>
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class XGMMLExporter {

    /**
     * Private constructor to prevent instantiation.
     */
    private XGMMLExporter() {
    }

    /**
     * Export KAM to XGMML format using the KAM API.
     *
     * @param kam {@link Kam} the kam to export to XGMML
     * @param kamStore {@link KAMStore} the kam store to read kam details from
     * @param outputPath {@link String} the output path to write XGMML file to,
     * which can be null, in which case the kam's name will be used and it will
     * be written to the current directory (user.dir).
     *
     * @throws KamStoreException Thrown if an error occurred retrieving the KAM
     * @throws FileNotFoundException Thrown if the export file cannot be
     * written to
     * @throws InvalidArgument Thrown if either the kam, kamInfo, kamStore, or
     * outputPath arguments were null
     */
    public static void exportKam(final Kam kam, final KamStore kamStore,
            String outputPath) throws KamStoreException, FileNotFoundException {
        if (nulls(kam, kamStore, outputPath)) {
            throw new InvalidArgument("argument(s) were null");
        }

        // Set up a writer to write the XGMML
    	PrintWriter writer = new PrintWriter(outputPath);

    	// Start to process the Kam

        // Write xgmml <graph> element header
        XGMMLUtility.writeStart(kam.getKamInfo().getName(), writer);

        // We iterate over all the nodes in the Kam first
        for(KamNode kamNode : kam.getNodes()) {
            Node xNode = new Node();
            xNode.id = kamNode.getId();
            xNode.label = kamNode.getLabel();
            xNode.function = kamNode.getFunctionType();

            List<BelTerm> supportingTerms = kamStore.getSupportingTerms(kamNode);

            XGMMLUtility.writeNode(xNode, supportingTerms, writer);
        }

        // Iterate over all the edges
        for(KamEdge kamEdge : kam.getEdges()) {
            Edge xEdge = new Edge();
            xEdge.id = kamEdge.getId();
            xEdge.rel = kamEdge.getRelationshipType();
            KamNode knsrc = kamEdge.getSourceNode();
            KamNode kntgt = kamEdge.getTargetNode();
            xEdge.source = knsrc.getId();
            xEdge.target = kntgt.getId();

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
}

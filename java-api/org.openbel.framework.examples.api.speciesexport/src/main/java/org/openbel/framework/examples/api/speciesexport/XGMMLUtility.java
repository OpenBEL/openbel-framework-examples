package org.openbel.framework.examples.api.speciesexport;

import static java.lang.String.format;
import static org.openbel.framework.common.BELUtilities.hasLength;

import java.io.PrintWriter;
import java.util.List;
import java.util.Random;

import org.openbel.framework.common.enums.FunctionEnum;
import org.openbel.framework.common.enums.RelationshipType;
import org.openbel.framework.examples.api.speciesexport.XGMMLObjects.Edge;
import org.openbel.framework.examples.api.speciesexport.XGMMLObjects.Node;
import org.openbel.framework.internal.KAMStoreDaoImpl.BelTerm;

/**
 * XGMMLUtility provides utility methods for writing graph, node, and edge
 * sections of an XGMML xml document.
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class XGMMLUtility {
    /**
     * Format: {@code %s %s %d %d}. In order, graphics type, fill color, x-pos,
     * and y-pos.
     */
    private static String NODE_GRAPHICS;
    /**
     * Format: {@code %d %s %d %s}. In order, line width, fill color, target
     * arrow indicator, and edge label.
     */
    private static String EDGE_GRAPHICS;
    /**
     * Format: {@code %s %s %s}.
     */
    private static String EDGE_LABEL;
    private static String DFLT_NODE_SHAPE;
    private static String DFLT_EDGE_COLOR;
    private static String DFLT_NODE_COLOR;
    static {
        NODE_GRAPHICS = "    <graphics type='%s' fill='%s' ";
        NODE_GRAPHICS += "x='%d' y='%d' h='20.0' w='80.0' ";
        NODE_GRAPHICS += "cy:nodeLabel='%s'/>\n";
        EDGE_GRAPHICS = "    <graphics width='%d' fill='%s' ";
        EDGE_GRAPHICS += "cy:targetArrow='%d' cy:edgeLabel='%s'/>\n";
        EDGE_LABEL = "%s (%s) %s";
        DFLT_NODE_SHAPE = "rectangle";
        DFLT_EDGE_COLOR = "0,0,0";
        DFLT_NODE_COLOR = "150,150,150";
    }

    /**
     * Returns a shape for the supplied {@link FunctionEnum}.
     *
     * @param fe {@link FunctionEnum}
     * @return Non-null {@link String}
     * @see #DFLT_NODE_SHAPE
     */
    public static String type(FunctionEnum fe) {
        if (fe == null) {
            return DFLT_NODE_SHAPE;
        }
        switch (fe) {
        case ABUNDANCE:
            return "ver_ellipsis";
        case BIOLOGICAL_PROCESS:
            return "rhombus";
        case CATALYTIC_ACTIVITY:
            return "hexagon";
        case CELL_SECRETION:
            return "arc";
        case CELL_SURFACE_EXPRESSION:
            return "arc";
        case CHAPERONE_ACTIVITY:
            return "hexagon";
        case COMPLEX_ABUNDANCE:
            return "hor_ellipsis";
        case COMPOSITE_ABUNDANCE:
            return "hor_ellipsis";
        case DEGRADATION:
            return "hor_ellipsis";
        case GENE_ABUNDANCE:
            return "hor_ellipsis";
        case GTP_BOUND_ACTIVITY:
            return "hexagon";
        case KINASE_ACTIVITY:
            return "hexagon";
        case MICRORNA_ABUNDANCE:
            return "hor_ellipsis";
        case MOLECULAR_ACTIVITY:
            return "hexagon";
        case PATHOLOGY:
            return "rhombus";
        case PEPTIDASE_ACTIVITY:
            return "hexagon";
        case PHOSPHATASE_ACTIVITY:
            return "hexagon";
        case PRODUCTS:
        case PROTEIN_ABUNDANCE:
            return "hor_ellipsis";
        case REACTANTS:
        case RIBOSYLATION_ACTIVITY:
            return "hexagon";
        case RNA_ABUNDANCE:
            return "hor_ellipsis";
        case TRANSCRIPTIONAL_ACTIVITY:
            return "hexagon";
        case TRANSPORT_ACTIVITY:
            return "hexagon";
        }
        return DFLT_NODE_SHAPE;
    }

    /**
     * Returns an RGB tuple of the form {@code "x,x,x"} for the supplied
     * {@link FunctionEnum}. Defaults to gray; RGB {@code "150,150,150"}.
     *
     * @param fe {@link FunctionEnum}
     * @return Non-null {@link String}
     */
    public static String color(FunctionEnum fe) {
        if (fe == null) {
            return DFLT_NODE_COLOR;
        }
        switch (fe) {
        case ABUNDANCE:
            return "40,255,85";
        case BIOLOGICAL_PROCESS:
            return "255,51,102";
        case CATALYTIC_ACTIVITY:
            return "100,100,255";
        case CELL_SECRETION:
            return "204,204,255";
        case CELL_SURFACE_EXPRESSION:
            return "204,204,255";
        case CHAPERONE_ACTIVITY:
            return "100,100,255";
        case COMPLEX_ABUNDANCE:
            return "102,153,255";
        case COMPOSITE_ABUNDANCE:
            return "222,255,255";
        case DEGRADATION:
            return "255,51,102";
        case GENE_ABUNDANCE:
            return "204,255,204";
        case GTP_BOUND_ACTIVITY:
            return "100,100,255";
        case KINASE_ACTIVITY:
            return "100,100,255";
        case MICRORNA_ABUNDANCE:
            return "0,255,150";
        case MOLECULAR_ACTIVITY:
            return "100,100,255";
        case PATHOLOGY:
            return "255,51,102";
        case PEPTIDASE_ACTIVITY:
            return "100,100,255";
        case PHOSPHATASE_ACTIVITY:
            return "100,100,255";
        case PROTEIN_ABUNDANCE:
            return "85,255,255";
        case REACTION:
            return "255,51,102";
        case RIBOSYLATION_ACTIVITY:
            return "100,100,255";
        case RNA_ABUNDANCE:
            return "40,255,85";
        case TRANSCRIPTIONAL_ACTIVITY:
            return "100,100,255";
        case TRANSPORT_ACTIVITY:
            return "100,100,255";
        }
        return DFLT_NODE_COLOR;
    }

    /**
     * Returns an RGB tuple of the form {@code "x,x,x"} for the supplied
     * {@link RelationshipType}. Defaults to black; RGB {@code "0,0,0"}.
     *
     * @param fe {@link RelationshipType}
     * @return Non-null {@link String}
     */
    public static String color(RelationshipType rel) {
        if (rel == null) {
            return DFLT_EDGE_COLOR;
        }
        switch (rel) {
        case ACTS_IN:
            return "153,153,153";
        case HAS_COMPONENT:
            return "153,153,153";
        case HAS_MEMBER:
            return "153,153,153";
        case HAS_MODIFICATION:
            return "153,153,153";
        case HAS_PRODUCT:
            return "153,153,153";
        case HAS_VARIANT:
            return "153,153,153";
        case INCLUDES:
            return "153,153,153";
        case IS_A:
            return "153,153,153";
        case REACTANT_IN:
            return "153,153,153";
        case SUB_PROCESS_OF:
            return "153,153,153";
        case TRANSCRIBED_TO:
            return "153,153,153";
        case TRANSLATED_TO:
            return "153,153,153";
        case TRANSLOCATES:
            return "153,153,153";
        }
        return DFLT_EDGE_COLOR;
    }

    /**
     * Write the XGMML start using the {@code graphName} as the label.
     *
     * @param name {@link String}, the name of the XGMML graph
     * @param writer {@link PrintWriter}, the writer
     */
    public static void writeStart(String name, PrintWriter writer) {
        StringBuilder sb = new StringBuilder();
        sb.append("<graph xmlns='http://www.cs.rpi.edu/XGMML' ")
                .append("xmlns:ns2='http://www.w3.org/1999/xlink' ")
                .append("xmlns:cy='http://www.cytoscape.org' ")
                .append("Graphic='1' label='").append(name)
                .append("' directed='1'>\n");
        writer.write(sb.toString());
    }

    /**
     * Write an XGMML {@code <node>} from {@code node} properties.
     *
     * @param node {@link Node}, the node to write
     * @param writer {@link PrintWriter}, the writer
     */
    public static void writeNode(Node node, List<BelTerm> supportingTerms,
            PrintWriter writer) {
        int x = new Random().nextInt(200);
        int y = new Random().nextInt(200);

        String nodelabel = cleanBEL(node.label);
        
        StringBuilder sb = new StringBuilder();
        sb.append("  <node label='");
        sb.append(nodelabel);
        sb.append("' id='");
        sb.append(node.id.toString());
        sb.append("'>\n");
        sb.append("    <att name='function type'");
        sb.append(" value='");
        sb.append(node.function.getDisplayValue());
        sb.append("' />\n");
        sb.append("    <att name='parameters'");
        sb.append(" value='");

        String params = "";
        for (BelTerm t : supportingTerms) {
            if (hasLength(params)) {
                params = params.concat("&#10;");
            }
            String label = cleanBEL(t.getLabel());
            params = params.concat(label);
        }
        sb.append(params);
        sb.append("' />\n");

        // Graphics type and fill color
        String graphics = format(NODE_GRAPHICS, type(node.function),
                color(node.function), x, y, params);
        sb.append(graphics);

        sb.append("  </node>\n");
        writer.write(sb.toString());
    }

    /**
     * Write an XGMML &lt;edge&gt; from {@code edge} properties.
     *
     * @param edge {@link Edge}, the edge to write
     * @param writer {@link PrintWriter}, the writer
     */
    public static void writeEdge(Node src, Node tgt, Edge edge, PrintWriter writer) {
        StringBuilder sb = new StringBuilder();
        RelationshipType rel = edge.rel;
        String reldispval = rel.getDisplayValue();
        
        String srclabel = cleanBEL(src.label);
        String tgtlabel = cleanBEL(tgt.label);
        
        sb.append("  <edge label='");
        String dispval = format(EDGE_LABEL, srclabel, rel, tgtlabel);
        sb.append(dispval);
        sb.append("' source='");
        sb.append(edge.source.toString());
        sb.append("' target='");
        sb.append(edge.target.toString());
        sb.append("'>\n");
        sb.append("    <att name='relationship type'");
        sb.append(" value='");
        sb.append(reldispval);
        sb.append("' />\n");

        // Edge graphics
        String color = color(rel);
        String graphics = format(EDGE_GRAPHICS, 1, color, 1, reldispval);
        sb.append(graphics);

        sb.append("  </edge>\n");
        writer.write(sb.toString());
    }

    /**
     * Write the XGMML end.
     *
     * @param writer {@link PrintWriter}, the writer
     */
    public static void writeEnd(PrintWriter writer) {
        writer.write("</graph>");
    }
    
    private static String cleanBEL(String bel) {
        return bel.replaceAll("&", "&amp;").replaceAll("'", "&quot;");
    }
}

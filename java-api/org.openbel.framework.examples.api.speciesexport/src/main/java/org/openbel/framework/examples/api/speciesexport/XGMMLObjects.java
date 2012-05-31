package org.openbel.framework.examples.api.speciesexport;

import org.openbel.framework.common.enums.FunctionEnum;
import org.openbel.framework.common.enums.RelationshipType;

/**
 * Defines a node data structure for XGMML output.
 * 
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
class XGMMLObjects {
    
    /**
     * Defines a node data structure for XGMML output.
     * 
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    protected static class Node {
        public Integer id;
        public FunctionEnum function;
        public String label;
    }

    /**
     * Defines an edge data structure for XGMML output.
     * 
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    protected static class Edge {
        public Integer id;
        public Integer source;
        public Integer target;
        public RelationshipType rel;
    }
    
    /**
     * Defines a statement data structure for XGMML output.
     * 
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    protected static class Statement {
        public Integer id;
        public Integer documentId;
        public String belSyntax;
    }
    
    /**
     * Defines a document data structure to include as a graph attribute to
     * the XGMML output.
     * 
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    protected static class Document {
        public Integer id;
        public String documentName;
    }
    
    /**
     * Defines a term data structure to include as a node attribute to the
     * XGMML output.
     * 
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    protected static class Term {
        public Integer id;
        public String termLabel;
    }
}

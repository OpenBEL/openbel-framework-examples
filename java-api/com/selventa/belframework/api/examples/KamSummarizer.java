package com.selventa.belframework.api.examples;

import static com.selventa.belframework.common.enums.BELFrameworkVersion.VERSION_LABEL;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.selventa.belframework.api.KamStore;
import com.selventa.belframework.api.KamStoreImpl;
import com.selventa.belframework.common.InvalidArgument;
import com.selventa.belframework.common.cfg.SystemConfiguration;
import com.selventa.belframework.common.enums.FunctionEnum;
import com.selventa.belframework.common.enums.RelationshipType;
import com.selventa.belframework.df.DBConnection;
import com.selventa.belframework.df.DatabaseService;
import com.selventa.belframework.df.DatabaseServiceImpl;
import com.selventa.belframework.kamcatalog.model.KamDbObject;
import com.selventa.belframework.kamstore.data.jdbc.KAMCatalogDao.KamInfo;
import com.selventa.belframework.kamstore.data.jdbc.KAMStoreDaoImpl.Annotation;
import com.selventa.belframework.kamstore.data.jdbc.KAMStoreDaoImpl.BelStatement;
import com.selventa.belframework.kamstore.model.Kam;
import com.selventa.belframework.kamstore.model.Kam.KamEdge;
import com.selventa.belframework.kamstore.model.Kam.KamNode;
import com.selventa.belframework.kamstore.model.KamStoreException;

/**
 * KamSummarizer leverages the KAM API to summarize a KAM in the KAMStore.
 */
public class KamSummarizer {

	private static final String HUMAN_TAX_ID = "9606";
	private static final String MOUSE_TAX_ID = "10090";
	private static final String RAT_TAX_ID = "10116";

	/**
	 * Holds a reference to the system configuration
	 */
	private SystemConfiguration systemConfiguration;
	private KamStore kamStore;
	private DBConnection dbConnection;

	/**
	 * Constructs the KamSummarizer
	 */
	public KamSummarizer() {

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

	protected void tearDownKamStore() throws SQLException {
		// Tearsdown the KamStore. This removes any cached data and queries
		kamStore.teardown();

		// Close the DBConnection
		dbConnection.getConnection().close();
	}

	public void run(boolean listCatalog, String kamName) throws IOException, SQLException, KamStoreException {

		// connect to the KAM store
		setUpKamStore();

		// list the available kams in the kam store
		if (listCatalog) {
			List<KamInfo> kamInfos = kamStore.readCatalog();
			printKamCatalogSummary(kamInfos);
		}

		try {
			Kam kam;
			if (kamName != null) {
				// Look up the requested KAM and summarize.
				kam = kamStore.getKam(kamName);
				KamSummary summary = summarizeKam(kam);
				printKamSummary(summary);
			}
		} catch (InvalidArgument e) {
			System.out.println(e.getMessage());
		}
		tearDownKamStore();
	}

	/**
	 * returns the number of rnaAbundance nodes.
	 * 
	 * @param nodes
	 * @return
	 */
	protected int getNumRnaNodes(Collection<KamNode> nodes) {
		int count = 0;
		for (KamNode node : nodes) {
			if (node.getFunctionType() == FunctionEnum.RNA_ABUNDANCE) {
				count++;
			}
		}
		return count;
	}

	/**
	 * return number of protein with phosphorylation modification
	 * 
	 * @param nodes
	 * @return
	 */
	protected int getPhosphoProteinNodes(Collection<KamNode> nodes) {
		int count = 0;
		for (KamNode node : nodes) {
			if (node.getFunctionType() == FunctionEnum.PROTEIN_ABUNDANCE && node.getLabel().indexOf("modification(P") > -1) {
				count++;
			}
		}
		return count;
	}

	/**
	 * returns number unique gene reference
	 * 
	 * @param edges
	 * @return
	 */
	protected int getUniqueGeneReference(Collection<KamNode> nodes) {
		// count all protienAbundance reference
		Set<String> uniqueLabels = new HashSet<String>();
		for (KamNode node : nodes) {
			if (node.getFunctionType() == FunctionEnum.PROTEIN_ABUNDANCE && StringUtils.countMatches(node.getLabel(), "(") == 1
					&& StringUtils.countMatches(node.getLabel(), ")") == 1) {
				uniqueLabels.add(node.getLabel());
			}
		}

		return uniqueLabels.size();
	}

	/**
	 * returns number of inceases and directly_increases edges.
	 * 
	 * @param edges
	 * @return
	 */
	protected int getIncreasesEdges(Collection<KamEdge> edges) {
		int count = 0;
		for (KamEdge edge : edges) {
			if (edge.getRelationshipType() == RelationshipType.INCREASES || edge.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES) {
				count++;
			}
		}
		return count;
	}

	/**
	 * returns number of deceases and directly_decreases edges.
	 * 
	 * @param edges
	 * @return
	 */
	protected int getDecreasesEdges(Collection<KamEdge> edges) {
		int count = 0;
		for (KamEdge edge : edges) {
			if (edge.getRelationshipType() == RelationshipType.DECREASES || edge.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES) {
				count++;
			}
		}
		return count;
	}

	protected int getUpstreamCount(String label, Collection<KamEdge> edges) {
		int count = 0;
		for (KamEdge edge : edges) {
			if (edge.getSourceNode().getLabel().equals(label) && isCausal(edge)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * returns nodes with causal downstream to rnaAbundance() nodes.
	 * 
	 * @param edges
	 * @return
	 */
	protected Map<String, Integer> getTranscriptionalControls(Collection<KamEdge> edges) {
		Map<String, Integer> controlCountMap = new HashMap<String, Integer>();
		for (KamEdge edge : edges) {
			if (edge.getTargetNode().getFunctionType() == FunctionEnum.RNA_ABUNDANCE && isCausal(edge)) {
				if (controlCountMap.containsKey(edge.getSourceNode().getLabel())) {
					int count = controlCountMap.get(edge.getSourceNode().getLabel());
					count = count + 1;
					controlCountMap.put(edge.getSourceNode().getLabel(), count);
				} else {
					controlCountMap.put(edge.getSourceNode().getLabel(), 1);
				}
			}
		}

		return controlCountMap;
	}

	/**
	 * returns nodes with 4+ causal downstream to rnaAbundance() nodes.
	 * 
	 * @param edges
	 * @return
	 */
	protected Map<String, Integer> getHypotheses(Collection<KamEdge> edges) {
		Map<String, Integer> controlCountMap = getTranscriptionalControls(edges);
		Map<String, Integer> hypCountMap = new HashMap<String, Integer>();
		for (String hyp : controlCountMap.keySet()) {
			if (controlCountMap.get(hyp) >= 4) {
				hypCountMap.put(hyp, controlCountMap.get(hyp));
			}
		}
		return hypCountMap;
	}

	/**
	 * returns true if the edge has one of the 4 causal relationship types.
	 * 
	 * @param edge
	 * @return
	 */
	protected boolean isCausal(KamEdge edge) {
		return edge.getRelationshipType() == RelationshipType.INCREASES || edge.getRelationshipType() == RelationshipType.DIRECTLY_INCREASES
				|| edge.getRelationshipType() == RelationshipType.DECREASES || edge.getRelationshipType() == RelationshipType.DIRECTLY_DECREASES;
	}

	protected KamSummary summarizeKam(Kam kam) throws InvalidArgument, KamStoreException {
		KamSummary summary;
		summary = new KamSummary();
		summary.setKamInfo(kam.getKamInfo());
		summary.setNumOfNodes(kam.getNodes().size());
		summary.setNumOfEdges(kam.getEdges().size());
		summary.setNumOfBELDocuments(kamStore.getBelDocumentInfos(kam.getKamInfo()).size());
		summary.setNumOfNamespaces(kamStore.getNamespaces(kam.getKamInfo()).size());
		summary.setNumOfAnnotationTypes(kamStore.getAnnotationTypes(kam.getKamInfo()).size());
		summary.setNumOfRnaAbundanceNodes(getNumRnaNodes(kam.getNodes()));
		summary.setNumOfPhosphoProteinNodes(getPhosphoProteinNodes(kam.getNodes()));
		summary.setNumOfUniqueGeneReferences(getUniqueGeneReference(kam.getNodes()));
		summary.setNumOfIncreaseEdges(getIncreasesEdges(kam.getEdges()));
		summary.setNumOfDecreaseEdges(getDecreasesEdges(kam.getEdges()));
		summary.setNumOfTranscriptionalControls(getTranscriptionalControls(kam.getEdges()).size());
		summary.setNumOfHypotheses(getHypotheses(kam.getEdges()).size());

		for (KamEdge edge : kam.getEdges()) {
			List<BelStatement> statements = kamStore.getSupportingEvidence(edge);
			for (BelStatement statement : statements) {
				List<Annotation> annotations = statement.getAnnotationList();
				for (Annotation annotation : annotations) {
					String species = null;
					if (HUMAN_TAX_ID.equals(annotation.getValue())) {
						species = "Human";
					} else if (MOUSE_TAX_ID.equals(annotation.getValue())) {
						species = "Mouse";
					} else if (RAT_TAX_ID.equals(annotation.getValue())) {
						species = "Rat";
					}
					if (species != null) {
						addSpeciesCount(summary, species);
					}
				}
			}
		}

		// breakdown human, mouse, rat and summary sub-network
		summary.setFilteredKamSummaries(summarizeSpeciesSpecificEdges(kam));

		return summary;
	}

	private void addSpeciesCount(KamSummary summary, String species) {
		if (summary.getStatementBreakdownBySpeciesMap() == null) {
			summary.setStatementBreakdownBySpeciesMap(new HashMap<String, Integer>());
		}
		Integer count = summary.getStatementBreakdownBySpeciesMap().get(species);
		if (count == null) {
			count = 1;
		} else {
			count = count + 1;
		}
		summary.getStatementBreakdownBySpeciesMap().put(species, count);
	}

	protected Collection<KamEdge> filterEdges(Kam kam, String speciesTaxId) throws KamStoreException {
		Collection<KamEdge> filteredEdges = new ArrayList<KamEdge>();
		for (KamEdge edge : kam.getEdges()) {
			List<BelStatement> statements = kamStore.getSupportingEvidence(edge);
			for (BelStatement statement : statements) {
				List<Annotation> annotations = statement.getAnnotationList();
				boolean isSpeciesAnnotated = false;
				for (Annotation annotation : annotations) {
					if (HUMAN_TAX_ID.equals(annotation.getValue()) || MOUSE_TAX_ID.equals(annotation.getValue()) || RAT_TAX_ID.equals(annotation.getValue())) {
						isSpeciesAnnotated = true;
					}
					if (speciesTaxId.equals(annotation.getValue())) {
						filteredEdges.add(edge);
						break;
					}
				}
				if (!isSpeciesAnnotated) {
					// add all non species-specific edges
					// filteredEdges.add(edge);
				}
			}
		}
		return filteredEdges;
	}

	/**
	 * Summarize human, mouse, and rat individually
	 * 
	 * @param kam
	 * @param kamStore
	 * @throws KamStoreException
	 */
	protected Map<String, KamSummary> summarizeSpeciesSpecificEdges(Kam kam) throws KamStoreException {
		Map<String, KamSummary> summaries = new LinkedHashMap<String, KamSummary>();

		Collection<KamEdge> humanEdges = filterEdges(kam, HUMAN_TAX_ID);
		KamSummary humanSummary = summarizeKamNetwork(humanEdges);
		summaries.put("Human specific edges", humanSummary);

		Collection<KamEdge> mouseEdges = filterEdges(kam, MOUSE_TAX_ID);
		KamSummary mouseSummary = summarizeKamNetwork(mouseEdges);
		summaries.put("Mouse specific edges", mouseSummary);

		Collection<KamEdge> ratEdges = filterEdges(kam, RAT_TAX_ID);
		KamSummary ratSummary = summarizeKamNetwork(ratEdges);
		summaries.put("Rat specific edges", ratSummary);

		return summaries;
	}

	/**
	 * Summarize nodes and edges
	 * 
	 * @param edges
	 * @return
	 */
	protected KamSummary summarizeKamNetwork(Collection<KamEdge> edges) {
		KamSummary summary = new KamSummary();

		Set<KamNode> nodes = new HashSet<KamNode>(); // unique set of nodes
		for (KamEdge edge : edges) {
			nodes.add(edge.getSourceNode());
			nodes.add(edge.getTargetNode());
		}
		summary.setNumOfNodes(nodes.size());
		summary.setNumOfEdges(edges.size());
		summary.setNumOfRnaAbundanceNodes(getNumRnaNodes(nodes));
		summary.setNumOfPhosphoProteinNodes(getPhosphoProteinNodes(nodes));
		summary.setNumOfUniqueGeneReferences(getUniqueGeneReference(nodes));
		summary.setNumOfIncreaseEdges(getIncreasesEdges(edges));
		summary.setNumOfDecreaseEdges(getDecreasesEdges(edges));
		summary.setNumOfTranscriptionalControls(getTranscriptionalControls(edges).size());
		Map<String, Integer> hypCountMap = getHypotheses(edges);
		summary.setNumOfHypotheses(hypCountMap.size());
		// calculate average number of upstream nodes per hypothesis
		int sumUpStreamNodes = 0;
		for (String hyp : hypCountMap.keySet()) {
			sumUpStreamNodes += getUpstreamCount(hyp, edges);
		}
		summary.setAverageHypothesisUpstreamNodes(((double) sumUpStreamNodes) / hypCountMap.size());

		return summary;

	}

	protected void printKamCatalogSummary(List<KamInfo> kamInfos) {
		// Get a list of all the Kams available in the KamStore
		System.out.println("Available KAMS:");
		System.out.println("\tName\tLast Compiled\tSchema Name");
		System.out.println("\t------\t-------------\t-----------");
		for (KamInfo kamInfo : kamInfos) {
			KamDbObject kamDb = kamInfo.getKamDbObject();
			System.out.println(String.format("\t%s\t%s\t%s", kamDb.getName(), kamDb.getLastCompiled(), kamDb.getSchemaName()));
		}
		System.out.print("\n");
	}

	protected void printKamSummary(KamSummary summary) throws InvalidArgument, KamStoreException {

		System.out.println(String.format("\n\nSummarizing KAM: %s", summary.getKamInfo().getName()));
		System.out.println(String.format("\tLast Compiled:\t%s", summary.getKamInfo().getLastCompiled()));
		System.out.println(String.format("\tDescription:\t%s", summary.getKamInfo().getDescription()));
		System.out.println();
		System.out.println(String.format("\tNum BEL Documents:\t%d", summary.getNumOfBELDocuments()));
		System.out.println(String.format("\tNum Namespaces:\t\t%d", summary.getNumOfNamespaces()));
		System.out.println(String.format("\tNum Annotation Types:\t\t%d", summary.getNumOfAnnotationTypes()));
		System.out.println();
		for (String species : summary.getStatementBreakdownBySpeciesMap().keySet()) {
			System.out.println(String.format("\tNum Statements (%s):\t\t%d", species, summary.getStatementBreakdownBySpeciesMap().get(species)));
		}
		System.out.println();
		printNetworkSummary(summary);

		// print filtered kam summaries if they are available
		if (summary.getFilteredKamSummaries() != null && !summary.getFilteredKamSummaries().isEmpty()) {
			for (String filteredKam : summary.getFilteredKamSummaries().keySet()) {
				System.out.println(filteredKam + ":");
				printNetworkSummary(summary.getFilteredKamSummaries().get(filteredKam));
			}
		}
	}

	protected void printNetworkSummary(KamSummary summary) throws InvalidArgument, KamStoreException {
		System.out.println(String.format("\tNum Nodes:\t%d", summary.getNumOfNodes()));
		System.out.println(String.format("\tNum Edges:\t%d", summary.getNumOfEdges()));
		System.out.println();
		System.out.println(String.format("\tNum Unique Gene References:\t%d", summary.getNumOfUniqueGeneReferences()));
		System.out.println(String.format("\tNum RNA Abundances:\t\t%d", summary.getNumOfRnaAbundanceNodes()));
		System.out.println(String.format("\tNum Phospho-Proteins:\t\t%d", summary.getNumOfPhosphoProteinNodes()));
		System.out.println();
		System.out.println(String.format("\tNum Transcriptional Controls:\t%d", summary.getNumOfTranscriptionalControls()));
		System.out.println(String.format("\tNum Hypotheses:\t%d", summary.getNumOfHypotheses()));
		if (summary.getAverageHypothesisUpstreamNodes() != null && summary.getAverageHypothesisUpstreamNodes() > 0) {
			System.out.println(String.format("\tAverage Upstream Nodes/Hypothesis:\t%s", (new DecimalFormat("#.0")).format(summary
					.getAverageHypothesisUpstreamNodes())));
		}
		System.out.println(String.format("\tNum Increase Edges:\t\t%d", summary.getNumOfIncreaseEdges()));
		System.out.println(String.format("\tNum Decrease Edges:\t\t%d", summary.getNumOfDecreaseEdges()));
		System.out.println();
	}

	@SuppressWarnings("unused")
	private String printEdge(KamEdge kamEdge) {
		return String.format("%s %s %s", kamEdge.getSourceNode().getLabel(), kamEdge.getRelationshipType().getDisplayValue(), kamEdge.getTargetNode()
				.getLabel());
	}

	@SuppressWarnings("unused")
	private void printBelStatement(BelStatement belStatement) {
		System.out.println(String.format("\tBEL Statement: %d\t%s", belStatement.getId(), belStatement));
		System.out.println(String.format("\t\tNum Annotations: %d", belStatement.getAnnotationList().size()));
		for (Annotation annotation : belStatement.getAnnotationList()) {
			System.out.println(String.format("\t\t%s -> %s", annotation.getAnnotationType().getName(), annotation.getValue()));
		}
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		final StringBuilder bldr = new StringBuilder();
		bldr.append("\n");
		bldr.append(VERSION_LABEL).append(": KAM Summarizer\n");
		bldr.append("Copyright (c) 2011-2012, Selventa. All Rights Reserved.\n");
		bldr.append("\n");
		System.out.println(bldr.toString());

		String kamName = null;
		boolean listCatalog = false;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			if (arg.equals("-l") || arg.equals("--list-catalog")) {
				listCatalog = true;
			} else if (arg.equals("-k") || arg.equals("--kam-name")) {
				if ((i + 1) < args.length) {
					kamName = args[i + 1];
				} else {
					printUsageThenExit();
				}
			}
		}

		if (kamName == null && !listCatalog) {
			printUsageThenExit();
		}

		try {
			new KamSummarizer().run(listCatalog, kamName);
		} catch (Exception e) {
			System.out.println("Error summarizing KAM - " + e.getMessage());
		}

	}

	private static void printUsageThenExit() {
		System.out.println("Usage:\n" + "  -l       --list-catalog       Lists the KAMs in the KAM Store\n"
				+ "  -k KAM,  --kam-name KAM       The kam to summarize\n");
		System.exit(1);
	}
}

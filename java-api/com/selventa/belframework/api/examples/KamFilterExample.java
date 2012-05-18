package com.selventa.belframework.api.examples;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.selventa.belframework.api.KamStore;
import com.selventa.belframework.api.KamStoreImpl;
import com.selventa.belframework.common.cfg.SystemConfiguration;
import com.selventa.belframework.common.enums.CitationType;
import com.selventa.belframework.common.enums.RelationshipType;
import com.selventa.belframework.df.DBConnection;
import com.selventa.belframework.df.DatabaseService;
import com.selventa.belframework.df.DatabaseServiceImpl;
import com.selventa.belframework.kamstore.data.jdbc.KAMCatalogDao.KamFilter;
import com.selventa.belframework.kamstore.data.jdbc.KAMCatalogDao.KamInfo;
import com.selventa.belframework.kamstore.data.jdbc.KAMStoreDaoImpl.AnnotationType;
import com.selventa.belframework.kamstore.data.jdbc.KAMStoreDaoImpl.BelDocumentInfo;
import com.selventa.belframework.kamstore.data.jdbc.KAMStoreDaoImpl.Citation;
import com.selventa.belframework.kamstore.model.Kam;
import com.selventa.belframework.kamstore.model.KamStoreException;
import com.selventa.belframework.kamstore.model.filter.AnnotationFilterCriteria;
import com.selventa.belframework.kamstore.model.filter.CitationFilterCriteria;
import com.selventa.belframework.kamstore.model.filter.RelationshipTypeFilterCriteria;

/**
 * This examples shows how to use the KAM API to filter KAMs.
 * 
 */
public class KamFilterExample {

	private static final String HUMAN_TAX_ID = "9606";
	private static final String MOUSE_TAX_ID = "10090";
	private static final String RAT_TAX_ID = "10116";
	private static final String SPECIES_ANNOTATION_TYPE_NAME = "Species";

	/**
	 * Holds a reference to the system configuration
	 */
	private SystemConfiguration systemConfiguration;
	private KamStore kamStore;
	private DBConnection dbConnection;

	/**
	 * Constructs the KamFilterExample
	 */
	public KamFilterExample() {

	}

	public static void main(String[] args) throws Exception {
		KamFilterExample app = new KamFilterExample();
		app.run();
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
	 * Prints a quick summary for a KAM object
	 * 
	 * @param kam
	 */
	protected void printKamInformation(Kam kam) {
		if (kam != null) {
			System.out.println("KAM Name: " + kam.getKamInfo().getName());
			System.out.println("Total number of nodes: " + kam.getNodes().size());
			System.out.println("Total number of edges: " + kam.getEdges().size());
		} else {
			System.out.println("KAM is null.");
		}
		System.out.println();
	}
	
	/**
	 * Prints all citations used in a KAM
	 * @param kam
	 * @throws KamStoreException
	 */
	protected void printCitationInformation(Kam kam) throws KamStoreException {
		List<Citation> citations = kamStore.getCitations(kam.getKamInfo(), CitationType.PUBMED);
		System.out.println("KAM " + kam.getKamInfo().getName() + " has the following " + citations.size() + " citations:");
		for(Citation citation : citations) {
			System.out.println(citation.getId() + ": " + citation.getName());
		}
		System.out.println();
	}

	/**
	 * Returns the first annotation type matching the specified name 
	 * @param kam
	 * @param name
	 * @return AnnotationType, maybe null
	 */
	protected AnnotationType getAnnotationType(Kam kam, String name) throws KamStoreException {

		AnnotationType annoType = null;
		List<BelDocumentInfo> belDocs = kamStore.getBelDocumentInfos(kam.getKamInfo());
		//loop through all BEL documents used for this KAM
		for (BelDocumentInfo doc : belDocs) {
			//check annotation type on each document
			List<AnnotationType> annoTypes = doc.getAnnotationTypes();
			for (AnnotationType a : annoTypes) {
				if (a.getName().equals(name)) {
					annoType = a;
					break;
				}
			}
			if (annoType != null) {
				break;
			}
		}
		return annoType;
	}

	public void run() throws Exception {

		// set up the KAM store by supplying database information
		setUpKamStore();

		List<KamInfo> kamInfos = kamStore.readCatalog();
		System.out.println("There are " + kamInfos.size() + " KAMs in the KAM store.\n");

		// get a kam with the name "Example from the kam store, print summary
		System.out.println("Get a KAM from KAM Store and print summary:");
		Kam kam = kamStore.getKam("Example");
		printKamInformation(kam);
		printCitationInformation(kam);

		// filter the kam to only include causal increase and decrease
		// relationships
		System.out.println("The same KAM with only causal increase and decrease relationships:");
		KamFilter relFilter = kam.getKamInfo().createKamFilter();
		RelationshipTypeFilterCriteria causalRelationshipCriteria = new RelationshipTypeFilterCriteria();
		// include only these 4 relationships
		causalRelationshipCriteria.add(RelationshipType.INCREASES);
		causalRelationshipCriteria.add(RelationshipType.DECREASES);
		causalRelationshipCriteria.add(RelationshipType.DIRECTLY_INCREASES);
		causalRelationshipCriteria.add(RelationshipType.DIRECTLY_DECREASES);
		causalRelationshipCriteria.setInclude(true);
		relFilter.add(causalRelationshipCriteria);
		Kam causalKam = kamStore.getKam(kam.getKamInfo(), relFilter);
		printKamInformation(causalKam);

		//filter the kam so it only includes human or mouse specific knowledge
		System.out.println("The same KAM with only human- or mouse-specific knowledge:");
		KamFilter speciesIncludeFilter = kam.getKamInfo().createKamFilter();
		AnnotationFilterCriteria humanMouseIncludeCriteria = new AnnotationFilterCriteria(getAnnotationType(kam, SPECIES_ANNOTATION_TYPE_NAME));
		humanMouseIncludeCriteria.add(HUMAN_TAX_ID);
		humanMouseIncludeCriteria.add(MOUSE_TAX_ID);
		humanMouseIncludeCriteria.setInclude(true);
		speciesIncludeFilter.add(humanMouseIncludeCriteria);
		Kam speciesIncludeKam = kamStore.getKam(kam.getKamInfo(), speciesIncludeFilter);
		printKamInformation(speciesIncludeKam);
		
		//filter the kam so it includes everything but Rat specific knowledge
		System.out.println("The same KAM with rat-specific knowledge excluded:");
		KamFilter speciesExcludeFilter = kam.getKamInfo().createKamFilter();
		AnnotationFilterCriteria ratExcludeCriteria = new AnnotationFilterCriteria(getAnnotationType(kam, SPECIES_ANNOTATION_TYPE_NAME));
		ratExcludeCriteria.add(RAT_TAX_ID);
		ratExcludeCriteria.setInclude(false);
		speciesExcludeFilter.add(ratExcludeCriteria);
		Kam speciesExcludeKam = kamStore.getKam(kam.getKamInfo(), speciesExcludeFilter);
		printKamInformation(speciesExcludeKam);

		//filter the kam so it only includes human or mouse specific knowledge, and only include causal relationships
		System.out.println("The same KAM with only human- or mouse-specific knowledge, and with only causal relationships:");
		KamFilter casualHumanMouseFilter = kam.getKamInfo().createKamFilter();
		casualHumanMouseFilter.add(causalRelationshipCriteria);
		casualHumanMouseFilter.add(humanMouseIncludeCriteria);
		Kam causalHumanMouseKam = kamStore.getKam(kam.getKamInfo(), casualHumanMouseFilter);
		printKamInformation(causalHumanMouseKam);
		
		//filter the kam so knowledge from PubMed 12959952 and PubMed 14657031 are excluded
		System.out.println("The same KAM with  knowledge from PubMed 12959952 and PubMed 14657031 are excluded:");
		KamFilter citationFilter = kam.getKamInfo().createKamFilter();
		CitationFilterCriteria citationCriteria = new CitationFilterCriteria();
		List<Citation> citationsToExclude = kamStore.getCitations(kam.getKamInfo(), CitationType.PUBMED, "12959952", "14657031");
		for(Citation c : citationsToExclude) {
			citationCriteria.add(c);
		}
		citationCriteria.setInclude(false);
		citationFilter.add(citationCriteria);
		Kam citationKam = kamStore.getKam(kam.getKamInfo(), citationFilter);
		printKamInformation(citationKam);
		
		System.out.println("Done.");
		
		//close all connections to the KAM Store
		kamStore.teardown();

	}
}

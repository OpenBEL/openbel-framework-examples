package com.selventa.belframework.api.examples;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.selventa.belframework.api.KamStore;
import com.selventa.belframework.api.KamStoreImpl;
import com.selventa.belframework.common.cfg.SystemConfiguration;
import com.selventa.belframework.df.DBConnection;
import com.selventa.belframework.df.DatabaseService;
import com.selventa.belframework.df.DatabaseServiceImpl;
import com.selventa.belframework.kamstore.data.jdbc.KAMCatalogDao.KamFilter;
import com.selventa.belframework.kamstore.data.jdbc.KAMCatalogDao.KamInfo;
import com.selventa.belframework.kamstore.data.jdbc.KAMStoreDaoImpl.AnnotationType;
import com.selventa.belframework.kamstore.model.Kam;
import com.selventa.belframework.kamstore.model.KamStoreException;
import com.selventa.belframework.kamstore.model.filter.AnnotationFilterCriteria;

/**
 * {@link GetSpeciesFilteredKamExample} demonstrates how to load a filtered
 * {@link Kam kam} based on a species annotation.
 *
 * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
 */
public class GetSpeciesFilteredKamExample {

    /**
     * Holds a reference to the system configuration
     */
    private SystemConfiguration systemConfiguration;
    private KamStore kamStore;
    private DBConnection dbConnection;

    /**
     * Constructs the KamFilterExample
     */
    public GetSpeciesFilteredKamExample() {

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
        dbConnection = dbService.dbConnection(systemConfiguration.getKamURL(),
                systemConfiguration.getKamUser(),
                systemConfiguration.getKamPassword());

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
     * Run the example to report filtered {@link Kam kams} based on species
     * annotation combination.
     *
     * @throws Exception
     */
    public void run() throws Exception {

        // set up the KAM store by supplying database information
        setUpKamStore();

        // load unfiltered kam
        System.out.println("Get unfiltered KAM:");
        final KamInfo smallki = kamStore.getKamInfo("small_bel");
        Kam unfilteredKam = kamStore.getKam(smallki);
        printKamInformation(unfilteredKam);

        // load human-filtered kam
        final Set<Species> species = new HashSet<GetSpeciesFilteredKamExample.Species>();
        species.add(Species.HUMAN);
        reportFilteredKam("human", smallki, species);

        // load mouse-filtered kam
        species.clear();
        species.add(Species.MOUSE);
        reportFilteredKam("mouse", smallki, species);

        // load rat-filtered kam
        species.clear();
        species.add(Species.RAT);
        reportFilteredKam("rat", smallki, species);

        // load human/rat kam
        species.clear();
        species.add(Species.HUMAN);
        species.add(Species.RAT);
        reportFilteredKam("human/rat", smallki, species);

        // load human/mouse kam
        species.clear();
        species.add(Species.HUMAN);
        species.add(Species.MOUSE);
        reportFilteredKam("human/mouse", smallki, species);

        // load mouse/rat kam
        species.clear();
        species.add(Species.MOUSE);
        species.add(Species.RAT);
        reportFilteredKam("mouse/rat", smallki, species);

        // load human/mouse/rat kam
        species.clear();
        species.add(Species.HUMAN);
        species.add(Species.MOUSE);
        species.add(Species.RAT);
        reportFilteredKam("hmr", smallki, species);

        // species-less kam
        System.out.println("Get species-less KAM:");
        final KamFilter excludeFilter = excludeSpeciesFilter(smallki, kamStore);
        Kam speciesLessKam = kamStore.getKam(smallki, excludeFilter);
        printKamInformation(speciesLessKam);

        System.out.println("Done.");

        //close all connections to the KAM Store
        kamStore.teardown();
    }

    /**
     * Retrieve a filtered {@link Kam kam} and report metrics.
     *
     * @param label the filter {@link String label} to report
     * @param smallki the {@link KamInfo kam info} identifying the base kam
     * @param species the {@link Set set} of {@link Species species} to use
     * @throws KamStoreException Thrown if an error occurred loading the
     * filtered {@link Kam kam}
     */
    private void reportFilteredKam(final String label, final KamInfo smallki,
            final Set<Species> species) throws KamStoreException {
        final KamFilter humanFilter = createSpeciesFilter(species,
                smallki,
                kamStore);
        System.out.println("Get " + label + "-filtered KAM:");
        Kam humanKam = kamStore.getKam(smallki, humanFilter);
        printKamInformation(humanKam);
    }

    /**
     * Prints a quick summary for a KAM object
     *
     * @param kam
     */
    private void printKamInformation(Kam kam) {
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
     * Creates a "Species" annotation filter with included
     * {@link Species species}.
     *
     * @param includeSpecies the {@link Species species} to include
     * @param kamInfo the {@link KamInfo kam info} that represents the
     * {@link Kam}
     * @param kamStore the {@link KamStore kam store}
     * @return the species {@link KamFilter kam filter}
     * @throws KamStoreException Thrown if an error is encountered checking the
     * annotation types
     */
    private KamFilter createSpeciesFilter(final Set<Species> includeSpecies,
            final KamInfo kamInfo, final KamStore kamStore)
            throws KamStoreException {
        final List<AnnotationType> atypes = kamStore.getAnnotationTypes(kamInfo);
        AnnotationType species = null;
        for (final AnnotationType atype : atypes) {
            if ("Species".equals(atype.getName())) {
                species = atype;
                break;
            }
        }

        if (species == null) {
            throw new IllegalStateException(
                    "Kam does not contain 'Species' annotation.");
        }

        final KamFilter kf = kamInfo.createKamFilter();

        for (final Species s : Species.values()) {
            AnnotationFilterCriteria crit = s.createCriteria(species);
            crit.setInclude(includeSpecies.contains(s));
            kf.add(crit);
        }

        return kf;
    }

    /**
     * Create an exclude "Species" annotation filter.
     *
     * @param kamInfo the {@link KamInfo kam info} that represents the
     * {@link Kam}
     * @param kamStore the {@link KamStore kam store}
     * @return the species exclude {@link KamFilter kam filter}
     * @throws KamStoreException Thrown if an error is encountered checking the
     * annotation types
     */
    private KamFilter excludeSpeciesFilter(final KamInfo kamInfo,
            final KamStore kamStore) throws KamStoreException {
        final List<AnnotationType> atypes = kamStore.getAnnotationTypes(kamInfo);
        AnnotationType species = null;
        for (final AnnotationType atype : atypes) {
            if ("Species".equals(atype.getName())) {
                species = atype;
                break;
            }
        }

        if (species == null) {
            throw new IllegalStateException(
                    "Kam does not contain 'Species' annotation.");
        }

        final KamFilter kf = kamInfo.createKamFilter();
        final AnnotationFilterCriteria afc = new AnnotationFilterCriteria(species);
        afc.add("9606");
        afc.add("10090");
        afc.add("10116");
        afc.setInclude(false);
        kf.add(afc);
        return kf;
    }

    public static void main(String[] args) throws Exception {
        GetSpeciesFilteredKamExample app = new GetSpeciesFilteredKamExample();
        app.run();
    }

    /**
     * {@link Species} defines the allowed species domain for this example.
     *
     * @author Anthony Bargnesi &lt;abargnesi@selventa.com&gt;
     */
    private static enum Species {
        HUMAN(9606),
        MOUSE(10090),
        RAT(10116);

        private final int taxId;

        private Species(final int taxId) {
            this.taxId = taxId;
        }

        public AnnotationFilterCriteria createCriteria(
                final AnnotationType species) {
            final AnnotationFilterCriteria crit = new AnnotationFilterCriteria(species);
            crit.add(String.valueOf(taxId));
            return crit;
        }
    }
}

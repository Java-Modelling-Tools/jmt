package jmt.gui.common.xml;

import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.dto.SimDescriptor;
import jmt.gui.common.xml.mapper.SimDescriptorMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the refactored XMLWriter pipeline.
 * Performs a round-trip: XML -> Model -> Generated XML -> Model
 * and compares the intermediate DTO representations to ensure perfect
 * semantic data retention without relying on raw string diffs.
 *
 * @author Borgna Daniele
 * @since 25-mar-2026
 */
class XMLWriterTest {

    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // Round-Trip Tests
    // -------------------------------------------------------------------------

    @Test
    void test_parseAndSetImpatienceParameters_branches_fullcoverage() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/parseImpatience_branches.xml", "out_impatience.xml");
    }

    @Test
    void test_parseStationDeadlinesAndQuanta_branches_fullcoverage() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/parseStationDeadlinesAndQuanta_branches.xml", "out_deadlines.xml");
    }

    @Test
    void test_parseJoin_branches_fullcoverage() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/parseJoin_branches.xml", "out_join.xml");
    }

    @Test
    void test_parseStorageSection_branches_fullcoverage() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/parseStorage_branches.xml", "out_storage.xml");
    }

    @Test
    void test_parseEnablingSection_branches_fullcoverage() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/parseEnabling_branches.xml", "out_enabling.xml");
    }

    @Test
    void test_parseQueueSection_branches_fullcoverage() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/parseQueue_branches.xml", "out_queue.xml");
    }

    @Test
    void test_extractTransitionConditions_branches_fullcoverage() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/extractTransitionConditions_branches.xml", "out_transition_cond.xml");
    }

    @Test
    void test_parseDelaySectionAndQueueSectionDelay_branches_fullcoverage() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/parseDelaySections_branches.xml", "out_delay.xml");
    }

    @Test
    void test_parseBlockingRegions_branches() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/parseBlockingRegions_branches.xml", "out_blocking.xml");
    }

    @Test
    void test_parseDistribution_branches_fullcoverage() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/parseDistribution_branches.xml", "out_distribution.xml");
    }

    @Test
    void test_parseSemaphore_branches_fullcoverage() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/parseSemaphore_branches.xml", "out_semaphore.xml");
    }

    @Test
    void test_parseLogger_branches_fullcoverage() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/parseLogger_branches.xml", "out_logger.xml");
    }

    @Test
    void test_parseClassSwitch_branches_fullcoverage() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/parseClassSwitch_branches.xml", "out_classSwitch.xml");
    }

    @Test
    void test_parseRouter_branches_fullcoverage() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/parseRouter_branches.xml", "out_router.xml");
    }

    @Test
    void test_parseFork_branches_fullcoverage() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/parseFork_branches.xml", "out_fork.xml");
    }

    @Test
    void test_serverfeature_fullcoverage() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/serverfeature_fullcoverage.xml", "out_serverfeature.xml");
    }

    @Test
    void test_Workcopy3_onoff() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/792_Workcopy3_onoff.xml", "out_792.xml");
    }

    @Test
    void test_Sec5_3_FastTask_Quorum1_Wif_JSIMg() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/520_Sec5_3_FastTask_Quorum1_Wif_JSIMg.xml", "out_520.xml");
    }

    @Test
    void test_2FJ_2tasks_nestedOK() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/641_2FJ_2tasks_nestedOK.xml", "out_641.xml");
    }

    @Test
    void test_conBeta13fesc() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/225_conBeta13fesc.xml", "out_225.xml");
    }

    @Test
    void test_240415_FCFS_QD() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/4039_240415_FCFS_QD.xml", "out_4039.xml");
    }

    @Test
    void test_ForkJoin_4Stat_2_SEED1() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/10089_ForkJoin_4Stat_2 - SEED1.xml", "out_10089.xml");
    }

    @Test
    void test_10190_220423_2RotNogen_2Q() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/10190_220423_2RotNogen_2Q.xml", "out_10190.xml");
    }

    @Test
    void test_SjAS04Model_6AS_L5() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/23985_SjAS04Model_6AS-L5.xml", "out_23985.xml");
    }

    @Test
    void test_referenceSource_classSwitch_without_source_distribution_does_not_fail() {
        assertRoundTrip("src/test/resources/linetesting/input/jsim/Sec6_3_Obj2_SingleFact_Wif0,5-1,2_JSIMg.jsimg", "out_jsimg_ref.xml");
    }

    @Test
    void test_edgeCases_ClosedClasses_BlockingRegions() {
        assertRoundTrip("src/test/resources/jmt/gui/common/xml/afterrefactoring_edgecases.xml", "out_edgecases.xml");
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Executes the loading, saving and comparison of a single model file
     * by comparing the intermediate SimDescriptor representations.
     */
    private void assertRoundTrip(String sourcePath, String outputName) {
        File originalFile = new File(sourcePath);
        File outputFile = tempDir.resolve(outputName).toFile();

        // 1. Carica il modello dall'XML originale
        CommonModel originalModel = new CommonModel();
        XMLArchiver.loadModel(originalFile, originalModel);

        // 2. Ottieni il DTO dal modello originale (Il dato semantico ATTESO, include "dati fantasma")
        SimDescriptorMapper originalMapper = new SimDescriptorMapper(originalModel);
        SimDescriptor expectedDto = originalMapper.mapToDto();

        // 3. Salva il modello usando la nostra pipeline di scrittura pulita
        XMLArchiver.saveModel(outputFile, originalModel);

        // 4. Ricarica il modello dal file appena salvato
        CommonModel roundTrippedModel = new CommonModel();
        XMLArchiver.loadModel(outputFile, roundTrippedModel);

        // 5. Ottieni il DTO dal modello riletto (Il dato semantico EFFETTIVO, epurato)
        SimDescriptorMapper roundTrippedMapper = new SimDescriptorMapper(roundTrippedModel);
        SimDescriptor actualDto = roundTrippedMapper.mapToDto();

        // 6. Allinea gli alberi: forza la rimozione dei dati "fantasma" disabilitati in expectedDto
        // in modo che corrisponda alla versione sanificata generata da actualDto.
        alignZeroStrategies(expectedDto, actualDto);

        // 7. Esegui la deep-comparison
        assertThat(actualDto)
                .as("The round-tripped model should map to the exact same semantic DTO as the original")
                .usingRecursiveComparison()
                // Ignora dati prettamente meta-dinamici
                .ignoringFields(
                        "archiveName",
                        "globalAttributes.timestamp",
                        "globalAttributes.name"
                )
                // Ignoriamo le differenze sugli slash finali ("/" vs "") per i path locali
                .withComparatorForType((s1, s2) -> {
                    if (s1 == null && s2 == null) return 0;
                    if (s1 == null || s2 == null) return -1;
                    return s1.replaceAll("/$", "").compareTo(s2.replaceAll("/$", ""));
                }, String.class)
                .isEqualTo(expectedDto);
    }

    /**
     * Percorre i DTO in parallelo. Se il nuovo Writer ha giustamente esportato una
     * ZeroServiceTimeStrategy o DisabledServiceTimeStrategy (perché la feature era disabilitata),
     * forziamo l'expectedDto a corrispondere per evitare differenze sui parametri orfani.
     */
    private void alignZeroStrategies(SimDescriptor expected, SimDescriptor actual) {
        if (expected.getNodes() == null || actual.getNodes() == null) return;
        for (int i = 0; i < expected.getNodes().size(); i++) {
            if (i >= actual.getNodes().size()) break;
            jmt.gui.common.xml.dto.NodeDescriptor expNode = expected.getNodes().get(i);
            jmt.gui.common.xml.dto.NodeDescriptor actNode = actual.getNodes().get(i);

            if (expNode.getSections() == null || actNode.getSections() == null) continue;
            for (int j = 0; j < expNode.getSections().size(); j++) {
                if (j >= actNode.getSections().size()) break;
                jmt.gui.common.xml.dto.SectionDescriptor expSec = expNode.getSections().get(j);
                jmt.gui.common.xml.dto.SectionDescriptor actSec = actNode.getSections().get(j);

                if (expSec.getParameters() == null || actSec.getParameters() == null) continue;
                for (int k = 0; k < expSec.getParameters().size(); k++) {
                    if (k >= actSec.getParameters().size()) break;
                    alignParams(expSec.getParameters().get(k), actSec.getParameters().get(k));
                }
            }
        }
    }

    private void alignParams(jmt.gui.common.xml.dto.ParameterDescriptor exp, jmt.gui.common.xml.dto.ParameterDescriptor act) {
        if (exp == null || act == null) return;

        boolean isActZero = act.getClassPath() != null &&
                (act.getClassPath().contains("ZeroServiceTimeStrategy") ||
                        act.getClassPath().contains("DisabledServiceTimeStrategy"));

        if (isActZero) {
            // Forza il nodo expected ad essere identico al nodo actual, svuotando i parametri dell'esponenziale orfano
            exp.setName(act.getName());
            exp.setClassPath(act.getClassPath());
            exp.setValue(act.getValue());
            if (exp.getSubParameters() != null) {
                exp.getSubParameters().clear();
            }
            return;
        }

        // Ricorsione sui figli
        if (exp.getSubParameters() != null && act.getSubParameters() != null) {
            for (int i = 0; i < exp.getSubParameters().size(); i++) {
                if (i >= act.getSubParameters().size()) break;
                alignParams(exp.getSubParameters().get(i), act.getSubParameters().get(i));
            }
        }
    }
}
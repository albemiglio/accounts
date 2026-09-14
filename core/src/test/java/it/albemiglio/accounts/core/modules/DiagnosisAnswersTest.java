package it.albemiglio.accounts.core.modules;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The console command used to probe only the modules of the server it ran on, which on a proxy is a
 * handful — so an operator read "looks safe" having checked almost nothing. These cover the shape that
 * answers the whole network instead.
 */
class DiagnosisAnswersTest {

    private static final UUID PROBE = new UUID(0L, 7L);

    private static String row(String module, String where, String status, String detail) {
        return module + "\t" + where + "\t" + status + "\t" + detail;
    }

    @Test
    void countsEveryServerTogetherAndSaysHowManyAnswered() {
        Map<String, List<String>> answers = Map.of(
                "proxy-1", List.of(row("luckperms", "lp_players.uuid", "VERIFIED", "ok"),
                        row("maintenance", "config.yml", "NOT_FOUND", "none")),
                "kingdoms", List.of(row("cmi", "users.uuid", "VERIFIED", "ok"),
                        row("kingdoms", "data.uuid", "VERIFIED", "ok")));

        List<String> lines = DiagnosisReport.ofAnswers(PROBE, answers);

        assertTrue(lines.get(0).contains("2 server(s) answered"), lines.get(0));
        assertTrue(lines.stream().anyMatch(l -> l.contains("3 verified")), lines.toString());
        assertTrue(lines.stream().anyMatch(l -> l.contains("1 with no data")), lines.toString());
        assertTrue(lines.get(lines.size() - 1).startsWith("✓"), lines.toString());
    }

    /** A blocker on one server must name that server: "fix it" is useless without "where". */
    @Test
    void namesTheServerAblockerCameFrom() {
        Map<String, List<String>> answers = Map.of(
                "hub-1", List.of(row("essentials", "userdata", "MISSING", "table not there")));

        List<String> lines = DiagnosisReport.ofAnswers(PROBE, answers);

        assertTrue(lines.stream().anyMatch(l -> l.contains("hub-1") && l.contains("essentials")), lines.toString());
        assertTrue(lines.get(lines.size() - 1).startsWith("⚠"), lines.toString());
    }

    /** Nobody answering is not a clean bill of health, and must never read like one. */
    @Test
    void refusesToCallSilenceSafe() {
        List<String> lines = DiagnosisReport.ofAnswers(PROBE, Map.of());

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).toLowerCase().contains("no server answered"), lines.get(0));
        assertTrue(lines.stream().noneMatch(l -> l.startsWith("✓")), lines.toString());
    }

    /** A malformed answer is reported, not silently dropped — silence is what this whole thing fixes. */
    @Test
    void countsAnUnreadableRowAsSomethingToLookAt() {
        Map<String, List<String>> answers = Map.of("odd", List.of("not-tab-separated"));

        List<String> lines = DiagnosisReport.ofAnswers(PROBE, answers);

        assertTrue(lines.get(lines.size() - 1).startsWith("⚠"), lines.toString());
    }
}

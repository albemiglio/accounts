package it.albemiglio.accounts.core.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns every module's findings for one probe player into the lines an operator reads before running a
 * migration: a count per verdict, then the blockers spelled out. Shared so the proxy and the backends
 * answer {@code /accounts diagnose} identically — the proxy holds the network-wide modules (permissions,
 * bans, skins), so it is the one that most needs asking.
 */
public final class DiagnosisReport {

    private DiagnosisReport() {
    }

    /** Runs the read-only probe. Blocks on the modules' databases: call it off the main thread. */
    public static List<String> of(Collection<Module> modules, UUID probe) {
        List<String> flagged = new ArrayList<>();
        int verified = 0;
        int blockers = 0;
        int scanAll = 0;
        int empty = 0;
        int held = 0;
        for (Module module : modules) {
            for (Diagnosis d : module.diagnose(probe)) {
                switch (d.getStatus()) {
                    case VERIFIED:
                        verified++;
                        break;
                    case INFO:
                        scanAll++;
                        break;
                    case NOT_FOUND:
                        empty++;
                        break;
                    case HELD_BY_PLUGIN:
                        held++;
                        flagged.add("  · " + d.line());
                        break;
                    default: // FORMAT_MISMATCH, MISSING, ERROR
                        blockers++;
                        flagged.add("  ⚠ " + d.line());
                }
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add("Diagnosis vs " + probe + ": " + verified + " verified, " + blockers + " to FIX, "
                + scanAll + " scan-all, " + empty + " with no data for this player"
                + (held > 0 ? ", " + held + " waiting for a restart." : "."));
        lines.addAll(flagged);
        lines.add(blockers == 0
                ? "✓ Looks safe — every module found this player's data in the expected encoding (or has none). Back up first anyway."
                : "⚠ Fix the flagged modules (wrong 'format', or a missing table/path) before migrating.");
        return lines;
    }

    /**
     * The same verdict, for answers gathered from every server over Redis instead of from the modules of
     * whichever one the command was typed on. A proxy holds a handful of network-wide modules; the ranks,
     * homes and balances an operator is really asking about live on the backends, so a report that covers
     * only the proxy reads "looks safe" having checked almost nothing.
     *
     * @param answers instance id → one {@code module\tlocation\tstatus\tdetail} row per finding
     */
    public static List<String> ofAnswers(UUID probe, Map<String, List<String>> answers) {
        if (answers.isEmpty()) {
            return List.of("No server answered in time — nothing was checked. Is Redis reachable, and is "
                    + "accounts running on the backends?");
        }
        List<String> flagged = new ArrayList<>();
        int verified = 0;
        int blockers = 0;
        int scanAll = 0;
        int empty = 0;
        int held = 0;
        for (Map.Entry<String, List<String>> server : answers.entrySet()) {
            for (String row : server.getValue()) {
                String[] parts = row.split("\t", 4);
                // An answer we cannot read is counted as a blocker: dropping it quietly is the failure
                // this whole report exists to prevent.
                String status = parts.length >= 3 ? parts[2] : "ERROR";
                String where = server.getKey() + " · "
                        + (parts.length >= 2 ? parts[0] + " @ " + parts[1] : row);
                String detail = parts.length >= 4 ? parts[3] : "unreadable answer";
                switch (status) {
                    case "VERIFIED":
                        verified++;
                        break;
                    case "INFO":
                        scanAll++;
                        break;
                    case "NOT_FOUND":
                        empty++;
                        break;
                    case "HELD_BY_PLUGIN":
                        held++;
                        flagged.add("  · " + where + " — " + detail);
                        break;
                    default:
                        blockers++;
                        flagged.add("  ⚠ " + where + " — " + detail);
                }
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add(answers.size() + " server(s) answered about " + probe + ".");
        lines.add("Diagnosis vs " + probe + ": " + verified + " verified, " + blockers + " to FIX, "
                + scanAll + " scan-all, " + empty + " with no data for this player"
                + (held > 0 ? ", " + held + " waiting for a restart." : "."));
        lines.addAll(flagged);
        lines.add(blockers == 0
                ? "✓ Looks safe — every module found this player's data in the expected encoding (or has none). Back up first anyway."
                : "⚠ Fix the flagged modules (wrong 'format', or a missing table/path) before migrating.");
        return lines;
    }
}

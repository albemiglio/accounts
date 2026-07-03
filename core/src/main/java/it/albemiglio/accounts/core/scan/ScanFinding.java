package it.albemiglio.accounts.core.scan;

import it.albemiglio.accounts.core.modules.replacers.UuidCodec;
import lombok.Getter;

import java.nio.file.Path;

/**
 * One place the {@link Scanner}'s probe UUID was found. Immutable evidence, not a module: the static
 * factories say what each kind carries — FILE is the parent directory + extension of a uuid-named file
 * or directory, CONTENT is the text file whose content matched, SQLITE is database file + table +
 * column + stored format (or just the database + a note when it could not be probed), UNKNOWN_BINARY
 * is a binary file containing the raw uuid bytes (evidence only; no built-in type can rewrite it).
 */
@Getter
public final class ScanFinding {

    public enum Kind { FILE, CONTENT, SQLITE, UNKNOWN_BINARY }

    private final String pluginName;
    private final Kind kind;
    /** FILE: the directory holding the uuid-named entry; CONTENT/UNKNOWN_BINARY: the matched file; SQLITE: the original database file. */
    private final Path path;
    /** FILE only; empty = the bare uuid (an extensionless file or a whole per-player directory). */
    private final String extension;
    /** SQLITE only; null when the database could not be probed. */
    private final String table;
    private final String column;
    private final UuidCodec format;
    private final String note;

    private ScanFinding(String pluginName, Kind kind, Path path, String extension,
                        String table, String column, UuidCodec format, String note) {
        this.pluginName = pluginName;
        this.kind = kind;
        this.path = path;
        this.extension = extension;
        this.table = table;
        this.column = column;
        this.format = format;
        this.note = note;
    }

    public static ScanFinding file(String pluginName, Path directory, String extension, String note) {
        return new ScanFinding(pluginName, Kind.FILE, directory, extension, null, null, null, note);
    }

    public static ScanFinding content(String pluginName, Path file, String note) {
        return new ScanFinding(pluginName, Kind.CONTENT, file, null, null, null, null, note);
    }

    public static ScanFinding sqlite(String pluginName, Path database, String table, String column,
                                     UuidCodec format, String note) {
        return new ScanFinding(pluginName, Kind.SQLITE, database, null, table, column, format, note);
    }

    public static ScanFinding sqliteError(String pluginName, Path database, String note) {
        return new ScanFinding(pluginName, Kind.SQLITE, database, null, null, null, null, note);
    }

    public static ScanFinding unknownBinary(String pluginName, Path file, String note) {
        return new ScanFinding(pluginName, Kind.UNKNOWN_BINARY, file, null, null, null, null, note);
    }

    @Override
    public String toString() {
        return kind + " " + pluginName + " " + path
                + (table == null ? "" : " " + table + "." + column + " (" + format + ")")
                + (note == null ? "" : " — " + note);
    }
}

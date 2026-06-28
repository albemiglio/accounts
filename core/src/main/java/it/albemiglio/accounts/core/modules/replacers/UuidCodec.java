package it.albemiglio.accounts.core.modules.replacers;

import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * How a UUID column stores its value. Plugins don't agree: most keep a dashed string, some strip the
 * hyphens, and a few (CoreProtect and custom schemas) pack the 16 raw bytes into a {@code BINARY(16)}.
 * A migration that only rewrites dashed strings silently skips the other two, so the column picks its
 * encoding and {@link #bind} writes the value the same way the plugin would.
 */
public enum UuidCodec {

    /** The default: a 36-char dashed string, e.g. {@code 069a79f4-44e9-4726-a5be-fca90e38aaf5}. */
    DASHED {
        @Override
        public void bind(PreparedStatement ps, int index, UUID id) throws SQLException {
            ps.setString(index, id.toString());
        }
    },

    /** 32 hex chars with the hyphens removed, e.g. {@code 069a79f444e94726a5befca90e38aaf5}. */
    UNDASHED {
        @Override
        public void bind(PreparedStatement ps, int index, UUID id) throws SQLException {
            ps.setString(index, id.toString().replace("-", ""));
        }
    },

    /** 16 raw bytes, most-significant long first, bound as a {@code byte[]} so nothing is concatenated. */
    BINARY {
        @Override
        public void bind(PreparedStatement ps, int index, UUID id) throws SQLException {
            ps.setBytes(index, toBytes(id));
        }
    };

    public abstract void bind(PreparedStatement ps, int index, UUID id) throws SQLException;

    public static UuidCodec of(String name) {
        return name == null ? DASHED : valueOf(name.trim().toUpperCase());
    }

    static byte[] toBytes(UUID id) {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }
}

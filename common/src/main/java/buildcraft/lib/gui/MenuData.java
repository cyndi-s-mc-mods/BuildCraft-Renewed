package buildcraft.lib.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.inventory.ContainerData;

/** Syncs numbers from a block entity to an open menu. Vanilla only syncs 16-bit values, so longer values are split
 * across several slots. The server side reads values through suppliers; the client side has no suppliers and only
 * stores what it receives. Create the fields in the same order on both sides. */
public final class MenuData implements ContainerData {
    /** A synced value. Read it on the client with {@link #getLong}, {@link #getInt} or {@link #getDouble}. */
    public final class Field {
        private final int start;
        private final int width;
        private final @Nullable LongSupplier source;

        private Field(int width, @Nullable LongSupplier source) {
            this.start = values.size();
            this.width = width;
            this.source = source;
            for (int i = 0; i < width; i++) {
                values.add((short) 0);
            }
        }

        public long getLong() {
            if (source != null) return source.getAsLong();
            long value = 0;
            for (int i = 0; i < width; i++) {
                value |= (values.get(start + i) & 0xFFFFL) << (16 * i);
            }
            if (width < 4) {
                // Sign-extend
                int bits = width * 16;
                value = (value << (64 - bits)) >> (64 - bits);
            }
            return value;
        }

        public int getInt() {
            return (int) getLong();
        }

        public double getDouble() {
            return Double.longBitsToDouble(getLong());
        }

        public boolean getBoolean() {
            return getLong() != 0;
        }
    }

    private final List<Short> values = new ArrayList<>();
    private final List<Field> fields = new ArrayList<>();

    private Field add(int width, @Nullable LongSupplier source) {
        Field field = new Field(width, source);
        fields.add(field);
        return field;
    }

    public Field addLong(@Nullable LongSupplier source) {
        return add(4, source);
    }

    public Field addInt(@Nullable IntSupplier source) {
        return add(2, source == null ? null : () -> source.getAsInt());
    }

    public Field addDouble(@Nullable DoubleSupplier source) {
        return add(4, source == null ? null : () -> Double.doubleToLongBits(source.getAsDouble()));
    }

    public Field addBoolean(@Nullable BooleanSupplier source) {
        return add(1, source == null ? null : () -> source.getAsBoolean() ? 1 : 0);
    }

    @Override
    public int get(int index) {
        for (Field field : fields) {
            if (index >= field.start && index < field.start + field.width) {
                if (field.source != null) {
                    return (short) (field.source.getAsLong() >>> (16 * (index - field.start)));
                }
                break;
            }
        }
        return values.get(index);
    }

    @Override
    public void set(int index, int value) {
        values.set(index, (short) value);
    }

    @Override
    public int getCount() {
        return values.size();
    }
}

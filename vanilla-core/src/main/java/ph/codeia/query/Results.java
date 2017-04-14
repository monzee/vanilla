package ph.codeia.query;

/**
 * This file is a part of the vanilla project.
 */

public interface Results<T> extends Iterable<T> {

    boolean isEmpty();
    void dispose();

    interface Row {
        int getInt(int column);
        long getLong(int column);
        short getShort(int column);
        float getFloat(int column);
        String getString(int column);
        byte[] getBlob(int column);
    }

    interface Mapper<T> {
        T result(Row cursor);
    }

}

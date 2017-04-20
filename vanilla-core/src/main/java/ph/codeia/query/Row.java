package ph.codeia.query;

/**
 * This file is a part of the vanilla project.
 */

public interface Row {

    interface Mapper<T> {
        T from(Row cursor);
    }

    int getInt(int column);

    long getLong(int column);

    short getShort(int column);

    float getFloat(int column);

    double getDouble(int column);

    String getString(int column);

    byte[] getBlob(int column);

}

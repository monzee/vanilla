package ph.codeia.query;

/**
 * This file is a part of the vanilla project.
 */

public interface Params {
    String dataset();
    String[] projection();
    String selection();
    String[] selectionArgs();
    String sortOrder();
}

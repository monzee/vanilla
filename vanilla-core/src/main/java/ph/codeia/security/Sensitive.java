package ph.codeia.security;

import ph.codeia.meta.Experimental;

/**
 * This file is a part of the vanilla project.
 */

/**
 * An action that requires a set of permissions to proceed.
 */
@Experimental
public interface Sensitive extends Iterable<String> {

    /**
     * When received in a denied callback, an empty Sensitive means that
     * the user has permanently denied the permission and the app could not
     * appeal it any more unless the user changes it outside the app.
     *
     * @return whether or not this permit contains any permissions.
     */
    boolean isEmpty();

    /**
     * Used to identify which permissions can be appealed.
     *
     * @param permission The name to query.
     * @return whether or not this permission is included in the set of perms
     * being asked
     */
    boolean includes(String permission);

    /**
     * Sends the request.
     */
    void submit();

    /**
     * Receives the user's response to the permission request.
     *
     * @param code The request code.
     * @param permissions The list of permissions.
     * @param grants Each int indicates whether the permission at the same
     *               index was granted or denied.
     * @return whether or not this object handled this permission response
     */
    boolean decide(int code, String[] permissions, int[] grants);

}

package ph.codeia.security;

import java.util.Set;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * An action that requires a set of permissions to proceed.
 *
 * The introspection methods are meant to be used inside the
 * {@link Permit#denied(Do.Just)} blocks to identify which permissions are
 * still ungranted so that the app can display a message to the user
 * explaining why the permissions are needed. One should never unconditionally
 * submit the {@link Sensitive} object inside the deny callback or they might
 * loop forever.
 */
public interface Sensitive extends Iterable<String> {

    /**
     * Does this object contain anything?
     *
     * When received in a denied callback, an empty Sensitive means that
     * the user has permanently denied the permission and the app could not
     * appeal it any more unless the user changes it outside the app.
     *
     * @return whether or not this permit contains any permissions.
     */
    boolean isEmpty();

    /**
     * Does this object contain this permission?
     *
     * @param permission The name to query.
     * @return whether or not this permission is included in the set of perms
     * being asked
     */
    boolean contains(String permission);

    /**
     * Returns the denied permissions that can't be appealed.
     *
     * @return set of permissions.
     */
    Set<String> banned();

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
     * @return whether or not this object can handle this permission response
     */
    boolean check(int code, String[] permissions, int[] grants);

    /**
     * Applies the result of the permission check.
     *
     * This should only be called when {@link #check(int, String[], int[])}
     * returns true.
     *
     * The check is separated from the dispatch because of a peculiarity in
     * Android M with regard to fragment transactions. It seems impossible by
     * design to commit a fragment transaction inside the
     * #onRequestPermissionsResult method because it is too early. You need to
     * defer them until #onResume. It is possible to do #commitAllowingStateLoss
     * but that sounds scary (even the docs say so).
     */
    void dispatch();

}

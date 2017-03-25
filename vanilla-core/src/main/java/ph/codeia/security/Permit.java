package ph.codeia.security;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * A permission model that resembles android M's runtime permissions.
 *
 * The application initially asks the user a set of permissions. The user
 * can grant all or some of them. If some were denied, the app would then
 * receive another {@link Sensitive} object with the permissions that were
 * denied and are appealable. If none of the denied permissions are appealable,
 * the app will be called back with an empty Sensitive. Once everything has
 * been granted, the granted callback will be invoked.
 */
public interface Permit {

    /**
     * Adds a list of permissions to the set of permissions to ask.
     *
     * If no permissions were added or all permissions are already granted, the
     * runnable passed to {@link #granted(Runnable)} will be invoked
     * immediately when {@link Sensitive#submit()} is called.
     *
     * @param id Unique identifier for the permission set.
     * @param permissions The permissions to ask.
     * @return a builder object, probably this
     */
    Permit ask(int id, String... permissions);

    /**
     * Calls {@link #ask(int, String...)} with an auto-generated id.
     *
     * @see #ask(int, String...)
     */
    Permit ask(String... permissions);

    /**
     * What to do when all or some of the permissions are denied. Optional.
     *
     * NEVER UNCONDITIONALLY RESUBMIT THE APPEAL OBJECT! You will be stuck
     * in an infinite loop if the permissions are permanently denied.
     *
     * @param block The action; receives a {@link Sensitive} object containing
     *              the denied permissions that can be appealed. If all
     *              permissions were permanently denied, the object will be
     *              empty but not null.
     * @return a builder object, probably this
     */
    Permit denied(Do.Just<Sensitive> block);

    /**
     * Builds a {@link Sensitive} object.
     *
     * @param block The action to do when all of the permissions are granted.
     * @return an object that could initiate the request and receive grants
     */
    Sensitive granted(Runnable block);

}

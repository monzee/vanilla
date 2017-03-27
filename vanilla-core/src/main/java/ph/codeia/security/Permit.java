package ph.codeia.security;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * A permission model that resembles Android M's runtime permissions.
 *
 * The application initially asks the user a set of permissions. The app
 * can specify what to do before and after the request is submitted.
 *
 * The permissions are checked before submission. All previously granted
 * permissions are removed from the set. The remaining are looked up if they
 * were previously denied. If so, they are added to a set of appealable
 * permissions which the app can use to show a message to the user. If there are
 * no appealable permissions, the {@code before} callback is skipped and the
 * request is submitted directly.
 *
 * Inside the {@code before} callback, the app receives an object that contains
 * the set of appealable permissions and a method to submit the request. See
 * {@link Permission.Appeal#submit()} regarding the UX in a rationale dialog.
 *
 * At this point, the user is presented a dialog to decide whether or not the
 * app should be granted the permissions. This part is handled by the platform
 * and the app cannot influence it. The app can only receive the results
 * afterwards. The platform might also deny or grant on its own without user
 * input, possibly depending on previous user actions.
 *
 *
 */
public interface Permit {
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
     * @return whether or not this object can handle this permission response.
     * @see #dispatch()
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
     * #onRequestPermissionsResult method because it is still too early. You
     * need to defer them until #onResume. It is possible to call
     * #commitAllowingStateLoss instead of just #commit but that sounds scary.
     */
    void dispatch();

    interface Builder {
        /**
         * Adds a list of permissions to the set of permissions to ask.
         *
         * If no permissions were added or all permissions are already granted,
         * the runnable passed to {@link #granted(Runnable)} will be invoked
         * immediately when {@link #submit()} is called.
         *
         * @param permissions The permissions to ask.
         * @return a builder object, probably this
         */
        Builder ask(String... permissions);

        /**
         * What to do when the {@link Permission} object contains previously
         * denied permissions. Optional.
         *
         * @param block The function to call.
         * @return a builder object, probably this
         */
        Builder before(Do.Just<Permission.Appeal> block);

        /**
         * What to do when some permissions were denied by the user or platform.
         * Optional.
         *
         * @param block The function to call.
         * @return a builder object, probably this
         */
        Builder after(Do.Just<Permission.Denial> block);

        /**
         * What to do when all permissions are granted.
         *
         * @param block The function to call.
         * @return a fully built {@link Permit} object.
         */
        Permit granted(Runnable block);
    }
}

package ph.codeia.security;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * Set of permissions and their status.
 */
public class Permission {
    /**
     * Passed to a callback to show a rationale before sending a request.
     */
    public interface Appeal {
        /**
         * @return the permissions to be requested.
         */
        Set<String> permissions();

        /**
         * Submits the request.
         *
         * When showing a rationale, you should always give the user an option
         * to ignore your appeal and not send the request. You should only call
         * this in response to user input, never unconditionally.
         */
        void submit();
    }

    /**
     * Passed to a callback when the response arrives and some were denied.
     */
    public interface Denial {
        /**
         * @return set of permissions that were denied but can be reversed.
         */
        Set<String> denied();

        /**
         * @return set of permissions that were permanently denied. The user
         * will have to go to the device's settings to reverse them.
         */
        Set<String> forbidden();

        /**
         * Resubmits the request if not every permission was permanently denied.
         *
         * @return true if the request was resubmitted; false if there are no
         * appealable permissions left.
         */
        boolean appeal();
    }

    /**
     * Platform hooks for looking up the permission status.
     */
    public interface Adapter {
        /**
         * @param result The raw value of the permission grant.
         * @return whether or not this value corresponds to the granted status
         */
        boolean isGranted(int result);

        /**
         * @param permission The permission to lookup.
         * @return whether or not this permission is granted.
         */
        boolean isGranted(String permission);

        /**
         * @param permission The permission to lookup.
         * @return whether or not this permission was previously denied but not
         * permanently.
         */
        boolean isAppealable(String permission);
    }

    /**
     * A callback that unconditionally submits the appeal.
     *
     * Not a good idea in general, but sometimes you just don't care about your
     * users.
     */
    public static final Do.Just<Appeal> INSIST = new Do.Just<Appeal>() {
        @Override
        public void got(Appeal appeal) {
            appeal.submit();
        }
    };

    /**
     * The permissions that will be included in the request.
     *
     * This and the other sets below shouldn't be modified; I just can't be
     * arsed to make them private.
     */
    public final Set<String> pending = new HashSet<>();
    /**
     * Subset of {@link #pending} that were previously denied by the user.
     */
    public final Set<String> appealable = new HashSet<>();
    /**
     * Subset of {@link #pending} that will be automatically denied by the
     * platform without asking the user.
     */
    public final Set<String> forbidden = new HashSet<>();
    private final Adapter adapter;

    /**
     * @param permissionNames The permissions to request.
     * @param adapter Platform hooks.
     */
    public Permission(Iterable<String> permissionNames, Adapter adapter) {
        this.adapter = adapter;
        for (String p : permissionNames) if (!adapter.isGranted(p)) {
            pending.add(p);
            if (adapter.isAppealable(p)) {
                appealable.add(p);
            }
        }
    }

    /**
     * @return whether or not all permissions needed have been granted by
     * the user.
     */
    public boolean allGranted() {
        return pending.size() == 0;
    }

    /**
     * @return whether or not the app should show a message to explain why a
     * permission is needed.
     */
    public boolean someAppealable() {
        return appealable.size() > 0;
    }

    /**
     * Syncs the statuses.
     *
     * Should be called before {@link Permit#submit()} because the
     * permission status may have changed between instantiation time and
     * submission time, e.g. when a different Permission set that overlaps with
     * this was submitted and processed by the platform before this one is
     * submitted but after this was created.
     */
    public void refresh() {
        for (Iterator<String> it = pending.iterator(); it.hasNext();) {
            String p = it.next();
            if (adapter.isGranted(p)) {
                it.remove();
                appealable.remove(p);
            } else if (!adapter.isAppealable(p)) {
                appealable.remove(p);
            } else {
                appealable.add(p);
            }
        }
    }

    /**
     * Applies the permissions granted by the user.
     *
     * @param names The permissions granted or denied.
     * @param grants The raw grant statuses. Should be the same length as the
     *               names array.
     * @return a {@link Permission} with the new grants applied.
     */
    public Permission fold(String[] names, int[] grants) {
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            if (adapter.isGranted(grants[i])) {
                pending.remove(name);
                appealable.remove(name);
            } else if (!adapter.isAppealable(name)) {
                appealable.remove(name);
                forbidden.add(name);
            } else {
                appealable.add(name);
            }
        }
        return this;
    }
}


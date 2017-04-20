package ph.codeia.androidutils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.SparseArray;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import ph.codeia.meta.Untested;
import ph.codeia.security.Permission;
import ph.codeia.security.Permit;
import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * A {@link Permit.Builder} for Marshmallow and above.
 *
 * Can still (and should) be used before Marshmallow, except it just calls the
 * allow callback immediately after {@link #granted(Runnable)} if the permission
 * was declared in the manifest.
 */
@Untested
public class AndroidPermit implements Permit.Builder, Permission.Adapter {

    /**
     * Headless fragment helper for support clients.
     *
     * NEVER INSTANTIATE DIRECTLY! use one of the {@code #of()} static
     * factories.
     */
    public static class SupportHelper extends Fragment {
        Helper delegate;

        @Override
        public void onResume() {
            super.onResume();
            if (delegate != null) {
                delegate.dispatch();
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            if (delegate != null) {
                delegate.dispose();
                delegate = null;
            }
        }

        @Override
        public void onRequestPermissionsResult(
                int requestCode,
                @NonNull String[] permissions,
                @NonNull int[] grantResults) {
            if (delegate != null) {
                delegate.check(requestCode, permissions, grantResults);
            }
        }
    }

    /**
     * Headless fragment helper for platform clients.
     *
     * NEVER INSTANTIATE DIRECTLY! use one of the {@code #of()} static
     * factories.
     */
    public static class PlatformHelper extends android.app.Fragment {
        Helper delegate;

        @Override
        public void onResume() {
            super.onResume();
            if (delegate != null) {
                delegate.dispatch();
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            if (delegate != null) {
                delegate.dispose();
                delegate = null;
            }
        }

        @Override
        public void onRequestPermissionsResult(
                int requestCode,
                @NonNull String[] permissions,
                @NonNull int[] grantResults) {
            if (delegate != null) {
                delegate.check(requestCode, permissions, grantResults);
            }
        }
    }

    private interface FragmentAdapter {
        void match(Case of);
        interface Case {
            void support(SupportHelper fragment);
            void platform(PlatformHelper fragment);
        }
    }

    /**
     * Stores permit sets that may be submitted at any time by a client.
     *
     * Helper fragments forward to this class when the user grants arrive.
     */
    public static class Helper {

        private static final String TAG = Helper.class.getCanonicalName();

        /**
         * Uses the last half of legal request codes to make collisions unlikely
         */
        private final AtomicInteger counter = new AtomicInteger(1 << 15);
        private final SparseArray<Permit> requests = new SparseArray<>();
        private final Queue<Permit> deferred = new ArrayDeque<>();
        private final FragmentAdapter adapter;
        @Nullable private Do.Just<Permission.Appeal> beforeFallback = Permission.INSIST;
        @Nullable private Do.Just<Permission.Denial> afterFallback;

        private Helper(FragmentAdapter adapter) {
            this.adapter = adapter;
        }

        /**
         * Sets the default appeal callback.
         *
         * @param block Procedure to run before submitting a request.
         */
        public void setBefore(Do.Just<Permission.Appeal> block) {
            beforeFallback = block;
        }

        /**
         * Sets the default denied callback.
         *
         * @param block Procedure to run when at least one permission was
         *              denied by the user.
         */
        public void setAfter(Do.Just<Permission.Denial> block) {
            afterFallback = block;
        }

        /**
         * Wraps an {@link AndroidPermit} instance to remember all
         * {@link Permit} objects associated with a client.
         *
         * @param code Number that uniquely identifies this permission set.
         * @return a permit builder wrapping {@link AndroidPermit}
         */
        public Permit.Builder make(final int code) {
            final AndroidPermit androidPermit = new FragmentAdapter.Case() {
                AndroidPermit permit;

                AndroidPermit permit() {
                    adapter.match(this);
                    return permit;
                }

                @Override
                public void support(SupportHelper fragment) {
                    fragment.delegate = Helper.this;
                    permit = new AndroidPermit(fragment, code);
                }

                @Override
                public void platform(PlatformHelper fragment) {
                    fragment.delegate = Helper.this;
                    permit = new AndroidPermit(fragment, code);
                }
            }.permit();
            return new Permit.Builder() {
                Permit.Builder b = androidPermit.before(beforeFallback).after(afterFallback);

                @Override
                public Permit.Builder ask(String... permissions) {
                    b = b.ask(permissions);
                    return this;
                }

                @Override
                public Permit.Builder before(Do.Just<Permission.Appeal> block) {
                    b = b.before(block);
                    return this;
                }

                @Override
                public Permit.Builder after(Do.Just<Permission.Denial> block) {
                    b = b.after(block);
                    return this;
                }

                @Override
                public Permit granted(Runnable block) {
                    Permit p = b.granted(block);
                    requests.put(code, p);
                    return p;
                }
            };
        }

        /**
         * @return see {@link #make(int)}
         */
        public Permit.Builder make() {
            return make(counter.getAndIncrement());
        }

        /**
         * Shortcut for {@code make().ask(String...)}
         *
         * @param permissions The permissions to ask the user.
         * @return see {@link Permit.Builder#ask(String...)}
         */
        public Permit.Builder ask(String... permissions) {
            return make().ask(permissions);
        }

        /**
         * Shortcut for {@code make(int).ask(String...)}
         *
         * @param code Unique code for this permission set.
         * @param permissions The permissions to ask.
         * @return see {@link Permit.Builder#ask(String...)}
         */
        public Permit.Builder ask(int code, String... permissions) {
            return make(code).ask(permissions);
        }

        void dispatch() {
            for (Iterator<Permit> it = deferred.iterator(); it.hasNext(); it.remove()) {
                it.next().dispatch();
            }
        }

        void dispose() {
            requests.clear();
            beforeFallback = null;
            afterFallback = null;
        }

        void check(int requestCode, String[] permissions, int[] grantResults) {
            Permit p = requests.get(requestCode);
            if (p != null && p.check(requestCode, permissions, grantResults)) {
                deferred.add(p);
            }
        }
    }


    /**
     * Ensures only one instance of {@link Helper} is attached to the activity.
     *
     * @param fm Support fragment manager.
     * @return a helper object for building {@link Permit.Builder} objects.
     */
    public static Helper of(FragmentManager fm) {
        final SupportHelper helper;
        Fragment f = fm.findFragmentByTag(Helper.TAG);
        if (f != null) {
            helper = (SupportHelper) f;
            if (helper.delegate != null) {
                return helper.delegate;
            }
        } else {
            helper = new SupportHelper();
            fm.beginTransaction().add(helper, Helper.TAG).commitNow();
        }
        return new Helper(new FragmentAdapter() {
            @Override
            public void match(Case of) {
                of.support(helper);
            }
        });
    }

    /**
     * Ensures only one instance of {@link Helper} is attached to the activity.
     *
     * @param fm Platform fragment manager.
     * @return see {@link #of(FragmentManager)}
     */
    public static Helper of(android.app.FragmentManager fm) {
        final PlatformHelper helper;
        android.app.Fragment f = fm.findFragmentByTag(Helper.TAG);
        if (f != null) {
            helper = (PlatformHelper) f;
            if (helper.delegate != null) {
                return helper.delegate;
            }
        } else {
            helper = new PlatformHelper();
            fm.beginTransaction().add(helper, Helper.TAG).commit();
            fm.executePendingTransactions();
        }
        return new Helper(new FragmentAdapter() {
            @Override
            public void match(Case of) {
                of.platform(helper);
            }
        });
    }

    /**
     * Helper factory for support activities.
     *
     * @param activity Provides a FragmentManager to attach the fragment to.
     * @return see {@link #of(FragmentManager)}
     */
    public static Helper of(FragmentActivity activity) {
        return of(activity.getSupportFragmentManager());
    }

    /**
     * Helper factory for platform activities.
     *
     * @param activity Provides a platform fragment manager for the helper to
     *                 attach to.
     * @return see {@link #of(FragmentManager)}
     */
    public static Helper of(Activity activity) {
        return of(activity.getFragmentManager());
    }

    /**
     * Helper factory for support fragments.
     *
     * @param fragment Uses its parent's FM. Call {@link #of(FragmentManager)}
     *                 if you need/want to use a child FM.
     * @return see {@link #of(FragmentManager)}
     */
    public static Helper of(Fragment fragment) {
        return of(fragment.getFragmentManager());
    }

    /**
     * Helper factory for platform fragments.
     *
     * @param fragment Uses its parent FM.
     * @return see {@link #of(FragmentManager)}
     * @see #of(android.app.FragmentManager) to use a child FM.
     */
    public static Helper of(android.app.Fragment fragment) {
        return of(fragment.getFragmentManager());
    }

    private interface Client {
        void match(Case of);
        interface Case {
            void activity(Activity client);
            void fragment(Fragment client);
            void platformFragment(android.app.Fragment client);
        }
    }

    private final Context context;
    private final Client client;
    private final int code;
    private final Set<String> permissionNames = new HashSet<>();
    @Nullable private Do.Just<Permission.Appeal> before = Permission.INSIST;
    @Nullable private Do.Just<Permission.Denial> after;

    /**
     * @param client The object to call permission checks and requests on. You
     *               can pass platform activities here.
     * @param code Unique identifier
     */
    public AndroidPermit(final Activity client, int code) {
        this.client = new Client() {
            @Override
            public void match(Case of) {
                of.activity(client);
            }
        };
        this.code = code;
        context = client.getApplicationContext();
    }

    /**
     * @param client The object to call permission checks and requests on.
     * @param code Unique identifier
     */
    public AndroidPermit(final Fragment client, int code) {
        this.client = new Client() {
            @Override
            public void match(Case of) {
                of.fragment(client);
            }
        };
        this.code = code;
        context = client.getContext();
    }

    /**
     * Associate with a platform fragment.
     *
     * @param client A platform fragment.
     * @param code Unique identifier.
     */
    public AndroidPermit(final android.app.Fragment client, int code) {
        this.client = new Client() {
            @Override
            public void match(Case of) {
                of.platformFragment(client);
            }
        };
        this.code = code;
        context = client.getActivity().getApplicationContext();
    }

    @Override
    public Permit.Builder ask(String... permissions) {
        Collections.addAll(permissionNames, permissions);
        return this;
    }

    @Override
    public Permit.Builder before(Do.Just<Permission.Appeal> block) {
        before = block;
        return this;
    }

    @Override
    public Permit.Builder after(Do.Just<Permission.Denial> block) {
        after = block;
        return this;
    }

    @Override
    public Permit granted(final Runnable block) {
        return new Permit() {
            Permission is = new Permission(permissionNames, AndroidPermit.this);

            void sendRequest() {
                client.match(new Client.Case() {
                    final String[] request = is.pending
                            .toArray(new String[is.pending.size()]);

                    @Override
                    public void activity(Activity client) {
                        ActivityCompat.requestPermissions(client, request, code);
                    }

                    @Override
                    public void fragment(Fragment client) {
                        client.requestPermissions(request, code);
                    }

                    @Override
                    public void platformFragment(android.app.Fragment client) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            client.requestPermissions(request, code);
                            return;
                        }
                        PackageManager pm = context.getPackageManager();
                        String packageName = context.getPackageName();
                        int[] grants = new int[request.length];
                        for (int i = 0; i < request.length; i++) {
                            grants[i] = pm.checkPermission(request[i], packageName);
                        }
                        check(code, request, grants);
                        client.getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dispatch();
                            }
                        });
                    }
                });
            }

            @Override
            public void submit() {
                is.refresh();
                if (is.allGranted()) {
                    block.run();
                } else if (before != null && is.someAppealable()) {
                    before.got(new Permission.Appeal() {
                        @Override
                        public Set<String> permissions() {
                            return Collections.unmodifiableSet(is.appealable);
                        }

                        @Override
                        public void submit() {
                            sendRequest();
                        }
                    });
                } else {
                    sendRequest();
                }
            }

            @Override
            public boolean check(int code, String[] names, int[] grants) {
                if (AndroidPermit.this.code != code || names.length == 0) {
                    return false;
                }
                is = is.fold(names, grants);
                return true;
            }

            @Override
            public void dispatch() {
                if (is.allGranted()) {
                    block.run();
                } else if (after != null) {
                    after.got(new Permission.Denial() {
                        @Override
                        public Set<String> denied() {
                            return Collections.unmodifiableSet(is.appealable);
                        }

                        @Override
                        public Set<String> forbidden() {
                            return Collections.unmodifiableSet(is.forbidden);
                        }

                        @Override
                        public boolean appeal() {
                            if (is.someAppealable()) {
                                submit();
                                return true;
                            }
                            return false;
                        }
                    });
                }
            }
        };
    }

    @Override
    public boolean isGranted(int result) {
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean isGranted(String permission) {
        return isGranted(ActivityCompat.checkSelfPermission(context, permission));
    }

    @Override
    public boolean isAppealable(final String permission) {
        return new Client.Case() {
            boolean result;

            boolean result() {
                client.match(this);
                return result;
            }

            @Override
            public void activity(Activity client) {
                result = ActivityCompat.shouldShowRequestPermissionRationale(client, permission);
            }

            @Override
            public void fragment(Fragment client) {
                result = client.shouldShowRequestPermissionRationale(permission);
            }

            @Override
            public void platformFragment(android.app.Fragment client) {
                result = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        client.shouldShowRequestPermissionRationale(permission);
            }
        }.result();
    }
}

package ph.codeia.androidutils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.SparseArray;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import ph.codeia.meta.Untested;
import ph.codeia.security.Permit;
import ph.codeia.security.Permission;
import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * An {@link Permit.Builder} for Marshmallow and above.
 *
 * Can still (and should) be used before Marshmallow, except it just calls the
 * allow callback immediately after {@link #granted(Runnable)} if the permission
 * was declared in the manifest.
 */
@Untested
public class AndroidPermit implements Permit.Builder, Permission.Adapter {

    /**
     * Headless fragment that produces Permits and attaches to the permission
     * response hook.
     *
     * NEVER INSTANTIATE DIRECTLY; use any of the {@code #of} static methods.
     */
    public static class Helper extends Fragment {
        private static final String TAG = Helper.class.getCanonicalName();
        /**
         * uses the last half of usable numbers to make collisions unlikely
         */
        private final AtomicInteger counter = new AtomicInteger(1 << 15);
        private final SparseArray<Permit> requests = new SparseArray<>();
        @Nullable private Do.Just<Permission.Appeal> beforeFallback;
        @Nullable private Do.Just<Permission.Denial> afterFallback;
        @Nullable private Permit deferred;

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
            return new Permit.Builder() {
                Permit.Builder b = new AndroidPermit(Helper.this, code)
                        .before(beforeFallback)
                        .after(afterFallback);

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

        @Override
        public void onResume() {
            super.onResume();
            if (deferred != null) {
                deferred.dispatch();
                deferred = null;
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            requests.clear();
            deferred = null;
        }

        @Override
        public void onRequestPermissionsResult(
                int requestCode,
                @NonNull String[] permissions,
                @NonNull int[] grantResults) {
            Permit p = requests.get(requestCode);
            if (p != null && p.check(requestCode, permissions, grantResults)) {
                deferred = p;
                return;
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    /**
     * Ensures only one instance of {@link Helper} is attached to the activity.
     *
     * @param fm Does not work with platform fragment managers because that
     *           means copy-pasted code. You should construct {@link
     *           AndroidPermit} directly and delegate to it in
     *           #onRequestPermissionsResult.
     * @return a fragment that builds {@link Permit.Builder} objects.
     * @see #AndroidPermit(android.app.Fragment, int) if you are using
     * platform fragments.
     */
    public static Helper of(FragmentManager fm) {
        Fragment f = fm.findFragmentByTag(Helper.TAG);
        if (f == null) {
            f = new Helper();
            fm.beginTransaction().add(f, Helper.TAG).commitNow();
        }
        return (Helper) f;
    }

    /**
     * @param activity Provides a FragmentManager to attach the fragment to.
     *                 Can't use platform activities here either. You will need
     *                 to construct your own permit with
     *                 {@link #AndroidPermit(Activity, int)}.
     * @return see {@link #of(FragmentManager)}
     * @see #AndroidPermit(android.app.Fragment, int); same situation applies
     * when using platform activities instead of compat.
     */
    public static Helper of(FragmentActivity activity) {
        return of(activity.getSupportFragmentManager());
    }

    /**
     * @param fragment Uses its parent's FM. Call {@link #of(FragmentManager)}
     *                 if you need/want to use a child FM.
     * @return see {@link #of(FragmentManager)}
     */
    public static Helper of(Fragment fragment) {
        return of(fragment.getFragmentManager());
    }

    private interface Client {
        void match(Case some);
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
    @Nullable private Do.Just<Permission.Appeal> before;
    @Nullable private Do.Just<Permission.Denial> after;

    /**
     * @param client The object to call permission checks and requests on.
     * @param code Unique identifier
     */
    public AndroidPermit(final Activity client, int code) {
        this.client = new Client() {
            @Override
            public void match(Case some) {
                some.activity(client);
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
            public void match(Case some) {
                some.fragment(client);
            }
        };
        this.code = code;
        context = client.getContext();
    }

    /**
     * When using platform fragments, you get no help from the library. You
     * will have to declare {@link Permit} members, submit them and override
     * {@link android.app.Fragment#onRequestPermissionsResult(int, String[], int[])}
     * where you need to call {@link Permit#check(int, String[], int[])} and
     * {@link Permit#dispatch()}.
     *
     * @param client Platform fragment.
     * @param code Unique identifier.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public AndroidPermit(final android.app.Fragment client, int code) {
        this.client = new Client() {
            @Override
            public void match(Case some) {
                some.platformFragment(client);
            }
        };
        this.code = code;
        this.context = client.getContext();
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
                        } else {
                            // all permissions are allowed at install time for
                            // old versions, so this is safe.
                            block.run();
                        }
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
                            return is.appealable;
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
                            return is.appealable;
                        }

                        @Override
                        public Set<String> rejected() {
                            return is.autoDenied;
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    result = client.shouldShowRequestPermissionRationale(permission);
                }
            }
        }.result();
    }
}

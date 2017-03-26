package ph.codeia.androidutils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.SparseArray;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import ph.codeia.run.PassThrough;
import ph.codeia.run.Runner;
import ph.codeia.security.Permit;
import ph.codeia.security.Sensitive;
import ph.codeia.security.Synthetic;
import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * A {@link Permit} for marshmallow and above, implemented as a mutable builder.
 *
 * Can still (and should) be used before marshmallow, except it just calls the
 * allow callback immediately after {@link #granted(Runnable)} if the permission
 * was declared in the manifest.
 */
public class AndroidPermit implements Permit {

    /**
     * Headless fragment that produces Permits and attaches to the permission
     * response hook.
     *
     * Intended for screens that needs multiple sets of permissions on different
     * actions. You might want to use this anyway even for a single permission
     * because it saves you from overriding
     * {@link #onRequestPermissionsResult(int, String[], int[])} in your
     * activity or fragment.
     *
     * NEVER INSTANTIATE DIRECTLY; use any of the {@code #of} static methods.
     */
    public static class Helper extends Fragment {
        private static final String TAG = Helper.class.getCanonicalName();

        private final AtomicInteger counter = new AtomicInteger(1 << 15);
        private final SparseArray<Sensitive> requests = new SparseArray<>();
        // is it possible to have >1 requests active at the same time?
        private final Queue<Sensitive> dispatchable = new ArrayDeque<>();
        private Runner runner = PassThrough.RUNNER;
        private Do.Just<Sensitive> onDeny;

        /**
         * Change the context under which the denied/granted callbacks run.
         *
         * If the permissions were already previously granted or permanently
         * denied, the callbacks would be run in the same thread where
         * {@link Sensitive#submit()} was called. You should call this method
         * with {@link AndroidRunner#UI} if you need to update the UI in the
         * callbacks. If all your {@link Sensitive#submit()}} calls are in the
         * UI thread, there's no need to call this but there's no harm in
         * doing so aside from becoming slightly less efficient.
         *
         * @param runner used to wrap the {@link #denied(Do.Just)} and
         *               {@link #granted(Runnable)} callbacks
         */
        public void setRunner(Runner runner) {
            this.runner = runner;
        }

        /**
         * Set the default denied callback for all created {@link Sensitive}
         * objects.
         *
         * You can change the individual deny handlers by calling
         * {@link #denied(Do.Just)} on the {@link Permit} objects before
         * calling {@link #granted(Runnable)}.
         *
         * @param onDeny see {@link Permit#denied(Do.Just)}
         */
        public void setDeniedCallback(Do.Just<Sensitive> onDeny) {
            this.onDeny = onDeny;
        }

        /**
         * Create a {@link Permit} object.
         *
         * @param code Unique identifier for this permission set
         * @return wraps an AndroidPermit instance
         */
        public Permit make(final int code) {
            return new Permit() {
                Permit p = new AndroidPermit(Helper.this, code, runner).denied(onDeny);

                @Override
                public Permit ask(String... permissions) {
                    p = p.ask(permissions);
                    return this;
                }

                @Override
                public Permit denied(Do.Just<Sensitive> block) {
                    p = p.denied(block);
                    return this;
                }

                @Override
                public Sensitive granted(Runnable block) {
                    Sensitive s = p.granted(block);
                    requests.put(code, s);
                    return s;
                }
            };
        }

        /**
         * Creates a wrapped AndroidPermit with an auto generated id.
         *
         * @see #make(int)
         */
        public Permit make() {
            return make(counter.getAndIncrement());
        }

        /**
         * Shortcut for {@code o.make(int).ask(String...)}
         *
         * @see #make(int)
         */
        public Permit ask(int code, String... permissions) {
            return make(code).ask(permissions);
        }

        /**
         * Shortcut for {@code o.make().ask(String...)}
         *
         * @see #make()
         */
        public Permit ask(String... permissions) {
            return make().ask(permissions);
        }

        @Override
        public void onResume() {
            super.onResume();
            for (Iterator<Sensitive> it = dispatchable.iterator(); it.hasNext(); it.remove()) {
                it.next().dispatch();
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            requests.clear();
        }

        @Override
        public void onRequestPermissionsResult(
                int requestCode,
                @NonNull String[] permissions,
                @NonNull int[] grantResults) {
            Sensitive s = requests.get(requestCode);
            if (s != null && s.check(requestCode, permissions, grantResults)) {
                dispatchable.add(s);
                return;
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    /**
     * Ensures only one instance of {@link Helper} is attached to the activity.
     *
     * @param fm does not work with platform fragment managers. You shouldn't
     *           be using those anyway.
     * @return an AndroidPermit factory
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
     * @see #of(FragmentManager)
     */
    public static Helper of(FragmentActivity activity) {
        return of(activity.getSupportFragmentManager());
    }

    /**
     * @see #of(FragmentManager)
     */
    public static Helper of(Fragment fragment) {
        return of(fragment.getFragmentManager());
    }

    private interface Client {
        void match(Case some);
        interface Case {
            void activity(Activity client);
            void fragment(Fragment client);
        }
    }

    private final Context context;
    private final Client client;
    private final Set<String> permissions = new LinkedHashSet<>();
    private final Runner runner;
    private final int code;
    @Nullable private Do.Just<Sensitive> onDeny;

    public AndroidPermit(Activity activity, int code) {
        this(activity, code, PassThrough.RUNNER);
    }

    public AndroidPermit(final Activity activity, int code, Runner runner) {
        this.runner = runner;
        this.code = code;
        context = activity.getApplicationContext();
        client = new Client() {
            @Override
            public void match(Case some) {
                some.activity(activity);
            }
        };
    }

    public AndroidPermit(Fragment fragment, int code) {
        this(fragment, code, PassThrough.RUNNER);
    }

    public AndroidPermit(final Fragment fragment, int code, Runner runner) {
        this.runner = runner;
        this.code = code;
        context = fragment.getContext();
        client = new Client() {
            @Override
            public void match(Case some) {
                some.fragment(fragment);
            }
        };
    }

    @Override
    public Permit ask(String... permissions) {
        Collections.addAll(this.permissions, permissions);
        return this;
    }

    @Override
    public Permit denied(Do.Just<Sensitive> block) {
        onDeny = runner.run(block);
        return this;
    }

    @Override
    public Sensitive granted(final Runnable block) {
        final Do.Just<Void> onAllow = runner.run(new Do.Just<Void>() {
            @Override
            public void got(Void value) {
                block.run();
            }
        });
        return new Sensitive() {
            private final Set<String> banned = new LinkedHashSet<>();
            private boolean readyToRequest = false;

            @Override
            public Iterator<String> iterator() {
                return permissions.iterator();
            }

            @Override
            public boolean isEmpty() {
                return permissions.isEmpty();
            }

            @Override
            public boolean contains(String permission) {
                return permissions.contains(permission);
            }

            @Override
            public Set<String> banned() {
                return banned;
            }

            @Override
            public void submit() {
                if (isEmpty() && !banned.isEmpty()) {
                    throw new UnsupportedOperationException(
                            "You should not resubmit an empty Sensitive object.");
                }
                for (Iterator<String> it = permissions.iterator(); it.hasNext();) {
                    if (allowed(it.next())) {
                        it.remove();
                    }
                }
                String[] perms = permissions.toArray(new String[permissions.size()]);
                if (!readyToRequest && onDeny != null) {
                    readyToRequest = true;
                    List<String> primer = new ArrayList<>();
                    for (String p : perms) {
                        if (canAppeal(p)) {
                            primer.add(p);
                        }
                    }
                    if (primer.isEmpty()) {
                        reallySubmit(perms);
                    } else {
                        onDeny.got(new Synthetic(this, primer));
                    }
                } else {
                    reallySubmit(perms);
                }
            }

            @Override
            public boolean check(int code, String[] permissions, int[] grants) {
                if (AndroidPermit.this.code != code || permissions.length == 0) {
                    return false;
                }
                for (int i = 0; i < permissions.length; i++) {
                    boolean allowed = allowed(grants[i]);
                    String permission = permissions[i];
                    if (allowed || !canAppeal(permission)) {
                        AndroidPermit.this.permissions.remove(permission);
                        if (!allowed) {
                            banned.add(permission);
                        }
                    }
                }
                return true;
            }

            @Override
            public void dispatch() {
                if (isEmpty() && banned.isEmpty()) {
                    onAllow.got(null);
                } else if (onDeny != null) {
                    readyToRequest = true;
                    onDeny.got(this);
                }
            }

            private void reallySubmit(final String[] permissions) {
                if (permissions.length > 0) {
                    client.match(new Client.Case() {
                        @Override
                        public void activity(Activity client) {
                            ActivityCompat.requestPermissions(client, permissions, code);
                        }

                        @Override
                        public void fragment(Fragment client) {
                            client.requestPermissions(permissions, code);
                        }
                    });
                } else if (banned.isEmpty()) {
                    onAllow.got(null);
                } else if (onDeny != null) {
                    onDeny.got(this);
                }
            }
        };
    }

    private boolean canAppeal(final String permission) {
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
        }.result();
    }

    private boolean allowed(String permission) {
        return allowed(ActivityCompat.checkSelfPermission(context, permission));
    }

    private static boolean allowed(int result) {
        return result == PackageManager.PERMISSION_GRANTED;
    }

}

package ph.codeia.androidutils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import ph.codeia.meta.Experimental;
import ph.codeia.run.PassThrough;
import ph.codeia.run.Runner;
import ph.codeia.security.Permit;
import ph.codeia.security.Sensitive;
import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 */

/**
 * A {@link Permit} for marshmallow and above, implemented as a mutable builder.
 *
 * Can still (and should) be used before marshmallow, except it just calls the
 * allow callback immediately after {@link #granted(Runnable)} or throws if the
 * permission was not declared.
 */
@Experimental
public class AndroidPermit implements Permit {

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    private static class FalseStart implements Sensitive {
        private final Sensitive delegate;
        private final List<String> appeal;

        FalseStart(Sensitive delegate, List<String> appeal) {
            this.delegate = delegate;
            this.appeal = appeal;
        }

        @Override
        public boolean isEmpty() {
            return appeal.isEmpty();
        }

        @Override
        public boolean contains(String permission) {
            return appeal.contains(permission);
        }

        @Override
        public Set<String> banned() {
            return delegate.banned();
        }

        @Override
        public void submit() {
            delegate.submit();
        }

        @Override
        public boolean apply(int code, String[] permissions, int[] grants) {
            return delegate.apply(code, permissions, grants);
        }

        @Override
        public Iterator<String> iterator() {
            return appeal.iterator();
        }
    }

    private final Context context;
    private final Activity client;
    private final Set<String> permissions = new LinkedHashSet<>();
    private final int code = COUNTER.getAndIncrement();
    private final Runner runner;
    @Nullable private Do.Just<Sensitive> onDeny;

    public AndroidPermit(Context context, Activity client, Runner runner) {
        this.context = context;
        this.client = client;
        this.runner = runner;
    }

    public AndroidPermit(Context context, Activity client) {
        this(context, client, PassThrough.RUNNER);
    }

    public AndroidPermit(Activity activity) {
        this(activity.getApplicationContext(), activity);
    }

    public AndroidPermit(Activity activity, Runner runner) {
        this(activity.getApplicationContext(), activity, runner);
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
            private final Set<String> permaDenied = new LinkedHashSet<>();
            private boolean canRequestNow = false;

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
                return permaDenied;
            }

            @Override
            public void submit() {
                for (Iterator<String> it = permissions.iterator(); it.hasNext();) {
                    if (allowed(it.next())) {
                        it.remove();
                    }
                }
                int n = permissions.size();
                String[] perms = permissions.toArray(new String[n]);
                if (!canRequestNow && onDeny != null) {
                    canRequestNow = true;
                    List<String> primer = new ArrayList<>();
                    for (String p : perms) {
                        if (canAppeal(p)) {
                            primer.add(p);
                        }
                    }
                    if (primer.isEmpty()) {
                        reallySubmit(perms);
                    } else {
                        onDeny.got(new FalseStart(this, primer));
                    }
                } else {
                    reallySubmit(perms);
                }
            }

            @Override
            public boolean apply(int code, String[] permissions, int[] grants) {
                if (AndroidPermit.this.code != code) {
                    return false;
                }
                for (int i = 0; i < permissions.length; i++) {
                    boolean allowed = allowed(grants[i]);
                    String permission = permissions[i];
                    if (allowed || !canAppeal(permission)) {
                        AndroidPermit.this.permissions.remove(permission);
                        if (!allowed) {
                            permaDenied.add(permission);
                        }
                    }
                }
                if (isEmpty() && permaDenied.isEmpty()) {
                    onAllow.got(null);
                } else if (onDeny != null) {
                    onDeny.got(this);
                }
                return true;
            }

            private void reallySubmit(String[] permissions) {
                if (permissions.length > 0) {
                    ActivityCompat.requestPermissions(client, permissions, code);
                } else if (permaDenied.isEmpty()) {
                    onAllow.got(null);
                } else if (onDeny != null) {
                    onDeny.got(this);
                }
            }
        };
    }

    private boolean canAppeal(String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(client, permission);
    }

    private boolean allowed(String permission) {
        return allowed(ActivityCompat.checkSelfPermission(context, permission));
    }

    private static boolean allowed(int result) {
        return result == PackageManager.PERMISSION_GRANTED;
    }

}

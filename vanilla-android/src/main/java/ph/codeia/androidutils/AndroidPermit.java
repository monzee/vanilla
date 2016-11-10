package ph.codeia.androidutils;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import ph.codeia.meta.Experimental;
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

    private final Context context;
    private final Activity client;
    private final Set<String> permissions = new LinkedHashSet<>();
    private final int code = COUNTER.getAndIncrement();
    private Do.Just<Sensitive> onDeny;

    public AndroidPermit(Context context, Activity client) {
        this.context = context;
        this.client = client;
    }

    public AndroidPermit(Activity activity) {
        this(activity.getApplicationContext(), activity);
    }

    @Override
    public Permit ask(String... permissions) {
        for (String permission : permissions) {
            if (!allowed(permission)) {
                this.permissions.add(permission);
            }
        }
        return this;
    }

    @Override
    public Permit denied(Do.Just<Sensitive> block) {
        onDeny = AndroidRunner.UI.run(block);
        return this;
    }

    @Override
    public Sensitive granted(final Runnable block) {
        final Do.Just<Void> onAllow = AndroidRunner.UI.run(new Do.Just<Void>() {
            @Override
            public void got(Void value) {
                block.run();
            }
        });
        return new Sensitive() {
            private final Set<String> permaDenied = new LinkedHashSet<>();
            private boolean shouldRequestNow = false;

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
                int n = permissions.size();
                String[] perms = permissions.toArray(new String[n]);
                if (shouldRequestNow && n > 0) {
                    ActivityCompat.requestPermissions(client, perms, code);
                } else {
                    // route the first call through #apply(...) so that the
                    // app can show the rationale if needed
                    int[] grants = new int[n];
                    Arrays.fill(grants, PermissionChecker.PERMISSION_DENIED);
                    apply(code, perms, grants);
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
                    if (allowed) {
                        AndroidPermit.this.permissions.remove(permission);
                    } else if (shouldRequestNow && !canAppeal(permission)) {
                        AndroidPermit.this.permissions.remove(permission);
                        permaDenied.add(permission);
                    } else if (!shouldRequestNow) {

                    }
                }
                if (permaDenied.isEmpty()) {
                    onAllow.got(null);
                } else if (onDeny != null) {
                    onDeny.got(this);
                }
                return true;
            }
        };
    }

    private boolean canAppeal(String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(client, permission);
    }

    private boolean allowed(String permission) {
        return allowed(PermissionChecker.checkSelfPermission(context, permission));
    }

    private static boolean allowed(int result) {
        switch (result) {
            case PermissionChecker.PERMISSION_GRANTED:
                return true;
            case PermissionChecker.PERMISSION_DENIED:
                return false;
            default:
                throw new IllegalArgumentException("Permission not declared in manifest.");
        }
    }

}

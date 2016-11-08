package ph.codeia.androidutils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.HashSet;
import java.util.Iterator;
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
 */
@Experimental
public class AndroidPermit implements Permit {

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    private final Context context;
    private final Activity client;
    private final Set<String> permissions = new HashSet<>();
    private final int code = COUNTER.getAndIncrement();
    private Do.Just<Void> onAllow;
    private Do.Just<Sensitive> onDeny;
    private int round = 0;

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
    public Permit granted(final Runnable block) {
        onAllow = AndroidRunner.UI.run(new Do.Just<Void>() {
            @Override
            public void got(Void value) {
                block.run();
            }
        });
        return this;
    }

    @Override
    public Permit denied(Do.Just<Sensitive> block) {
        onDeny = AndroidRunner.UI.run(block);
        return this;
    }

    @Override
    public Sensitive action() {
        return new Sensitive() {
            @Override
            public Iterator<String> iterator() {
                return permissions.iterator();
            }

            @Override
            public boolean isEmpty() {
                return permissions.isEmpty();
            }

            @Override
            public boolean includes(String permission) {
                return permissions.contains(permission);
            }

            @Override
            public void submit() {
                if (!isEmpty()) {
                    ActivityCompat.requestPermissions(
                            client,
                            permissions.toArray(new String[0]),
                            code
                    );
                } else if (round == 0) {
                    onAllow.got(null);
                } else {
                    onDeny.got(this);
                }
            }

            @Override
            public boolean decide(int code, String[] permissions, int[] grants) {
                if (AndroidPermit.this.code != code) {
                    return false;
                }
                boolean allGranted = true;
                for (int i = 0; i < permissions.length; i++) {
                    if (allowed(grants[i]) || !canAppeal(permissions[i])) {
                        allGranted = allGranted && allowed(grants[i]);
                        AndroidPermit.this.permissions.remove(permissions[i]);
                    }
                }
                if (!allGranted) {
                    onDeny.got(this);
                    round++;
                }
                return true;
            }
        };
    }

    private boolean canAppeal(String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(client, permission);
    }

    private boolean allowed(String permission) {
        return allowed(ContextCompat.checkSelfPermission(context, permission));
    }

    private static boolean allowed(int result) {
        return result == PackageManager.PERMISSION_GRANTED;
    }

}

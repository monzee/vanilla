package ph.codeia.androidutils;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import ph.codeia.values.Do;
import ph.codeia.values.Store;

/**
 * This file is a part of the vanilla project.
 */

/**
 * A store backed by a {@link LoaderManager} and loaders + callbacks.
 *
 * Values stored here remain valid until the user explicitly leaves the
 * activity/fragment where the manager is scoped or until the process
 * is killed. I.e. they survive configuration changes like device rotation.
 */
public class AndroidLoaderStore implements Store {

    private static class Holder<T> {
        boolean valid = false;
        T value;

        T ensureStarted() {
            if (!valid) {
                throw new IllegalStateException(
                        "Don't use this store before Activity#onStart()."
                );
            }
            return value;
        }
    }

    private final LoaderManager manager;
    private final Context context;

    private AndroidLoaderStore(LoaderManager manager, Context context) {
        this.manager = manager;
        this.context = context.getApplicationContext();
    }

    public AndroidLoaderStore(FragmentActivity activity) {
        this(activity.getSupportLoaderManager(), activity);
    }

    public AndroidLoaderStore(Fragment fragment) {
        this(fragment.getLoaderManager(), fragment.getContext());
    }

    @Override
    public void put(String key, final Object value) {
        int id = id(key);
        manager.destroyLoader(id);
        save(id, value);
    }

    @Override
    public void softPut(String key, Do.Make<Object> lazyValue) {
        int id = id(key);
        if (!exists(id)) {
            save(id, lazyValue.get());
        }
    }

    @Override
    public <T> T get(String key, T defaultValue) {
        int id = id(key);
        if (exists(id)) {
            return get(id, null);
        }
        return defaultValue;
    }

    @Override
    public <T> T hardGet(String key, final Do.Make<T> lazyValue) {
        int id = id(key);
        if (exists(id)) {
            return get(id, null);
        }
        return get(id, lazyValue.get());
    }

    private static int id(String key) {
        return key.hashCode();
    }

    private <T> Loader<T> loader(final T value) {
        return new Loader<T>(context) {
            @Override
            protected void onStartLoading() {
                deliverResult(value);
            }
        };
    }

    private void save(int id, final Object value) {
        manager.initLoader(id, null, new LoaderManager.LoaderCallbacks<Object>() {
            @Override
            public Loader<Object> onCreateLoader(int id, Bundle args) {
                return loader(value);
            }

            @Override
            public void onLoadFinished(Loader<Object> loader, Object data) {}

            @Override
            public void onLoaderReset(Loader<Object> loader) {}
        });
    }

    private <T> T get(int id, final T value) {
        final Holder<T> holder = new Holder<>();
        manager.initLoader(id, null, new LoaderManager.LoaderCallbacks<T>() {
            @Override
            public Loader<T> onCreateLoader(int id, Bundle args) {
                return loader(value);
            }

            @Override
            public void onLoadFinished(Loader<T> loader, T data) {
                holder.valid = true;
                holder.value = data;
            }

            @Override
            public void onLoaderReset(Loader<T> loader) {}
        });
        return holder.ensureStarted();
    }

    private boolean exists(int id) {
        return manager.getLoader(id) != null;
    }

}

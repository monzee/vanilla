package ph.codeia.security;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import ph.codeia.values.Do;

/**
 * This file is a part of the vanilla project.
 *
 * @author mon
 */

/**
 * A wrapper that delegates to an existing {@link Sensitive} instance but
 * yields a different set of permissions.
 *
 * Meant to be passed to a {@link OldPermit#denied(Do.Just)} callback.
 * Specifically in the android implementation, the first submit must obtain
 * a list of appealable permissions and present it to the user, but doing
 * it the normal way would require me to track more state that isn't really
 * necessary in the subsequent submits. I could see this being useful in non-
 * android contexts too that's why I put it in the core library.
 */
@Deprecated
public class Synthetic implements Sensitive {

    private final Sensitive delegate;
    private final Collection<String> permissions;

    public Synthetic(Sensitive delegate, Collection<String> permissions) {
        this.delegate = delegate;
        this.permissions = permissions;
    }

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
        return delegate.banned();
    }

    @Override
    public void submit() {
        delegate.submit();
    }

    @Override
    public boolean check(int code, String[] permissions, int[] grants) {
        return delegate.check(code, permissions, grants);
    }

    @Override
    public void dispatch() {
        delegate.dispatch();
    }
}

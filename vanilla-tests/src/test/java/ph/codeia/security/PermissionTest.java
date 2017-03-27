package ph.codeia.security;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;


/**
 * This file is a part of the vanilla project.
 */

public class PermissionTest {

    static Permission.Adapter adapter(List<String> allowed, List<String> appealable) {
        return new Permission.Adapter() {
            @Override
            public boolean isGranted(int result) {
                return result > 0;
            }

            @Override
            public boolean isGranted(String permission) {
                return allowed.contains(permission);
            }

            @Override
            public boolean isAppealable(String permission) {
                return appealable.contains(permission);
            }
        };
    }

    static final List<String> EMPTY = Collections.emptyList();

    @Test
    public void permissions_are_correctly_initialized() {
        Permission.Adapter a = adapter(
                Arrays.asList("foo", "bar", "baz"),
                Arrays.asList("bat", "quux"));
        Permission p;

        p = new Permission(Arrays.asList("foo", "bar", "baz"), a);
        assertTrue(p.allGranted());
        assertFalse(p.someAppealable());

        p = new Permission(Arrays.asList("foo", "the", "quick", "quux"), a);
        assertFalse(p.allGranted());
        assertTrue(p.someAppealable());
    }

    @Test
    public void granted_permissions_are_excluded_after_folding() {
        Permission.Adapter a = adapter(EMPTY, EMPTY);
        Permission p = new Permission(Collections.singletonList("foo"), a);
        p = p.fold(new String[] {"foo"}, new int[] {1});
        assertTrue(p.allGranted());
        assertFalse(p.someAppealable());
    }

    @Test
    public void ungranted_appealable_permissions_are_appealable_after_folding() {
        Permission.Adapter a = adapter(EMPTY, Collections.singletonList("foo"));
        Permission p = new Permission(Collections.singletonList("foo"), a);
        p = p.fold(new String[] {"foo"}, new int[] {0});
        assertFalse(p.allGranted());
        assertTrue(p.someAppealable());
    }

    @Test
    public void ungranted_unappealable_permissions_are_autodenied_after_folding() {
        Permission.Adapter a = adapter(EMPTY, EMPTY);
        Permission p = new Permission(Collections.singletonList("foo"), a);
        p = p.fold(new String[] {"foo"}, new int[] {0});
        assertFalse(p.allGranted());
        assertFalse(p.someAppealable());
    }

    @Test
    public void refresh_syncs_an_old_permission_object_with_the_adapter_state() {
        List<String> allowed = new ArrayList<>();
        List<String> appealable = new ArrayList<>();
        Permission.Adapter a = adapter(allowed, appealable);
        Permission p = new Permission(Arrays.asList("foo", "bar"), a);

        appealable.add("foo");
        assertFalse(p.someAppealable());
        p.refresh();
        assertTrue(p.someAppealable());
        appealable.remove("foo");
        p.refresh();
        assertFalse(p.someAppealable());
        allowed.add("foo");
        allowed.add("bar");
        assertFalse(p.allGranted());
        p.refresh();
        assertTrue(p.allGranted());
    }
}

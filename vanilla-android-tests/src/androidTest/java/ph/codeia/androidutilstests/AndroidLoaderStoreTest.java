package ph.codeia.androidutilstests;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.pm.ActivityInfo;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import ph.codeia.androidutils.AndroidLoaderStore;
import ph.codeia.signal.Channel;
import ph.codeia.values.Store;

import static org.junit.Assert.*;

/**
 * This file is a part of the vanilla project.
 *
 * @author mon
 */

@RunWith(AndroidJUnit4.class)
@MediumTest
public class AndroidLoaderStoreTest {

    @Rule
    public ActivityTestRule<InterFragmentActivity> rule =
            new ActivityTestRule<>(InterFragmentActivity.class, true, false);

    private Instrumentation.ActivityMonitor monitor;

    @Before
    public void setup() {
        Instrumentation instr = InstrumentationRegistry.getInstrumentation();
        monitor = new Instrumentation.ActivityMonitor(
                InterFragmentActivity.class.getCanonicalName(), null, false);
        instr.addMonitor(monitor);
    }

    @After
    public void tearDown() {
        monitor.getLastActivity().finish();
    }

    @Test(timeout = 3000)
    public void instance_is_retained_after_rotation() throws Throwable {
        BlockingQueue<Boolean> ready = new LinkedBlockingDeque<>();
        Channel.Link link = InterFragmentActivity.READY.link(ready::add);

        InterFragmentActivity activity = rule.launchActivity(null);
        assertTrue(ready.take());

        Store store = new AndroidLoaderStore(activity);
        Object foo = new Object();
        store.put("foo", foo);

        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        assertTrue(ready.take());

        Activity nextActivity = monitor.getLastActivity();
        assertNotSame(activity, nextActivity);
        store = new AndroidLoaderStore((InterFragmentActivity) nextActivity);
        assertNotNull(store.get("foo", null));
        assertSame(foo, store.get("foo", null));

        link.unlink();
        activity.finish();
    }

    @Test
    public void hardGet_puts_an_instance_when_absent() {
        InterFragmentActivity activity = rule.launchActivity(null);
        Store store = new AndroidLoaderStore(activity);
        assertNull(store.get("foo", null));
        Object foo = new Object();
        store.hardGet("foo", () -> foo);
        assertSame(foo, store.get("foo", null));
    }

    @Test(expected = AssertionError.class)
    public void get_does_not_modify_the_store() {
        InterFragmentActivity activity = rule.launchActivity(null);
        Store store = new AndroidLoaderStore(activity);
        assertNull(store.get("foo", null));
        Object foo = new Object();
        assertSame(foo, store.get("foo", foo));
        store.hardGet("foo", Store.Presence.expected());
    }

    @Test(expected = AssertionError.class)
    public void clear_removes_the_instance() {
        InterFragmentActivity activity = rule.launchActivity(null);
        Store store = new AndroidLoaderStore(activity);
        Object foo = new Object();
        assertSame(foo, store.hardGet("foo", () -> foo));
        store.clear("foo");
        store.hardGet("foo", Store.Presence.expected());
    }

    @Test
    public void softPut_does_not_replace_existing_instance() {
        InterFragmentActivity activity = rule.launchActivity(null);
        Store store = new AndroidLoaderStore(activity);
        Object foo = new Object();
        Object bar = new Object();
        store.put("foo", foo);
        assertSame(foo, store.get("foo", null));
        store.softPut("foo", () -> bar);
        assertNotSame(bar, store.get("foo", null));
    }

}

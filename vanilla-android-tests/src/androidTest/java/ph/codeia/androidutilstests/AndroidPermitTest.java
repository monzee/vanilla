package ph.codeia.androidutilstests;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ph.codeia.signal.Channel;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static org.hamcrest.CoreMatchers.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AndroidPermitTest {

    private static final String PACKAGE = "ph.codeia.androidutilstests";

    private ActivityTestRule<PermissionsActivity> rule =
            new ActivityTestRule<>(PermissionsActivity.class);

    @Rule
    public RuleChain rules = RuleChain
            .outerRule(Timeout.seconds(10))
            .around(rule);

    private Context context;
    private Instrumentation instrumentation;
    private UiDevice phone;

    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        phone = UiDevice.getInstance(instrumentation);
        context = InstrumentationRegistry.getTargetContext();
        resetPermissions();
    }

    @After
    public void tearDown() {
        PermissionsActivity.GRANTED.unlinkAll();
        PermissionsActivity.APPEAL.unlinkAll();
        PermissionsActivity.DENIED.unlinkAll();
    }

    @Test
    public void immediately_runs_granted_action_when_previously_granted() throws UiObjectNotFoundException, InterruptedException {
        BlockingQueue<String> granted = observe(PermissionsActivity.GRANTED);
        String permission = Manifest.permission.ACCESS_COARSE_LOCATION;
        preGrant("grant", permission);
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("Coarse location")).perform(click());
        assertThat(granted.take(), is(permission));
    }

    @Test
    public void granted_action_proceeds_when_allowed() throws UiObjectNotFoundException, InterruptedException {
        BlockingQueue<String> granted = observe(PermissionsActivity.GRANTED);
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("Coarse location")).perform(click());
        checkExternalViewWithText("Allow").click();
        assertThat(granted.take(), is(Manifest.permission.ACCESS_COARSE_LOCATION));
    }

    @Test
    public void can_show_rationale_when_denied() throws UiObjectNotFoundException, InterruptedException {
        BlockingQueue<String> appeal = observe(PermissionsActivity.APPEAL);
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("Coarse location")).perform(click());
        checkExternalViewWithText("Deny").click();
        assertThat(appeal.take(), is(Manifest.permission.ACCESS_COARSE_LOCATION));
        Espresso.pressBack();
    }

    @Test
    public void can_show_rationale_when_denied_many_times() throws UiObjectNotFoundException, InterruptedException {
        BlockingQueue<String> appeal = observe(PermissionsActivity.APPEAL);
        int n = 3;
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("Coarse location")).perform(click());
        while (n --> 0) {
            checkExternalViewWithText("Deny").click();
            assertThat(appeal.take(), is(Manifest.permission.ACCESS_COARSE_LOCATION));
            onView(withText("Ask me again")).perform(click());
        }
        checkExternalViewWithText("Allow").click();
    }

    @Test
    public void does_not_ask_for_permission_belonging_to_a_group_previously_allowed() throws UiObjectNotFoundException, InterruptedException {
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("Coarse location")).perform(click());
        checkExternalViewWithText("Allow").click();
        BlockingQueue<String> granted = observe(PermissionsActivity.GRANTED);
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("Fine location")).perform(click());
        assertThat(granted.take(), is(Manifest.permission.ACCESS_FINE_LOCATION));
    }

    @Test
    public void permanent_deny() throws UiObjectNotFoundException, InterruptedException {
        BlockingQueue<String> banned = observe(PermissionsActivity.DENIED);
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("Read contacts")).perform(click());
        checkExternalViewWithText("Deny").click();
        onView(withText("Ask me again")).perform(click());
        checkExternalViewWithText("Never ask again").click();
        checkExternalViewWithText("Deny").click();
        assertThat(banned.take(), is(Manifest.permission.READ_CONTACTS));
    }

    @Test
    public void permission_set_all_allowed() throws UiObjectNotFoundException, InterruptedException {
        BlockingQueue<String> ok = observe(PermissionsActivity.GRANTED);
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("2-in-1")).perform(click());
        checkExternalViewWithText("Allow").click();
        checkExternalViewWithText("Allow").click();
        assertThat(ok.take(), is(Manifest.permission.ACCESS_COARSE_LOCATION));
        assertThat(ok.take(), is(Manifest.permission.READ_CONTACTS));
    }

    @Test
    public void permission_set_one_permanently_denied() throws UiObjectNotFoundException, InterruptedException {
        BlockingQueue<String> banned = observe(PermissionsActivity.DENIED);
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("2-in-1")).perform(click());
        checkExternalViewWithText("Allow").click();
        checkExternalViewWithText("Deny").click();
        onView(withText("Ask me again")).perform(click());
        checkExternalViewWithText("Never ask again").click();
        checkExternalViewWithText("Deny").click();
        assertThat(banned.take(), is(Manifest.permission.READ_CONTACTS));
    }

    private void preGrant(String action, String permission) {
        try {
            instrumentation.getUiAutomation().executeShellCommand("pm "
                    + action + " "
                    + PACKAGE + " "
                    + permission).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetPermissions() {
        try {
            instrumentation.getUiAutomation().executeShellCommand("pm reset-permissions").close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private <T> BlockingQueue<T> observe(Channel<T> channel) {
        BlockingQueue<T> values = new LinkedBlockingQueue<>();
        channel.link(values::add);
        return values;
    }

    private UiObject checkExternalViewWithText(String text) {
        UiObject view = phone.findObject(new UiSelector().text(text));
        assertThat(view.exists(), is(true));
        return view;
    }

}

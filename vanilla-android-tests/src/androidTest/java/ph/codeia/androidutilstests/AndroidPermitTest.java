package ph.codeia.androidutilstests;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.filters.FlakyTest;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.text.TextUtils;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ph.codeia.security.Sensitive;
import ph.codeia.signal.Channel;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.action.ViewActions.*;
import static org.hamcrest.CoreMatchers.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AndroidPermitTest {

    private static final String PACKAGE = "ph.codeia.androidutilstests";

    private final ActivityTestRule<PermissionsActivity> activityRule =
            new ActivityTestRule<>(PermissionsActivity.class);

    @Rule
    public RuleChain rules = RuleChain
            .outerRule(Timeout.seconds(10))
            .around(activityRule);

    private Context context;
    private Instrumentation instrumentation;
    private UiDevice phone;

    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        phone = UiDevice.getInstance(instrumentation);
        context = InstrumentationRegistry.getTargetContext();
    }

    @After
    public void tearDown() {
        exec("pm reset-permissions");
        PermissionsActivity.COARSE.unlinkAll();
        PermissionsActivity.FINE.unlinkAll();
        PermissionsActivity.CONTACTS.unlinkAll();
        PermissionsActivity.EDGE_CASE.unlinkAll();
    }

    @Test
    public void immediately_runs_granted_action_when_previously_granted() throws UiObjectNotFoundException, InterruptedException {
        BlockingQueue<String> coarse = observe(PermissionsActivity.COARSE);
        exec("pm grant", PACKAGE, Manifest.permission.ACCESS_COARSE_LOCATION);
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("Coarse location")).perform(click());
        assertThat(coarse.take(), is("granted"));
    }

    @Test
    public void granted_action_proceeds_when_allowed() throws UiObjectNotFoundException, InterruptedException {
        BlockingQueue<String> coarse = observe(PermissionsActivity.COARSE);
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("Coarse location")).perform(click());
        checkExternalViewWithText("Allow").click();
        assertThat(coarse.take(), is("granted"));
    }

    @Test
    public void can_show_rationale_when_denied() throws UiObjectNotFoundException, InterruptedException {
        BlockingQueue<String> coarse = observe(PermissionsActivity.COARSE);
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("Coarse location")).perform(click());
        checkExternalViewWithText("Deny").click();
        assertThat(coarse.take(), is("appeal"));
        Espresso.pressBack();
    }

    @Test
    public void can_show_rationale_when_denied_many_times() throws UiObjectNotFoundException, InterruptedException {
        BlockingQueue<String> coarse = observe(PermissionsActivity.COARSE);
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("Coarse location")).perform(click());
        for (int n = 3; n --> 0;) {
            checkExternalViewWithText("Deny").click();
            assertThat(coarse.take(), is("appeal"));
            onView(withText("Ask me again")).perform(click());
        }
        checkExternalViewWithText("Allow").click();
    }

    @Test
    public void does_not_ask_for_permission_belonging_to_a_group_previously_allowed() throws UiObjectNotFoundException, InterruptedException {
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("Coarse location")).perform(click());
        checkExternalViewWithText("Allow").click();
        BlockingQueue<String> fine = observe(PermissionsActivity.FINE);
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("Fine location")).perform(click());
        assertThat(fine.take(), is("granted"));
    }

    @Test
    @FlakyTest
    public void permanent_deny() throws UiObjectNotFoundException, InterruptedException {
        BlockingQueue<String> contacts = observe(PermissionsActivity.CONTACTS);
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("Read contacts")).perform(click());
        checkExternalViewWithText("Deny").click();  // this randomly fails for some reason
        assertThat(contacts.take(), is("appeal"));
        onView(withText("Ask me again")).perform(click());
        checkExternalViewWithText("Never ask again").click();
        checkExternalViewWithText("Deny").click();  // between the two denies i'd expect this one
                                                    // to randomly fail but it never does.
        assertThat(contacts.take(), is("denied"));
    }

    @Test
    public void permission_set_all_allowed() throws UiObjectNotFoundException, InterruptedException {
        BlockingQueue<String> coarse = observe(PermissionsActivity.COARSE);
        BlockingQueue<String> contacts = observe(PermissionsActivity.CONTACTS);
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("2-in-1")).perform(click());
        checkExternalViewWithText("Allow").click();
        checkExternalViewWithText("Allow").click();
        assertThat(coarse.take(), is("granted"));
        assertThat(contacts.take(), is("granted"));
    }

    @Test
    public void permission_set_one_permanently_denied() throws UiObjectNotFoundException, InterruptedException {
        BlockingQueue<String> contacts = observe(PermissionsActivity.CONTACTS);
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText("2-in-1")).perform(click());
        checkExternalViewWithText("Allow").click();
        checkExternalViewWithText("Deny").click();
        assertThat(contacts.take(), is("appeal"));
        onView(withText("Ask me again")).perform(click());
        checkExternalViewWithText("Never ask again").click();
        checkExternalViewWithText("Deny").click();
        assertThat(contacts.take(), is("denied"));
    }

    @Test
    public void undeclared_permissions_are_automatically_permanently_denied() throws InterruptedException {
        BlockingQueue<String> edgeCase = observe(PermissionsActivity.EDGE_CASE);
        PermissionsActivity activity = activityRule.getActivity();
        Sensitive invalid = activity.ask(Manifest.permission.CAMERA)
                .granted(() -> Assert.fail("should be unreachable"));
        activity.requests.add(invalid);
        invalid.submit();
        assertThat(edgeCase.take(), is("denied " + Manifest.permission.CAMERA));
    }

    private void exec(String... parts) {
        try {
            instrumentation.getUiAutomation()
                    .executeShellCommand(TextUtils.join(" ", parts))
                    .close();
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

package ph.codeia.androidutilstests;

import android.app.Instrumentation;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;
import android.text.TextUtils;

import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ph.codeia.signal.Channel;

import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.is;

//@RunWith(AndroidJUnit4.class)
//@LargeTest
public class AndroidPermitTest {

    private static final String PACKAGE = "ph.codeia.androidutilstests";

    private final ActivityTestRule<PermissionsActivity> activityRule =
            new ActivityTestRule<>(PermissionsActivity.class);

    //@Rule
    public RuleChain rules = RuleChain
            .outerRule(Timeout.seconds(10))
            .around(activityRule);

    private Context context;
    private Instrumentation instrumentation;
    private UiDevice phone;

    //@Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        phone = UiDevice.getInstance(instrumentation);
        context = InstrumentationRegistry.getTargetContext();
    }

    //@After
    public void tearDown() {
        exec("pm reset-permissions");
        PermissionsActivity.COARSE.unlinkAll();
        PermissionsActivity.FINE.unlinkAll();
        PermissionsActivity.CONTACTS.unlinkAll();
        PermissionsActivity.EDGE_CASE.unlinkAll();
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

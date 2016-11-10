package ph.codeia.androidutilstests;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class AndroidPermissionsTest {

    @Rule
    public ActivityTestRule<PermissionsActivity> rule =
            new ActivityTestRule<>(PermissionsActivity.class);

    @Test
    public void launches_activity() {
        onView(withText("Hello World!")).check(matches(isDisplayed()));
    }

    @Test
    public void show_menu() {
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
        onView(withText("hello")).check(matches(isDisplayed()));
    }
}

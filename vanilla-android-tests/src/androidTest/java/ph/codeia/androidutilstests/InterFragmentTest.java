package ph.codeia.androidutilstests;

import android.content.pm.ActivityInfo;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * This file is a part of the vanilla project.
 *
 * @author mon
 */

@RunWith(AndroidJUnit4.class)
@MediumTest
public class InterFragmentTest {

    private final ActivityTestRule<InterFragmentActivity> testRule =
            new ActivityTestRule<>(InterFragmentActivity.class);

    @Rule
    public RuleChain rule = RuleChain.outerRule(Timeout.seconds(5))
            .around(testRule);

    @Test
    public void sibling_fragments_can_communicate() {
        press("1");
        press("2");
        press("3");
        onView(withText("123")).check(matches(isDisplayed()));
    }

    @Test
    public void fragment_can_communicate_with_parent_activity() {
        press("±");
        onView(withText(containsString("negative zero"))).check(matches(isDisplayed()));
        press("0");
        onView(withText(containsString("insignificant"))).check(matches(isDisplayed()));
        press(".");
        press(".");
        onView(withText(containsString("decimal point"))).check(matches(isDisplayed()));
    }

    @Test
    public void replay_last_sent_value_after_rotate() {
        press("±");
        testRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        onView(withText(containsString("negative"))).check(matches(isDisplayed()));
    }

    @Test
    public void state_is_restored_after_rotate() {
        press("1");
        press("2");
        press(".");
        press("9");
        press("±");
        press("6");
        testRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        onView(withText("-12.96")).check(matches(isDisplayed()));
    }

    private void press(String buttonText) {
        onView(allOf(withText(buttonText), isClickable())).perform(click());
    }

}

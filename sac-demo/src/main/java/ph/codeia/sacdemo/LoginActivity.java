package ph.codeia.sacdemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ph.codeia.androidutils.AndroidMachine;
import ph.codeia.androidutils.AndroidPermit;
import ph.codeia.arch.sm.Machine;
import ph.codeia.arch.sm.RootState;
import ph.codeia.arch.sm.Sm;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    static class State extends RootState<State, Action> {
        enum Contacts { UNKNOWN, LOADING, LOADED, }
        enum Login { UNKNOWN, LOGGING_IN, LOGGED_IN, FAILED, }

        Contacts contacts = Contacts.UNKNOWN;
        Login login = Login.UNKNOWN;
        List<String> emails = Collections.emptyList();
        int loginAttempts = 0;
    }

    interface Action extends Sm.Action<State, Action, LoginActivity> {
        Action NOOP = (m, v) -> m;

        Action LOAD_CONTACTS = (m, v) -> {
            switch (m.contacts) {
                case UNKNOWN:
                    v.populateAutoComplete();
                    break;
                case LOADING:
                    break;
                case LOADED:
                    v.addEmailsToAutoComplete(m.emails);
                    break;
            }
            return m;
        };

        Action START_LOADING = (m, v) -> {
            m.contacts = State.Contacts.LOADING;
            v.log("loading");
            ContentResolver resolver = v.getContentResolver();
            return m.async(() -> {
                Cursor cursor = resolver.query(
                        // Retrieve data rows for the device user's 'profile' contact.
                        Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                                ContactsContract.Contacts.Data.CONTENT_DIRECTORY),
                        ProfileQuery.PROJECTION,

                        // Select only email addresses.
                        ContactsContract.Contacts.Data.MIMETYPE + " = ?",
                        new String[] { ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE },

                        // Show primary email addresses first. Note that there won't be
                        // a primary email address if the user hasn't specified one.
                        ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");

                return (futureM, futureV) -> {
                    if (cursor == null) {
                        futureM.contacts = State.Contacts.UNKNOWN;
                        futureV.log("got a null cursor");
                        return futureM;
                    }
                    futureV.log("got rows: %d", cursor.getCount());
                    List<String> emails = new ArrayList<>();
                    for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                        emails.add(cursor.getString(ProfileQuery.ADDRESS));
                    }
                    futureM.emails = emails;
                    futureM.contacts = State.Contacts.LOADED;
                    cursor.close();
                    return futureM.plus(LOAD_CONTACTS);
                };
            });
        };

        Action CONTINUE_LOGIN = (m, v) -> {
            switch (m.login) {
                case UNKNOWN:
                    v.showProgress(false);
                    break;
                case LOGGING_IN:
                    v.showProgress(true);
                    break;
                case LOGGED_IN:
                    // next screen?
                    m.login = State.Login.UNKNOWN;
                    v.showProgress(false);
                    v.finish();
                    break;
                case FAILED:
                    // wrong password; ask to register?
                    v.tell(R.string.error_incorrect_password);
                    v.showProgress(false);
                    break;
            }
            return m;
        };

        Action LOGIN_CANCELLED = (m, v) -> {
            m.login = State.Login.UNKNOWN;
            return m.plus(CONTINUE_LOGIN);
        };

        Action LOGIN_OK = (m, v) -> {
            m.login = State.Login.LOGGED_IN;
            return m.plus(CONTINUE_LOGIN);
        };

        Action LOGIN_FAILED = (m, v) -> {
            m.login = State.Login.FAILED;
            return m.plus(CONTINUE_LOGIN);
        };

        Action TRY_LOGIN = (m, v) -> {
            m.loginAttempts++;
            m.login = State.Login.LOGGING_IN;
            String email = v.mEmailView.getText().toString();
            String password = v.mPasswordView.getText().toString();
            v.showProgress(true);
            return m.async(() -> {
                try {
                    // Simulate network access.
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    return LOGIN_CANCELLED;
                }

                for (String credential : DUMMY_CREDENTIALS) {
                    String[] pieces = credential.split(":");
                    if (pieces[0].equals(email)) {
                        // Account exists, return true if the password matches.
                        if (pieces[1].equals(password)) {
                            return LOGIN_OK;
                        }
                        break;
                    }
                }

                return LOGIN_FAILED;
            });
        };

        Action LOGIN_IF_VALID = (m, v) -> {
            switch (m.login) {
                case LOGGING_IN:
                    return m;
                default:
                    return v.attemptLogin() ? m.plus(TRY_LOGIN) : m;
            }
        };

    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };

    private State model = new State();
    private Machine.Bound<State, Action, LoginActivity> machine;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        State saved = (State) getLastCustomNonConfigurationInstance();
        if (saved != null) {
            model = saved;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == R.id.login || id == EditorInfo.IME_NULL) {
                machine.apply(Action.LOGIN_IF_VALID);
                return true;
            }
            return false;
        });

        findViewById(R.id.email_sign_in_button)
                .setOnClickListener(_v -> machine.apply(Action.LOGIN_IF_VALID));

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        machine = new AndroidMachine.Builder<>(model).build(AsyncTask.SERIAL_EXECUTOR, this);
        machine.apply(Action.LOAD_CONTACTS);
        machine.apply(Action.CONTINUE_LOGIN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        machine.start(Action.NOOP);
    }

    @Override
    protected void onPause() {
        super.onPause();
        machine.stop();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return model;
    }

    private void log(String message, Object... fmtArgs) {
        Log.i("mz", String.format(message, fmtArgs));
    }

    private void tell(@StringRes int message, Object... fmtArgs) {
        tell(getString(message, fmtArgs));
    }

    private void tell(String message, Object... fmtArgs) {
        Snackbar.make(mEmailView, String.format(message, fmtArgs), Snackbar.LENGTH_SHORT)
                .show();
    }

    private void populateAutoComplete() {
        AndroidPermit.of(this)
                .ask(READ_CONTACTS)
                .before(appeal -> Snackbar
                        .make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok, _v -> appeal.submit())
                        .show())
                .granted(() -> machine.apply(Action.START_LOADING))
                .submit();
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private boolean attemptLogin() {
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
            return false;
        } else {
            return true;
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_longAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }

}

# Vanilla [![Build Status](https://travis-ci.org/monzee/vanilla.svg?branch=master)](https://travis-ci.org/monzee/vanilla) [![Download](https://api.bintray.com/packages/monzee/jvm/vanilla-android/images/download.svg)](https://bintray.com/monzee/jvm/vanilla-android/_latestVersion)

A small library of little things I do all the time in android. Maybe you'll find
them useful too.

## Usage

Here's a brief demonstration of the common use cases. All of these components
implement generic, Android platform-free interfaces that might be used to
abstract concrete android code into junit-testable bits.


### AndroidChannel

An implementation of the observer pattern. First, create a channel.

```java
private final Channel<String> status = new AndroidChannel<>();
```

Attach a listener using `#link(T -> ())`. Save the `Link` reference so that it can be detached later
if needed.

```java
private Channel.Link link;

@Override protected void onResume() {
  // ...
  link = status.link(statusView::setText);
}

@Override protected void onPause() {
  // ...
  link.unlink();
}

@Override protected void onDestroy() {
  // ...
  // it is also possible to unlink all listeners at once from the channel if
  // they are all over the place
  status.unlinkAll();
}
```

The listeners will be called in the UI thread when `#send(T)` is called.

```java
// in a button listener somewhere
status.send("Button was clicked.");
```

This becomes a lot more useful when the message sender and listeners are in different
places. E.g. the listener is in an activity and the child fragments send events
to it, or fragment to other fragments. The next component makes this possible.


### AndroidLoaderStore

A cache implementation that is backed by a `LoaderManager` and dynamically
created pairs of synchronous `Loader`s and `LoaderManager.LoaderCallback`s. Objects
stored here will survive configuration changes with no extra effort.

```java
public static final String TAG = "TheActivity";
public static final String CHANNEL_KEY = "from-child-to-activity";

private Channel<String> onChildEvent;

@Override protected void onResume() {
  // ...
  // This should not be used before Activity#onStart
  Store cache = new AndroidLoaderStore(this);

  // hardGet() initializes the stored value if it's not found. Later calls to
  // get() and hardGet() will return the same instance returned by the factory
  onChildEvent = cache.hardGet(CHANNEL_KEY, AndroidChannel::new);
  onChildEvent.link(s -> Log.d(TAG, s));
}

@Override protected void onPause() {
  // ...
  onChildEvent.unlinkAll();
}

// fragment launch code somewhere
```

The fragment and activity can share the same store backend by scoping the
fragment store to the host activity:

```java
// fragment class
private Channel<String> toActivity;

@Override protected void onResume() {
  // ...
  Store cache = new AndroidLoaderStore(getActivity());

  // It can be hard to tell sometimes if this or the activity method will
  // be called first. By calling the cache in the exact same way in both places,
  // it doesn't matter which one was called first. They are guaranteed to
  // receive the same instance.
  toActivity = cache.hardGet(TheActivity.CHANNEL_KEY, AndroidChannel::new);
}

// some event will eventually call this method
private void somethingHappened() {
  toActivity.send("what's going on");
}
```

You can put any object in the store. There's no need to implement `Parcelable`
or `Serializable`.


### AndroidPermit

Android Marshmallow introduced a new security model that requires apps to ask
permission from the user during runtime and not during installation.

There are two ways to ask permissions: the sloppy way and the proper way. Both
involve getting a reference to `AndroidPermit.Helper`, a headless fragment that
stores all permission sets being requested by an activity or fragment and
dispatches the appropriate callback depending on the user response.

```java
// You should never instantiate the helper fragment directly. Always use the
// static factory. You can call it anytime after #onStart()
AndroidPermit.Helper permits = AndroidPermit.of(this);
```

#### The sloppy way

You can simply ask permission just before you call the priviledged method:

```java
// We're in a button click handler somewhere.

permits
    // The #ask() method can take a variable number of string arguments.
    .ask(Manifest.permission.READ_CONTACTS)

    // Called after #submit() when there's at least one permission that was
    // previously 'soft-denied' by the user. The idea here is that the app
    // would explain to the user at this point why the permissions are needed
    // and to give the user another chance to accept (or reject) your request.
    .before(appeal -> {
      // You must call appeal#submit() at some point here, otherwise it's just
      // like your permissions are permanently denied. Your granted callback
      // will never be called. That said, you shouldn't unconditionally call
      // appeal#submit(). It must be called as a result of some user input, e.g.
      // a button press in a dialog or snackbar.
      showDialog(
          "Permissions",
          "Please allow me to do\n" + TextUtils.join("\n- ", appeal.permissions()),
          appeal::submit);
    })

    // Called after #onRequestPermissionsResult when at least one of the
    // permissions you asked were denied.
    .after(response -> {
      // response#denied() returns the set of appealable permissions.
      // response#rejected() returns the set of permanently denied permissions.
      tell("not much you can do about that you guys");
    })

    // Called when every permission in the set is granted by the user
    .granted(this::displayContacts)

    // Check the permission status and call either the denied or granted
    // callback.
    .submit();
```

That's it. You don't have to override `#onRequestPermissionsResult` or annotate
anything. You don't need to track the request codes. The helper fragment does it
all for you.

This is sloppy because when the phone is rotated while the permission
dialog is visible, the dialog will remain visible and the response will take
effect, but the callbacks would be lost. The user would have to redo the action
that triggered the permission request in the first place.

#### The proper way

The proper way to request permissions is to declare them early on, like
during `#onCreate`. Note that you only have to _declare_ them early, not submit.
This way, the callbacks are restored after rotation and the fragment can invoke
one of them when the permissions dialog is dismissed.

```java
// We're in #onCreate(Bundle)

// this Permit object must be declared as member variable
displayContactsRequest = permits
    .ask(Manifest.permission.READ_CONTACTS)
    .before(Permissions.Appeal::submit)
    // #before() and #after() are optional, by the way.
    // The default #before() autosubmits the appeal like here. Default #after()
    // does nothing.
    .granted(this::displayContacts);
```
> There's no guarantee that the generated integer codes would match. If all
> permits are declared in the same place, there's a good chance they would. If
> you find that they don't match, you can specify an id by calling `#ask` with
> an integer argument (e.g. `ask(123, PERMISSION_1, PERMISSION_2)`)

You then call `#submit` on this object in a UI event handler:

```java
// In a click handler
displayContactsRequest.submit();
```

This has the disadvantage of needing to declare fields for every permission set
and having the action declared far away from where it actually happens. I am not
a big fan of that, that's why this is presented merely as an option. The
permission dialog appears so infrequently that I think it's often times fine to
use the sloppy style.


## Installation

This library is published at jcenter.

```gradle
dependencies {
    // ...
    compile "ph.codeia.vanilla:vanilla-android:0.3.3"
}
```

Retrolambda is highly recommended.

```gradle
plugins {
    id "me.tatarka.retrolambda" version "3.6.0"
}
```


### Hacking

The repository does not include a root settings file so you might not be able to
import this project properly into Android Studio nor run any of the gradle tasks
in the cli. Just copy `settings-travis-ci.gradle` into `settings.gradle` then
stick `, ':vanilla-android'` at the end of the `include`. The reason I have
excluded the settings is because I have a bunch of modules and other garbage
in the directory where I'm writing this that I do not want to publish. Maybe
I'll clean it up in the future.


## License

```
MIT License

Copyright (c) 2016-17 Mon Zafra

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```


# Vanilla [![Build Status](https://travis-ci.org/monzee/vanilla.svg?branch=master)](https://travis-ci.org/monzee/vanilla) [![Download](https://api.bintray.com/packages/monzee/jvm/vanilla-android/images/download.svg)](https://bintray.com/monzee/jvm/vanilla-android/_latestVersion)

A small library of little things I do all the time in android. Maybe you'll find
them useful too.

## Usage

Here's a brief demonstration of the common use cases. All of these components
implement generic, android platform-free interfaces that might be used to
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
to it, or fragment to child fragments. The next component makes this possible.


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


### AndroidRunner

Somewhat like an `Executor` but runs functions that take and return values rather than
plain `Runnable`s and `Callable`s. `AndroidChannel` uses `AndroidRunner.UI` to
run the listeners in a main looper handler. It also offers additional lazy static async
runners that calls a function in a background thread and executes the
continuation in the UI thread.

```java
// - This uses AsyncTask's static thread pool executor. The other async runner is
//   called AndroidRunner.ASYNC_SERIAL and enqueues the background tasks in one
//   thread.
// - This is a Lazy<T> object. It will only be created once. Later calls will
//   return the same instance. Same with ASYNC_SERIAL.
AndroidRunner.ASYNC_POOL.get()
    .<String>apply(next -> {
      // this function will be called in the background. feel free to block the thread.
      String result = longRunningTask();
      // uses CPS. Do not return the result, call the continuation with it.
      next.got(result);
    })
    .begin(result -> {
      // this will be called in the UI thread
      view.show(result);
    });
```

It's basically a more abstract `AsyncTask`. I use its base interface in my
presenters/use cases and pass synchronous runners in my tests. It is possible to
do some interesting things like joining parallel calls, memoization, looping or
even jumping to labelled blocks. They will be documented once the interface has
solidified.


### AndroidPermit

Declare your code that requires certain permissions. This has to be done early
and unconditionally because it is possible for the activity to be killed and
recreated before you actually receive the grants.

```java
private Sensitive accessLocation;

@Override protected void onStart() {
  // ...
  accessLocation = new AndroidPermit(this)
      .ask(Manifest.permission.ACCESS_FINE_LOCATION /* add more here, it's variadic */)
      // or you can call #ask(String...) again

      .denied(appeal -> {
        if (!appeal.isEmpty()) {

          // show rationale. you can iterate over the appeal object to get all
          // the denied permissions, or call #contains(String) to query if the
          // appeal includes a specific permission.

          // this is just an example. you can do whatever you want as long as
          // you don't unconditionally call #submit() because that would be
          // very annoying to the user.
          explain(""
              + "I need these permissions to proceed: "
              + TextUtils.join(", ", appeal),

              // ask permission again only when the dialog ok button is hit.
              // we give up if the dialog was dismissed any other way.
              appeal::submit);
        } else {

          // at least one permission was permanently denied. call #banned() to
          // get them as a set.

          // depending on your use case, you might be able to proceed even
          // with a partial grant. here we just show a toast saying we
          // cannot proceed.
          tell("go to your device settings if you changed your mind.");
        }
      })

      // the actual action
      .granted(this::displayLocation);
}

private void displayLocation() {
  // you can get the location now.
}

private void tell(String message) {
  // Toast.make etc etc show()
}

private void explain(String message, Runnable ok) {
  new AlertDialog.Builder(this)
      .setTitle("Please?")
      .setMessage(message)
      .setPositiveButton("Ask me again", (dialog, id) -> ok.run())
      .create()
      .show();
}
```

Submit the grant request:

```java
@OnClick(R.id.do_show_location) void doShowLocation() {
  accessLocation.submit();
}
```

Delegate to the `Sensitive` action when the user responds to the request:

```java
@Override
public void onRequestPermissionsResult(
    int requestCode,
    @NonNull String[] permissions,
    @NonNull int[] grantResults
) {
  if (!accessLocation.apply(requestCode, permissions, grantResults)) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }
}
```

If you have many `Sensitive` actions, you'll need to call them one-by-one until
something returns true.


## Installation

This library is published at jcenter.

```gradle
dependencies {
    // ...
    compile "ph.codeia.vanilla:vanilla-android:$LATEST_VERSION"
}
```

(Scroll up to the beginning of this document to see the latest version number.)

Retrolambda is highly recommended.

```gradle
plugins {
    id 'me.tatarka.retrolambda' version '3.2.5'
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

> MIT License
>
> Copyright (c) 2016 Mon Zafra
>
> Permission is hereby granted, free of charge, to any person obtaining a copy
> of this software and associated documentation files (the "Software"), to deal
> in the Software without restriction, including without limitation the rights
> to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
> copies of the Software, and to permit persons to whom the Software is
> furnished to do so, subject to the following conditions:
>
> The above copyright notice and this permission notice shall be included in all
> copies or substantial portions of the Software.
>
> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
> IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
> FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
> AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
> LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
> OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
> SOFTWARE.


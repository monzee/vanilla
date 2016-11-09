# Vanilla [![Build Status](https://travis-ci.org/monzee/vanilla.svg?branch=master)](https://travis-ci.org/monzee/vanilla) [![Download](https://api.bintray.com/packages/monzee/jvm/vanilla-android/images/download.svg)](https://bintray.com/monzee/jvm/vanilla-android/_latestVersion)

A small library of little things I do all the time in android. Maybe you'll find
them useful too.

## What?

The android library at the moment contains 3 utility classes. All of these
implement generic, android platform-free interfaces that might be used to
abstract concrete android code into junit-testable bits.

### AndroidChannel

An implementation of the observer pattern. First, create a channel.

~~~java
private final Channel<String> status = new AndroidChannel<>();
~~~

Attach a listener using `#link(T -> ())`. Save the `Link` reference so that it can be detached later
if needed.

~~~java
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
~~~

The listeners will be called in the UI thread when `#send(T)` is called.

~~~java
// in a button listener somewhere
status.send("Button was clicked.");
~~~

This becomes a lot more useful when the message sender and listeners are in different
places. E.g. the listener is in an activity and the child fragments send events
to it, or fragment to child fragments. The next component makes this possible.

### AndroidLoaderStore

A cache implementation that is backed by a `LoaderManager` and dynamically
created pairs of synchronous `Loader`s and `LoaderManager.LoaderCallback`s. Objects
stored here will survive configuration changes with no extra effort.

~~~java
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
~~~

The fragment and activity can share the same store backend by scoping the
fragment store to the host activity:

~~~java
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
~~~

### AndroidRunner

Somewhat like an `Executor` but runs functions that take and return values rather than
plain `Runnable`s and `Callable`s. `AndroidChannel` uses `AndroidRunner.UI` to
run the listeners in a main looper handler. It also offers additional lazy static async
runners that calls a function in a background thread and executes the
continuation in the UI thread.

~~~java
// - This uses AsyncTask's static thread pool executor. The other async runner is
//   called AndroidRunner.ASYNC_SERIAL and enqueues the background tasks in one
//   thread.
// - This is a Lazy<T> object. It will only be created once. Later calls will
//   return the same instance. Same with ASYNC_SERIAL.
AndroidRunner.ASYNC_POOL.get().<String>apply(next -> {
  // this function will be called in the background. feel free to block the thread.
  String result = longRunningTask();
  // uses CPS. Do not return the result, call the continuation with it.
  next.got(result);
}).begin(result -> {
  // this will be called in the UI thread
  view.show(result);
});
~~~

It's basically a more abstract `AsyncTask`. I use its base interface in my
presenters/use cases and pass synchronous runners in my tests. It is possible to
do some interesting things like joining parallel calls, memoization, looping or
even jumping to labelled blocks. They will be documented once the interface has
solidified.

## Installation

~~~groovy
dependencies {
    // ...
    compile "ph.codeia.vanilla:vanilla-android:$LATEST_VERSION"
}
~~~

(Scroll up to the beginning of this document to see the latest version number.)

Retrolambda is highly recommended.

~~~groovy
plugins {
    id 'me.tatarka.retrolambda' version '3.2.5'
}
~~~

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


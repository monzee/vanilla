package ph.codeia.sacdemo;

/*
 * This file is a part of the vanilla project.
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import ph.codeia.androidutils.AndroidMooreMachine;
import ph.codeia.arch.moore.Msm;

public class MooreActivity extends AppCompatActivity implements C.View {

    private static class Scope {
        final ExecutorService junction = Executors.newSingleThreadExecutor();
        C.Model state = C.View::unknown;
    }

    private static final ExecutorService IO = Executors.newCachedThreadPool();

    private Scope my;
    private Msm.Machine<C.Model, C.View> machine;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        my = (Scope) getLastCustomNonConfigurationInstance();
        if (my == null) {
            my = new Scope();
        }
        super.onCreate(savedInstanceState);
        machine = new AndroidMooreMachine<>(my.junction, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        machine.exec(my.state);
    }

    @Override
    protected void onPause() {
        super.onPause();
        my.state = machine.stop();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return my;
    }

    @Override
    public void unknown() {
        Future<C.Model> task = IO.submit(MooreActivity::loadData);
        machine.exec(v -> v.loading(task));
    }

    @Override
    public void loading(Future<C.Model> task) {
        // show progress
        machine.await(task);
    }

    @Override
    public void loaded(List<String> items) {
        // hide progress
        // show items
    }

    @Override
    public void fold(Iterable<C.Model> backlog) {
        for (C.Model model : backlog) {
            machine.exec(model);
        }
    }

    private static C.Model loadData() throws InterruptedException {
        Thread.sleep(10_000);
        List<String> data = Arrays.asList("foo", "bar", "baz");
        return v -> v.loaded(data);
    }

}

interface C {
    interface Model extends Msm.Action<Model, View> {}

    interface View extends Msm.Effect<Model, View> {
        void unknown();

        void loading(Future<Model> task);

        void loaded(List<String> items);
    }
}


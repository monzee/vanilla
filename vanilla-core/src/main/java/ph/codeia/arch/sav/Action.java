package ph.codeia.arch.sav;

/**
 * This file is a part of the vanilla project.
 */

public interface Action<
        S extends State<S, A>,
        A extends Action<S, A, V>,
        V> {
    S fold(S from, V view);
}

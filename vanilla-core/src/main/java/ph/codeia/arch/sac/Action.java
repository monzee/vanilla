package ph.codeia.arch.sac;

/**
 * This file is a part of the vanilla project.
 */

public interface Action<
        S extends State<S, A>,
        A extends Action<S, A, C>,
        C> {
    S fold(S from, C client);
}

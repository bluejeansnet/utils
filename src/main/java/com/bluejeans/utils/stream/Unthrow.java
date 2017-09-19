package com.bluejeans.utils.stream;

import java.util.stream.Stream;

/**
 * Throw unchecker
 *
 * @author SeregaLBN how to use - see unit test
 */
public class Unthrow {
    @SuppressWarnings("unchecked")
    static <R, E extends Exception> R rethrow(final Exception ex) throws E {
        throw (E) ex;
    }

    public static <R, E extends Exception> Stream<R> of(final Stream<R> stream) throws E {
        return stream;
    }

    public static <R, E1 extends Exception, E2 extends Exception> Stream<R> of2(final Stream<R> stream) throws E1, E2 {
        return stream;
    }

    public static <R, E1 extends Exception, E2 extends Exception, E3 extends Exception> Stream<R> of3(
            final Stream<R> stream) throws E1, E2, E3 {
        return stream;
    }

    public static <R, E1 extends Exception, E2 extends Exception, E3 extends Exception, E4 extends Exception> Stream<R> of4(
            final Stream<R> stream) throws E1, E2, E3, E4 {
        return stream;
    }

    ////////////////////////////////// interfaces ProcedureN //////////////////////////////////

    /** like as {@link java.lang.Runnable} */
    @FunctionalInterface
    public interface IProc0 {
        void accept() throws Exception;
    }

    /** like as {@link java.util.function.Consumer} */
    @FunctionalInterface
    public interface IProc1<T> {
        void accept(T t) throws Exception;
    }

    /** like as {@link java.util.function.BiConsumer} */
    @FunctionalInterface
    public interface IProc2<T1, T2> {
        void accept(T1 t1, T2 t2) throws Exception;
    }

    @FunctionalInterface
    public interface IProc3<T1, T2, T3> {
        void accept(T1 t1, T2 t2, T3 t3) throws Exception;
    }

    ////////////////////////////////// interfaces FunctionN //////////////////////////////////

    /** like as {@link java.util.concurrent.Callable} */
    @FunctionalInterface
    public interface IFunc0<R> {
        R apply() throws Exception;
    }

    /** like as {@link java.util.function.Function} */
    @FunctionalInterface
    public interface IFunc1<R, T> {
        R apply(T t) throws Exception;
    }

    /** like as {@link java.util.function.BiFunction} */
    @FunctionalInterface
    public interface IFunc2<R, T1, T2> {
        R apply(T1 t1, T2 t2) throws Exception;
    }

    @FunctionalInterface
    public interface IFunc3<R, T1, T2, T3> {
        R apply(T1 t1, T2 t2, T3 t3) throws Exception;
    }

    ////////////////////////////////// wrap Procedures //////////////////////////////////

    public static void wrapProc(final IProc0 proc) {
        try {
            proc.accept();
        } catch (final Exception ex) {
            rethrow(ex);
        }
    }

    public static <T> void wrapProc(final IProc1<T> proc, final T t) {
        try {
            proc.accept(t);
        } catch (final Exception ex) {
            rethrow(ex);
        }
    }

    public static <T1, T2> void wrapProc(final IProc2<T1, T2> proc, final T1 t1, final T2 t2) {
        try {
            proc.accept(t1, t2);
        } catch (final Exception ex) {
            rethrow(ex);
        }
    }

    public static <T1, T2, T3> void wrapProc(final IProc3<T1, T2, T3> proc, final T1 t1, final T2 t2, final T3 t3) {
        try {
            proc.accept(t1, t2, t3);
        } catch (final Exception ex) {
            rethrow(ex);
        }
    }

    ////////////////////////////////// wrap Functions //////////////////////////////////

    public static <R> R wrap(final IFunc0<R> func) {
        try {
            return func.apply();
        } catch (final Exception ex) {
            return rethrow(ex);
        }
    }

    public static <R, T> R wrap(final IFunc1<R, T> func, final T t) {
        try {
            return func.apply(t);
        } catch (final Exception ex) {
            return rethrow(ex);
        }
    }

    public static <R, T1, T2> R wrap(final IFunc2<R, T1, T2> func, final T1 t1, final T2 t2) {
        try {
            return func.apply(t1, t2);
        } catch (final Exception ex) {
            return rethrow(ex);
        }
    }

    public static <R, T1, T2, T3> R wrap(final IFunc3<R, T1, T2, T3> func, final T1 t1, final T2 t2, final T3 t3) {
        try {
            return func.apply(t1, t2, t3);
        } catch (final Exception ex) {
            return rethrow(ex);
        }
    }

}

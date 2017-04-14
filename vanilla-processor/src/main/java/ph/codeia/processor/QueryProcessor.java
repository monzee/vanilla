package ph.codeia.processor;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import ph.codeia.arch.sm.Machine;
import ph.codeia.arch.sm.RootState;
import ph.codeia.arch.sm.Sm;
import ph.codeia.meta.Query;
import ph.codeia.meta.Query.Order;
import ph.codeia.meta.Query.Select;
import ph.codeia.meta.Query.Where;

@AutoService(Processor.class)
public class QueryProcessor extends AbstractProcessor {

    interface Action extends Sm.Action<State, Action, QueryProcessor> {}

    enum ColumnType { INT, SHORT, LONG, FLOAT, STRING, BLOB, }

    enum Error {
        INVALID_TYPE, FINAL_FIELD, INVISIBLE_FIELD,
        COLUMNLESS_SORT, COLUMNLESS_PREDICATE,
        SORT_EXCLUSION, NULL_EXCLUSION, COMP_EXCLUSION,
    }

    static class State extends RootState<State, Action> {
        int errors = 0;
        Map<TypeElement, QueryModel> model = new HashMap<>();
        Elements elements;
        Types types;

        private TypeMirror stringType;

        boolean isString(TypeMirror type) {
            if (stringType == null) {
                stringType = elements
                        .getTypeElement(String.class.getCanonicalName())
                        .asType();
            }
            return types.isAssignable(stringType, type);
        }
    }

    static class QueryModel {
        String dataset;
        List<ColumnMap> columns = new ArrayList<>();
        Set<SortBy> sortCriteria = new TreeSet<>();
        List<WhereMap> filters = new ArrayList<>();
        List<Binding> vars = new ArrayList<>();

        @Override
        public String toString() {
            return ""
                    + "\ndataset: " + dataset
                    + "\ncolumns: " + columns
                    + "\nfilters: " + filters
                    + "\nsortBy : " + sortCriteria;
        }
    }

    static class ColumnMap {
        final CharSequence field;
        final String column;
        final ColumnType type;

        ColumnMap(CharSequence field, String column, ColumnType type) {
            this.field = field;
            this.column = column;
            this.type = type;
        }

        @Override
        public String toString() {
            return "{" + column + "=>" + field + ":" + type + "}";
        }
    }

    static class SortBy implements Comparable<SortBy> {
        final int priority;
        final String criterion;

        SortBy(int priority, String criterion) {
            this.priority = priority;
            this.criterion = criterion;
        }

        @Override
        public int compareTo(SortBy that) {
            return priority - that.priority;
        }

        @Override
        public String toString() {
            return priority + ":" + criterion;
        }
    }

    static class WhereMap {
        final CharSequence field;
        final String clause;

        WhereMap(CharSequence field, String clause) {
            this.field = field;
            this.clause = clause;
        }

        @Override
        public String toString() {
            return "{" + field + ":" + clause + "}";
        }
    }

    interface Binding {
        void match(Case of);
        interface Case {
            void string(String constant);
            void field(VariableElement element);
            void other(String literal);
        }
    }

    private final Machine.Bound<State, Action, QueryProcessor> processor = new Machine.Bound<>(
            Machine.IMMEDIATE,
            this,
            new Machine<State, Action, QueryProcessor>(new State()) {
                { isRunning = true; }

                @Override
                public void handle(Throwable error, QueryProcessor client) {
                    client.err("[runtime] uh oh.");
                    assert error instanceof RuntimeException;
                    throw (RuntimeException) error;
                }
            });

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        for (Class<?> c : new Class<?>[] {
                Query.class,
                Select.class,
                Where.class,
                Where.Eq.class,
                Where.NotEq.class,
                Where.Lt.class,
                Where.Lte.class,
                Where.Gt.class,
                Where.Gte.class,
                Where.In.class,
                Where.NotIn.class,
                Where.Like.class,
                Where.NotLike.class,
                Where.Null.class,
                Where.NotNull.class,
                Order.class,
                Order.Descending.class,
        }) {
            types.add(c.getCanonicalName());
        }
        return types;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processor.apply(init());
        processor.apply(processQueryRoots(roundEnv.getElementsAnnotatedWith(Query.class)));
        if (processor.machine.state().errors > 0) {
            return false;
        }
        processor.apply(generateCode());
        processor.apply(clear());
        return processor.machine.state().errors == 0;
    }

    void log(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
    }

    void log(String message, Element elem) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message, elem);
    }

    void err(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }

    void err(String message, Element elem) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, elem);
    }

    static <T> boolean forSome(T nullable, Consumer<T> fun) {
        if (nullable != null) {
            fun.accept(nullable);
            return true;
        }
        return false;
    }

    static Action init() {
        return (s, p) -> {
            s.elements = p.processingEnv.getElementUtils();
            s.types = p.processingEnv.getTypeUtils();
            return s;
        };
    }

    static Action processQueryRoots(Set<? extends Element> elements) {
        return (s, p) -> {
            for (Element e : elements) {
                TypeElement key = MoreElements.asType(e);
                QueryModel query = new QueryModel();
                s.model.put(key, query);
                query.dataset = e.getAnnotation(Query.class).value();
                for (VariableElement field :
                        ElementFilter.fieldsIn(s.elements.getAllMembers(key))) {
                    if (MoreElements.isAnnotationPresent(field, Select.class)) {
                        s.plus(processProjection(query, field));
                    } else if (MoreElements.isAnnotationPresent(field, Order.class)
                            || MoreElements.isAnnotationPresent(field, Order.Descending.class)) {
                        s.plus(fail(Error.COLUMNLESS_SORT, field));
                    } else if (MoreElements.isAnnotationPresent(field, Where.Null.class)
                            || MoreElements.isAnnotationPresent(field, Where.NotNull.class)) {
                        s.plus(fail(Error.COLUMNLESS_PREDICATE, field));
                    } else {
                        s.plus(processFilters(query, field));
                    }
                }
            }
            return s;
        };
    }

    static Action processProjection(QueryModel query, VariableElement field) {
        return (s, p) -> {
            Set<Modifier> mods = field.getModifiers();
            if (mods.contains(Modifier.FINAL)) {
                return s.plus(fail(Error.FINAL_FIELD, field));
            }
            if (mods.contains(Modifier.PRIVATE) || mods.contains(Modifier.PROTECTED)) {
                return s.plus(fail(Error.INVISIBLE_FIELD, field));
            }
            String colName = field.getAnnotation(Select.class).value();
            CharSequence fieldName = field.getSimpleName();
            TypeMirror type = field.asType();
            final ColumnMap colMap;
            switch (type.getKind()) {
                case SHORT:
                    colMap = new ColumnMap(fieldName, colName, ColumnType.SHORT);
                    break;
                case INT:
                    colMap = new ColumnMap(fieldName, colName, ColumnType.INT);
                    break;
                case LONG:
                    colMap = new ColumnMap(fieldName, colName, ColumnType.LONG);
                    break;
                case FLOAT:
                    colMap = new ColumnMap(fieldName, colName, ColumnType.FLOAT);
                    break;
                case ARRAY:
                    ArrayType colAsArray = MoreTypes.asArray(type);
                    if (colAsArray.getComponentType().getKind() == TypeKind.BYTE) {
                        colMap = new ColumnMap(fieldName, colName, ColumnType.BLOB);
                        break;
                    }
                case DECLARED:
                    if (s.isString(type)) {
                        colMap = new ColumnMap(fieldName, colName, ColumnType.STRING);
                        break;
                    }
                default:
                    return s.plus(fail(Error.INVALID_TYPE, field));
            }
            query.columns.add(colMap);

            boolean sortedAlready = forSome(field.getAnnotation(Order.class), order -> query
                    .sortCriteria.add(new SortBy(order.value(), colName)));
            forSome(field.getAnnotation(Order.Descending.class), desc -> {
                if (sortedAlready) {
                    s.plus(fail(Error.SORT_EXCLUSION, field));
                } else {
                    query.sortCriteria.add(new SortBy(desc.value(), colName + " DESC"));
                }
            });

            boolean isNull = forSome(field.getAnnotation(Where.Null.class), ann -> query
                    .filters.add(new WhereMap(colName, " is NULL")));
            forSome(field.getAnnotation(Where.NotNull.class), ann -> {
                if (isNull) {
                    s.plus(fail(Error.NULL_EXCLUSION, field));
                } else {
                    query.filters.add(new WhereMap(colName, " is not NULL"));
                }
            });
            // TODO: e.g. if @Where.Eq is also present, add a COL1 = COL2 filter
            return s;
        };
    }

    static Action processFilters(QueryModel query, VariableElement field) {
        List<WhereMap> filters = query.filters;
        Name name = field.getSimpleName();
        if (forSome(field.getAnnotation(Where.Eq.class), ann -> {
            filters.add(new WhereMap(name, ann.value() + " = ?"));
        }) || forSome(field.getAnnotation(Where.NotEq.class), ann -> {
            filters.add(new WhereMap(name, ann.value() + " <> ?"));
        }) || forSome(field.getAnnotation(Where.Lt.class), ann -> {
            filters.add(new WhereMap(name, ann.value() + " < ?"));
        }) || forSome(field.getAnnotation(Where.Lte.class), ann -> {
            filters.add(new WhereMap(name, ann.value() + " <= ?"));
        }) || forSome(field.getAnnotation(Where.Gt.class), ann -> {
            filters.add(new WhereMap(name, ann.value() + " > ?"));
        }) || forSome(field.getAnnotation(Where.Gte.class), ann -> {
            filters.add(new WhereMap(name, ann.value() + " >= ?"));
        }) || forSome(field.getAnnotation(Where.Like.class), ann -> {
            filters.add(new WhereMap(name, ann.value() + " like ?"));
        }) || forSome(field.getAnnotation(Where.NotLike.class), ann -> {
            filters.add(new WhereMap(name, ann.value() + " not like ?"));
        })) {
            query.vars.add(of -> of.field(field));
        }
        // TODO: @In, @NotIn
        return (s, p) -> s;
    }

    static Action generateCode() {
        return (s, p) -> {
            for (TypeElement root : s.model.keySet()) {
                for (QueryModel m : s.model.values()) {
                    p.log("#generateCode:" + root.getQualifiedName() + m, root);
                }
            }
            return s;
        };
    }

    static Action clear() {
        return (_s, p) -> new State();
    }

    static Action fail(Error error, Element elem) {
        final String message;
        switch (error) {
            case INVALID_TYPE:
                message = "[column-type] must be one of int, short, long, float, String or byte[]";
                break;
            case FINAL_FIELD:
                message = "[column-immutable] projection field must not be final";
                break;
            case INVISIBLE_FIELD:
                message = "[column-invisible] projection field must be package visible";
                break;
            case SORT_EXCLUSION:
                message = "[sort-conflict] a field should only have one sort annotation";
                break;
            case COLUMNLESS_SORT:
                message = "[sort-column] a sort criterion must have a @Select annotation";
                break;
            case COLUMNLESS_PREDICATE:
                message = "[where-null-column] @Null/@NotNull must have a @Select annotation";
                break;
            case NULL_EXCLUSION:
                message = "[where-null-conflict] cannot filter on @Null and @NotNull on one field";
                break;
            case COMP_EXCLUSION:
                message = "[where-conflict] cannot filter on complementary predicates";
                break;
            default:
                message = "[memory-problem] i forgot to write a message for this error enum";
                break;
        }
        return (s, p) -> {
            p.err(message, elem);
            s.errors++;
            return s;
        };
    }
}

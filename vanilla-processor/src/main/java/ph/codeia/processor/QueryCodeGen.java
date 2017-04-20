package ph.codeia.processor;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Generated;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import ph.codeia.meta.GeneratedFrom;
import ph.codeia.query.Params;
import ph.codeia.query.Row;
import ph.codeia.query.Template;

/**
 * This file is a part of the vanilla project.
 */

@SuppressWarnings("Guava")
public class QueryCodeGen implements QueryProcessor.Action {

    private interface Destination {
        void match(Case of) throws IOException;
        interface Case {
            void directory(File dir) throws IOException;
            void filer(Filer filer) throws IOException;
            void nowhere();
        }
    }

    private final Destination out;

    public QueryCodeGen() {
        out = Destination.Case::nowhere;
    }

    public QueryCodeGen(String path) {
        this(new File(path));
    }

    public QueryCodeGen(File dir) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("File must be a directory");
        }
        out = of -> of.directory(dir);
    }

    public QueryCodeGen(Filer filer) {
        out = of -> of.filer(filer);
    }

    @Override
    public QueryProcessor.State fold(QueryProcessor.State state, QueryProcessor proc) {
        for (TypeElement root : state.model.keySet()) {
            QueryProcessor.QueryModel m = state.model.get(root);

            TypeMirror rootType = root.asType();
            DeclaredType mapperType = state.types.getDeclaredType(
                    state.elements.getTypeElement(Row.Mapper.class.getCanonicalName()),
                    rootType);
            TypeSpec.Builder queryClass = TypeSpec.classBuilder(Util.nameAfter(root, "Query"))
                    .addJavadoc(Util.SIGNATURE)
                    .addAnnotation(AnnotationSpec.builder(Generated.class)
                            .addMember("value", "{$S}", QueryCodeGen.class.getCanonicalName())
                            .addMember("date", "$S", Util.ISO_DATE_FORMAT.format(new Date()))
                            .build())
                    .addAnnotation(AnnotationSpec.builder(GeneratedFrom.class)
                            .addMember("value", "$S", root.getQualifiedName())
                            .build())
                    .addSuperinterface(TypeName.get(mapperType))
                    .addSuperinterface(Params.class)
                    .addModifiers(Modifier.PUBLIC);

            queryClass.addField(
                    TypeName.get(rootType),
                    "receiver",
                    Modifier.PRIVATE, Modifier.FINAL);

            queryClass.addMethod(MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(TypeName.get(rootType), "receiver")
                    .addStatement("this.receiver = receiver")
                    .build());

            queryClass.addMethod(MethodSpec.methodBuilder("dataset")
                    .returns(String.class)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("return $S", m.dataset)
                    .build());

            Object[] columns = columnNames(m.columns);
            queryClass.addMethod(MethodSpec.methodBuilder("projection")
                    .returns(String[].class)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("return new String[] { "
                            + Strings.repeat("$S, ", columns.length)
                            + "}",
                            columns)
                    .build());

            List<String> args = new ArrayList<>();
            CharSequence where = buildFilters(m.filters, args, state::isString);
            queryClass.addMethod(MethodSpec.methodBuilder("selection")
                    .returns(String.class)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("return $L", where)
                    .build());

            Object[] bindings = args.toArray(new Object[0]);
            queryClass.addMethod(MethodSpec.methodBuilder("selectionArgs")
                    .returns(String[].class)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("return new String[] { "
                            + Strings.repeat("$L, ", bindings.length)
                            + "}",
                            bindings)
                    .build());

            String sortBy = sortCriteria(m.sortCriteria);
            queryClass.addMethod(MethodSpec.methodBuilder("sortOrder")
                    .returns(String.class)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("return $S", sortBy)
                    .build());

            DeclaredType templateType = state.types.getDeclaredType(
                    state.elements.getTypeElement(Template.class.getCanonicalName()),
                    rootType);
            MethodSpec.Builder mapBody = MethodSpec.methodBuilder("from")
                    .returns(TypeName.get(rootType))
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Row.class, "cursor")
                    .addStatement(
                            state.types.isSubtype(rootType, templateType)
                                    ? "$T row = receiver.copy()"
                                    : "$T row = receiver",
                            rootType);
            buildRowMap(m.columns, mapBody);
            queryClass.addMethod(mapBody
                    .addStatement("return row")
                    .build());

            String packageName = state.elements
                    .getPackageOf(root)
                    .getQualifiedName()
                    .toString();

            JavaFile javaFile = JavaFile.builder(packageName, queryClass.build())
                    .skipJavaLangImports(true)
                    .build();

            try {
                out.match(new Destination.Case() {
                    @Override
                    public void directory(File dir) throws IOException {
                        javaFile.writeTo(dir);
                    }

                    @Override
                    public void filer(Filer filer) throws IOException {
                        javaFile.writeTo(filer);
                    }

                    @Override
                    public void nowhere() {
                        proc.log(javaFile.toString(), root);
                    }
                });
            } catch (IOException e) {
                proc.err("[io] " + e.getMessage());
                state.errors++;
            }
        }
        return state;
    }

    static void buildRowMap(List<QueryProcessor.ColumnMap> columns, MethodSpec.Builder mapBody) {
        int i = 0;
        for (QueryProcessor.ColumnMap column : columns) switch (column.type) {
            case INT:
                mapBody.addStatement("row.$L = cursor.getInt($L)", column.field, i++);
                break;
            case SHORT:
                mapBody.addStatement("row.$L = cursor.getShort($L)", column.field, i++);
                break;
            case LONG:
                mapBody.addStatement("row.$L = cursor.getLong($L)", column.field, i++);
                break;
            case FLOAT:
                mapBody.addStatement("row.$L = cursor.getFloat($L)", column.field, i++);
                break;
            case DOUBLE:
                mapBody.addStatement("row.$L = cursor.getDouble($L)", column.field, i++);
                break;
            case STRING:
                mapBody.addStatement("row.$L = cursor.getString($L)", column.field, i++);
                break;
            case BLOB:
                mapBody.addStatement("row.$L = cursor.getBlob($L)", column.field, i++);
                break;
        }
    }

    static String sortCriteria(Set<QueryProcessor.SortBy> sortCriteria) {
        StringBuilder sortBy = new StringBuilder();
        for (QueryProcessor.SortBy by : sortCriteria) {
            sortBy.append(", ").append(by.criterion);
        }
        return sortBy.substring(2);
    }

    static CharSequence buildFilters(
            List<QueryProcessor.Filter> filters,
            List<String> args,
            Predicate<TypeMirror> isString) {
        StringBuilder where = new StringBuilder();
        for (QueryProcessor.Filter f : filters) {
            where.append(String.format(" AND %s %s ",
                    f.column.replaceAll("(\\\\|\")", "\\\\$1"),
                    f.comparator));
            f.other.match(new QueryProcessor.Operand.Case() {
                @Override
                public void field(VariableElement elem) {
                    TypeMirror fieldType = elem.asType();
                    Name fieldName = elem.getSimpleName();
                    if (isString.apply(fieldType)) {
                        where.append('?');
                        args.add("receiver." + fieldName);
                    } else switch (fieldType.getKind()) {
                        case INT:
                        case FLOAT:
                        case SHORT:
                        case LONG:
                            where.append("\" + receiver.")
                                    .append(fieldName)
                                    .append(" + \"");
                            break;
                        case BOOLEAN:
                            where.append("\" + (receiver.")
                                    .append(fieldName)
                                    .append(" ? 1 : 0) + \"");
                            break;
                        default:
                            where.append('?');
                            args.add("String.valueOf(receiver." + fieldName + ")");
                            break;
                    }
                }

                @Override
                public void literal(String literal) {
                    where.append(literal);
                }
            });
        }
        return where.delete(0, 5).insert(0, '"').append('"');
    }

    static String[] columnNames(List<QueryProcessor.ColumnMap> columns) {
        String[] names = new String[columns.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = columns.get(i).column;
        }
        return names;
    }

}

package io.github.mhradek.aurkitu.plugin.core.model;

import java.util.List;

/**
 * {@code rpc_service Name { Method(Request):Response; ... }}.
 */
public record RpcServiceDeclaration(TypeName name, String source, String comment, List<Method> methods)
        implements Declaration {

    public RpcServiceDeclaration {
        methods = List.copyOf(methods);
    }

    /**
     * A method taking one table and returning one.
     *
     * @param request  the request table, or null if the Java method does not take exactly one class
     * @param response the response table, or null if the Java method does not return a class
     */
    public record Method(String name, TypeName request, TypeName response, String comment) {
        public boolean valid() {
            return request != null && response != null;
        }
    }

    public RpcServiceDeclaration withMethods(List<Method> replacement) {
        return new RpcServiceDeclaration(name, source, comment, replacement);
    }

    @Override
    public void write(SchemaWriter out) {
        out.comment(comment);
        out.line("rpc_service " + name.name() + " {");
        for (Method method : methods) {
            out.indentedComment(method.comment());
            out.indented(method.name() + "(" + method.request().referenceFrom(out.scope()) + "):"
                    + method.response().referenceFrom(out.scope()) + ";");
        }
        out.line("}").blankLine();
    }
}

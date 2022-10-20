import org.xbib.net.buffer.DataBufferFactory;
import org.xbib.net.buffer.DefaultDataBufferFactory;

module org.xbib.net {
    exports org.xbib.net;
    exports org.xbib.net.buffer;
    exports org.xbib.net.scheme;
    exports org.xbib.net.template;
    exports org.xbib.net.template.expression;
    exports org.xbib.net.template.parse;
    exports org.xbib.net.template.render;
    exports org.xbib.net.template.vars;
    exports org.xbib.net.template.vars.specs;
    exports org.xbib.net.template.vars.values;
    exports org.xbib.net.util;
    requires transitive org.xbib.datastructures.common;
    requires java.management;
    requires java.logging;
    uses DataBufferFactory;
    provides DataBufferFactory with DefaultDataBufferFactory;
}

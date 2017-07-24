package org.xbib.net.template.vars.values;

import org.xbib.net.template.expression.ExpressionType;
import org.xbib.net.template.render.ListRenderer;
import org.xbib.net.template.render.MapRenderer;
import org.xbib.net.template.render.NullRenderer;
import org.xbib.net.template.render.StringRenderer;
import org.xbib.net.template.render.ValueRenderer;

/**
 */
public enum ValueType {

    NULL("null") {
        @Override
        public ValueRenderer selectRenderer(ExpressionType type) {
            return new NullRenderer(type);
        }
    },
    /**
     * Render scalar values (simple string values).
     */
    SCALAR("scalar") {
        @Override
        public ValueRenderer selectRenderer(ExpressionType type) {
            return new StringRenderer(type);
        }
    },
    /**
     * Render array/list values.
     */
    ARRAY("list") {
        @Override
        public ValueRenderer selectRenderer(ExpressionType type) {
            return new ListRenderer(type);
        }
    },
    /**
     * Map values.
     *
     * Note: the RFC calls these "associative arrays".
     */
    MAP("map") {
        @Override
        public ValueRenderer selectRenderer(ExpressionType type) {
            return new MapRenderer(type);
        }
    };

    private final String name;

    ValueType(String name) {
        this.name = name;
    }

    /**
     * Get the renderer for this value type and expression type.
     *
     * @param type the expression type
     * @return the appropriate renderer
     */
    public abstract ValueRenderer selectRenderer(ExpressionType type);

    @Override
    public String toString() {
        return name;
    }
}

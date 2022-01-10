package wniemiec.mobilang.parser.screens.behavior;

class TemplateElement extends Expression {
    public TemplateElement(String value, boolean tail) {
        this.value = value;
        this.tail = tail;
    }

    String value;
    boolean tail;

    @Override
    public String toCode() {
        return "{value: " + value + "; tail: " + tail + "}";
    }
}
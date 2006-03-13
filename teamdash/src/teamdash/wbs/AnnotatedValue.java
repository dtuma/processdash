package teamdash.wbs;

public class AnnotatedValue extends WrappedValue implements Annotated {

    public String annotation;

    public AnnotatedValue(String value, String annotation) {
        this.value = value;
        this.annotation = annotation;
    }

    public String getAnnotation() {
        return annotation;
    }

}

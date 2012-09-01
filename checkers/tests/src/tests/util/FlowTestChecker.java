package tests.util;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.CompilationUnitTree;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.Bottom;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@TypeQualifiers( { Value.class, Odd.class, Unqualified.class, Bottom.class } )
public final class FlowTestChecker extends BaseTypeChecker {

    protected AnnotationMirror VALUE, BOTTOM;

    @Override
    public void initChecker() {
        super.initChecker();

        AnnotationUtils annoFactory = AnnotationUtils.getInstance(processingEnv);
        VALUE = annoFactory.fromClass(Value.class);
        BOTTOM = annoFactory.fromClass(Bottom.class);
    }

    @Override
    public AnnotatedTypeFactory createFactory(CompilationUnitTree tree) {
        return new BasicAnnotatedTypeFactory<FlowTestChecker>(this, tree, true);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new FlowQualifierHierarchy(factory);
    }

    private final class FlowQualifierHierarchy extends GraphQualifierHierarchy {

        public FlowQualifierHierarchy(MultiGraphFactory f) {
            super(f, BOTTOM);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(lhs, VALUE) &&
                    AnnotationUtils.areSameIgnoringValues(rhs, VALUE)) {
                return AnnotationUtils.areSame(lhs, rhs);
            }
            if (AnnotationUtils.areSameIgnoringValues(lhs, VALUE)) {
                lhs = VALUE;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, VALUE)) {
                rhs = VALUE;
            }
            return super.isSubtype(rhs, lhs);
        }
    }
}

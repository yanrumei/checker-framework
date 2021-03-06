package org.checkerframework.checker.lock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.GuardedByTop;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

// Disclaimer:
// This class is currently in its alpha form.  For sample code on how to write
// org.checkerframework.checker, please review other org.checkerframework.checker for code samples.

/**
 * The type factory for {@code Lock} type system.
 *
 * The annotated types returned by class contain {@code GuardedBy} type
 * qualifiers only for the locks that are not currently held.
 *
 */
public class LockAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    private List<String> heldLocks = new ArrayList<String>();
    protected final AnnotationMirror GUARDED_BY, GUARDEDBY_TOP, GUARDEDBY_BOT;

    public LockAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        GUARDED_BY = AnnotationUtils.fromClass(elements, GuardedBy.class);
        GUARDEDBY_TOP = AnnotationUtils.fromClass(elements, GuardedByTop.class);
        GUARDEDBY_BOT = AnnotationUtils.fromClass(elements, GuardedByBottom.class);

        addAliasedAnnotation(net.jcip.annotations.GuardedBy.class, GUARDED_BY);

        this.postInit();
    }

    public void setHeldLocks(List<String> heldLocks) {
        this.heldLocks = heldLocks;
    }

    public List<String> getHeldLock() {
        return Collections.unmodifiableList(heldLocks);
    }

    private void removeHeldLocks(AnnotatedTypeMirror type) {
        AnnotationMirror guarded = type.getAnnotation(GuardedBy.class);
        if (guarded == null) {
            return;
        }

        String lock = AnnotationUtils.getElementValue(guarded, "value", String.class, false);
        if (heldLocks.contains(lock)) {
            type.replaceAnnotation(GUARDEDBY_BOT);
        }
    }

    private AnnotationMirror createGuarded(String lock) {
        AnnotationBuilder builder =
            new AnnotationBuilder(processingEnv, GuardedBy.class);
        builder.setValue("value", lock);
        return builder.build();
    }

    private ExpressionTree receiver(ExpressionTree expr) {
        if (expr.getKind() == Tree.Kind.METHOD_INVOCATION)
            expr = ((MethodInvocationTree)expr).getMethodSelect();
        expr = TreeUtils.skipParens(expr);
        if (expr.getKind() == Tree.Kind.MEMBER_SELECT)
            return ((MemberSelectTree)expr).getExpression();
        else
            return null;
    }

    private void replaceThis(AnnotatedTypeMirror type, Tree tree) {
        if (tree.getKind() != Tree.Kind.IDENTIFIER
            && tree.getKind() != Tree.Kind.MEMBER_SELECT
            && tree.getKind() != Tree.Kind.METHOD_INVOCATION)
            return;
        ExpressionTree expr = (ExpressionTree)tree;

        if (!type.hasAnnotationRelaxed(GUARDED_BY) || isMostEnclosingThisDeref(expr))
            return;

        AnnotationMirror guardedBy = type.getAnnotation(GuardedBy.class);
        if (!"this".equals(AnnotationUtils.getElementValue(guardedBy, "value", String.class, false)))
            return;
        ExpressionTree receiver = receiver(expr);
        assert receiver != null;
        if (receiver != null) {
            AnnotationMirror newAnno = createGuarded(receiver.toString());
            type.replaceAnnotation(newAnno);
        }
    }

    private void replaceItself(AnnotatedTypeMirror type, Tree tree) {
        if (tree.getKind() != Tree.Kind.IDENTIFIER
            && tree.getKind() != Tree.Kind.MEMBER_SELECT
            && tree.getKind() != Tree.Kind.METHOD_INVOCATION)
            return;
        ExpressionTree expr = (ExpressionTree)tree;

        if (!type.hasAnnotationRelaxed(GUARDED_BY))
            return;

        AnnotationMirror guardedBy = type.getAnnotation(GuardedBy.class);
        if (!"itself".equals(AnnotationUtils.getElementValue(guardedBy, "value", String.class, false)))
            return;

        AnnotationMirror newAnno = createGuarded(expr.toString());
        type.replaceAnnotation(newAnno);
    }

    // TODO: Aliasing is not handled nicely by getAnnotation.
    // It would be nicer if we only needed to write one class here and
    // aliases were resolved internally.
    protected boolean hasGuardedBy(AnnotatedTypeMirror t) {
        return t.hasAnnotation(org.checkerframework.checker.lock.qual.GuardedBy.class) ||
               t.hasAnnotation(net.jcip.annotations.GuardedBy.class);
    }

    @Override
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type, boolean useFlow) {
        if (!hasGuardedBy(type)) {
            /* TODO: I added STRING_LITERAL to the list of types that should get defaulted.
             * This resulted in Flow inference to infer Unqualified for strings, which is a
             * subtype of guardedby. This broke the Constructors test case.
             * This check ensures that an existing annotation doesn't get removed by flow.
             * However, I'm not sure this is the nicest way to do things.
             */
            super.annotateImplicit(tree, type, useFlow);
        }
        replaceThis(type, tree);
        replaceItself(type, tree);
        removeHeldLocks(type);
    }

    @Override
    public AnnotationMirror aliasedAnnotation(AnnotationMirror a) {
        if (TypesUtils.isDeclaredOfName(a.getAnnotationType(),
                net.jcip.annotations.GuardedBy.class.getCanonicalName())) {
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, GuardedBy.class);
            builder.setValue("value", AnnotationUtils.getElementValue(a, "value", String.class, false));
            return builder.build();
        } else {
            return super.aliasedAnnotation(a);
        }
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory ignorefactory) {
        MultiGraphQualifierHierarchy.MultiGraphFactory factory = createQualifierHierarchyFactory();

        factory.addQualifier(GUARDEDBY_TOP);
        factory.addQualifier(GUARDED_BY);
        factory.addQualifier(GUARDEDBY_BOT);

        factory.addSubtype(GUARDEDBY_BOT, GUARDED_BY);
        factory.addSubtype(GUARDED_BY, GUARDEDBY_TOP);

        return new LockQualifierHierarchy(factory);
    }

    private final class LockQualifierHierarchy extends GraphQualifierHierarchy {

        public LockQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory, GUARDEDBY_BOT);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(rhs, GUARDEDBY_BOT)
                    && AnnotationUtils.areSameIgnoringValues(lhs, GUARDED_BY)) {
                return true;
            }
            // Ignore annotation values to ensure that annotation is in supertype map.
            if (AnnotationUtils.areSameIgnoringValues(lhs, GUARDED_BY)) {
                lhs = GUARDED_BY;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, GUARDED_BY)) {
                rhs = GUARDED_BY;
            }
            return super.isSubtype(rhs, lhs);
        }
    }

}

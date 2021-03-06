package org.checkerframework.framework.type;

import java.util.Collections;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntry;
import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntryKind;

/**
 * A utility class used to extract the annotations from an element and inserts
 * them into the elements type.
 *
 * In a way, this class is a hack: the Type representation for the Elements should
 * contain all annotations that we want.
 * However, due to javac bugs
 * http://mail.openjdk.java.net/pipermail/type-annotations-dev/2013-December/001449.html
 * decoding the type annotations from the Element is necessary.
 *
 * Even once these bugs are fixed, this class might be useful: in TypesIntoElements
 * it is easy to add additional annotations to the element and have them stored in the
 * bytecode by the compiler.
 * It would be more work (and might not work in the end) to instead modify the Type
 * directly.
 * The interaction between TypeFromElement and TypesIntoElements allows us to write
 * the defaulted annotations into the Element and have them read later by other parts.
 */
public class TypeFromElement {
    /**
     * Whether to throw a CheckerError if an error in the elements was found.
     */
    private static final boolean strict = true;

    private static final boolean debug = false;

    /**
     * Extracts type annotations from the element and inserts them into the
     * type of the element.
     *
     * The element could be one of:
     * 1. {@link TypeElement} of a class or an interface
     * 2. {@link ExecutableElement} of a method or a constructor
     * 3. {@link VariableElement} of a field or a method parameter
     * 4. {@link TypeParameterElement} of a method or a class type parameter
     *
     */
    public static void annotate(AnnotatedTypeMirror type, Element element) {
        if (debug) {
            System.out.println("TypeFromElement.annotate: type: " + type + " element: " + element);
        }
        if (element == null) {
            ErrorReporter.errorAbort("TypeFromElement.annotate: element cannot be null");
        } else if (element.getKind().isField()) {
            annotateField(type, (VariableElement) element);
        } else if (element.getKind() == ElementKind.LOCAL_VARIABLE) {
            annotateLocal(type, (VariableElement) element);
        } else if (element instanceof TypeElement) {
            annotateType((AnnotatedDeclaredType) type, (TypeElement) element);
        } else if (element instanceof ExecutableElement) {
            annotateExec((AnnotatedExecutableType) type, (ExecutableElement) element);
        } else if (element.getKind() == ElementKind.PARAMETER) {
            annotateParam(type, element);
        } else if (element.getKind() == ElementKind.TYPE_PARAMETER) {
            annotateTypeParam(type, element);
        } else if (element.getKind() == ElementKind.EXCEPTION_PARAMETER) {
            annotateLocal(type, (VariableElement) element);
        } else if (element.getKind() == ElementKind.RESOURCE_VARIABLE) {
            annotateLocal(type, (VariableElement) element);
        } else {
            ErrorReporter.errorAbort("TypeFromElement.annotate: illegal argument: " + element +
                    " [" + element.getKind() + "]");
        }
    }

    private static void annotateTypeParam(AnnotatedTypeMirror type, Element element) {
        if (debug) {
            System.out.println("TypeFromElement.annotateTypeParam: type: " + type + " element: " + element);
        }
        Element enclosing = element.getEnclosingElement();
        if (enclosing instanceof TypeElement) {
            TypeElement clsElt = (TypeElement)enclosing;
            if (clsElt.getTypeParameters().contains(element)) {
                int param_index = clsElt.getTypeParameters().indexOf(element);
                for (Attribute.TypeCompound typeAnno : ((ClassSymbol) clsElt).getRawTypeAttributes()) {
                    switch (typeAnno.position.type) {
                    case CLASS_TYPE_PARAMETER:
                    case CLASS_TYPE_PARAMETER_BOUND:
                        if (typeAnno.position.parameter_index == param_index) {
                            annotatePossibleBound(type, typeAnno,
                                    typeAnno.position.type == TargetType.CLASS_TYPE_PARAMETER_BOUND);
                        } /*else if (strict) {
                            ErrorReporter.errorAbort("TypeFromElement.annotateTypeParam(class): " +
                                    "invalid type parameter index " + typeAnno.position.parameter_index + " for annotation: " + typeAnno + " in class: " + clsElt);
                        }*/
                        break;
                    case CLASS_EXTENDS:
                        // Valid in this location, but handled elsewhere.
                        break;
                    default: if (strict) {
                        ErrorReporter.errorAbort("TypeFromElement.annotateTypeParam(class): " +
                                "invalid position " + typeAnno.position +
                                " for annotation: " + typeAnno +
                                " for element: " + ElementUtils.getVerboseName(element));
                    }
                    }
                }
            } else if (strict) {
                ErrorReporter.errorAbort("TypeFromElement.annotateTypeParam(class): " +
                        "not found in enclosing element: "  + ElementUtils.getVerboseName(element));
            }
        } else if (enclosing instanceof ExecutableElement) {
            ExecutableElement execElt = (ExecutableElement) enclosing;
            if (execElt.getTypeParameters().contains(element)) {
                int param_index = execElt.getTypeParameters().indexOf(element);
                for (Attribute.TypeCompound typeAnno : ((MethodSymbol) execElt).getRawTypeAttributes()) {
                    switch (typeAnno.position.type) {
                    case METHOD_TYPE_PARAMETER:
                    case METHOD_TYPE_PARAMETER_BOUND:
                        if (typeAnno.position.parameter_index == param_index) {
                            annotatePossibleBound(type, typeAnno,
                                    typeAnno.position.type == TargetType.METHOD_TYPE_PARAMETER_BOUND);
                        }/* else if (strict) {
                            ErrorReporter.errorAbort("TypeFromElement.annotate: " +
                                    "invalid method type parameter index " + typeAnno.position.parameter_index + " for annotation: " + typeAnno);
                        }*/
                        break;
                    case METHOD_RETURN:
                    case METHOD_FORMAL_PARAMETER:
                    case METHOD_RECEIVER:
                    case THROWS:
                    case LOCAL_VARIABLE:
                    case RESOURCE_VARIABLE:
                    case EXCEPTION_PARAMETER:
                    case NEW:
                    case CAST:
                    case INSTANCEOF:
                    case METHOD_INVOCATION_TYPE_ARGUMENT:
                    case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
                    case METHOD_REFERENCE:
                    case CONSTRUCTOR_REFERENCE:
                    case METHOD_REFERENCE_TYPE_ARGUMENT:
                    case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
                        // Valid in this location, but handled elsewhere.
                        break;
                    default: if (strict) {
                        ErrorReporter.errorAbort("TypeFromElement.annotateTypeParam(method): " +
                                "invalid position " + typeAnno.position +
                                " for annotation: " + typeAnno +
                                " for element: " + ElementUtils.getVerboseName(element));
                    }
                    }
                }
            } else if (strict) {
                ErrorReporter.errorAbort("TypeFromElement.annotateTypeParam(method): " +
                        "not found in enclosing element: " + ElementUtils.getVerboseName(element));
            }
        } else if (((com.sun.tools.javac.code.Symbol)enclosing).kind == com.sun.tools.javac.code.Kinds.NIL) {
            // We might hit com.sun.tools.javac.code.Symtab.noSymbol. Ignore.
        } else if (strict) {
            ErrorReporter.errorAbort("TypeFromElement.annotateTypeParam: enclosing element not a type or executable: " +
                    enclosing + " [" + enclosing.getKind() + ", " + enclosing.getClass() + ": \"" + enclosing + "\"]");
        }
    }

    private static void annotateParam(AnnotatedTypeMirror type, Element element) {
        Element enclosing = element.getEnclosingElement();
        if (enclosing instanceof ExecutableElement) {
            ExecutableElement execElt = (ExecutableElement) enclosing;
            if (execElt.getParameters().contains(element)) {
                int param_index = execElt.getParameters().indexOf(element);
                for (Attribute.TypeCompound typeAnno : ((MethodSymbol) execElt).getRawTypeAttributes()) {
                    switch (typeAnno.position.type) {
                    case METHOD_FORMAL_PARAMETER:
                        if (typeAnno.position.parameter_index == param_index) {
                            annotate(type, typeAnno);
                        }
                        break;
                    case METHOD_RECEIVER:
                    case METHOD_RETURN:
                    case THROWS:
                    case METHOD_TYPE_PARAMETER:
                    case METHOD_TYPE_PARAMETER_BOUND:
                    case LOCAL_VARIABLE:
                    case RESOURCE_VARIABLE:
                    case EXCEPTION_PARAMETER:
                    case NEW:
                    case CAST:
                    case INSTANCEOF:
                    case METHOD_INVOCATION_TYPE_ARGUMENT:
                    case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
                    case METHOD_REFERENCE:
                    case CONSTRUCTOR_REFERENCE:
                    case METHOD_REFERENCE_TYPE_ARGUMENT:
                    case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
                        // Valid in this location, but handled elsewhere.
                        break;
                    default: if (strict) {
                        ErrorReporter.errorAbort("TypeFromElement.annotateParam: " +
                                "invalid position " + typeAnno.position +
                                " for annotation: " + typeAnno +
                                " for element: " + ElementUtils.getVerboseName(element));
                    }
                    }
                }
            } else if (element.getSimpleName().contentEquals("this")) {
                // TODO: Should the ExecutableElement have a way to get the receiver element?
                // Is there such a thing as the receiver element?
                for (Attribute.TypeCompound typeAnno : ((MethodSymbol) execElt).getRawTypeAttributes()) {
                    switch (typeAnno.position.type) {
                    case METHOD_RECEIVER:
                        annotate(type, typeAnno);
                        break;
                    case METHOD_FORMAL_PARAMETER:
                    case METHOD_RETURN:
                    case THROWS:
                    case METHOD_TYPE_PARAMETER:
                    case METHOD_TYPE_PARAMETER_BOUND:
                    case LOCAL_VARIABLE:
                    case RESOURCE_VARIABLE:
                    case EXCEPTION_PARAMETER:
                    case NEW:
                    case CAST:
                    case INSTANCEOF:
                    case METHOD_INVOCATION_TYPE_ARGUMENT:
                    case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
                    case METHOD_REFERENCE:
                    case CONSTRUCTOR_REFERENCE:
                    case METHOD_REFERENCE_TYPE_ARGUMENT:
                    case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
                        // Valid in this location, but handled elsewhere.
                        break;
                    default: if (strict) {
                        ErrorReporter.errorAbort("TypeFromElement.annotateParam: " +
                                "invalid position " + typeAnno.position +
                                " for annotation: " + typeAnno +
                                " for element: " + ElementUtils.getVerboseName(element));
                    }
                    }
                }
            } else if (strict) {
                ErrorReporter.errorAbort("TypeFromElement.annotateParam: element: " + element +
                        " not found in enclosing executable: " + enclosing);
            }
        } else if (strict) {
            ErrorReporter.errorAbort("TypeFromElement.annotateParam: enclosing element not an executable: " + enclosing);
        }
    }

    /**
     * Extracts type annotations from the element and inserts them into the
     * type of the element.
     *
     * The element needs to be that of a field.
     *
     * @param type  the type of the field
     * @param element the element of a field
     */
    private static void annotateField(AnnotatedTypeMirror type, VariableElement element) {
        if (!element.getKind().isField()) {
            ErrorReporter.errorAbort("TypeFromElement.annotateField: " +
                    "invalid non-field element " + element + " [" + element.getKind() + "]");
        }

        VarSymbol symbol = (VarSymbol) element;

        // Add declaration annotations to the field type
        addAnnotationsToElt(type, symbol.getAnnotationMirrors());

        for (Attribute.TypeCompound anno : symbol.getRawTypeAttributes()) {
            TypeAnnotationPosition pos = anno.position;
            switch (pos.type) {
            case FIELD:
                annotate(type, anno);
                break;
            case NEW:
            case CAST:
            case INSTANCEOF:
            case METHOD_INVOCATION_TYPE_ARGUMENT:
            case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
            case METHOD_REFERENCE:
            case CONSTRUCTOR_REFERENCE:
            case METHOD_REFERENCE_TYPE_ARGUMENT:
            case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
                // Valid in this location, but handled elsewhere.
                break;
            default: if (strict) {
                ErrorReporter.errorAbort("TypeFromElement.annotateField: " +
                        "invalid position " + pos.type +
                        " for annotation: " + anno +
                        " for element: " + ElementUtils.getVerboseName(element));
            }
            }
        }
    }

    /**
     * Extracts type annotations from the element and inserts them into the
     * type of the element.
     *
     * The element needs to be that of a local variable.
     *
     * @param type  the type of the field
     * @param element the element of a field
     */
    private static void annotateLocal(AnnotatedTypeMirror type, VariableElement element) {
        if (element.getKind() != ElementKind.LOCAL_VARIABLE &&
                element.getKind() != ElementKind.RESOURCE_VARIABLE &&
                element.getKind() != ElementKind.EXCEPTION_PARAMETER) {
            ErrorReporter.errorAbort("TypeFromElement.annotateLocal: " +
                    "invalid non-local-variable element " + element + " [" + element.getKind() + "]");
        }

        VarSymbol symbol = (VarSymbol) element;

        // Add declaration annotations to the local variable type
        addAnnotationsToElt(type, symbol.getAnnotationMirrors());

        for (Attribute.TypeCompound anno : symbol.getRawTypeAttributes()) {

            TypeAnnotationPosition pos = anno.position;
            switch (pos.type) {
            case LOCAL_VARIABLE:
            case RESOURCE_VARIABLE:
            case EXCEPTION_PARAMETER:
                annotate(type, anno);
                break;
            case NEW:
            case CAST:
            case INSTANCEOF:
            case METHOD_INVOCATION_TYPE_ARGUMENT:
            case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
            case METHOD_REFERENCE:
            case CONSTRUCTOR_REFERENCE:
            case METHOD_REFERENCE_TYPE_ARGUMENT:
            case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
                // Valid in this location, but handled elsewhere.
                break;
            default: if (strict) {
                ErrorReporter.errorAbort("TypeFromElement.annotateLocal: " +
                        "invalid position " + pos.type +
                        " for annotation: " + anno +
                        " for element: " + ElementUtils.getVerboseName(element));
            }
            }
        }
    }

    /**
     * Extracts type annotations from the element and inserts them into the
     * type of the element.
     *
     * The element needs to be that of a class or an interface.
     *
     * @param type  the type of the class/interface
     * @param element the element of a class/interface
     */
    private static void annotateType(AnnotatedDeclaredType type, TypeElement element) {
        ClassSymbol symbol = (ClassSymbol) element;

        // Annotate raw types
        type.addAnnotations(symbol.getAnnotationMirrors());

        List<AnnotatedTypeMirror> typeParameters = type.getTypeArguments();

        for (Attribute.TypeCompound anno : symbol.getRawTypeAttributes()) {
            TypeAnnotationPosition pos = anno.position;
            switch(pos.type) {
            case CLASS_TYPE_PARAMETER:
                if (pos.parameter_index >= 0 && pos.parameter_index < typeParameters.size()) {
                    AnnotatedTypeMirror typeParam = typeParameters.get(pos.parameter_index);
                    typeParam.addAnnotation(anno);
                    if (typeParam.getKind() == TypeKind.TYPEVAR) {
                        // Add an annotation on the type parameter also to the upper bound
                        ((AnnotatedTypeVariable) typeParam).getUpperBound().addAnnotation(anno);
                    } else {
                        ErrorReporter.errorAbort("TypeFromElement.annotateType: " +
                                "type parameter: " + typeParam + " is not a type variable");
                    }
                } else if (strict) {
                    ErrorReporter.errorAbort("TypeFromElement.annotateType: " +
                            "invalid parameter index " + pos.parameter_index +
                            " for annotation: " + anno +
                            " for element: " + ElementUtils.getVerboseName(element));
                }
                break;
            case CLASS_TYPE_PARAMETER_BOUND:
                if (pos.parameter_index >= 0 && pos.parameter_index < typeParameters.size()) {
                    List<AnnotatedTypeMirror> bounds = getBounds(typeParameters.get(pos.parameter_index));
                    int boundIndex = pos.bound_index;
                    if (((Type)bounds.get(0).getUnderlyingType()).isInterface()) {
                        boundIndex -= 1;
                    }
                    if (boundIndex >= 0 && boundIndex < bounds.size()) {
                        annotate(bounds.get(boundIndex), anno);
                    } else if (strict) {
                        ErrorReporter.errorAbort("TypeFromElement.annotateType: " +
                                "invalid bound index " + pos.bound_index +
                                " for annotation: " + anno +
                                " for element: " + ElementUtils.getVerboseName(element));
                    }
                } else if (strict) {
                    ErrorReporter.errorAbort("TypeFromElement.annotateType: " +
                            "invalid parameter index " + pos.parameter_index +
                            " for annotation: " + anno +
                            " for element: " + ElementUtils.getVerboseName(element));
                }
                break;
            case CLASS_EXTENDS:
                // Add annotations on the top-level of extends/implements to
                // the type itself.
                if (pos.type_index >= -1 && pos.location.isEmpty()) {
                    // Should check that type_index < number of superinterfaces
                    // TODO: must location be empty? should INNER_TYPE be allowed?
                    type.addAnnotation(anno);
                }
                break;
            case LOCAL_VARIABLE: // ? TODO: check why those appear on a type element
            case RESOURCE_VARIABLE:
            case EXCEPTION_PARAMETER:
            case NEW: // ?
            case CAST: // ?
            case INSTANCEOF:
            case METHOD_INVOCATION_TYPE_ARGUMENT:
            case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
            case METHOD_REFERENCE:
            case CONSTRUCTOR_REFERENCE:
            case METHOD_REFERENCE_TYPE_ARGUMENT:
            case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
                // Valid in this location, but handled elsewhere.
                break;
            default: if (strict) {
                ErrorReporter.errorAbort("TypeFromElement.annotateType: " +
                        "invalid position " + pos.type +
                        " for annotation: " + anno +
                        " for element: " + ElementUtils.getVerboseName(element));
            }
            }
        }
    }

    public static void annotateSupers(List<AnnotatedDeclaredType> supertypes, TypeElement element) {
        final ClassSymbol symbol = (ClassSymbol)element;
        // Add the ones in the extends clauses
        final boolean hasSuperClass = element.getSuperclass().getKind() != TypeKind.NONE;

        AnnotatedDeclaredType superClassType = hasSuperClass ? supertypes.get(0) : null;
        List<AnnotatedDeclaredType> superInterfaces = hasSuperClass ? tail(supertypes) : supertypes;
        for (Attribute.TypeCompound anno : symbol.getRawTypeAttributes()) {
            TypeAnnotationPosition pos = anno.position;
            switch(pos.type) {
            case CLASS_EXTENDS:
                if (pos.type_index == -1 && superClassType != null) {
                    annotate(superClassType, anno);
                } else if (pos.type_index >= 0 && pos.type_index < superInterfaces.size()) {
                    annotate(superInterfaces.get(pos.type_index), anno);
                } else if (strict) {
                    ErrorReporter.errorAbort("TypeFromElement.annotateSupers: " +
                            "invalid type index " + pos.type_index +
                            " for annotation: " + anno +
                            " for element: " + ElementUtils.getVerboseName(element));
                }
                break;
            case CLASS_TYPE_PARAMETER:
            case CLASS_TYPE_PARAMETER_BOUND:
                // Valid in this location, but handled elsewhere.
                break;
            default: if (strict) {
                ErrorReporter.errorAbort("TypeFromElement.annotateSupers: " +
                        "invalid position " + pos.type +
                        " for annotation: " + anno +
                        " for element: " + ElementUtils.getVerboseName(element));
            }
            }
        }
    }

    /**
     * Extracts type annotations from the element and inserts them into the
     * type of the element.
     *
     * The element needs to be that of a method or a constructor.
     *
     * @param type  the type of the method
     * @param element the element of a method
     */
    private static void annotateExec(AnnotatedExecutableType type, ExecutableElement element) {
        type.setElement(element);

        // System.out.println("AnnotateExec: " + element);
        MethodSymbol symbol = (MethodSymbol) element;

        // Add declaration annotations to the return type
        addAnnotationsToElt(type.getReturnType(), symbol.getAnnotationMirrors());

        final List<AnnotatedTypeMirror> params = type.getParameterTypes();
        for (int i = 0; i < params.size(); ++i) {
            // Add declaration annotations to the parameter type
            addAnnotationsToElt(params.get(i), element.getParameters().get(i).getAnnotationMirrors());
        }

        // Used in multiple cases below
        final List<AnnotatedTypeVariable> typeParams = type.getTypeVariables();

        for (Attribute.TypeCompound typeAnno : symbol.getRawTypeAttributes()) {
            final TypeAnnotationPosition pos = typeAnno.position;

            switch (pos.type) {
            case METHOD_RECEIVER:
                // Assumes that the receiver type is non-null if
                // we find a METHOD_RECEIVER position.
                annotate(type.getReceiverType(), typeAnno);
                break;

            case METHOD_RETURN:
                annotate(type.getReturnType(), typeAnno);
                break;

            case METHOD_FORMAL_PARAMETER:
                if (pos.parameter_index >= 0 && pos.parameter_index < params.size()) {
                    annotate(params.get(pos.parameter_index), typeAnno);
                } else if (strict) {
                    ErrorReporter.errorAbort("TypeFromElement.annotateExec: " +
                            "invalid parameter index " + pos.parameter_index +
                            " for annotation: " + typeAnno +
                            " for element: " + ElementUtils.getVerboseName(element));
                }
                break;

            case THROWS:
                final List<AnnotatedTypeMirror> thrown = type.getThrownTypes();
                if (pos.type_index >= 0 && pos.type_index < thrown.size()) {
                    annotate(thrown.get(pos.type_index), typeAnno);
                } else if (strict) {
                    ErrorReporter.errorAbort("TypeFromElement.annotateExec: " +
                            "invalid throws index " + pos.type_index +
                            " for annotation: " + typeAnno+
                            " for element: " + ElementUtils.getVerboseName(element));
                }
                break;

            case METHOD_TYPE_PARAMETER:
                if (pos.parameter_index >= 0 && pos.parameter_index < typeParams.size()) {
                    annotate(typeParams.get(pos.parameter_index), typeAnno);
                } else if (strict) {
                    ErrorReporter.errorAbort("TypeFromElement.annotateExec: " +
                            "invalid method type parameter index " + pos.parameter_index +
                            " for annotation: " + typeAnno +
                            " for element: " + ElementUtils.getVerboseName(element));
                }
                break;

            case METHOD_TYPE_PARAMETER_BOUND:
                if (pos.parameter_index >= 0 && pos.parameter_index < typeParams.size()) {
                    List<AnnotatedTypeMirror> bounds = getBounds(typeParams.get(pos.parameter_index));
                    int boundIndex = pos.bound_index;
                    if (((Type)bounds.get(0).getUnderlyingType()).isInterface()) {
                        boundIndex -= 1;
                    }
                    if (boundIndex >= 0 && boundIndex < bounds.size()) {
                        annotate(bounds.get(boundIndex), typeAnno);
                    } else if (strict) {
                        ErrorReporter.errorAbort("TypeFromElement.annotateExec: " +
                                "invalid method type parameter bound index " + pos.bound_index +
                                " for annotation: " + typeAnno +
                                " for element: " + ElementUtils.getVerboseName(element));
                    }
                } else if (strict) {
                    // TODO: parameter_index is -1 a few times in Daikon. What does that mean?
                    // I think that's an incorrect wildcard bound, e.g. also see ThrowableExample.
                    // System.out.println("element: " + element);
                    ErrorReporter.errorAbort("TypeFromElement.annotateExec: " +
                            "invalid method type parameter index (bound) " + pos.parameter_index +
                            " for annotation: " + typeAnno +
                            " for element: " + ElementUtils.getVerboseName(element));
                }
                break;
            case LOCAL_VARIABLE:
            case RESOURCE_VARIABLE:
            case EXCEPTION_PARAMETER:
            case NEW:
            case CAST:
            case INSTANCEOF:
            case METHOD_INVOCATION_TYPE_ARGUMENT:
            case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
            case METHOD_REFERENCE:
            case CONSTRUCTOR_REFERENCE:
            case METHOD_REFERENCE_TYPE_ARGUMENT:
            case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
                // Valid in this location, but handled elsewhere.
                break;
            default: if (strict) {
                ErrorReporter.errorAbort("TypeFromElement.annotateExec: " +
                        "invalid position " + pos.type +
                        " for annotation: " + typeAnno +
                        " for element: " + ElementUtils.getVerboseName(element));
            }
            }
        }
    }

    /**
     * For backwards-compatibility: treat declaration annotations
     * as type annotations, if we now understand them as type annotations.
     * In particular, this allows the transition from JSR 305 declaration
     * annotations to JSR 308 type annotations.
     *
     * There are some caveats to this: the interpretation for declaration
     * and type annotations differs, in particular for arrays and inner
     * types. See the manual for a discussion.
     *
     * @param type The type to annotate
     * @param annotations The annotations to add
     */
    private static void addAnnotationsToElt(AnnotatedTypeMirror type,
            List<? extends AnnotationMirror> annotations) {
        AnnotatedTypeMirror innerType = AnnotatedTypes.innerMostType(type);
        innerType.addAnnotations(annotations);
    }

    private static void annotate(AnnotatedTypeMirror type, Attribute.TypeCompound anno) {
        TypeAnnotationPosition pos = anno.position;
        if (pos.location.isEmpty()) {
            // This check prevents that annotations on the declaration of
            // the type variable are also added to the type variable use.
            if (type.getKind() == TypeKind.TYPEVAR) {
                type.removeAnnotationInHierarchy(anno);
            }
            type.addAnnotation(anno);
        } else {
            annotate(type, pos.location, Collections.singletonList(anno));
        }
    }

    private static void annotatePossibleBound(AnnotatedTypeMirror type, Attribute.TypeCompound anno,
            boolean onBound) {
        if (!onBound) {
            annotate(type, anno);
        } else {
            if (type.getKind() != TypeKind.TYPEVAR
                    && type.getKind() != TypeKind.WILDCARD) {
                if (strict) {
                    ErrorReporter.errorAbort("TypeFromElement.annotatePossibleBound: " +
                            "trying to add a bound annotation: " + anno +
                            "to something that is not a type variable or wildcard: " + type);
                }
                return;
            }
            List<AnnotatedTypeMirror> bounds = getBounds(type);
            int boundIndex = anno.position.bound_index;
            if (((Type)bounds.get(0).getUnderlyingType()).isInterface()) {
                boundIndex -= 1;
            }
            if (boundIndex >= 0 && boundIndex < bounds.size()) {
                annotate(bounds.get(boundIndex), anno);
            } else if (strict) {
                ErrorReporter.errorAbort("TypeFromElement.annotatePossibleBound: " +
                        "invalid boundIndex " + boundIndex + " for annotation: " + anno);
            }
        }
    }

    private static void annotate(AnnotatedTypeMirror type, List<TypePathEntry> location, List<? extends AnnotationMirror> annotations) {
        if (debug) {
            System.out.printf("TypeFromElement.annotate: type: %s, location: %s, annos: %s%n", type, location, annotations);
        }
        AnnotatedTypeMirror inner = getLocationTypeATM(type, location);
        inner.addAnnotations(annotations);
    }

    private static AnnotatedTypeMirror getLocationTypeATM(AnnotatedTypeMirror type, List<TypePathEntry> location) {
        if (debug) {
            System.out.println("getLocationTypeATM type: " + type + " location: " + location);
        }
        if (location.isEmpty()) {
            return type;
        } else if (type.getKind() == TypeKind.DECLARED) {
            return getLocationTypeADT((AnnotatedDeclaredType)type, location);
        } else if (type.getKind() == TypeKind.WILDCARD) {
            return getLocationTypeAWT((AnnotatedWildcardType)type, location);
        } else if (type.getKind() == TypeKind.ARRAY) {
            return getLocationTypeAAT((AnnotatedArrayType)type, location);
        } else {
            ErrorReporter.errorAbort("TypeFromElement.getLocationTypeATM: only declared types and arrays can have annotations with location; " +
                    "found type: " + type + " location: " + location);
            return null; // dead code
        }
    }

    private static AnnotatedTypeMirror getLocationTypeADT(AnnotatedDeclaredType type,  List<TypePathEntry> location) {
        if (debug) {
            System.out.println("getLocationTypeADT type: " + type + " location: " + location);
        }
        if (location.isEmpty()) {
            // TODO: should this be the most enclosing, left most type?
            return type;
        } else if (location.get(0).tag.equals(TypePathEntryKind.TYPE_ARGUMENT) &&
                location.get(0).arg < type.getTypeArguments().size()) {
            return getLocationTypeATM(type.getTypeArguments().get(location.get(0).arg), tail(location));
        } else if (location.get(0).tag.equals(TypePathEntryKind.INNER_TYPE)) {
            // TODO: annotations on enclosing classes (e.g. @A Map.Entry<K, V>) not tested yet
            int totalEncl = countEnclosing(type);
            int totalInner = countInner(location);
            if (totalInner > totalEncl) {
                if (strict) {
                    System.out.println("TypeFromElement.getLocationTypeADT: too many INNER_TYPE tags!\n" +
                            "    Found location: " + location + " for type: " + type);
                } return type;
            } else if (totalInner == totalEncl) {
                List<TypePathEntry> loc = location;
                for (int i = 0; i < totalEncl; ++i) {
                    loc = tail(loc);
                }
                return getLocationTypeATM(type, loc);
            } else {
                AnnotatedDeclaredType toret = type;
                List<TypePathEntry> loc = location;
                for (int i = 0; i < (totalEncl-totalInner); ++i) {
                    if (toret.getEnclosingType() != null) {
                        toret = toret.getEnclosingType();
                        loc = tail(loc);
                    } else {
                        if (strict) {
                            System.out.println("TypeFromElement.getLocationTypeADT: not enough enclosing types!\n" +
                                    "    Found location: " + location + " for type: " + type);
                        }
                    }
                }
                return getLocationTypeATM(toret, loc);
            }
        } else {
            // ErrorReporter.errorAbort("TypeFromElement.getLocationTypeADT: " +
            //        "invalid locations " + location + " for type: " + type);
            if (strict) {
                System.out.println("TypeFromElement.getLocationTypeADT: something is wrong!\n" +
                        "    Found location: " + location + " for type: " + type);
            }
            return type;
        }
    }

    private static int countInner(List<TypePathEntry> location) {
        int cnt = 0;
        while (!location.isEmpty() &&
                location.get(0).tag.equals(TypePathEntryKind.INNER_TYPE)) {
            ++cnt;
            location = tail(location);
        }
        return cnt;
    }

    private static int countEnclosing(AnnotatedDeclaredType type) {
        int cnt = 0;
        while (type.getEnclosingType() != null) {
            ++cnt;
            type = type.getEnclosingType();
        }
        return cnt;
    }

    private static AnnotatedTypeMirror getLocationTypeAWT(AnnotatedWildcardType type,  List<TypePathEntry> location) {
        if (debug) {
            System.out.println("getLocationTypeAWT type: " + type + " location: " + location);
        }
        if (location.isEmpty()) {
            return type;
        } else if (location.get(0).tag.equals(TypePathEntryKind.WILDCARD)) {
            List<AnnotatedTypeMirror> bounds = getBounds(type);
            // TODO: what should happen if bounds is empty or has more than one entry?
            return getLocationTypeATM(bounds.get(0), tail(location));
        } else {
            if (strict) {
                System.out.println("TypeFromElement.getLocationTypeAWT: type not handled.\n" +
                        "    Found location: " + location + " for type: " + type);
            }
            return type;
        }
    }

    // Dealing with arrays requires much testing
    private static AnnotatedTypeMirror getLocationTypeAAT(AnnotatedArrayType type, List<TypePathEntry> location) {
        if (debug) {
            System.out.println("getLocationTypeAAT type: " + type + " location: " + location);
        }
        if (location.size() >= 1 &&
                location.get(0).tag.equals(TypePathEntryKind.ARRAY)) {
            AnnotatedTypeMirror comptype = type.getComponentType();
            return getLocationTypeATM(comptype, tail(location));
        } else {
            ErrorReporter.errorAbort("TypeFromElement.annotateAAT: " +
                    "invalid location " + location + " for type: " + type);
            return null; // dead code
        }
    }

    private static <T> List<T> tail(List<T> list) {
        return list.subList(1, list.size());
    }

    /**
     * Return the bounds for the type and needs to be either
     * {@link AnnotatedTypeVariable} or
     * {@link AnnotatedWildcard}.  If the type has no bounds, it returns an
     * empty list.
     *
     * @param type a wildcard or type variable
     * @return bounds of a type variable
     */
    private static List<AnnotatedTypeMirror> getBounds(AnnotatedTypeMirror type) {
        AnnotatedTypeMirror bound = null;
        if (type.getKind() == TypeKind.TYPEVAR) {
            bound = ((AnnotatedTypeVariable)type).getUpperBound();
        } else if (type.getKind() == TypeKind.WILDCARD) {
            AnnotatedWildcardType wt = (AnnotatedWildcardType)type;
            if (wt.getUnderlyingType().getExtendsBound() != null) {
                bound = wt.getExtendsBound();
            } else {
                bound = wt.getSuperBound();
            }
            if (bound == null) {
                // If neither bound is set explicitly, this will
                // set a meaningful default (java.lang.Object or the type
                // variable bound.
                bound = wt.getExtendsBound();
            }
        } else {
            ErrorReporter.errorAbort("TypeFromElement.getBounds: " +
                    "type has no bounds: " + type + " [" + type.getKind() + "]");
        }

        if (bound == null) {
            return Collections.emptyList();
        } else if (bound.getKind() == TypeKind.INTERSECTION) {
            return Collections.unmodifiableList(bound.directSuperTypes());
        } else {
            return Collections.singletonList(bound);
        }
    }
}

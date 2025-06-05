package org.folio.ssp.model.validation.constraints;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.folio.ssp.model.validation.constraints.impl.GenericNotBlankValidator.NotBlankKeyValidator;

@Constraint(validatedBy = NotBlankKeyValidator.class)
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
public @interface NotBlankKey {
  
  String message() default "{org.folio.ssp.model.validation.constraints.NotBlankKey.message}";

  Class<?>[] groups() default { };

  Class<? extends Payload>[] payload() default { };
}

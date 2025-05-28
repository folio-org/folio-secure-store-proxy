package org.folio.ssp.model.validation.constraints.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.annotation.Annotation;
import org.apache.commons.lang3.StringUtils;
import org.folio.ssp.model.validation.constraints.NotBlankKey;
import org.folio.ssp.model.validation.constraints.NotBlankValue;

public class GenericNotBlankValidator<A extends Annotation> implements ConstraintValidator<A, CharSequence> {

  @Override
  public boolean isValid(CharSequence charSequence, ConstraintValidatorContext context) {
    return StringUtils.isNotBlank(charSequence);
  }

  public static final class NotBlankKeyValidator extends GenericNotBlankValidator<NotBlankKey> {
  }

  public static final class NotBlankValueValidator extends GenericNotBlankValidator<NotBlankValue> {
  }
}

package me.desair.tus.server.validation;

import me.desair.tus.server.util.TusServletRequest;

import java.util.Optional;

@FunctionalInterface
public interface RequestValidatorFunctional {
    Optional<ValidationResult> validate(TusServletRequest request);
}

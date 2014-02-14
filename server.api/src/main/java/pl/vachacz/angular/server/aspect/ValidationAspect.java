package pl.vachacz.angular.server.aspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validator;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;

import pl.vachacz.angular.server.exception.BusinessException;
import pl.vachacz.angular.server.exception.BusinessException.Builder;

@Aspect
public class ValidationAspect {

	private static final Class<?>[] DEFAULT_PROFILE = new Class[] {};
	
	@Autowired Validator validator;

	@Around("execution(public * pl.vachacz.angular.server..*BCI.* (..))")
	public Object validateParams(ProceedingJoinPoint joinPoint) throws Throwable {

		// ConstraintViolations to return
		Set<ConstraintViolation<?>> violations = new HashSet<ConstraintViolation<?>>();

		// Get the target method
		Method interfaceMethod = ((MethodSignature) joinPoint.getSignature())
				.getMethod();
		
		Method implementationMethod = joinPoint
				.getTarget()
				.getClass()
				.getMethod(interfaceMethod.getName(),
						interfaceMethod.getParameterTypes());

		// Get the annotated parameters and validate those with the @Valid
		// annotation
		Annotation[][] annotationParameters = implementationMethod
				.getParameterAnnotations();
		for (int i = 0; i < annotationParameters.length; i++) {
			Annotation[] annotations = annotationParameters[i];
			for (Annotation annotation : annotations) {
				if (annotation.annotationType().equals(Valid.class)) {
					Object arg = joinPoint.getArgs()[i];
					violations.addAll(validator.validate(arg, DEFAULT_PROFILE));
				}
			}
		}

		// Throw an exception if ConstraintViolations are found
		if (!violations.isEmpty()) {
			Builder builder = BusinessException.build();
			for (ConstraintViolation<?> violation : violations) {
				builder.addMessage(violation.getMessage());
			}
			throw builder.excetpion();
		}
		
		return joinPoint.proceed();
	}

}

package com.trackit.data.web;

import com.trackit.data.exception.MissingUserContextException;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class UserContextArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String HEADER = "X-User-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String value = webRequest.getHeader(HEADER);
        if (value == null || value.isBlank()) {
            throw new MissingUserContextException();
        }
        try {
            long userId = Long.parseLong(value.trim());
            if (userId <= 0) {
                throw new MissingUserContextException();
            }
            return userId;
        } catch (NumberFormatException e) {
            throw new MissingUserContextException();
        }
    }
}

package com.pickeat.sse.global.auth.participant;

import com.pickeat.sse.global.exception.BusinessException;
import com.pickeat.sse.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class ParticipantInPickeatArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String PREFIX = "Bearer ";
    private final ParticipantTokenProvider participantTokenProvider;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ParticipantInPickeat.class)
                && parameter.getParameterType().equals(ParticipantPrincipal.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        ParticipantInPickeat participantInPickeatAnnotation = parameter.getParameterAnnotation(
                ParticipantInPickeat.class);
        boolean required = participantInPickeatAnnotation.required();

        String authHeader = webRequest.getHeader("Pickeat-Participant-Token");

        if (!hasAuthToken(authHeader)) {
            if (required) {
                throw new BusinessException(ErrorCode.HEADER_IS_EMPTY);
            }
            return null;
        }

        return getParticipantPrincipalByHeader(authHeader);
    }

    private boolean hasAuthToken(String authHeader) {
        return authHeader != null && authHeader.startsWith(PREFIX);
    }

    private ParticipantPrincipal getParticipantPrincipalByHeader(String authHeader) {
        String token = authHeader.substring(PREFIX.length());
        String participantCode = participantTokenProvider.getParticipantCode(token);
        String pickeatCode = participantTokenProvider.getPickeatCode(token);
        return new ParticipantPrincipal(participantCode, pickeatCode);
    }
}

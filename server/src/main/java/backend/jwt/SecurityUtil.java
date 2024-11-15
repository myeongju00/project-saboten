package backend.jwt;

import backend.exception.ApiException;
import common.message.BasicResponseMessage;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {
    private SecurityUtil() { }

    // SecurityContext 에 유저 정보가 저장되는 시점
    // Request 가 들어올 때 JwtFilter 의 doFilter 에서 저장
    public static Long getCurrentUserId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new ApiException(BasicResponseMessage.UNAUTHORIZED);
//            throw  new RuntimeException("Security Context 에 인증 정보가 없습니다.");
        }

        String name = authentication.getName();
        if(name.equals("anonymousUser") || name.equals("anonymous"))
            return null;
        return Long.parseLong(name);
    }
}
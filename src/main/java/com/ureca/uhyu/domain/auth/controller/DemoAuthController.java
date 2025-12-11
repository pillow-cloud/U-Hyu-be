package com.ureca.uhyu.domain.auth.controller;

import com.ureca.uhyu.domain.auth.service.TokenService;
import com.ureca.uhyu.domain.user.entity.User;
import com.ureca.uhyu.domain.user.enums.Grade;
import com.ureca.uhyu.domain.user.enums.Status;
import com.ureca.uhyu.domain.user.enums.UserRole;
import com.ureca.uhyu.domain.user.repository.UserRepository;
import com.ureca.uhyu.global.response.CommonResponse;
import com.ureca.uhyu.global.response.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "데모 로그인", description = "포트폴리오용 데모 로그인 API")
@Slf4j
@RestController
@RequestMapping("/guest")
@RequiredArgsConstructor
public class DemoAuthController {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    @Operation(summary = "데모 유저 로그인", description = "데모 계정으로 강제 로그인하고 토큰을 발급받습니다.")
    @PostMapping("/demo-login")
    public CommonResponse<ResultCode> demoLogin(HttpServletResponse response) {
        String demoEmail = "demo@u-hyu.site";
        
        User user = userRepository.findByEmail(demoEmail)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(demoEmail)
                                .kakaoId(0L) // Fixed Kakao ID for demo
                                .userName("Demo User")
                                .nickname("체험용 유저")
                                .role(UserRole.USER) // 일반 유저 권한
                                .status(Status.ACTIVE)
                                .grade(Grade.CHALLENGER)
                                .build()
                ));

        // 토큰 발급
        tokenService.addAccessTokenCookie(response, String.valueOf(user.getId()), user.getRole());
        tokenService.saveRefreshToken(user);

        log.info("Demo user logged in: {}", user.getEmail());

        return CommonResponse.success(ResultCode.SUCCESS, null);
    }
}

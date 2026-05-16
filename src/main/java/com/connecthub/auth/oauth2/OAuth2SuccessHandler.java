package com.connecthub.auth.oauth2;

import com.connecthub.auth.config.JwtConfig;
import com.connecthub.auth.entity.User;
import com.connecthub.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtConfig jwtConfig;

    // application.properties mein set karo:
    // oauth2.redirect-uri=http://localhost:4200/oauth2/callback
    @Value("${oauth2.redirect-uri:http://localhost:4200/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email   = oauthUser.getAttribute("email");
        String name    = oauthUser.getAttribute("name");
        String picture = oauthUser.getAttribute("picture");

        // ── Find existing user ya new user create karo ───────────────────────
        boolean[] isNew = {false};  // array trick for lambda

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            isNew[0] = true;    // << YEH naya user hai

            String base     = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
            String username = base;
            int suffix      = 1;

            while (userRepository.findByUsername(username).isPresent()) {
                username = base + suffix++;
            }

            return userRepository.save(
                User.builder()
                    .email(email)
                    .username(username)
                    .fullName(name)
                    .avatarUrl(picture)
                    .passwordHash("")        // OAuth user ka koi password nahi
                    .provider("GOOGLE")
                    .status("ONLINE")
                    .isActive(true)
                    .build()
            );
        });

        // Agar existing user hai aur avatar nahi hai, update karo
        if (picture != null && (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank())) {
            user.setAvatarUrl(picture);
            userRepository.save(user);
        }

        // ── Generate JWT ─────────────────────────────────────────────────────
        String token = jwtConfig.generateToken(user.getUserId(), user.getEmail());

        // ── Redirect with token + isNewUser flag ─────────────────────────────
        // Frontend /oauth2/callback route yeh flags padhega aur decide karega
        String target = redirectUri
                + "?token=" + token
                + "&isNewUser=" + isNew[0];

        getRedirectStrategy().sendRedirect(request, response, target);
    }
}

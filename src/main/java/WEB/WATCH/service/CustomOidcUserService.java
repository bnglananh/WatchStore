package WEB.WATCH.service;

import WEB.WATCH.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final CustomOAuth2UserService customOAuth2UserService;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String email = oidcUser.getAttribute("email");
        String name = oidcUser.getAttribute("name");
        
        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        if (name == null || name.trim().isEmpty()) {
            name = email.split("@")[0];
        }

        User user = customOAuth2UserService.processOAuth2User(email, name, registrationId);

        java.util.Map<String, Object> userInfoClaims = new java.util.HashMap<>();
        if (oidcUser.getUserInfo() != null) {
            userInfoClaims.putAll(oidcUser.getUserInfo().getClaims());
        } else {
            userInfoClaims.putAll(oidcUser.getAttributes());
        }
        userInfoClaims.put("custom_username", user.getUsername());
        org.springframework.security.oauth2.core.oidc.OidcUserInfo customUserInfo = new org.springframework.security.oauth2.core.oidc.OidcUserInfo(userInfoClaims);

        java.util.List<org.springframework.security.core.GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.getName()))
                .collect(java.util.stream.Collectors.toList());

        return new DefaultOidcUser(
                authorities,
                oidcUser.getIdToken(),
                customUserInfo,
                "custom_username"
        );
    }
}

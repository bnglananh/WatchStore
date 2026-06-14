package WEB.WATCH.service;

import WEB.WATCH.Role;
import WEB.WATCH.model.User;
import WEB.WATCH.repository.IRoleRepository;
import WEB.WATCH.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // google, facebook
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        
        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        // Robust name handling: if name is null or empty, use the part of email before @
        if (name == null || name.trim().isEmpty()) {
            name = email.split("@")[0];
        }

        User user = processOAuth2User(email, name, registrationId);

        java.util.Map<String, Object> attributes = new java.util.HashMap<>(oauth2User.getAttributes());
        attributes.put("custom_username", user.getUsername());

        java.util.List<org.springframework.security.core.GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.getName()))
                .collect(java.util.stream.Collectors.toList());

        return new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                authorities,
                attributes,
                "custom_username"
        );
    }

    public User processOAuth2User(String email, String name, String registrationId) {
        Optional<User> userByEmail = userRepository.findByEmail(email.toLowerCase());

        User user;
        if (userByEmail.isPresent()) {
            user = userByEmail.get();
        } else {
            // New user, generate unique username from email prefix
            String baseUsername = email.split("@")[0].toLowerCase();
            
            // Remove any special characters to keep username clean (optional but recommended)
            baseUsername = baseUsername.replaceAll("[^a-z0-9]", "");
            if (baseUsername.isEmpty()) {
                baseUsername = "user";
            }
            
            String username = baseUsername;
            int counter = 1;
            while (userRepository.findByUsername(username).isPresent()) {
                username = baseUsername + counter;
                counter++;
            }

            user = new User();
            user.setUsername(username);
            user.setEmail(email.toLowerCase());
            user.setPassword(java.util.UUID.randomUUID().toString());
            user.setEnabled(true);
            
            // Assign default USER role
            WEB.WATCH.model.Role userRole = roleRepository.findRoleById(WEB.WATCH.Role.USER.value);
            if (userRole != null) {
                java.util.Set<WEB.WATCH.model.Role> roles = new java.util.HashSet<>();
                roles.add(userRole);
                user.setRoles(roles);
            }
        }

        // Update basic info if needed
        if (user.getFullName() == null || user.getFullName().isEmpty()) {
            user.setFullName(name);
        }
        user.setProvider(registrationId.toUpperCase());
        return userRepository.save(user);
    }
}

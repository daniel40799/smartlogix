package com.smartlogix.security;

import com.smartlogix.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads the user record by email address for Spring Security authentication.
     * <p>
     * The {@link com.smartlogix.domain.entity.User} entity implements {@link UserDetails},
     * so it is returned directly. The username convention used throughout SmartLogix is the
     * user's email address.
     * </p>
     *
     * @param email the email address identifying the user (acts as the username)
     * @return the matching {@link UserDetails} instance
     * @throws UsernameNotFoundException if no user with the given email is found in the database
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}

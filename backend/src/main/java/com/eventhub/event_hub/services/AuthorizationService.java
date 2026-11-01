package com.eventhub.event_hub.services;

import com.eventhub.event_hub.repositories.UserRepository;
import com.eventhub.event_hub.utils.Logins;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Lets Spring Security load users by login during authentication. */
@Service
public class AuthorizationService implements UserDetailsService {

    private final UserRepository userRepository;

    public AuthorizationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByLogin(Logins.normalize(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}

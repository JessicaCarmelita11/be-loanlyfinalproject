package com.example.loanlyFinalProject.security;

import com.example.loanlyFinalProject.entity.User;
import com.example.loanlyFinalProject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
            .orElseThrow(
                () ->
                    new UsernameNotFoundException(
                        "User not found with username or email: " + usernameOrEmail));

    return new CustomUserDetails(user);
  }

  @Transactional(readOnly = true)
  public UserDetails loadUserById(Long id) {
    // Use findByIdWithRoles to eagerly fetch roles and prevent lazy loading issues
    User user =
        userRepository
            .findByIdWithRoles(id)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

    return new CustomUserDetails(user);
  }
}

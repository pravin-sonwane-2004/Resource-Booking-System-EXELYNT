package com.pravin.Resource_Booking.security;

import com.pravin.Resource_Booking.entity.Role;
import com.pravin.Resource_Booking.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security principal that carries the application {@link User} identity
 * (id and role) alongside the standard authorities.
 */
public class CustomUserDetails extends User implements UserDetails {

    private final Long id;
    private final Role role;

    public CustomUserDetails(Long id, String username, String password, Role role,
                             Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
        this.role = role;
    }

    public static CustomUserDetails from(User user) {
        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}

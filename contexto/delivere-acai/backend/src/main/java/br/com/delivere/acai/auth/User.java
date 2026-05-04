package br.com.delivere.acai.auth;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    /** Roles separadas por vírgula, ex: "ROLE_USER,ROLE_ADMIN". */
    @Column(nullable = false, length = 100)
    private String roles = "ROLE_USER";

    /** Setor: VENDAS ou GESTAO. GESTAO tem acesso ao módulo de gestão. Se null, usa app.gestao.usuarios. */
    @Column(length = 20)
    private String setor;

    private boolean enabled = true;

    public User() {
    }

    public User(String username, String password, String roles) {
        this.username = username;
        this.password = password;
        this.roles = roles != null ? roles : "ROLE_USER";
    }

    public User(String username, String password, String roles, String setor) {
        this.username = username;
        this.password = password;
        this.roles = roles != null ? roles : "ROLE_USER";
        this.setor = setor;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(roles.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Long getId() {
        return id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSetor() {
        return setor;
    }

    public void setSetor(String setor) {
        this.setor = setor;
    }
}

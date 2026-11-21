package com.agroprecision.controller;

import com.agroprecision.dto.AuthDtos.ApiMessage;
import com.agroprecision.entity.UserRole;
import com.agroprecision.exception.ApiException;
import com.agroprecision.repository.UserRepository;
import com.agroprecision.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** Own profile, password change, and admin-only user management. */
@RestController
@RequestMapping("/api")
public class AccountController {
    private static final Set<String> ROLES = Set.of("admin", "farmer", "field_worker");
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JdbcTemplate db;

    public AccountController(UserRepository users, PasswordEncoder encoder, JdbcTemplate db) {
        this.users = users;
        this.encoder = encoder;
        this.db = db;
    }

    public record ProfileUpdate(
            String fullName,
            String firstName,
            String lastName,
            String phone,
            String phoneNumber,
            String gender,
            String dateOfBirth,
            String address,
            String city,
            String state,
            String pincode
    ) {}
    public record PasswordChange(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 72) String newPassword) {}
    public record RoleChange(@NotBlank String role, boolean enabled) {}

    @GetMapping("/account")
    public Map<String, Object> profile(@AuthenticationPrincipal AuthUser me) {
        var u = users.findById(me.id()).orElseThrow(ApiException::notFound);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("user_id", u.getId());
        m.put("first_name", u.getFirstName());
        m.put("last_name", u.getLastName());
        m.put("email", u.getEmail());
        m.put("phone_number", u.getPhoneNumber() != null ? u.getPhoneNumber() : u.getPhone());
        m.put("gender", u.getGender());
        m.put("date_of_birth", u.getDateOfBirth() != null ? u.getDateOfBirth().toString() : null);
        m.put("address", u.getAddress());
        m.put("city", u.getCity());
        m.put("state", u.getState());
        m.put("pincode", u.getPincode());

        // Also keep legacy keys for backwards compatibility
        m.put("fullName", u.getFullName());
        m.put("phone", u.getPhone());
        return m;
    }

    @PutMapping("/account")
    public ApiMessage update(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ProfileUpdate r) {
        var u = users.findById(me.id()).orElseThrow(ApiException::notFound);

        if (r.firstName() != null) u.setFirstName(r.firstName().trim().isEmpty() ? null : r.firstName().trim());
        if (r.lastName() != null) u.setLastName(r.lastName().trim().isEmpty() ? null : r.lastName().trim());

        String fn = u.getFirstName();
        String ln = u.getLastName();
        String computedName = ((fn != null ? fn : "") + (ln != null ? " " + ln : "")).trim();
        if (r.fullName() != null && !r.fullName().isBlank()) {
            u.setFullName(r.fullName().trim());
        } else if (!computedName.isBlank()) {
            u.setFullName(computedName);
        }

        String p = r.phoneNumber() != null && !r.phoneNumber().isBlank() ? r.phoneNumber().trim() :
                (r.phone() != null && !r.phone().isBlank() ? r.phone().trim() : null);
        if (r.phoneNumber() != null || r.phone() != null) {
            u.setPhone(p);
            u.setPhoneNumber(p);
        }

        if (r.gender() != null) u.setGender(r.gender().trim().isEmpty() ? null : r.gender().trim());
        if (r.dateOfBirth() != null) {
            if (r.dateOfBirth().isBlank()) {
                u.setDateOfBirth(null);
            } else {
                try {
                    u.setDateOfBirth(java.time.LocalDate.parse(r.dateOfBirth().trim()));
                } catch (Exception ex) {
                    throw ApiException.bad("Invalid date format for date of birth. Please use YYYY-MM-DD.");
                }
            }
        }
        if (r.address() != null) u.setAddress(r.address().trim().isEmpty() ? null : r.address().trim());
        if (r.city() != null) u.setCity(r.city().trim().isEmpty() ? null : r.city().trim());
        if (r.state() != null) u.setState(r.state().trim().isEmpty() ? null : r.state().trim());
        if (r.pincode() != null) u.setPincode(r.pincode().trim().isEmpty() ? null : r.pincode().trim());

        users.save(u);
        return new ApiMessage("Profile saved.");
    }

    @PostMapping("/account/password")
    public ApiMessage password(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody PasswordChange r) {
        var u = users.findById(me.id()).orElseThrow(ApiException::notFound);
        if (!encoder.matches(r.currentPassword(), u.getPasswordHash())) throw ApiException.bad("Current password is incorrect.");
        u.setPasswordHash(encoder.encode(r.newPassword()));
        users.save(u);
        return new ApiMessage("Password changed.");
    }

    @GetMapping("/admin/users")
    public List<Map<String, Object>> listUsers(@AuthenticationPrincipal AuthUser me) {
        if (!me.isAdmin()) throw ApiException.forbidden();
        return users.findAll().stream().sorted(Comparator.comparing(u -> u.getEmail())).map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("user_id", u.getId());
            m.put("first_name", u.getFirstName());
            m.put("last_name", u.getLastName());
            m.put("email", u.getEmail());
            m.put("phone_number", u.getPhoneNumber() != null ? u.getPhoneNumber() : u.getPhone());
            m.put("phone", u.getPhone());
            m.put("gender", u.getGender());
            m.put("date_of_birth", u.getDateOfBirth() != null ? u.getDateOfBirth().toString() : null);
            m.put("address", u.getAddress());
            m.put("city", u.getCity());
            m.put("state", u.getState());
            m.put("pincode", u.getPincode());
            m.put("full_name", u.getFullName());
            m.put("roles", u.getRoles().stream().map(UserRole::getRole).sorted().toList());
            m.put("created_at", u.getCreatedAt() == null ? null : u.getCreatedAt().toString() + "Z");
            return m;
        }).toList();
    }

    @Transactional
    @PostMapping("/admin/users/{id}/roles")
    public ApiMessage setRole(@AuthenticationPrincipal AuthUser me, @PathVariable String id, @Valid @RequestBody RoleChange r) {
        if (!me.isAdmin()) throw ApiException.forbidden();
        if (!ROLES.contains(r.role())) throw ApiException.bad("Unknown role.");
        if (me.id().equals(id) && r.role().equals("admin") && !r.enabled())
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot remove your own admin role.");
        users.findById(id).orElseThrow(ApiException::notFound);
        if (r.enabled())
            db.update("INSERT IGNORE INTO user_roles (id, user_id, role) VALUES (?, ?, ?)", UUID.randomUUID().toString(), id, r.role());
        else
            db.update("DELETE FROM user_roles WHERE user_id = ? AND role = ?", id, r.role());
        return new ApiMessage("Role updated.");
    }
}

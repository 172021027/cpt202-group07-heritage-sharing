package com.example.heritage_sharing_api.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
@Check(constraints = """
        role in ('user', 'contributor', 'admin')
        and role_request_status in ('none', 'pending', 'approved', 'rejected', 'revoked')
        and (
            (role = 'contributor' and role_request_status = 'approved')
            or (role = 'user' and role_request_status in ('none', 'pending', 'rejected', 'revoked'))
            or (role = 'admin' and role_request_status = 'none')
        )
        """)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "personal_description", columnDefinition = "TEXT")
    private String personalDescription;

    @Column(name = "profile_picture_path", length = 500)
    private String profilePicturePath;

    @Column(name = "role", nullable = false, length = 20, columnDefinition = "varchar(20) default 'user'")
    @Convert(converter = UserRoleConverter.class)
    private UserRole role = UserRole.USER;

    @Column(name = "role_request_status", nullable = false, length = 30, columnDefinition = "varchar(30) default 'none'")
    @Convert(converter = ContributorRequestStatusConverter.class)
    private ContributorRequestStatus roleRequestStatus = ContributorRequestStatus.NONE;

    public User() {}

    public User(Long userId, String username, String email, String passwordHash, String personalDescription, String profilePicturePath, String role) {
        this(userId, username, email, null, passwordHash, personalDescription, profilePicturePath, role);
    }

    public User(Long userId, String username, String email, String gender, String passwordHash, String personalDescription, String profilePicturePath, String role) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.gender = gender;
        this.passwordHash = passwordHash;
        this.personalDescription = personalDescription;
        this.profilePicturePath = profilePicturePath;
        setRole(role);
    }

    public UserRole getRole() { return role == null ? UserRole.USER : role; }
    public void setRole(UserRole role) { this.role = role == null ? UserRole.USER : role; }
    public void setRole(String role) { setRole(UserRole.fromValue(role)); }
    public String getRoleValue() { return getRole().getApiValue(); }

    public ContributorRequestStatus getRoleRequestStatus() {
        return roleRequestStatus == null ? ContributorRequestStatus.NONE : roleRequestStatus;
    }
    public void setRoleRequestStatus(ContributorRequestStatus roleRequestStatus) {
        this.roleRequestStatus = roleRequestStatus == null ? ContributorRequestStatus.NONE : roleRequestStatus;
    }
    public void setRoleRequestStatus(String roleRequestStatus) {
        setRoleRequestStatus(ContributorRequestStatus.fromValue(roleRequestStatus));
    }
    public String getRoleRequestStatusValue() { return getRoleRequestStatus().getApiValue(); }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getPersonalDescription() { return personalDescription; }
    public void setPersonalDescription(String personalDescription) { this.personalDescription = personalDescription; }

    public String getProfilePicturePath() { return profilePicturePath; }
    public void setProfilePicturePath(String profilePicturePath) { this.profilePicturePath = profilePicturePath; }
}

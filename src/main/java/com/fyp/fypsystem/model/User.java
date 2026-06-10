package com.fyp.fypsystem.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private Integer rating;
    private String goals;

    @Enumerated(EnumType.STRING)
    private Role role;

    private Long coachId;       // for STUDENT users: ID of their assigned coach

    // Extended profile fields
    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(columnDefinition = "LONGTEXT")
    private String profilePicture;

    private String phone;
    private String location;
    private String chessUsername;
    private String fideId;
    private String playingStyle;
    private String chessTitle;
    private String favouriteWhiteOpening;
    private String favouriteBlackOpening;
    private String joinedAt;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) joinedAt = java.time.LocalDate.now().toString();
    }

    public User() {}

    public User(String name, String email, String password, Integer rating, String goals, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.rating = rating;
        this.goals = goals;
        this.role = role;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getGoals() { return goals; }
    public void setGoals(String goals) { this.goals = goals; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Long getCoachId() { return coachId; }
    public void setCoachId(Long coachId) { this.coachId = coachId; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getChessUsername() { return chessUsername; }
    public void setChessUsername(String chessUsername) { this.chessUsername = chessUsername; }

    public String getFideId() { return fideId; }
    public void setFideId(String fideId) { this.fideId = fideId; }

    public String getPlayingStyle() { return playingStyle; }
    public void setPlayingStyle(String playingStyle) { this.playingStyle = playingStyle; }

    public String getChessTitle() { return chessTitle; }
    public void setChessTitle(String chessTitle) { this.chessTitle = chessTitle; }

    public String getFavouriteWhiteOpening() { return favouriteWhiteOpening; }
    public void setFavouriteWhiteOpening(String favouriteWhiteOpening) { this.favouriteWhiteOpening = favouriteWhiteOpening; }

    public String getFavouriteBlackOpening() { return favouriteBlackOpening; }
    public void setFavouriteBlackOpening(String favouriteBlackOpening) { this.favouriteBlackOpening = favouriteBlackOpening; }

    public String getJoinedAt() { return joinedAt; }
    public void setJoinedAt(String joinedAt) { this.joinedAt = joinedAt; }
}

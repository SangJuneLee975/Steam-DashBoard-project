package com.example.steam.entity;

import com.example.steam.dto.User;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "auth_social_login")
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SteamLogin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String steamId;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;


}
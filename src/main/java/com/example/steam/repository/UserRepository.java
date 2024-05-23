package com.example.steam.repository;

import com.example.steam.dto.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUserId(String userId);

   // Optional<User> findByUsername(String username);

    // SteamLogin 테이블과 조인하여 steamId로 User를 찾는 쿼리문
   // @Query("SELECT u FROM User u JOIN SteamLogin sl ON u.id = sl.user.id WHERE sl.steamId = :steamId")
  //  Optional<User> findBySteamId(@Param("steamId") String steamId);

    Optional<User> findBySteamId(String steamId);

    Optional<User> findByUserIdOrSteamId(String userId, String steamId);
}
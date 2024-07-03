package com.example.steam.dto;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SteamLinkRequest {
    private String steamId;
    private String steamNickname;
    private boolean isSteamLinked;


    // Getters and Setters
    public String getSteamId() {
        return steamId;
    }

    public void setSteamId(String steamId) {
        this.steamId = steamId;
    }

    public String getSteamNickname() {
        return steamNickname;
    }

    public void setSteamNickname(String steamNickname) {
        this.steamNickname = steamNickname;
    }
}

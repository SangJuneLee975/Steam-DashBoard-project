package com.example.steam.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamUser {
    private String steamId;
    private String displayName;
    private String personaname;
    private String profileurl;
    private String avatar;
    private String avatarmedium;
    private String avatarfull;
    private int personastate;
    private int communityvisibilitystate;
    private int profilestate;
    private long lastlogoff;
    private String realname;
    private String primaryclanid;
    private long timecreated;
    private String loccountrycode;
    private String locstatecode;
    private int loccityid;
}
package com.cricriser.cricriser.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerCardDto {
    private String id;
    private String name;
    private String role;
    private String photoUrl;
}

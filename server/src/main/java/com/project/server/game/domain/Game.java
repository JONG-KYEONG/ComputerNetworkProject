package com.project.server.game.domain;

import jakarta.persistence.*;
import lombok.*;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "game")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private boolean gameStatus;
    @NotNull
    private int userCount;

    public void updateUser(boolean isAdd){
        if(isAdd)
            this.userCount++;
        else
            this.userCount--;
    }
}

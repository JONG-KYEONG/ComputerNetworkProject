package com.project.server.game.repository;

import com.project.server.game.domain.Game;
import com.project.server.game.domain.GameUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameUserRepository extends JpaRepository<GameUser, Long> {
    List<GameUser> findAllByGame(Game game);


    List<GameUser> findAllByGameOrderByGameScoreDesc(Game game);

    Optional<GameUser> findFirstByGame(Game game);
}

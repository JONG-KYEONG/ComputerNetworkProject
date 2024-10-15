package com.project.server.game.repository;

import com.project.server.game.domain.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    @Query("SELECT g FROM Game g WHERE g.userCount < :maxuser  order by RAND() limit 1")
    Optional<Game> findOneByRandom(@Param("maxuser") int maxuser);
}

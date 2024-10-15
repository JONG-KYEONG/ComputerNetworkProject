package com.project.server.game.repository;

import com.project.server.game.domain.GameAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameAnswerRepository extends JpaRepository<GameAnswer, Long> {
    Optional<GameAnswer> findByGameId(Long gameId);
}

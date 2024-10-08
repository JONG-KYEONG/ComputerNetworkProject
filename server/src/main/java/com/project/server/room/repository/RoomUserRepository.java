package com.project.server.room.repository;

import com.project.server.room.domain.Room;
import com.project.server.room.domain.RoomUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomUserRepository extends JpaRepository<RoomUser, Long> {
    List<RoomUser> findAllByRoom(Room room);

    Optional<RoomUser> findFirstByRoom(Room room);
}

package com.project.server.room.service;

import com.project.server.exception.BadRequestException;
import com.project.server.room.domain.Room;
import com.project.server.room.domain.RoomUser;
import com.project.server.room.repository.RoomRepository;
import com.project.server.room.repository.RoomUserRepository;
import com.project.server.socket.dto.RoomInfoDto;
import com.project.server.socket.dto.RoomUserDto;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Transactional
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;

    public RoomInfoDto enterRoom(Long roomId, String username){
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BadRequestException("방을 찾을 수 없습니다"));

        if(room.getUserCount() < 4){
            RoomUser roomUser = RoomUser.builder()
                    .room(room)
                    .isCaptain(room.getUserCount()==0)
                    .roomNickname(username)
                    .build();
            roomUserRepository.save(roomUser);
            room.updateUser(true);
        }
        else{
            throw new BadRequestException("방에 입장 불가");
        }

        return RoomInfoDto.builder()
                .userCount(room.getUserCount())
                .roomId(roomId)
                .build();
    }

    public RoomInfoDto leaveRoom(Long roomId, Long userId){
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BadRequestException("방을 찾을 수 없습니다"));

        RoomUser roomUser = roomUserRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("유저를 찾을 수 없습니다"));

        if(room.getUserCount()==1) {  // 나간 사람이 마지막 사람이면 방도 같이 삭제.
            roomUserRepository.delete(roomUser);
            roomRepository.delete(room);
            return null;
        }
        else{
            if(roomUser.isCaptain()) {
                RoomUser nextCaption = roomUserRepository.findFirstByRoom(room).get();
                nextCaption.setCaptain(true);
            }
            roomUserRepository.delete(roomUser);
            room.updateUser(false);
        }

        return RoomInfoDto.builder()
                .userCount(room.getUserCount())
                .roomId(roomId)
                .build();
    }

    public List<RoomUserDto> getRoomUsers(long roomId){
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BadRequestException("방을 찾을 수 없습니다"));

        List<RoomUser> roomUsers = roomUserRepository.findAllByRoom(room);
        List<RoomUserDto> roomUserDtos = new ArrayList<>();

        for(RoomUser roomUser : roomUsers){
            roomUserDtos.add(RoomUserDto.builder()
                    .isCaptain(roomUser.isCaptain())
                    .userId(roomUser.getId())
                    .roomNickname(roomUser.getRoomNickname())
                    .build());
        }

        return roomUserDtos;
    }
}

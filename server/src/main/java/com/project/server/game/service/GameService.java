package com.project.server.game.service;

import com.project.server.exception.BadRequestException;
import com.project.server.game.domain.Game;
import com.project.server.game.domain.GameUser;
import com.project.server.game.repository.GameRepository;
import com.project.server.game.repository.GameUserRepository;
import com.project.server.socket.dto.GameInfoDto;
import com.project.server.socket.dto.GameUserDto;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Transactional
public class GameService {
    private final GameRepository gameRepository;
    private final GameUserRepository gameUserRepository;

    public GameInfoDto enterGame(Long gameId, String username){
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BadRequestException("방을 찾을 수 없습니다"));

        if(game.getUserCount() < 4){
            GameUser gameUser = GameUser.builder()
                    .game(game)
                    .isCaptain(game.getUserCount()==0)
                    .gameNickname(username)
                    .build();
            gameUserRepository.save(gameUser);
            game.updateUser(true);
        }
        else{
            throw new BadRequestException("방에 입장 불가");
        }

        return GameInfoDto.builder()
                .userCount(game.getUserCount())
                .gameId(gameId)
                .build();
    }

    public GameInfoDto leaveGame(Long gameId, Long userId){
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BadRequestException("방을 찾을 수 없습니다"));

        GameUser gameUser = gameUserRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("유저를 찾을 수 없습니다"));

        if(game.getUserCount()==1) {  // 나간 사람이 마지막 사람이면 방도 같이 삭제.
            gameUserRepository.delete(gameUser);
            gameRepository.delete(game);
            return null;
        }
        else{
            if(gameUser.isCaptain()) {
                GameUser nextCaption = gameUserRepository.findFirstByGame(game).get();
                nextCaption.setCaptain(true);
            }
            gameUserRepository.delete(gameUser);
            game.updateUser(false);
        }

        return GameInfoDto.builder()
                .userCount(game.getUserCount())
                .gameId(gameId)
                .build();
    }

    public List<GameUserDto> getGameUsers(long gameId){
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BadRequestException("방을 찾을 수 없습니다"));

        List<GameUser> GameUsers = gameUserRepository.findAllByGame(game);
        List<GameUserDto> gameUserDtos = new ArrayList<>();

        for(GameUser gameUser : GameUsers){
            gameUserDtos.add(GameUserDto.builder()
                    .isCaptain(gameUser.isCaptain())
                    .userId(gameUser.getId())
                    .gameNickname(gameUser.getGameNickname())
                    .gameScore(gameUser.getGameScore())
                    .build());
        }

        return gameUserDtos;
    }
}

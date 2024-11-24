package com.project.server.game.service;

import com.project.server.answer.domain.Answer;
import com.project.server.answer.repository.AnswerRepository;
import com.project.server.exception.BadRequestException;
import com.project.server.game.domain.Game;
import com.project.server.game.domain.GameAnswer;
import com.project.server.game.domain.GameUser;
import com.project.server.game.repository.GameAnswerRepository;
import com.project.server.game.repository.GameRepository;
import com.project.server.game.repository.GameUserRepository;
import com.project.server.socket.dto.GameInfoDto;
import com.project.server.socket.dto.GameUserDto;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class GameService {
    private final GameRepository gameRepository;
    private final GameUserRepository gameUserRepository;
    private final GameAnswerRepository gameAnswerRepository;
    private final AnswerRepository answerRepository;
    private static final int GAME_END_TURN = 9;

    public GameService(GameRepository gameRepository,
                       GameUserRepository gameUserRepository,
                       GameAnswerRepository gameAnswerRepository,
                       AnswerRepository answerRepository) {
        this.gameRepository = gameRepository;
        this.gameUserRepository = gameUserRepository;
        this.gameAnswerRepository = gameAnswerRepository;
        this.answerRepository = answerRepository;
    }

    private final int GAME_MAX_USER = 4;

    public Long enterGame(Long gameId, String username) {  // 유저가 방에 입장
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BadRequestException("방을 찾을 수 없습니다"));

        if (game.getUserCount() < GAME_MAX_USER) {
            GameUser gameUser = GameUser.builder()
                    .game(game)
                    .isCaptain(game.getUserCount() == 0)
                    .gameNickname(username)
                    .build();
            Long userId = gameUserRepository.save(gameUser).getId();
            game.updateUser(true);
            return userId;
        } else {
            throw new BadRequestException("방에 입장 불가");
        }
    }

    public String getAnswer(Long gameId) {
        GameAnswer gameAnswer = gameAnswerRepository.findByGameId(gameId)
                .orElseThrow(() -> new BadRequestException("방을 찾을 수 없습니다."));

        return gameAnswer.getAnswerName();
    }

    public GameInfoDto getGameInfoDto(Long gameId) { // 게임 방 정보 가져오기
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BadRequestException("방을 찾을 수 없습니다"));

        return GameInfoDto.builder()
                .userCount(game.getUserCount())
                .gameId(gameId)
                .build();
    }

    public GameInfoDto leaveGame(Long gameId, Long userId) {  // 유저가 방을 떠남
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BadRequestException("방을 찾을 수 없습니다"));

        GameUser gameUser = gameUserRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("유저를 찾을 수 없습니다"));

        if (game.getUserCount() == 1) {  // 나간 사람이 마지막 사람이면 방도 같이 삭제.
            gameUserRepository.delete(gameUser);
            gameRepository.delete(game);
            return null;
        } else {
            if (gameUser.isCaptain()) {
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

    public List<GameUserDto> getGameUsers(long gameId) {  // 해당 게임에 있는 유저 리스트 가져오기
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BadRequestException("방을 찾을 수 없습니다"));

        List<GameUser> GameUsers = gameUserRepository.findAllByGame(game);
        List<GameUserDto> gameUserDtos = new ArrayList<>();

        for (GameUser gameUser : GameUsers) {
            gameUserDtos.add(GameUserDto.builder()
                    .isCaptain(gameUser.isCaptain())
                    .userId(gameUser.getId())
                    .gameNickname(gameUser.getGameNickname())
                    .gameScore(gameUser.getGameScore())
                    .build());
        }

        return gameUserDtos;
    }

    public void gameStart(Long gameId) {   // 게임 시작
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BadRequestException("방을 찾을 수 없습니다."));
        game.setGameStatus(true);
        game.setNowTurn(1);
    }

    public boolean gameEnd(Long gameId) {   // 게임 종료
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BadRequestException("방을 찾을 수 없습니다."));
        if (game.getNowTurn() >= GAME_END_TURN) {
            return true;
        } else {
            return false;
        }
    }

    public String getResult(Long gameId){
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BadRequestException("방을 찾을 수 없습니다."));

        game.setGameStatus(false);
        game.setNowTurn(0);

        List<GameUser> gameUsers = gameUserRepository.findAllByGameOrderByGameScoreDesc(game);

        String result = "";

        for(GameUser gameUser : gameUsers){
            result += "  " + gameUser.getGameNickname() + "님 " + gameUser.getGameScore() + "점 \n";
        }

        for(GameUser gameUser : gameUsers){
            gameUser.setGameScore(0);
        }

        return result;
    }

    public Boolean isCorrect(String answer, Long gameId, Long userId) { // 정답인지 아닌지 판단하기
        GameAnswer gameAnswer = gameAnswerRepository.findByGameId(gameId)
                .orElseThrow(() -> new BadRequestException("방을 찾을 수 없습니다."));

        GameUser gameUser = gameUserRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("유저를 찾을 수 없습니다"));

        if (answer.contains(gameAnswer.getAnswerName())) {
            gameUser.setGameScore(gameUser.getGameScore()+1);
            return true;
        } else {
            return false;
        }
    }

    public GameInfoDto changeTurn(Long gameId) {  // 새로운 인물 저장하고 가져오기
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BadRequestException("방을 찾을 수 없습니다."));
        Answer answer = answerRepository.findOneWithRandom()
                .orElseThrow(() -> new BadRequestException("정답 목록을 가져오는데에 실패하였습니다."));

        Optional<GameAnswer> optionalGameAnswer = gameAnswerRepository.findByGameId(gameId);

        if(optionalGameAnswer.isPresent()){
            gameAnswerRepository.delete(optionalGameAnswer.get());
        }

        game.setNowTurn(game.getNowTurn() + 1);

        GameAnswer gameAnswer = GameAnswer.builder()
                .gameId(gameId)
                .answerName(answer.getName())
                .answerImage(answer.getImage())
                .build();

        gameAnswerRepository.save(gameAnswer);

        return GameInfoDto.builder()
                .userCount(game.getUserCount())
                .gameId(gameId)
                .gameAnswerImage(gameAnswer.getAnswerImage())
                .build();
    }

    public Long createGame() { // 게임 생성
        Game game = Game.builder()
                .gameStatus(false)
                .nowTurn(0)
                .userCount(0)
                .build();

        return gameRepository.save(game).getId();
    }

    public Long getGame() { // 게임방 가져오기
        Optional<Game> game = gameRepository.findOneByRandom(GAME_MAX_USER);
        if (game.isEmpty()) {
            return -1L;
        } else {
            return game.get().getId();
        }
    }
}

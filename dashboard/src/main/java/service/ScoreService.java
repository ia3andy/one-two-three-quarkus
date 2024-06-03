package service;

import entity.Game;
import entity.Score;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ScoreService {

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void persistRank(List<GameService.Runner> rank) {
        if (rank == null || rank.isEmpty()) {
            return;
        }

        Game game = new Game();
        game.dateTime = LocalDateTime.now();
        game.scores = new ArrayList<>();

        for (int i = 0; i < rank.size(); i++) {
            game.scores.add(createScore(rank.get(i).name(), i + 1));
        }

        game.persist();
    }

    private Score createScore(String name, int position) {
        Score score = new Score();
        score.name = name;
        score.position = position;
        return score;
    }

    public List<AverageScore> averageScores() {
        return Score.<Score>streamAll().collect(Collectors.groupingBy(score -> score.name, Collectors.averagingDouble(value -> value.position)))
            .entrySet().stream().map(e -> new AverageScore(e.getKey(), e.getValue()))
            .sorted(Comparator.comparingDouble(value -> value.average))
            .toList();
    }

    public record AverageScore(String name, double average) {

    }
}

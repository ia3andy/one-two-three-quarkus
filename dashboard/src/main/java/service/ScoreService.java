package service;

import entity.Score;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
@Named("scoreService")
public class ScoreService {

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void persistRank(List<GameService.Runner> rank) {
        if (rank == null || rank.isEmpty()) {
            return;
        }
        for (int i = 0; i < rank.size(); i++) {
            createScore(rank.get(i).name(), calculatePoints(i + 1, rank.size())).persist();
        }
    }

    private int calculatePoints(int position, int playersSize) {
        return (playersSize - position) * 10;
    }

    private Score createScore(String name, int points) {
        Score score = new Score();
        score.name = name;
        score.points = points;
        return score;
    }

    public boolean hasScore() {
        return Score.count() > 0;
    }

    public List<ScorePoints> calculatePoints() {
        return Score.<Score>streamAll().collect(Collectors.groupingBy(score -> score.name, Collectors.summingInt(score -> score.points)))
            .entrySet().stream().map(e -> new ScorePoints(e.getKey(), e.getValue()))
            .sorted(Comparator.comparingInt(ScorePoints::points).reversed())
            .toList();
    }

    public record ScorePoints(String name, int points) {}
}

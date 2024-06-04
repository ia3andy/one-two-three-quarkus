package service;

import entity.Score;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ScoreService {

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void persistRank(List<GameService.Runner> rank) {
        if (rank == null || rank.isEmpty()) {
            return;
        }
        for (int i = 0; i < rank.size(); i++) {
            createScore(rank.get(i).name(), i + 1).persist();
        }
    }

    private Score createScore(String name, int position) {
        Score score = new Score();
        score.name = name;
        score.position = position;
        return score;
    }


    public List<ScoreAverage> calculateScoreAverages() {
        return Score.<Score>streamAll().collect(Collectors.groupingBy(score -> score.name, Collectors.averagingDouble(score -> score.position)))
            .entrySet().stream().sorted(Comparator.comparingDouble(Map.Entry::getValue))
            .map(e -> new ScoreAverage(e.getKey(), e.getValue()))
            .toList();
    }

    public record ScoreAverage(String name, double average) {}
}

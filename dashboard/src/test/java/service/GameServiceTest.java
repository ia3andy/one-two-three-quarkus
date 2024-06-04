package service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import utils.NamesUtil;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {

    @Test
    void testRankComparator() {
        GameService.Runner runner1 = new GameService.Runner("1", 1);
        GameService.Runner runner2 = new GameService.Runner("2", 2);
        GameService.Runner runner3 = new GameService.Runner("3", 3);
        GameService.Runner runner4 = new GameService.Runner("4", 4);
        GameService.Runner runner5 = new GameService.Runner("5", 5);
        GameService.Runner runner6 = new GameService.Runner("6", 6);
        GameService.Runner runner7 = new GameService.Runner("7", 7);
        GameService.Runner runner8 = new GameService.Runner("8", 8);
        List<GameService.RunnerState> runners = new ArrayList<>();
        runners.add(runner1.initialState());
        runners.add(runner2.newState(10, 3000, GameService.RunnerState.Status.alive));
        runners.add(runner3.newState(10, 3000, GameService.RunnerState.Status.dead));
        runners.add(runner4.newState(100, 8000, GameService.RunnerState.Status.saved));
        runners.add(runner5.newState(100, 10000, GameService.RunnerState.Status.saved));
        runners.add(runner6.newState(0, 0, GameService.RunnerState.Status.inactive));
        runners.add(runner7.newState(99, 12000, GameService.RunnerState.Status.alive));
        runners.add(runner8.newState(10, 300, GameService.RunnerState.Status.dead));
        final List<GameService.Runner> sorted = runners.stream().sorted(GameService.rankComparator()).map(
                GameService.RunnerState::runner).toList();
        assertEquals(List.of(runner4, runner5, runner7, runner2, runner1, runner8, runner3, runner6), sorted);
    }

    @Test
    public void testName() {
        Assertions.assertEquals(NamesUtil.getNameById(1), "Zipp");
    }
}
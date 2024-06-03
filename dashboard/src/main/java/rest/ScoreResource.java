package rest;

import entity.Score;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import service.ScoreService;

import java.util.List;

@Path("/score")
public class ScoreResource {

    @Inject
    ScoreService scoreService;

    @GET
    public List<Score> all() {
        return Score.listAll();
    }

    @GET
    @Path("/average")
    public List<ScoreService.ScoreAverage> average() {
        return scoreService.calculateScoreAverages();
    }




}

package rest;

import entity.Score;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import service.ScoreService;

import java.util.List;

@Path("/score")
public class ScoreResource {

    @GET
    public List<Score> all() {
        return Score.listAll();
    }

   @Inject
   ScoreService scoreService;

   @GET
   @Path("/points")
   public List<ScoreService.ScorePoints> points() {
       return scoreService.calculatePoints();
   }




}

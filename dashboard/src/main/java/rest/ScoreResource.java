package rest;

import entity.Score;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.util.List;

@Path("/score")
public class ScoreResource {

    @GET
    public List<Score> all() {
        return Score.listAll();
    }

   @GET
   @Path("/total")
   public List<Score.Total> total() {
       return Score.total();
   }




}

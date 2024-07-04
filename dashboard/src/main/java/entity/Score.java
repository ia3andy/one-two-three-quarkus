package entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.Entity;

import java.util.List;

@Entity
public class Score extends PanacheEntity {

    public int game;
    public String name;
    public int points;

    public Score() {
    }

    public Score(int game, String name, int points) {
        this.game = game;
        this.name = name;
        this.points = points;
    }

    public static List<Total> total() {
        return Score.find("select s.name, sum(s.points) from Score s group by s.name").project(Total.class).list();
    }

    @RegisterForReflection
    public record Total(String name, Long points) {}


}

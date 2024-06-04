package entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Game extends PanacheEntity {

    public LocalDateTime dateTime;

    @OneToMany(cascade = CascadeType.PERSIST)
    public List<Score> scores;

}

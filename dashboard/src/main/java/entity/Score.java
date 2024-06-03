package entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Score extends PanacheEntity {

    public String name;
    public int position;
}

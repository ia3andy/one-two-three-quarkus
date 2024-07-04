package entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class PollVote extends PanacheEntity {

    @ManyToOne
    public PollAnswer answer;

    public String player;

    public PollVote() {
    }

    public PollVote(PollAnswer answer, String player) {
        this.answer = answer;
        this.player = player;
    }


}

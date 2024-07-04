package entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
public class PollAnswer extends PanacheEntity {

    @ManyToOne
    public Poll poll;

    public String answer;

    @OneToMany
    public List<PollVote> votes;

    public PollAnswer() {
    }

    public PollAnswer(Poll poll, String answer) {
        this.poll = poll;
        this.answer = answer;
        this.votes = new ArrayList<>();
    }


}

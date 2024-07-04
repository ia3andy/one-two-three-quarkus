package entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Poll extends PanacheEntity {

    public String question;

    @OneToMany(cascade = CascadeType.ALL)
    public List<PollAnswer> answers;

    public Poll() {
    }

    public Poll(String question) {
        this.question = question;
        this.answers = new ArrayList<>();
    }

    public Poll addAnswer(String answer) {
        var a = new PollAnswer(this, answer);
        this.answers.add(a);
        return this;
    }

    public PollResult results(int id) {

    }

    private record PollResult(String question, ) {
    }
}

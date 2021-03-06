/**
 * This file was generated by the Jeddict
 */
package fish.payara.conference.domain;

import java.util.ArrayList;
import java.util.List;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @author gaura
 */
@Entity
@Table(name = "CONF_SPEAKER")
public class Speaker {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Basic
    @JsonbProperty("speakerName")
    @NotEmpty
    private String name;

    @Basic
    @Email
    private String email;

    @Basic
    @JsonbTransient
    private String pin;

    @Basic
    @Enumerated
    private DesignationType designation;

    @Basic
    private String bio;

    @Basic
    private boolean featured;

    @ManyToMany(targetEntity = Session.class, mappedBy = "speakers")
    private List<Session> sessions;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPin() {
        return this.pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public DesignationType getDesignation() {
        return this.designation;
    }

    public void setDesignation(DesignationType designation) {
        this.designation = designation;
    }

    public String getBio() {
        return this.bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isFeatured() {
        return this.featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public List<Session> getSessions() {
        if (sessions == null) {
            sessions = new ArrayList<>();
        }
        return this.sessions;
    }

    public void setSessions(List<Session> sessions) {
        this.sessions = sessions;
    }

    public void addSession(Session session) {
        getSessions().add(session);
    }

    public void removeSession(Session session) {
        getSessions().remove(session);
    }

}
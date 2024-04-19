package fr.norsys.docsapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "metadata")
public class MetaData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    private String key;
    private String value;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "document_id")
    private Document document;

    @Override
    public String toString() {
        return "MetaData: key=" + key + ", value=" + value;
    }
}

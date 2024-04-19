package fr.norsys.docsapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;
    private String name;
    private long size;
    private String type;
    @Column(name = "creation_date")
    private Timestamp creationDate;
    @Column(name = "modification_date")
    private Timestamp modificationDate;
    private String checksum;
    @Column(name = "storage_location")
    private String storageLocation;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL)
    private List<MetaData> metadata;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL)
    private List<PermissionEntry> permissions;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User user;

}

package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.base.BaseEntity;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "center")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Center extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @OneToMany(mappedBy = "center", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Branch> branches = new HashSet<>();
}

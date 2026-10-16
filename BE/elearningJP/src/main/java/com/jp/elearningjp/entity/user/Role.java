package com.jp.elearningjp.entity.user;


import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Entity(name = "roles")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@RequiredArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class Role extends SoftDeletableEntity {
    String name;
    String description;

    @ManyToMany(mappedBy = "roles")
    Set<User> users;


}
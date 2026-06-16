package com.tutornotes.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "standards")
@Data
@NoArgsConstructor
public class Standard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String state;

    @Column(nullable = false, length = 20)
    private String grade;

    @Column(nullable = false, length = 50)
    private String code;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String description; 
}

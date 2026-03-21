package com.Notes.rep.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "notes")
@Data
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String fileUrl; // The link to the actual PDF/Image
    private boolean isPublic; // true = everyone, false = private

    @ManyToOne // Connects this note to a specific Student
    @JoinColumn(name = "user_id")
    private User owner;
}

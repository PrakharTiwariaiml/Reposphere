package com.Notes.rep.Repository;

import com.Notes.rep.entity.Note;
import com.Notes.rep.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
   // List<Note> findByIsPublicTrue();

    // Find all notes belonging to a specific logged-in student
    List<Note> findByOwner(User owner);
}

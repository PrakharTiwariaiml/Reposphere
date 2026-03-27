package com.Notes.rep.Repository;

import com.Notes.rep.entity.Folder;
import com.Notes.rep.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    // This finds all folders belonging to a specific student
    List<Folder> findByOwnerId(Long userId);
    List<Folder> findByOwner(User owner);
    List<Folder> findByIsPublicTrue();


}
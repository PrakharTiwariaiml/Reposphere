package com.Notes.rep.restapi;

import com.Notes.rep.Repository.FolderRepository;
import com.Notes.rep.Repository.NoteRepository;
import com.Notes.rep.config.UserRepository;
import com.Notes.rep.entity.Folder;
import com.Notes.rep.entity.Note;
import com.Notes.rep.entity.User;
import com.Notes.rep.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    @Autowired
    private NoteService noteService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private FolderRepository folderRepository;
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         @RequestParam("folderId") Long folderId,
                         @RequestParam(value = "title", required = false) String title,
                         Authentication authentication) {

        String email = ((OAuth2User) authentication.getPrincipal()).getAttribute("email");
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            return noteService.uploadNote(file, folderId, currentUser, title);
        } catch (IOException e) {
            return "Upload failed: " + e.getMessage();
        }
    }

    @DeleteMapping("/notes/{id}")
    public ResponseEntity<?> deleteNote(@PathVariable Long id, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);

        return noteRepository.findById(id).map(note -> {
            // Check ownership of the specific note
            if (!note.getOwner().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("Error: Permission denied!");
            }

            noteService.deleteNote(id); // Deletes from Cloudinary and DB
            return ResponseEntity.ok("Note deleted!");
        }).orElse(ResponseEntity.status(404).body("Note not found"));
    }
    @GetMapping("/my-notes")
    public ResponseEntity<List<Note>> getMyNotes(Principal principal) {
        // 1. Get user from DB
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Fetch notes from your NoteRepository
        List<Note> notes = noteRepository.findByOwner(user);

        return ResponseEntity.ok(notes);
    }
@PostMapping("/folders/create")
public ResponseEntity<Folder> createFolder(
        @RequestParam String name,
        @RequestParam boolean isPublic,
        Authentication authentication) { // Changed to Authentication

    User user = getAuthenticatedUser(authentication); // Use Helper

    Folder folder = new Folder();
    folder.setFolderName(name);
    folder.setOwner(user);
    folder.setPublic(isPublic);

    return ResponseEntity.ok(folderRepository.save(folder));
}
    @GetMapping("/folders")
    public ResponseEntity<List<Folder>> getAllFolders(Authentication authentication) {
        User user = getAuthenticatedUser(authentication); // Use Helper
        List<Folder> folders = folderRepository.findByOwner(user);
        return ResponseEntity.ok(folders);
    }
    @DeleteMapping("/folders/{id}")
    public ResponseEntity<?> deleteFolder(@PathVariable Long id, Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);

        Folder folder = folderRepository.findById(id).orElse(null);
        if (folder == null) {
            return ResponseEntity.status(404).body("Error: Folder not found.");
        }

        if (folder.getOwner() == null || !folder.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body("Error: Permission denied.");
        }

        try {
            // 1. First, tell Cloudinary to delete all files inside this folder
            // We pass the whole 'folder' object so the service can loop through its notes
            noteService.deleteFolderAssets(folder);

            // 2. Now delete the folder from the DB
            // Because of CascadeType.ALL, this will automatically delete the Note rows in Supabase
            folderRepository.delete(folder);

            return ResponseEntity.ok("Folder and all associated Cloudinary assets deleted!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    @PutMapping("/folders/{id}")
    public ResponseEntity<?> renameFolder(
            @PathVariable Long id,
            @RequestParam String newName,
            Authentication authentication) { // Changed to Authentication

        // 1. Find the folder
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        // 2. Extract Email from Google Authentication
        String email = (authentication.getPrincipal() instanceof OAuth2User oAuth2User)
                ? oAuth2User.getAttribute("email")
                : authentication.getName();

        // 3. Security Check: Compare emails instead of usernames
        if (!folder.getOwner().getEmail().equals(email)) {
            return ResponseEntity.status(403).body("Error: You cannot rename someone else's repo!");
        }

        // 4. Update and Save
        folder.setFolderName(newName);
        folderRepository.save(folder);

        return ResponseEntity.ok(folder);
    }
    @GetMapping("/explore")
    public ResponseEntity<List<Folder>> getPublicFolders() {
        // 1. Fetch all folders marked as public
        List<Folder> publicFolders = folderRepository.findByIsPublicTrue();

        // 2. Return the list (Notes are included because of the @OneToMany relationship)
        return ResponseEntity.ok(publicFolders);
    }
    private User getAuthenticatedUser(Authentication authentication) {
        String email;
        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
        } else {
            email = authentication.getName();
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
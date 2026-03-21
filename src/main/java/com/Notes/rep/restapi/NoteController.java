package com.Notes.rep.restapi;

import com.Notes.rep.Repository.NoteRepository;
import com.Notes.rep.config.UserRepository;
import com.Notes.rep.entity.Note;
import com.Notes.rep.entity.User;
import com.Notes.rep.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notes")
@CrossOrigin(origins = "*")
public class NoteController {

    @Autowired
    private NoteService noteService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NoteRepository noteRepository;
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         @RequestParam("title") String title,
                         @RequestParam("isPublic") boolean isPublic,
                         Principal principal) { // Principal holds the logged-in username
        try {
            // 1. Get the username from the JWT Token (Principal)
            String username = principal.getName();

            // 2. Find the actual User object from your Database
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 3. Pass the REAL user object to the service
            return noteService.uploadNote(file, title, isPublic, currentUser);
        } catch (Exception e) {
            return "Upload failed: " + e.getMessage();
        }
    }
    @GetMapping("/public")
    public List<Note> getAllPublicNotes() {
        // This calls the Repository method we created earlier
        return noteRepository.findByIsPublicTrue();
    }
}
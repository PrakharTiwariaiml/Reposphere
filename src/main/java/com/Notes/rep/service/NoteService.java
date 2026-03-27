package com.Notes.rep.service;

import com.Notes.rep.Repository.FolderRepository;
import com.Notes.rep.Repository.NoteRepository;
import com.Notes.rep.entity.Folder;
import com.Notes.rep.entity.Note;
import com.Notes.rep.entity.User;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.cloudinary.utils.ObjectUtils;

@Service
public class NoteService {

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private FolderRepository folderRepository;

    // Add this import for Cloudinary deletion

    public String uploadNote(MultipartFile file, Long folderId, User owner, String customTitle) throws IOException {
        Folder dbFolder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        // 1. Upload to Cloudinary
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto",
                "folder", "reposphere_notes"
        ));

        String secureUrl = (String) uploadResult.get("secure_url");
        String cloudinaryPublicId = (String) uploadResult.get("public_id");

        // 2. Save to DB with custom title
        Note note = new Note();
        // Use custom title if provided, otherwise fallback to original filename
        note.setTitle(customTitle != null && !customTitle.isEmpty() ? customTitle : file.getOriginalFilename());
        note.setFileUrl(secureUrl);
        note.setPublicId(cloudinaryPublicId);
        note.setOwner(owner);
        note.setFolder(dbFolder);

        noteRepository.save(note);
        return secureUrl;
    }

    public void deleteNote(Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        try {
            // 1. Delete from Cloudinary
            if (note.getPublicId() != null) {
                cloudinary.uploader().destroy(note.getPublicId(), ObjectUtils.emptyMap());
            }
            // 2. Delete from Database
            noteRepository.delete(note);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file from Cloudinary");
        }
    }
    public void deleteFolderAssets(Folder folder) {
        List<Note> notes = folder.getNotes();

        if (notes != null) {
            for (Note note : notes) {
                try {
                    if (note.getPublicId() != null) {
                        // Delete each file from Cloudinary using its unique publicId
                        cloudinary.uploader().destroy(note.getPublicId(), ObjectUtils.emptyMap());
                    }
                } catch (IOException e) {
                    System.err.println("Cloudinary cleanup failed for note: " + note.getId());
                }
            }
        }
    }
    public List<Note> getAllPublicNotes() {
        List<Folder> publicFolders = folderRepository.findByIsPublicTrue();
        List<Note> allPublicNotes = new ArrayList<>();

        for (Folder folder : publicFolders) {
            allPublicNotes.addAll(folder.getNotes());
        }
        return allPublicNotes;
    }
}
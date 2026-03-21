package com.Notes.rep.service;

import com.Notes.rep.Repository.NoteRepository;
import com.Notes.rep.entity.Note;
import com.Notes.rep.entity.User;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class NoteService {

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private NoteRepository noteRepository;

    public String uploadNote(MultipartFile file, String title, boolean isPublic, User owner) throws IOException {
        // 1. Upload the file to Cloudinary
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());

        // 2. Get the secure URL from the result
        String url = uploadResult.get("secure_url").toString();

        // 3. Save the Metadata to your Postgres Database
        Note note = new Note();
        note.setTitle(title);
        note.setFileUrl(url); // Now storing the Cloudinary link!
        note.setPublic(isPublic);
        note.setOwner(owner);

        noteRepository.save(note);
        return "Note uploaded to cloud: " + url;
    }
}
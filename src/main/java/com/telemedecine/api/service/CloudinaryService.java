package com.telemedecine.api.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public interface CloudinaryService {
    public String uploadImage(MultipartFile file) throws IOException;;

}
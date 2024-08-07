package com.marcosdjs.logprocessor.resource;

import com.marcosdjs.logprocessor.service.LogProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@RestController
public class LogProcessorResource {

    private final LogProcessorService logProcessorService;

    @Autowired
    public LogProcessorResource(LogProcessorService logProcessorService) {
        this.logProcessorService = logProcessorService;
    }

    @PostMapping
    public ResponseEntity<InputStreamResource> processarLogs(FilesFinderDto dto) {
        File file = logProcessorService.processarArquivosLogs(dto.files(), dto.idsNfes());
        if (file == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() +"\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(mapToInputStreamResource(file));
    }

    private InputStreamResource mapToInputStreamResource(File file) {
        try {
            return new InputStreamResource(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private record FilesFinderDto(List<MultipartFile> files, List<Long> idsNfes){}
}

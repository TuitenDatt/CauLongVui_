package com.example.CauLongVui.controller;

import com.example.CauLongVui.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    // Lưu vào thư mục src/main/resources/static/uploads/ (được Spring serve tự động qua /uploads/**)
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/";

    @PostMapping
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Vui lòng chọn file ảnh!"));
        }

        // Kiểm tra định dạng file
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "image");
        String ext = "";
        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx >= 0) ext = originalName.substring(dotIdx).toLowerCase();

        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp)")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Chỉ chấp nhận file ảnh: jpg, png, gif, webp"));
        }

        // Tạo tên file duy nhất
        String newFileName = UUID.randomUUID().toString() + ext;

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);
            Path targetFile = uploadPath.resolve(newFileName);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/" + newFileName;
            log.info("Uploaded file: {}", targetFile.toAbsolutePath());
            return ResponseEntity.ok(ApiResponse.success("Upload thành công", fileUrl));

        } catch (IOException e) {
            log.error("Upload failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Lỗi khi lưu file: " + e.getMessage()));
        }
    }
}

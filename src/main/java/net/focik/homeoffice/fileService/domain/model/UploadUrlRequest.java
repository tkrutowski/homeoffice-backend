package net.focik.homeoffice.fileService.domain.model;

import net.focik.homeoffice.utils.share.Module;

public record UploadUrlRequest(String fileName, String contentType, Module module) {}

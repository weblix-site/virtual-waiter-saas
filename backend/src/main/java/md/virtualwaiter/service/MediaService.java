package md.virtualwaiter.service;

import md.virtualwaiter.config.MediaProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaService {
  private static final DateTimeFormatter PATH_FMT = DateTimeFormatter.ofPattern("yyyy/MM");
  private static final Set<String> ALLOWED_TYPES = Set.of("staff", "food", "logo", "hall", "other");

  private final Path storageRoot;
  private final String publicBaseUrl;
  private final long maxUploadBytes;
  private final long maxVideoBytes;
  private final Set<String> allowedExts;
  private final Set<String> allowedVideoExts;

  public MediaService(MediaProperties props) {
    this.storageRoot = Path.of(props.storageRoot).toAbsolutePath().normalize();
    this.publicBaseUrl = trimTrailingSlash(props.publicBaseUrl);
    this.maxUploadBytes = props.maxUploadBytes;
    this.maxVideoBytes = props.maxVideoBytes;
    this.allowedExts = parseExts(props.allowedPhotoExts);
    this.allowedVideoExts = parseExts(props.allowedVideoExts);
  }

  public record UploadResult(String url, String relativePath, long size, String contentType) {}

  public UploadResult store(MultipartFile file, String rawType) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
    }
    String type = normalizeType(rawType);
    String ext = resolveExt(file);
    if (ext == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to detect file type");
    }
    boolean isVideo = allowedVideoExts.contains(ext);
    if (isVideo) {
      if (maxVideoBytes > 0 && file.getSize() > maxVideoBytes) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video file is too large");
      }
      if (!allowedVideoExts.isEmpty() && !allowedVideoExts.contains(ext)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported video type");
      }
    } else {
      if (maxUploadBytes > 0 && file.getSize() > maxUploadBytes) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is too large");
      }
      if (!allowedExts.isEmpty() && !allowedExts.contains(ext)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file type");
      }
    }

    String datePath = LocalDate.now().format(PATH_FMT);
    String filename = UUID.randomUUID() + "." + ext;
    Path dir = storageRoot.resolve(type).resolve(datePath);
    try {
      Files.createDirectories(dir);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create directory");
    }
    Path target = dir.resolve(filename);
    try (InputStream in = file.getInputStream()) {
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file");
    }

    String relative = "/media/" + type + "/" + datePath + "/" + filename;
    String url = publicBaseUrl + relative;
    return new UploadResult(url, relative, file.getSize(), file.getContentType());
  }

  private String normalizeType(String raw) {
    if (raw == null || raw.isBlank()) return "other";
    String t = raw.trim().toLowerCase(Locale.ROOT);
    if (!ALLOWED_TYPES.contains(t)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown media type");
    }
    return t;
  }

  private String resolveExt(MultipartFile file) {
    String name = file.getOriginalFilename();
    if (name != null) {
      int dot = name.lastIndexOf('.');
      if (dot >= 0 && dot < name.length() - 1) {
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
      }
    }
    String ct = file.getContentType();
    if (ct == null) return null;
    return switch (ct.toLowerCase(Locale.ROOT)) {
      case "image/jpeg" -> "jpg";
      case "image/png" -> "png";
      case "image/webp" -> "webp";
      case "image/gif" -> "gif";
      case "video/mp4" -> "mp4";
      case "video/webm" -> "webm";
      case "video/quicktime" -> "mov";
      default -> null;
    };
  }

  private static Set<String> parseExts(String raw) {
    Set<String> out = new HashSet<>();
    if (raw == null) return out;
    for (String p : raw.split(",")) {
      String t = p.trim().toLowerCase(Locale.ROOT);
      if (!t.isEmpty()) out.add(t);
    }
    return out;
  }

  private static String trimTrailingSlash(String v) {
    if (v == null || v.isBlank()) return "";
    String s = v.trim();
    if (s.endsWith("/")) return s.substring(0, s.length() - 1);
    return s;
  }
}

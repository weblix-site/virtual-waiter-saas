package md.virtualwaiter.api.admin;

import md.virtualwaiter.domain.StaffUser;
import md.virtualwaiter.repo.StaffUserRepo;
import md.virtualwaiter.service.MediaService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/media")
public class MediaController {
  private final MediaService mediaService;
  private final StaffUserRepo staffUserRepo;

  public MediaController(MediaService mediaService, StaffUserRepo staffUserRepo) {
    this.mediaService = mediaService;
    this.staffUserRepo = staffUserRepo;
  }

  public record UploadResponse(String url, String relativePath, long size, String contentType) {}

  @PostMapping("/upload")
  public UploadResponse upload(
    @RequestParam("file") MultipartFile file,
    @RequestParam(value = "type", required = false) String type,
    Authentication auth
  ) {
    requireAdmin(auth);
    MediaService.UploadResult r = mediaService.store(file, type);
    return new UploadResponse(r.url(), r.relativePath(), r.size(), r.contentType());
  }

  private StaffUser requireAdmin(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No auth");
    }
    StaffUser u = staffUserRepo.findByUsername(auth.getName())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    String role = u.role == null ? "" : u.role.toUpperCase(Locale.ROOT);
    if (!Set.of("ADMIN", "MANAGER", "SUPER_ADMIN").contains(role)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
    }
    return u;
  }
}

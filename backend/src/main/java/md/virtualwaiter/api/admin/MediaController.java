package md.virtualwaiter.api.admin;

import md.virtualwaiter.domain.StaffUser;
import md.virtualwaiter.repo.StaffUserRepo;
import md.virtualwaiter.security.AuthzService;
import md.virtualwaiter.security.Permission;
import md.virtualwaiter.service.MediaService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/api/admin/media")
public class MediaController {
  private final MediaService mediaService;
  private final StaffUserRepo staffUserRepo;
  private final AuthzService authzService;

  public MediaController(MediaService mediaService, StaffUserRepo staffUserRepo, AuthzService authzService) {
    this.mediaService = mediaService;
    this.staffUserRepo = staffUserRepo;
    this.authzService = authzService;
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
    authzService.require(u, Permission.MEDIA_MANAGE);
    return u;
  }
}

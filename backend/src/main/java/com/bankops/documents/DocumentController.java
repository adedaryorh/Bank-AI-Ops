package com.bankops.documents;
import com.bankops.common.UserContext;
import com.bankops.common.PageResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/documents")
public class DocumentController {
    private final DocumentService service;
    public DocumentController(DocumentService service) { this.service=service; }
    @GetMapping public PageResponse<DocumentService.Document> list(@RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) UUID caseId,@RequestParam(required=false) String purpose) {
        return service.list(user,page,size,caseId,purpose);
    }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public DocumentService.Document upload(@RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user,
        @RequestPart MultipartFile file,@RequestParam String purpose,@RequestParam(required=false) UUID caseId,@RequestParam boolean synthetic) {
        return service.upload(user,file,purpose,caseId,synthetic);
    }
    @GetMapping("/{id}/download") public ResponseEntity<byte[]> download(@RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user,@PathVariable UUID id) {
        var file=service.download(user,id);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.filename()).build().toString())
            .header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore()).body(file.data());
    }
}

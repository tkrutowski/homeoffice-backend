package net.focik.homeoffice.fileService.infrastructure.dto;

import jakarta.persistence.*;
import lombok.*;
import net.focik.homeoffice.utils.share.Module;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "files")
public
class FileInfoDbDto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private String url;
    private String type;
    private Integer size;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(nullable = false)
    private LocalDateTime uploadDate = LocalDateTime.now();
    private String description;

    @Enumerated(EnumType.STRING)
    private Module module;  // np. "DEVICE", "INVOICE"

    @Column(name = "owner_id")
    private Integer ownerId;

}
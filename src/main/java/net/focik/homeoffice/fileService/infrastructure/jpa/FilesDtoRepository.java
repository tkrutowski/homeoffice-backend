package net.focik.homeoffice.fileService.infrastructure.jpa;

import net.focik.homeoffice.fileService.infrastructure.dto.FileInfoDbDto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilesDtoRepository extends JpaRepository<FileInfoDbDto, Integer> {

}

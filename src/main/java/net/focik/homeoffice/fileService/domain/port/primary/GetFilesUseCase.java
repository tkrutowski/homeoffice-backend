package net.focik.homeoffice.fileService.domain.port.primary;

import net.focik.homeoffice.utils.share.Module;
import org.springframework.core.io.Resource;

public interface GetFilesUseCase {
    Resource downloadFile(Module module, String fileName);
}

    package net.focik.homeoffice.fileService.domain.port.primary;

    import net.focik.homeoffice.utils.share.Module;

    public interface DeleteFileUseCase {

    void deleteFile(Module module, String fileName);
}

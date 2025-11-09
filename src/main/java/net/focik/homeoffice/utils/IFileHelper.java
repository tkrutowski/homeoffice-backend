package net.focik.homeoffice.utils;

import net.focik.homeoffice.utils.share.Module;

public interface IFileHelper {
    String downloadAndSaveImage(String imageUrl, String name, Module module);
}

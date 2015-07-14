package de.thm.nfcmemory.util;

/**
 * Created by Nils on 14.07.2015.
 */
public class FileName {
    private static final String EXT_SEPARATOR = ".";
    private static final String PATH_SEPARATOR = "/";

    public static String extension(String path){
        return path.substring(path.lastIndexOf(EXT_SEPARATOR) + 1);
    }

    public static String basename(String path){
        return path.substring(
                path.lastIndexOf(PATH_SEPARATOR) + 1,
                path.lastIndexOf(EXT_SEPARATOR));
    }
}

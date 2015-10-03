package de.thm.nfcmemory.util;

/**
 * Created by Nils on 14.07.2015.
 */
public class FileName {
    private static final String EXT_SEPARATOR = ".";
    private static final String PATH_SEPARATOR = "/";

    public static String extension(String path){
        int i = path.lastIndexOf(EXT_SEPARATOR);
        int p = path.lastIndexOf(PATH_SEPARATOR);

        return i > p ? path.substring(i+1) : "";
    }

    public static String basename(String path){
        return path.substring(
                path.lastIndexOf(PATH_SEPARATOR) + 1,
                path.lastIndexOf(EXT_SEPARATOR));
    }
}

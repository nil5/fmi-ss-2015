package de.thm.nfcmemory.model;

import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.thm.nfcmemory.NFCMemory;
import de.thm.nfcmemory.util.FileName;

/**
 * Created by Nils on 13.07.2015.
 */
public class CardSet extends ArrayList<Card> {
    private static final boolean D = true;
    private static final String TAG = "CardSet";
    private static final String CARD_SET_FOLDER = Environment.getExternalStorageDirectory() + NFCMemory.Const.SD_FOLDER + "/CardSets/";
    private static final String ALLOWED_EXTENSIONS[] = new String[]{"png"};

    public final String name;

    public CardSet(String name) throws IllegalArgumentException, FileNotFoundException {
        if(name == null || name.length() < 1)
            throw new IllegalArgumentException("The argument 'name' must neither be null nor an empty string.");
        this.name = name;

        final File dir = new File(CARD_SET_FOLDER + name);

        if(!dir.exists())
            throw new FileNotFoundException("The directory '" + dir + "' does not exist.");

        if(D) Log.d(TAG, "Listing files of CardSet '" + name + "'");
        int value = 0;
        for(File file : dir.listFiles()){
            final String fileName = file.getName();
            final String extension = FileName.extension(fileName);
            if(D) Log.d(TAG, "Found file: " + fileName + ", detected extension: " + extension);
            if(!Arrays.asList(ALLOWED_EXTENSIONS).contains(extension)){
                if(D) Log.d(TAG, "Invalid extension. Skipping file.");
                continue;
            } else if(D) Log.d(TAG, "Creating Bitmap...");
            add(new Card(FileName.basename(fileName), ++value, BitmapFactory.decodeFile(dir + "/" + fileName)));
        }
    }

    public static String[] getList() throws FileNotFoundException {
        final File dir = new File(CARD_SET_FOLDER);
        final List<String> cardSets = new ArrayList<>();
        final FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return Arrays.asList(ALLOWED_EXTENSIONS)
                        .contains(FileName.extension(filename));
            }
        };

        if(!dir.exists())
            throw new FileNotFoundException("The directory '" + dir + "' does not exist.");

        cardSets.add("default");

        if(D) Log.d(TAG, "Searching for card sets.");
        for(File file : dir.listFiles()){
            if(file.isFile()) continue;
            String fileName = file.getName();
            File images[] = new File(CARD_SET_FOLDER + fileName).listFiles(filter);
            if(images.length < 2){
                if(D) Log.d(TAG, "Card set contains less then 2 valid images. Skipping card set.");
                continue;
            } else if(D) Log.d(TAG, "Adding card set to list: '" + fileName + "'");
            cardSets.add(fileName);
        }

        return cardSets.toArray(new String[cardSets.size()]);
    }
}

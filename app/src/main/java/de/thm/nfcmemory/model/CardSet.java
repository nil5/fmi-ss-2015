package de.thm.nfcmemory.model;

import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import de.thm.nfcmemory.util.FileName;

/**
 * Created by Nils on 13.07.2015.
 */
public class CardSet extends ArrayList<Card> {
    private static final boolean D = true;
    private static final String TAG = "CardSet";
    private static final String SD_FOLDER = "/NFCMemory/CardSets";

    private String name;

    public CardSet(String name) throws IllegalArgumentException, FileNotFoundException {
        if(name == null || name.length() < 1)
            throw new IllegalArgumentException("The argument 'name' must neither be null nor an empty string.");

        File sd = Environment.getExternalStorageDirectory();
        File dir = new File(sd + SD_FOLDER + "/" + name);

        if(!dir.exists())
            throw new FileNotFoundException("The directory '" + dir + "' does not exist.");

        if(D) Log.d(TAG, "Listing files of CardSet '" + name + "'");
        for(File file : dir.listFiles()){
            String fileName = file.getName();
            if(D) Log.d(TAG, "Found file: " + fileName);
            if(!fileName.endsWith("png")) continue;
            if(D) Log.d(TAG, "Creating Bitmap...");
            add(new Card(FileName.basename(fileName), BitmapFactory.decodeFile(dir + "/" + fileName)));
        }
    }
}

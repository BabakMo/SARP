package whi.ucla.erlab.gimbal;

/**
 * Kaitlin 2015/4/2
 */

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class Kompressor{

/*
 * Data that is sent to the phone will be compressed and saved to a folder in the watch's root directory
 */
    

/*
* Preliminary algorithm:
*
* Check the files we wish to have sent to the phone
*   if files sent correctly
*       compress
*   else
*       retry
*   ...
*
*   Compress files that were sent into a gzip file
*       save this gzip file into the appropriate folder in the root directory
*
*
*      go through the list of the files we have sent
*      for files in list
*           attempt to compress
*           if compression works
*              add to Compressed folder
*          else
*               leave the file in the list and go to the next
*/

    //CHECK IF WE WANT TO SAVE THIS LIST OR ADD TO FOLDER, THEN COMPRESS
    private static final int byteArraySize = 64 * 1024;

    /*
    * ASSUMPTIONS: All files that are obtained by the function getSentFiles are assumed to have
    *   have been successfully sent to the phone(gateway)
    * */

    /*
    * Compress data into a gzip file
    */
        public static void kompressFiles(File sourceDirectoryFile, String ignore_extension){
            //Create the CompressedFile directory if it does not already exist

        Constants.createDirIfNotExists(Constants.compressed_directory);

            //Get a list of files that we wish to compress
        List<File> sent_files = Constants.getListFiles(sourceDirectoryFile, ignore_extension);
        for(File file : sent_files){
            //if the compression was successful, then add it to the Compressed folder
            if(kompressSingleFile(file)){
                //Deletes source file if it was compressed succesfully.
                try{
                   file.delete();
                }catch(Exception e){
                    //e.printStackTrace();
                }
            }
        }
    }
    /*http://www.journaldev.com/966/java-gzip-example-compress-and-decompress-file-in-gzip-format-in-java*/
    private static boolean kompressSingleFile(File file){

        String outputFile = Constants.compressed_directory+"/"+file.getName()+".gz";
        BufferedInputStream bfis = null;
        BufferedOutputStream bos = null;
        GZIPOutputStream gzos = null;
        try{
            bfis = new BufferedInputStream(new FileInputStream(file));
            bos = new BufferedOutputStream(new FileOutputStream(outputFile, false));
            gzos = new GZIPOutputStream(bos, byteArraySize, false);

            byte[] buffer = new byte[byteArraySize];
            int length;

            while((length = bfis.read(buffer)) != -1){
                gzos.write(buffer, 0, length);
            }
                gzos.finish();
        }catch(Exception e){
            //file failed to compress
            //e.printStackTrace();
            return false;
        }

        try {
            gzos.close();
            bfis.close();
        } catch (Exception e) {
            //e.printStackTrace();
            if(Constants.DEBUG)  Log.d("Kompression Error : ", file.getName());
        }
        //successful compression of the file
        if(Constants.DEBUG)  Log.d("Kompressing : ", file.getName());
        return true;
    }



}


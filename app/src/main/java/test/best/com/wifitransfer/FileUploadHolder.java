package test.best.com.wifitransfer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUploadHolder {

    private File recievedFile;
    public String fileName;
    public BufferedOutputStream fileOutPutStream;
    public long totalSize;

    public BufferedOutputStream getFileOutPutStream(){
        return fileOutPutStream;
    }

    public void setFileName(String fileName){
        this.fileName = fileName;
        totalSize = 0;
        if (!Constants.DIR.exists()){
            Constants.DIR.mkdirs();
        }
        this.recievedFile = new File(Constants.DIR, this.fileName);
        try {
            fileOutPutStream = new BufferedOutputStream(new FileOutputStream(recievedFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void reset(){
        if (fileOutPutStream != null){
            try {
                fileOutPutStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        fileOutPutStream = null;
    }

    public void write(byte[] data){
        if (fileOutPutStream != null){
            try {
                fileOutPutStream.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        totalSize += data.length;
    }
}

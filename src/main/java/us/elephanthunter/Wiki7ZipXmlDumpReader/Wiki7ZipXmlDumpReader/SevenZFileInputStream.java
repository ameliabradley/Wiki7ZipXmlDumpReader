package us.elephanthunter.Wiki7ZipXmlDumpReader.Wiki7ZipXmlDumpReader;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.sevenz.SevenZFile;

/**
 * Created by elephanthunter on 6/9/16.
 */
public class SevenZFileInputStream extends InputStream {
    private final SevenZFile sevenZFile;

    private long bytesRead = 0;

    public SevenZFileInputStream(SevenZFile sevenZFile) {
        this.sevenZFile = sevenZFile;
    }

    @Override
    public int read() throws IOException {
        bytesRead++;
        return this.sevenZFile.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        int retVal = this.sevenZFile.read(b);
        bytesRead += b.length;
        return retVal;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int retVal = this.sevenZFile.read(b, off, len);
        bytesRead += b.length;
        return retVal;
    }

    public long getBytesRead() {
        return bytesRead;
    }
}
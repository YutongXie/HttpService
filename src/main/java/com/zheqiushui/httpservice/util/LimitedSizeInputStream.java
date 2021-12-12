package com.zheqiushui.httpservice.util;

import lombok.Data;

import javax.print.event.PrintJobAttributeEvent;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Qiushui.Zhe
 * @date 2021/12/12 21:44
 */
@Data
public class LimitedSizeInputStream extends InputStream {

    private final InputStream original;
    private final long maxSize;
    private long totalSize;

    public LimitedSizeInputStream(InputStream original, long maxSize) {
        this.original = original;
        this.maxSize = maxSize;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int i = original.read(b, off, len);
        if(i >=0) incrementalCounter(i);
        return i;
    }

    @Override
    public int read() throws IOException {
        int i = original.read();
        if(i >=0) incrementalCounter(1);
        return i;
    }

    private void incrementalCounter(int size) throws IOException {
        totalSize += size;
        if(totalSize > maxSize) throw new IOException("InputStream exceeded max size in bytes");
    }
}

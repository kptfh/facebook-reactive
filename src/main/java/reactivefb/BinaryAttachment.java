package reactivefb;

import com.restfb.util.ObjectUtil;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;

public class BinaryAttachment {

    private final String filename;
    private final Publisher<ByteBuffer> data;
    private final String contentType;
    private final String fieldName;

    public BinaryAttachment(String filename, Publisher<ByteBuffer> data, String contentType, String fieldName) {
        ObjectUtil.requireNotEmpty(filename, "Binary attachment filename cannot be blank.");
        ObjectUtil.requireNotEmpty(fieldName, "Field name cannot be null.");
        ObjectUtil.requireNotEmpty(contentType, "Content type cannot be null.");
        this.filename = filename;
        this.data = data;
        this.contentType = contentType;
        this.fieldName = fieldName;
    }

    public String getFilename() {
        return filename;
    }

    public Publisher<ByteBuffer> getData() {
        return data;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFieldName() {
        return fieldName;
    }
}
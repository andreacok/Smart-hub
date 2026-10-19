package com.smarthub.service;

import com.smarthub.exception.RoomImageException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomImageProcessorTest {

    private final RoomImageProcessor processor = new RoomImageProcessor();

    @Test
    void largeJpegIsResizedAndReencodedWithoutMetadata() throws IOException {
        byte[] input = encode(new BufferedImage(3000, 2000, BufferedImage.TYPE_INT_RGB), "jpg");

        byte[] output = processor.process(input);

        assertEquals(RoomImageProcessor.Format.JPEG, RoomImageProcessor.detect(output));
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(output));
        assertEquals(2400, decoded.getWidth());
        assertEquals(1600, decoded.getHeight());
    }

    @Test
    void exifAndGpsMarkersAreNotPresentInOutput() throws IOException {
        // Un JPEG con un APP1 "Exif" + GPS fittizio: l'output ricodificato non deve contenerli.
        byte[] base = encode(new BufferedImage(200, 150, BufferedImage.TYPE_INT_RGB), "jpg");
        byte[] withExif = injectApp1(base, "Exif\0\0GPSLatitude");

        byte[] output = processor.process(withExif);

        assertFalse(contains(output, "Exif"));
        assertFalse(contains(output, "GPS"));
    }

    @Test
    void pngWithTransparencyBecomesJpeg() throws IOException {
        BufferedImage transparent = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        transparent.setRGB(10, 10, new Color(255, 0, 0, 0).getRGB());
        byte[] input = encode(transparent, "png");

        byte[] output = processor.process(input);

        assertEquals(RoomImageProcessor.Format.JPEG, RoomImageProcessor.detect(output));
    }

    @Test
    void smallImageIsNotUpscaled() throws IOException {
        byte[] input = encode(new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB), "jpg");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(processor.process(input)));
        assertEquals(640, decoded.getWidth());
        assertEquals(480, decoded.getHeight());
    }

    @Test
    void tooLargeFileIsRejectedWith413() {
        byte[] huge = new byte[(int) RoomImageProcessor.MAX_BYTES + 1];
        RoomImageException e = assertThrows(RoomImageException.class, () -> processor.process(huge));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, e.getStatus());
        assertEquals("FILE_TOO_LARGE", e.getCode());
    }

    @Test
    void textDisguisedAsImageIsRejected() {
        byte[] text = "non sono una foto".getBytes(StandardCharsets.UTF_8);
        RoomImageException e = assertThrows(RoomImageException.class, () -> processor.process(text));
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getStatus());
        assertEquals("UNSUPPORTED_FORMAT", e.getCode());
    }

    @Test
    void heicGetsAClearMessage() {
        byte[] heic = new byte[32];
        System.arraycopy("ftypheic".getBytes(StandardCharsets.US_ASCII), 0, heic, 4, 8);
        RoomImageException e = assertThrows(RoomImageException.class, () -> processor.process(heic));
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getStatus());
        assertTrue(e.getMessage().contains("HEIC"));
    }

    @Test
    void webpIsRejectedServerSideWithAClearMessage() {
        byte[] webp = new byte[32];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, webp, 0, 4);
        System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, webp, 8, 4);
        RoomImageException e = assertThrows(RoomImageException.class, () -> processor.process(webp));
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getStatus());
        assertTrue(e.getMessage().contains("WebP"));
    }

    @Test
    void corruptJpegIsReportedAsUnprocessable() {
        byte[] corrupt = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02, 0x03, 0x04};
        RoomImageException e = assertThrows(RoomImageException.class, () -> processor.process(corrupt));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, e.getStatus());
        assertEquals("CORRUPT_FILE", e.getCode());
    }

    @Test
    void emptyFileIsBadRequest() {
        RoomImageException e = assertThrows(RoomImageException.class, () -> processor.process(new byte[0]));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void detectRecognisesMagicNumbers() {
        assertEquals(RoomImageProcessor.Format.JPEG, RoomImageProcessor.detect(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}));
        assertEquals(RoomImageProcessor.Format.PNG, RoomImageProcessor.detect(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0}));
        assertEquals(RoomImageProcessor.Format.UNKNOWN, RoomImageProcessor.detect(new byte[]{'G', 'I', 'F', '8'}));
    }

    private static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, out));
        return out.toByteArray();
    }

    /** Inserisce un segmento APP1 subito dopo SOI, simulando metadati EXIF/GPS. */
    private static byte[] injectApp1(byte[] jpeg, String payload) {
        byte[] body = payload.getBytes(StandardCharsets.US_ASCII);
        int length = body.length + 2;
        byte[] out = new byte[jpeg.length + body.length + 4];
        out[0] = jpeg[0];
        out[1] = jpeg[1];
        out[2] = (byte) 0xFF;
        out[3] = (byte) 0xE1;
        out[4] = (byte) (length >> 8);
        out[5] = (byte) length;
        System.arraycopy(body, 0, out, 6, body.length);
        System.arraycopy(jpeg, 2, out, 6 + body.length, jpeg.length - 2);
        return out;
    }

    private static boolean contains(byte[] haystack, String needle) {
        byte[] n = needle.getBytes(StandardCharsets.US_ASCII);
        outer:
        for (int i = 0; i + n.length <= haystack.length; i++) {
            for (int j = 0; j < n.length; j++) {
                if (haystack[i + j] != n[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}

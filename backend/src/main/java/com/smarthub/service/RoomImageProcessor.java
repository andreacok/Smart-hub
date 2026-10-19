package com.smarthub.service;

import com.smarthub.exception.RoomImageException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * Valida e normalizza una fotografia caricata dall'utente.
 *
 * Il contenuto viene verificato dai byte iniziali e poi decodificato davvero: nome file e MIME dichiarato
 * non sono fidati. L'output è sempre un JPEG ricodificato da zero, quindi senza EXIF, GPS né altri metadati,
 * con lato lungo massimo {@link #MAX_EDGE} px.
 */
@Component
public class RoomImageProcessor {

    /** Dimensione massima del file caricato. */
    public static final long MAX_BYTES = 10L * 1024 * 1024;

    /** Lato lungo massimo dell'immagine salvata (px). Sufficiente per le hero a tutta larghezza. */
    static final int MAX_EDGE = 2400;

    /** Limite di pixel decodificati: protegge da immagini compresse che esplodono in memoria. */
    static final long MAX_PIXELS = 40_000_000L;

    static final float JPEG_QUALITY = 0.88f;

    /** Brand HEIF/HEIC riconosciuti nel box "ftyp". */
    private static final Set<String> HEIF_BRANDS = Set.of(
            "heic", "heix", "hevc", "hevx", "heim", "heis", "mif1", "msf1");

    public enum Format { JPEG, PNG, WEBP, HEIF, UNKNOWN }

    /** Riconosce il formato dai byte iniziali (magic number), mai dal nome o dal MIME dichiarato. */
    public static Format detect(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Format.JPEG;
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return Format.PNG;
        }
        if (b.length >= 12 && ascii(b, 0, "RIFF") && ascii(b, 8, "WEBP")) {
            return Format.WEBP;
        }
        if (b.length >= 12 && ascii(b, 4, "ftyp")) {
            String brand = new String(b, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if (HEIF_BRANDS.contains(brand)) {
                return Format.HEIF;
            }
        }
        return Format.UNKNOWN;
    }

    /** Valida e converte in JPEG web-friendly. Lancia {@link RoomImageException} con un messaggio utilizzabile in UI. */
    public byte[] process(byte[] input) {
        if (input.length == 0) {
            throw new RoomImageException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "Il file è vuoto.");
        }
        if (input.length > MAX_BYTES) {
            throw new RoomImageException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                    "La foto supera il limite di 10 MB.");
        }
        switch (detect(input)) {
            case JPEG, PNG -> { }
            case HEIF -> throw new RoomImageException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FORMAT",
                    "Le foto HEIC vanno convertite in JPEG prima del caricamento.");
            case WEBP -> throw new RoomImageException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FORMAT",
                    "Formato WebP non elaborabile dal server: caricare una foto JPEG o PNG.");
            default -> throw new RoomImageException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FORMAT",
                    "Formato non supportato: usa JPEG, PNG, WebP o HEIC.");
        }

        BufferedImage decoded = decode(input);
        BufferedImage scaled = scaleToFit(decoded, MAX_EDGE);
        return encodeJpeg(scaled);
    }

    private static BufferedImage decode(byte[] input) {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
            if (stream == null) {
                throw corrupt();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw corrupt();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels <= 0 || pixels > MAX_PIXELS) {
                    throw new RoomImageException(HttpStatus.UNPROCESSABLE_ENTITY, "IMAGE_TOO_LARGE",
                            "Le dimensioni della foto sono troppo grandi.");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw corrupt();
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof RoomImageException re) {
                throw re;
            }
            throw corrupt();
        }
    }

    /** Ridimensiona mantenendo le proporzioni e disegna su RGB: l'alpha dei PNG diventa sfondo bianco. */
    static BufferedImage scaleToFit(BufferedImage source, int maxEdge) {
        int w = source.getWidth();
        int h = source.getHeight();
        double factor = Math.min(1.0, (double) maxEdge / Math.max(w, h));
        int nw = Math.max(1, (int) Math.round(w * factor));
        int nh = Math.max(1, (int) Math.round(h * factor));
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, nw, nh);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    /** Codifica JPEG senza metadati: nessun IIOMetadata viene passato, quindi EXIF e GPS non esistono nel file. */
    static byte[] encodeJpeg(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(JPEG_QUALITY);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(image, null, null), params);
        } catch (IOException e) {
            throw new RoomImageException(HttpStatus.INTERNAL_SERVER_ERROR, "PROCESSING_ERROR",
                    "Non è stato possibile elaborare la foto. Riprova.");
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    private static RoomImageException corrupt() {
        return new RoomImageException(HttpStatus.UNPROCESSABLE_ENTITY, "CORRUPT_FILE",
                "Il file è corrotto o non può essere letto come immagine.");
    }

    private static boolean ascii(byte[] b, int offset, String expected) {
        if (b.length < offset + expected.length()) {
            return false;
        }
        for (int i = 0; i < expected.length(); i++) {
            if (b[offset + i] != expected.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}

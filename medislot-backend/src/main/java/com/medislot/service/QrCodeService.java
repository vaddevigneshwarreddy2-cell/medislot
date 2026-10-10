package com.medislot.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.medislot.config.AppProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Opaque check-in codes: "MS1.&lt;appointmentId&gt;.&lt;signature&gt;". The signature is an HMAC of the appointment id and
 * slot start under a key derived from the JWT secret, so the code carries no personal or medical data
 * and cannot be forged or edited. Nothing is stored.
 */
@Service
public class QrCodeService {

    public record Parsed(long appointmentId, String signature) {}

    private static final Pattern FORMAT = Pattern.compile("^MS1\\.(\\d{1,18})\\.([A-Za-z0-9_-]{22})$");

    private final byte[] key;

    public QrCodeService(AppProperties props) {
        this.key = hmac(props.jwt().secret().getBytes(StandardCharsets.UTF_8),
                "medislot-qr-v1".getBytes(StandardCharsets.UTF_8));
    }

    public String code(long appointmentId, Instant slotStart) {
        return "MS1." + appointmentId + "." + signature(appointmentId, slotStart);
    }

    public Optional<Parsed> parse(String code) {
        if (code == null) return Optional.empty();
        Matcher m = FORMAT.matcher(code.trim());
        if (!m.matches()) return Optional.empty();
        return Optional.of(new Parsed(Long.parseLong(m.group(1)), m.group(2)));
    }

    public boolean valid(Parsed parsed, Instant slotStart) {
        byte[] expected = signature(parsed.appointmentId(), slotStart).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, parsed.signature().getBytes(StandardCharsets.UTF_8));
    }

    public String pngDataUrl(String content) {
        try {
            BitMatrix m = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 300, 300,
                    Map.of(EncodeHintType.MARGIN, 2));
            BufferedImage img = new BufferedImage(m.getWidth(), m.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < m.getWidth(); x++) {
                for (int y = 0; y < m.getHeight(); y++) {
                    img.setRGB(x, y, m.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Could not render QR code", e);
        }
    }

    private String signature(long appointmentId, Instant slotStart) {
        byte[] mac = hmac(key, ("appt:" + appointmentId + ":" + slotStart.getEpochSecond())
                .getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(mac, 16)); // 22 chars
    }

    private static byte[] hmac(byte[] k, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(k, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
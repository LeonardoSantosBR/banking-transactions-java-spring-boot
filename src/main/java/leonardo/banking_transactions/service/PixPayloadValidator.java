package leonardo.banking_transactions.service;

import leonardo.banking_transactions.enums.QrCodeTypeEnum;
import leonardo.banking_transactions.exception.validations.InvalidTransactionException;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class PixPayloadValidator {
    private static final Pattern TXID = Pattern.compile("[A-Za-z0-9.\\-]{1,100}");

    public void validate(QrCodeTypeEnum type, String payload, String txid) {
        if (type == null && payload == null && txid == null) return;
        if (type == null || payload == null || payload.isBlank())
            throw new InvalidTransactionException("QR Code type and payload must be provided together");
        if (!isEmvPayload(payload))
            throw new InvalidTransactionException("Invalid Pix QR Code payload");
        if (txid != null && !TXID.matcher(txid).matches())
            throw new InvalidTransactionException("Invalid Pix txid");
        if (type == QrCodeTypeEnum.DYNAMIC && (txid == null || txid.isBlank()))
            throw new InvalidTransactionException("Dynamic QR Code requires txid");
    }

    private boolean isEmvPayload(String payload) {
        return payload.length() <= 10000 && payload.startsWith("000201")
                && payload.matches("[\\x20-\\x7E]+") && payload.length() >= 8;
    }
}

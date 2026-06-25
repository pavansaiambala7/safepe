package com.safepe.layer3.service;

import com.safepe.layer3.entity.EncryptedCard;
import com.safepe.layer3.entity.EncryptedUPI;
import com.safepe.layer3.entity.EncryptedAccount;
import com.safepe.layer3.repository.CardVaultRepository;
import com.safepe.layer3.repository.UPIVaultRepository;
import com.safepe.layer3.repository.AccountVaultRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Layer 3 — Vault Service
 * ========================
 * The ONLY service in the entire application that can encrypt and decrypt
 * sensitive payment credentials (cards, UPI IDs, bank accounts).
 *
 * Uses AES-256-GCM (Galois/Counter Mode) for authenticated encryption:
 *   - AES-256: 256-bit key, virtually impossible to brute-force
 *   - GCM: Provides both confidentiality AND integrity verification
 *   - If even 1 bit of encrypted data is tampered, decryption fails
 *
 * No controller exists in Layer 3 — this service is called internally
 * by Layer 2 services only. The frontend NEVER talks to Layer 3 directly.
 */
@Service
@Slf4j
public class VaultService {

    // ── Constants ──────────────────────────────────────────────────────
    // AES-GCM requires a 12-byte IV (Initialization Vector)
    private static final int IV_LENGTH = 12;

    // GCM authentication tag is 128 bits (16 bytes)
    private static final int TAG_LENGTH = 128;

    // Algorithm identifier
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    // ── The Master Key (injected from application.yml → .env) ──────
    private final SecretKeySpec secretKey;

    // ── Repositories (database access) ─────────────────────────────
    private final CardVaultRepository cardVaultRepository;
    private final UPIVaultRepository upiVaultRepository;
    private final AccountVaultRepository accountVaultRepository;

    /**
     * Constructor — Spring Boot automatically injects the master key
     * and all three repositories when the application starts.
     *
     * @param masterKey The VAULT_MASTER_KEY from .env file
     */
    public VaultService(
            @Value("${safepe.encryption.master-key}") String masterKey,
            CardVaultRepository cardVaultRepository,
            UPIVaultRepository upiVaultRepository,
            AccountVaultRepository accountVaultRepository
    ) {
        // Convert the string master key into a 256-bit AES key
        // We use SHA-256 to ensure the key is exactly 32 bytes (256 bits)
         // even if the user provides a key of different length
        byte[] keyBytes = sha256Bytes(masterKey);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");

        this.cardVaultRepository = cardVaultRepository;
        this.upiVaultRepository = upiVaultRepository;
        this.accountVaultRepository = accountVaultRepository;

        log.info("✅ VaultService initialized — AES-256-GCM encryption ready");
    }

    // ════════════════════════════════════════════════════════════════
    //  CORE ENCRYPTION / DECRYPTION METHODS
    // ════════════════════════════════════════════════════════════════

    /**
     * ENCRYPT — Converts plain text into encrypted bytes.
     *
     * How it works step by step:
     * 1. Generate a random 12-byte IV (so same input produces different output each time)
     * 2. Initialize AES-256-GCM cipher with our master key + IV
     * 3. Encrypt the plain text into ciphertext bytes
     * 4. Combine: [12-byte IV] + [encrypted bytes + 16-byte auth tag]
     * 5. Return the combined byte array
     *
     * @param plainText The sensitive data to encrypt (e.g., "4111-2222-3333-4444")
     * @return Encrypted byte array containing IV + ciphertext + auth tag
     */
    public byte[] encrypt(String plainText) {
        try {
            // Step 1: Generate a random IV
            // SecureRandom is cryptographically strong (not predictable like Math.random)
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);

            // Step 2: Set up the AES-GCM cipher for encryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // Step 3: Encrypt the plain text
            byte[] encryptedBytes = cipher.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8)
            );

            // Step 4: Combine IV + encrypted data into one array
            // We prepend the IV so we can extract it during decryption
            byte[] combined = new byte[IV_LENGTH + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encryptedBytes, 0, combined, IV_LENGTH, encryptedBytes.length);

            return combined;

        } catch (Exception e) {
            log.error("❌ Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * DECRYPT — Converts encrypted bytes back to plain text.
     *
     * How it works step by step:
     * 1. Extract the first 12 bytes as the IV
     * 2. Extract the remaining bytes as the ciphertext + auth tag
     * 3. Initialize AES-256-GCM cipher in DECRYPT mode with our master key + IV
     * 4. Decrypt and verify the authentication tag
     * 5. If the tag doesn't match → data was tampered → throws exception!
     *
     * @param encryptedData The encrypted byte array (IV + ciphertext + tag)
     * @return The original plain text
     */
    public String decrypt(byte[] encryptedData) {
        try {
            // Step 1: Extract the IV (first 12 bytes)
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, IV_LENGTH);

            // Step 2: Extract the ciphertext (everything after the IV)
            byte[] cipherText = new byte[encryptedData.length - IV_LENGTH];
            System.arraycopy(encryptedData, IV_LENGTH, cipherText, 0, cipherText.length);

            // Step 3: Set up cipher for decryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // Step 4: Decrypt (also verifies the auth tag automatically)
            // If data was tampered, this line throws AEADBadTagException
            byte[] decryptedBytes = cipher.doFinal(cipherText);

            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("❌ Decryption failed — data may have been tampered: {}", e.getMessage());
            throw new RuntimeException("Decryption failed — possible data tampering", e);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  HASHING METHODS (SHA-256)
    // ════════════════════════════════════════════════════════════════

    /**
     * Generate a SHA-256 hash string from input.
     * Used for creating card_hash, upi_hash, account_hash for lookups.
     *
     * @param input The value to hash (e.g., a card number)
     * @return 64-character hex string
     */
    public String sha256Hash(String input) {
        byte[] hashBytes = sha256Bytes(input);
        // Convert byte array to hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Internal helper — returns raw SHA-256 bytes.
     * Used both for hashing inputs AND for converting the master key to 32 bytes.
     */
    private byte[] sha256Bytes(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CARD VAULT OPERATIONS
    // ════════════════════════════════════════════════════════════════

    /**
     * Save a credit/debit card to the encrypted vault.
     *
     * @param userId      Clerk user ID
     * @param cardNumber  Full card number (e.g., "4111222233334444")
     * @param cardData    Full card JSON (number + CVV + expiry)
     * @param cardNetwork VISA, MASTERCARD, or RUPAY
     * @return The saved EncryptedCard entity
     */
    public EncryptedCard saveCard(String userId, String cardNumber,
                                  String cardData, String cardNetwork) {
        // Check if card already exists using hash lookup
        String hash = sha256Hash(cardNumber);
        Optional<EncryptedCard> existing = cardVaultRepository.findByCardHash(hash);
        if (existing.isPresent()) {
            log.warn("⚠️ Card already exists for hash: {}...", hash.substring(0, 8));
            return existing.get();
        }

        // Encrypt the full card data
        byte[] encrypted = encrypt(cardData);

        // Extract last 4 digits for safe display
        String lastFour = cardNumber.substring(cardNumber.length() - 4);

        // Build and save the entity
        EncryptedCard card = EncryptedCard.builder()
                .userId(userId)
                .cardHash(hash)
                .encryptedData(encrypted)
                .cardLastFour(lastFour)
                .cardNetwork(cardNetwork)
                .build();

        EncryptedCard saved = cardVaultRepository.save(card);
        log.info("✅ Card saved to vault — last four: {}, network: {}", lastFour, cardNetwork);
        return saved;
    }

    /**
     * Retrieve and decrypt a card from the vault.
     *
     * @param cardNumber The full card number to look up
     * @return Decrypted card data JSON, or null if not found
     */
    public String getCardData(String cardNumber) {
        String hash = sha256Hash(cardNumber);
        Optional<EncryptedCard> card = cardVaultRepository.findByCardHash(hash);

        if (card.isPresent()) {
            return decrypt(card.get().getEncryptedData());
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════════
    //  UPI VAULT OPERATIONS
    // ════════════════════════════════════════════════════════════════

    /**
     * Save a UPI ID to the encrypted vault.
     */
    public EncryptedUPI saveUPI(String userId, String upiId, String upiData) {
        String hash = sha256Hash(upiId);
        Optional<EncryptedUPI> existing = upiVaultRepository.findByUpiHash(hash);
        if (existing.isPresent()) {
            return existing.get();
        }

        byte[] encrypted = encrypt(upiData);

        // Mask UPI for display: "pavan@ybl" → "pa***@ybl"
        String masked = maskUPI(upiId);

        EncryptedUPI upi = EncryptedUPI.builder()
                .userId(userId)
                .upiHash(hash)
                .encryptedUpi(encrypted)
                .maskedUpi(masked)
                .build();

        EncryptedUPI saved = upiVaultRepository.save(upi);
        log.info("✅ UPI saved to vault — masked: {}", masked);
        return saved;
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPER METHODS
    // ════════════════════════════════════════════════════════════════

    /**
     * Mask a UPI ID for safe display.
     * "pavansai@ybl" → "pa***@ybl"
     */
    private String maskUPI(String upiId) {
        int atIndex = upiId.indexOf('@');
        if (atIndex <= 2) {
            return "***" + upiId.substring(atIndex);
        }
        return upiId.substring(0, 2) + "***" + upiId.substring(atIndex);
    }
}

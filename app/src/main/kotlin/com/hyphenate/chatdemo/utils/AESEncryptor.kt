package com.hyphenate.chatdemo.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


object AESEncryptor {

    // GCM建议的IV长度是12字节（96位）
    private const val IV_LENGTH: Int = 12
    // 认证标签长度，通常为128位
    private const val AUTH_TAG_LENGTH: Int = 128

    // 加密方法（与iOS的encryptWithAES功能一致）
    fun encrypt(data: String,secretKey: SecretKeySpec): String {
        return try {

            // 2. 初始化加密器（使用相同的AES/GCM/NoPadding模式）
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            // 3. 使用12字节IV（与iOS默认行为一致）
            // 注意：iOS的sealedBox会自动生成IV，我们需要在Android端模拟相同行为
            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }

            // 4. 使用128位认证标签（与iOS一致）
            val parameterSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

            // 5. 加密数据
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            // 6. 组合IV + 加密数据 + 认证标签（与iOS的sealedBox.combined结构一致）
            // 结构：IV(12) + 加密数据 + 认证标签(16)
            val combined = iv + encryptedBytes

            // 7. Base64编码返回（与iOS一致）
            Base64.encodeToString(combined,Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    //解密
    fun decrypt(encryptedText: String?, secretKey: SecretKeySpec): String {
        return try {
            val combined: ByteArray = Base64.decode(encryptedText,Base64.DEFAULT)
            // 提取IV
            val iv = ByteArray(IV_LENGTH)
            val cipherText = ByteArray(combined.size - IV_LENGTH)

            System.arraycopy(combined, 0, iv, 0, IV_LENGTH)
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(AUTH_TAG_LENGTH, iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedData = cipher.doFinal(cipherText)
            String(decryptedData)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }

    }
}
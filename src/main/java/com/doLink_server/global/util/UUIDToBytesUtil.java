package com.doLink_server.global.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

/**
 * UUID ↔ BINARY(16) 매핑용 컨버터
 */
@Slf4j
public class UUIDToBytesUtil {

    /**
     * UUID를 BINARY(16) 형식으로 변환 (DB 저장용)
     *
     * @param uuid Java UUID 객체
     * @return 16바이트 배열 (BINARY)
     */
    public static byte[] convertToDatabaseColumn(UUID uuid) {
        if (uuid == null) return null;
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        log.info(">>> [convertToDatabaseColumn] UUID = {}" , uuid);
        return bb.array();
    }

    /**
     * String > BINARY(16) 형식으로 변환 (DB 저장용)
     *
     * @param uuid Java UUID 객체
     * @return 16바이트 배열 (BINARY)
     */
    public static byte[] convertToDatabaseColumn(String uuid) {
        return convertToDatabaseColumn(UUID.fromString(uuid));
    }

    /**
     * BINARY(16) 값을 UUID로 변환
     *
     * @param bytes 16바이트 배열
     * @return Java UUID 객체
     */
    public static UUID convertToEntityAttribute(byte[] bytes) {
        if (bytes == null) return null;
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        log.info(">>> [convertToEntityAttribute] byte[] = {}", Arrays.toString(bytes));
        return new UUID(high, low);
    }
}
package cn.cordys.common.uid;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IDGeneratorTest {

    @Test
    void generatesCompactUuidV7WithFreshRandomTailsConcurrently() throws InterruptedException {
        long before = System.currentTimeMillis();
        List<String> ids = IntStream.range(0, 1000).parallel()
                .mapToObj(index -> IDGenerator.nextStr())
                .toList();
        long after = System.currentTimeMillis();

        HashSet<String> randomTails = new HashSet<>();
        for (String id : ids) {
            assertTrue(id.matches("[0-9a-f]{12}7[0-9a-f]{3}[89ab][0-9a-f]{15}"));
            long timestamp = Long.parseLong(id.substring(0, 12), 16);
            assertTrue(timestamp >= before && timestamp <= after);
            assertTrue(randomTails.add(id.substring(13)), "随机部分不应依靠时间戳变化来避免重复");
        }

        // 只检查跨毫秒排序，不要求同一毫秒内的随机尾部递增。
        Thread.sleep(2);
        String later = IDGenerator.nextStr();
        assertTrue(Long.parseLong(later.substring(0, 12), 16) > after);
        assertTrue(ids.stream().allMatch(id -> id.compareTo(later) < 0));
    }

    @Test
    void generatesFullNumericUuidV7WithoutSpring() throws InterruptedException {
        long before = System.currentTimeMillis();
        List<BigInteger> ids = IntStream.range(0, 1000).parallel()
                .mapToObj(index -> IDGenerator.nextNum())
                .toList();
        long after = System.currentTimeMillis();

        assertEquals(ids.size(), new HashSet<>(ids).size());
        for (BigInteger id : ids) {
            assertTrue(id.signum() > 0);
            assertTrue(id.bitLength() > 63 && id.bitLength() <= 128, "数字 ID 不应截断为 Long");
            assertEquals(7, id.shiftRight(76).and(BigInteger.valueOf(15)).intValue());
            assertEquals(2, id.shiftRight(62).and(BigInteger.valueOf(3)).intValue());
            long timestamp = id.shiftRight(80).longValueExact();
            assertTrue(timestamp >= before && timestamp <= after);
        }

        Thread.sleep(2);
        BigInteger later = IDGenerator.nextNum();
        assertTrue(later.shiftRight(80).longValueExact() > after);
        assertTrue(ids.stream().allMatch(id -> id.compareTo(later) < 0));
    }
}

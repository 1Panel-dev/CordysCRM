package cn.cordys.common.uid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SerialNumGeneratorTest {
    private StringRedisTemplate redis;
    private SerialNumGenerator generator;
    private final Map<String, Long> counters = new HashMap<>();
    private Date now;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(anyString()))
                .thenAnswer(invocation -> counters.merge(invocation.getArgument(0), 1L, Long::sum));
        generator = new SerialNumGenerator(redis);
        now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2026-10-12 14:30:20");
    }

    @ParameterizedTest
    @ValueSource(strings = { "yyyyMMdd", "yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd", "yyyy年MM月dd日" })
    void dailyFormatsUseTheSameCanonicalCounterDate(String pattern) {
        String serial = generator.generateByRules(rules(pattern), "org", "form", now);
        assertEquals("Opp-" + new SimpleDateFormat(pattern).format(now) + "-000001", serial);
        assertEquals(Map.of("serial:org:form:20261012:Opp", 1L), counters);
    }

    @Test
    void formatChangesContinueTheCounterAndTheNextDayUsesANewCounter() throws Exception {
        assertEquals("Opp-20261012-000001", generator.generateByRules(rules("yyyyMMdd"), "org", "form", now));
        assertEquals("Opp-2026-10-12-000002", generator.generateByRules(rules("yyyy-MM-dd"), "org", "form", now));
        assertEquals("Opp-2026/10/12-000003", generator.generateByRules(rules("yyyy/MM/dd"), "org", "form", now));
        assertEquals("Opp-20261012-000001", generator.generateByRules(rules("yyyyMMdd"), "org", "other-form", now));
        Date tomorrow = new SimpleDateFormat("yyyyMMdd").parse("20261013");
        assertEquals("Opp-20261013-000001", generator.generateByRules(rules("yyyyMMdd"), "org", "form", tomorrow));
    }

    @Test
    void monthlyCountersRemainCompatibleAndYearlyCountersUseTheFullYear() {
        counters.put("serial:org:form:202610:Opp", 12L);
        assertEquals("Opp-202610-000013", generator.generateByRules(rules("yyyyMM"), "org", "form", now));
        assertEquals("Opp-2026/10-000014", generator.generateByRules(rules("yyyy/MM"), "org", "form", now));
        assertEquals("Opp-2026-000001", generator.generateByRules(rules("yyyy"), "org", "form", now));
        assertEquals("yyyyMM", SerialNumGenerator.counterDatePattern("yyyy-MM-'dd'"));
        assertEquals("yyyy", SerialNumGenerator.counterDatePattern("yyyy 'MM dd'"));
        assertEquals("yyyyMMdd", SerialNumGenerator.counterDatePattern("yyyy''MMdd"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "yyyy-Q", "MM-dd", "yyMMdd", "yyyy-dd", "yyyy-M-d", "YYYYMMdd",
            "yyyy-MM-dd'", "'yyyyMMdd'", "yyyy-DDD", "yyyyMMdd\nYYYY",
            "yyyyMMddH", "yyyyMMddh", "yyyyMMddk", "yyyyMMddK", "yyyyMMddm", "yyyyMMdds", "yyyyMMddS",
            "yyyyMMdda", "yyyyMMddz", "yyyyMMddZ", "yyyyMMddX", "yyyyMMddG", "yyyyMMddE",
            "yyyyMMddHHmmss", "yyyy-MM-dd HH:mm:ss", "yyyyMMdd'T'HHmmss", "yyyy-MM-dd'T'HH:mm:ssXXX" })
    void invalidPatternsAreRejectedBeforeIncrementingRedis(String pattern) {
        assertThrows(IllegalArgumentException.class,
                () -> generator.generateByRules(rules(pattern), "org", "form", now));
        verify(redis, never()).opsForValue();
    }

    @Test
    void cleaningDeletesExpiredAndMalformedKeysWithoutResettingValidCounters() throws Exception {
        List<String> removed = List.of("serial:org:form:2025:Opp", "serial:org:form:202609:Opp",
                "serial:org:form:20261011:Opp", "serial:org:form:20261011:",
                "serial:org:form:20260230:Opp", "serial:org:form:202613:Opp", "serial:org:form:0000:Opp",
                "serial:org:form:2026-10-11:Opp", "serial:org:form:20261011", "serial::form:202610:Opp",
                "serial:org::202610:Opp", "serial:org:form:invalid:Opp", "serial:org");
        List<String> retained = List.of("serial:org:form:2026:Opp", "serial:org:form:202610:Opp",
                "serial:org:form:20261012:Opp", "serial:org:form:2027:Opp", "serial:org:form:202611:Opp",
                "serial:org:form:20261013:Opp", "serial:org:form:20261012:Opp:extra",
                "serial:org:form:202610:", "serial:org:form:202610:Other");
        List<String> keys = new ArrayList<>(removed);
        keys.addAll(retained);
        Cursor<String> cursor = scanKeys(keys);

        generator.clean(now);

        ArgumentCaptor<String> deleted = ArgumentCaptor.forClass(String.class);
        verify(redis, times(removed.size())).delete(deleted.capture());
        assertEquals(removed, deleted.getAllValues());
        verify(cursor).close();
        ArgumentCaptor<ScanOptions> options = ArgumentCaptor.forClass(ScanOptions.class);
        verify(redis).scan(options.capture());
        assertEquals("serial:*", options.getValue().getPattern());
    }

    @Test
    void cleaningRemovesOrphanedFormsButKeepsAllPrefixesOfAnExistingForm() {
        List<String> orphaned = List.of("serial:org:customformdata:202610:Opp", "serial:org:removed:202610:TCC",
                "serial:other-org:form:202610:TCC");
        List<String> keys = new ArrayList<>(orphaned);
        keys.addAll(List.of("serial:org:form:202610:TCC", "serial:org:form:202610:", "serial:org:form:2026:Other"));
        scanKeys(keys);

        generator.clean(now, (orgId, formKey) -> "org".equals(orgId) && "form".equals(formKey));

        ArgumentCaptor<String> deleted = ArgumentCaptor.forClass(String.class);
        verify(redis, times(orphaned.size())).delete(deleted.capture());
        assertEquals(orphaned, deleted.getAllValues());
    }

    @Test
    void failedFormLookupDoesNotDeleteValidCounters() throws Exception {
        Cursor<String> cursor = scanKeys(List.of("serial:org:form:202610:TCC", "serial:org:form:202611:TCC"));

        generator.clean(now, (orgId, formKey) -> {
            throw new IllegalStateException("表单查询不可用");
        });

        verify(redis, never()).delete(anyString());
        verify(cursor).close();
    }

    @Test
    void ruleChangesRemoveAllPreviousPrefixesAndPeriodsWithinTheForm() {
        List<String> removed = List.of("serial:org:form:202610:Opp", "serial:org:form:20261012:TCC",
                "serial:org:form:2026:X", "serial:org:form:broken:X", "serial:org:form:20261012");
        List<String> retained = List.of("serial:org:form:20261012:X", "serial:org:form:20261013:X",
                "serial:other-org:form:20261012:TCC", "serial:org:form-other:20261012:TCC");
        List<String> keys = new ArrayList<>(removed);
        keys.addAll(retained);
        scanKeys(keys);
        counters.put(retained.getFirst(), 12L);
        List<String> currentRules = List.of("X", "-", "yyyy-MM-dd", "-", "6");

        generator.cleanUnusedRules("org", "form", currentRules, false);

        ArgumentCaptor<String> deleted = ArgumentCaptor.forClass(String.class);
        verify(redis, times(removed.size())).delete(deleted.capture());
        assertEquals(removed, deleted.getAllValues());
        assertEquals("X-2026-10-12-000013", generator.generateByRules(currentRules, "org", "form", now));
        ArgumentCaptor<ScanOptions> options = ArgumentCaptor.forClass(ScanOptions.class);
        verify(redis).scan(options.capture());
        assertEquals("serial:org:form:*", options.getValue().getPattern());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "X:branch", "X*?[]" })
    void ruleCleanupMatchesTheCompleteLiteralPrefix(String prefix) {
        String retained = "serial:org:form:20261012:" + prefix;
        String removed = retained + ":other";
        scanKeys(List.of(retained, removed));

        generator.cleanUnusedRules("org", "form", List.of(prefix, "-", "yyyyMMdd", "-", "6"), false);

        verify(redis).delete(removed);
        verify(redis, never()).delete(retained);
    }

    @Test
    void formulaPrefixCleanupRetainsDynamicPrefixesOfTheCurrentPeriodType() {
        scanKeys(List.of("serial:org:form:20261012:A", "serial:org:form:20261012:B:part",
                "serial:org:form:20261012:", "serial:org:form:202610:A"));

        generator.cleanUnusedRules("org", "form", rules("yyyyMMdd"), true);

        verify(redis).delete("serial:org:form:202610:A");
        verify(redis, times(1)).delete(anyString());
    }

    @Test
    void removingTheSerialFieldClearsOnlyItsFormCounters() {
        String removed = "serial:org:form:20261012:A";
        scanKeys(List.of(removed, "serial:org:form-other:20261012:A"));

        generator.cleanUnusedRules("org", "form", null, false);

        verify(redis).delete(removed);
        verify(redis, times(1)).delete(anyString());
    }

    @Test
    void returningToPreviousDateOrPrefixContinuesItsCounterAfterCleaning() {
        List<String> monthly = List.of("TCC", "-", "yyyyMM", "-", "6");
        List<String> daily = List.of("TCC", "-", "yyyyMMdd", "-", "6");
        assertEquals("TCC-202610-000001", generator.generateByRules(monthly, "org", "form", now));
        assertEquals("TCC-20261012-000001", generator.generateByRules(daily, "org", "form", now));
        assertEquals("Opp-202610-000001", generator.generateByRules(rules("yyyyMM"), "org", "form", now));
        scanKeys(List.copyOf(counters.keySet()));

        generator.clean(now, (orgId, formKey) -> true);

        verify(redis, never()).delete(anyString());
        assertEquals("TCC-202610-000002", generator.generateByRules(monthly, "org", "form", now));
        assertEquals("TCC-20261012-000002", generator.generateByRules(daily, "org", "form", now));
        assertEquals("Opp-202610-000002", generator.generateByRules(rules("yyyyMM"), "org", "form", now));
    }

    @SuppressWarnings("unchecked")
    private Cursor<String> scanKeys(List<String> keys) {
        Cursor<String> cursor = mock(Cursor.class);
        when(redis.scan(any(ScanOptions.class))).thenReturn(cursor);
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(0);
            keys.forEach(consumer);
            return null;
        }).when(cursor).forEachRemaining(any());
        return cursor;
    }

    private static List<String> rules(String pattern) {
        return List.of("Opp", "-", pattern, "-", "6");
    }
}

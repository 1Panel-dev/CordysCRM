package cn.cordys.common.uid.impl;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.BitsAllocator;
import cn.cordys.common.uid.worker.WorkerIdAssigner;
import cn.cordys.common.util.TimeUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class DefaultUidGenerator implements DisposableBean {

    /**
     * ===== Bits allocation =====
     * sign(1) + time + worker + sequence = 64
     */
    protected int timeBits = 30; // 秒级时间，≈34 年
    protected int workerBits = 21; // 200w+ worker
    protected int seqBits = 12; // 每秒 4096

    /**
     * ===== Fixed epoch (DO NOT CHANGE AFTER RELEASE) =====
     * 2025-01-01 00:00:00
     */
    protected String epochStr = "2025-01-01";
    protected long epochSeconds;

    /**
     * ===== Stable fields =====
     */
    protected BitsAllocator bitsAllocator;
    protected long workerId;

    /**
     * ===== Volatile fields =====
     */
    protected long sequence = 0L;
    protected long lastSecond = -1L;

    /**
     * ===== WorkerId =====
     */
    @Resource
    protected WorkerIdAssigner workerIdAssigner;

    /**
     * Spring lifecycle init
     */
    @PostConstruct
    public void init() {
        // init epoch
        setEpochStr(epochStr);

        // init bits allocator
        this.bitsAllocator = new BitsAllocator(timeBits, workerBits, seqBits);

        // init worker id
        this.workerId = workerIdAssigner.assignWorkerId();
        if (workerId < 0 || workerId > bitsAllocator.getMaxWorkerId()) {
            throw new IllegalStateException(
                    "WorkerId " + workerId + " exceeds max " + bitsAllocator.getMaxWorkerId()
            );
        }

        log.info(
                "Initialized UID Generator: epoch={}, bits=(time:{}, worker:{}, seq:{}), workerId={}",
                epochStr, timeBits, workerBits, seqBits, workerId
        );
    }

    /**
     * Get UID
     */
    public long getUID() {
        Assert.notNull(bitsAllocator, "UidGenerator not initialized");
        return nextId();
    }

    /**
     * Generate next UID
     */
    protected synchronized long nextId() {
        long currentSecond = getCurrentSecond();

        // Clock moved backwards
        if (currentSecond < lastSecond) {
            long refusedSeconds = lastSecond - currentSecond;
            throw new GenericException(
                    String.format("Clock moved backwards. Refusing for %d seconds", refusedSeconds)
            );
        }

        if (currentSecond == lastSecond) {
            sequence = (sequence + 1) & bitsAllocator.getMaxSequence();
            if (sequence == 0) {
                currentSecond = waitNextSecond(lastSecond);
            }
        } else {
            sequence = 0L;
        }

        lastSecond = currentSecond;

        return bitsAllocator.allocate(
                currentSecond - epochSeconds,
                workerId,
                sequence
        );
    }

    /**
     * Wait until next second
     */
    private long waitNextSecond(long lastSecond) {
        long current = getCurrentSecond();
        while (current <= lastSecond) {
            current = getCurrentSecond();
        }
        return current;
    }

    /**
     * Get current second
     */
    private long getCurrentSecond() {
        long currentSecond = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        if (currentSecond - epochSeconds > bitsAllocator.getMaxDeltaSeconds()) {
            throw new GenericException(
                    "Timestamp bits exhausted. Refusing UID generation. Now=" + currentSecond
            );
        }
        return currentSecond;
    }

    public void setEpochStr(String epochStr) {
        if (StringUtils.isNotBlank(epochStr)) {
            this.epochStr = epochStr;
            this.epochSeconds = TimeUnit.MILLISECONDS.toSeconds(
                    TimeUtils.parseByDayPattern(epochStr).getTime()
            );
        }
    }

    @Override
    public void destroy() {
        log.info("Shutdown UID Generator...");
    }
}

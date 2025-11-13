-- set innodb lock wait timeout
SET SESSION innodb_lock_wait_timeout = 7200;

-- add opportunity clue relation key (include index)
ALTER TABLE opportunity ADD COLUMN clue_id VARCHAR(32) COMMENT '线索ID' AFTER `follow_time`;
CREATE INDEX idx_clue_id ON opportunity(clue_id ASC);

-- set innodb lock wait timeout to default
SET SESSION innodb_lock_wait_timeout = DEFAULT;




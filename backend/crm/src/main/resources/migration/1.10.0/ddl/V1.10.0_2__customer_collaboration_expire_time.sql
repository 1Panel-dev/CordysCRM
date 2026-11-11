ALTER TABLE customer_collaboration
    ADD COLUMN expire_time BIGINT NULL COMMENT '协作权限有效期截止时间，空表示长期有效';

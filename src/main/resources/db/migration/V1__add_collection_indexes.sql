-- 전체 모음 조회용 인덱스
CREATE INDEX idx_collection_user_created_at
    ON collection (user_id, created_at);

-- 카테고리별 모음 조회용 인덱스
CREATE INDEX idx_collection_user_category_created_at
    ON collection (user_id, category, created_at);

ALTER TABLE dolink.task
    ADD COLUMN thumbnail_key VARCHAR(500) NULL;

-- B-2 조회 성능을 위해 인덱스 추천
CREATE INDEX idx_task_collection_created_thumb
    ON dolink.task (collection_id, created_at, thumbnail_key);

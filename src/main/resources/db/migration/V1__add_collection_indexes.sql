-- 전체 모음 조회용 인덱스
CREATE INDEX idx_collection_user_created_at
    ON collection (user_id, created_at);

-- 카테고리별 모음 조회용 인덱스
CREATE INDEX idx_collection_user_category_created_at
    ON collection (user_id, category, created_at);

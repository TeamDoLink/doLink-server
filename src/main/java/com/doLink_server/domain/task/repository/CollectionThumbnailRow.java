package com.doLink_server.domain.task.repository;

/**
 * 모음 썸네일 조회용 Projection
 * - collectionId + thumbnailKey만 가져온다.
 */
public interface CollectionThumbnailRow {
    Long getCollectionId();
    String getThumbnailKey();
}

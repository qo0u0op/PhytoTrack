package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.d0w0b.phytotrack.models.CaseSearchView;

/**
 * 案件篩選視圖資料存取層（唯讀）
 *
 * 供 CaseService.list/exportCsv 以 Specification 對視圖分頁，
 * 再以 case_id 回補 Case 實體，保留 VIEWER 遮蔽等業務邏輯。
 */
public interface CaseSearchViewRepository
    extends JpaRepository<CaseSearchView, Long>, JpaSpecificationExecutor<CaseSearchView> {
}

package de.gerdiproject.store.datamodel;

import lombok.Data;

import java.util.List;

public @Data class StoreTask {

    private String bookmarkId;
    private String bookmarkName;
    private List<String> docs;
    private String userId;

}

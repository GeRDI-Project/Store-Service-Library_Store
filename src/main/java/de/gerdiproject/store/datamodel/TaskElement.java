package de.gerdiproject.store.datamodel;

import lombok.Data;

public @Data class TaskElement {

    private final String fileName;
    private Integer progressInPercent = new Integer(0);

}

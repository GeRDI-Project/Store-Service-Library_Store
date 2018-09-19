package de.gerdiproject.store.datamodel;

import lombok.Data;

public @Data(staticConstructor = "of") class ListElement {

    private final String displayName;
    private final String type;
    private final String uri;

}
